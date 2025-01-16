(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [clojure
             [set :as set]]
            [honey.sql :as sql]
            [honey.sql.helpers :as sql.helpers]
            [metabase.db.spec :as db.spec]
            [metabase.config :as config]
            [metabase.driver :as driver]
            [metabase.util :as u]
            [metabase.driver.sql-jdbc.execute :as sql-jdbc.execute]
            [metabase.driver.sql.query-processor :as sql.qp]
            [metabase.util.honey-sql-2 :as h2x]
            [metabase.driver.sync :as driver.s]
            [metabase.driver.sql-jdbc
             [common :as sql-jdbc.common]
             [connection :as sql-jdbc.conn]
             [sync :as sql-jdbc.sync]]))

(driver/register! :materialize, :parent :postgres)

(defmethod sql.qp/add-interval-honeysql-form :materialize
  [_driver hsql-form amount unit]
  ;; Convert weeks to days because Materialize doesn't support weeks and the rest should work as is
  (let [adjusted-amount (if (= unit :week) (* 7 amount) amount)
        adjusted-unit (if (= unit :week) :day unit)]
    (h2x// (sql.qp/add-interval-honeysql-form :postgres hsql-form adjusted-amount adjusted-unit))))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                          metabase.driver method impls                                          |
;;; +----------------------------------------------------------------------------------------------------------------+

(doseq [[feature supported?] {:foreign-keys              (not config/is-test?)
                              :metadata/key-constraints  (not config/is-test?)
                              :foreign-keys-as-required-by-tests false
                              ;; Materialize defaults to UTC, and this is the only supported value
                              :set-timezone              false
                              :datetime-diff             false
                              :convert-timezone          (not config/is-test?)
                              :temporal-extract          (not config/is-test?)
                              ;; Disabling during tests as the data load fails with:
                              ;; metabase.driver.sql-jdbc.sync.describe-table-test/describe-big-nested-field-columns-test (impl.clj:141)
                              ;; ERROR: column "big_json" is of type jsonb but expression is of type character varying
                              :nested-field-columns      (not config/is-test?)
                              ;; Disabling nested queries during tests as they try to use Foreign Keys
                              :nested-queries            (not config/is-test?)
                              ;; Disabling the expressions support due to the following error:
                              ;; Error executing query: ERROR: function substring(text, character varying) does not exist
                              :expressions               false
                              ;; Disabling model caching:
                              :persist-models            false
                              ;; Disable percentile aggregations due to missing support for PERCENTILE_CONT
                              :percentile-aggregations   false
                              ;; Disabling the support for the `:connection-impersonation` feature as it's not supported
                              :connection-impersonation  false
                              ;; Disable uploads
                              :uploads                   false
                              :test/jvm-timezone-setting false}]
  (defmethod driver/database-supports? [:materialize feature] [_driver _feature _db] supported?))

(defmethod sql-jdbc.execute/set-timezone-sql :materialize
  [_]
  "SET TIMEZONE TO %s;")

; ;;; +----------------------------------------------------------------------------------------------------------------+
; ;;; |                                         metabase.driver.sql-jdbc impls                                         |
; ;;; +----------------------------------------------------------------------------------------------------------------+

(def ^:private default-materialize-connection-details
  {:host "materialize", :port 6875, :db "materialize", :cluster "quickstart"})

(defn- validate-connection-details
  [{:keys [host]}]
  (when-not (re-matches #"^[a-zA-Z0-9.-]+$" host)
    (throw (IllegalArgumentException. (str "Invalid host: " host)))))

(defmethod sql-jdbc.conn/connection-details->spec :materialize
  [_ details]
  (let [merged-details (merge default-materialize-connection-details details)
        ;; TODO: get the driver version from the plugin manifest instead of hardcoding it
        driver-version "v1.4.0"
        app-name       (format "Metabase Materialize driver %s %s"
                             driver-version
                             config/mb-app-id-string)]
    (validate-connection-details merged-details)
    (let [{:keys [host port db cluster ssl], :as opts} merged-details]
      (sql-jdbc.common/handle-additional-options
       (merge
        {:classname                     "org.postgresql.Driver"
         :subprotocol                   "postgresql"
         :subname                       (str "//" host ":" port "/" db "?options=--cluster%3D" cluster)
         :sslmode                       (if ssl "require" "disable")
         :OpenSourceSubProtocolOverride false
         :ApplicationName               app-name}
        (dissoc opts :host :port :db :cluster :ssl))))))

(defmethod driver/describe-table :materialize
  [driver database table]
  (sql-jdbc.sync/describe-table driver database table))

(defmethod sql-jdbc.sync/excluded-schemas :materialize [_driver] #{"mz_catalog" "mz_internal" "pg_catalog"})

(defn ^:private get-tables-sql
  "Materialize doesn't support the pg_stat_user_tables table
   Overriding the default implementation to exclude the pg_stat_user_tables table"
  [schemas table-names]
  (sql/format
   (cond->  {:select    [[:n.nspname :schema]
                         [:c.relname :name]
                         [[:case-expr :c.relkind
                           [:inline "r"] [:inline "TABLE"]
                           [:inline "p"] [:inline "PARTITIONED TABLE"]
                           [:inline "v"] [:inline "VIEW"]
                           [:inline "f"] [:inline "FOREIGN TABLE"]
                           [:inline "m"] [:inline "MATERIALIZED VIEW"]
                           :else nil]
                          :type]
                         [:d.description :description]]
             :from      [[:pg_catalog.pg_class :c]]
             :join      [[:pg_catalog.pg_namespace :n]   [:= :c.relnamespace :n.oid]]
             :left-join [[:pg_catalog.pg_description :d] [:and [:= :c.oid :d.objoid] 
                                                               [:= :d.objsubid 0] 
                                                               [:= :d.classoid [:raw "'pg_class'::regclass"]]]]
             :where     [:and [:= :c.relnamespace :n.oid]
                         ;; filter out system tables (pg_ and mz_)
                         [:and
                          [(keyword "!~") :n.nspname "^pg_"]
                          [(keyword "!~") :n.nspname "^mz_"]
                          [:<> :n.nspname "information_schema"]]
                         ;; only get tables of type: TABLE, PARTITIONED TABLE, VIEW, FOREIGN TABLE, MATERIALIZED VIEW
                         [:raw "c.relkind in ('r', 'p', 'v', 'f', 'm')"]]
             :order-by  [:type :schema :name]}
     (seq schemas)
     (sql.helpers/where [:in :n.nspname schemas])

     (seq table-names)
     (sql.helpers/where [:in :c.relname table-names]))
   {:dialect :ansi}))

(defn- describe-database-tables
  [database]
  (let [[inclusion-patterns
         exclusion-patterns] (driver.s/db-details->schema-filter-patterns database)
        syncable? (fn [schema]
                    (driver.s/include-schema? inclusion-patterns exclusion-patterns schema))]
    (eduction
     (comp (filter (comp syncable? :schema))
           (map #(dissoc % :type)))
     (sql-jdbc.execute/reducible-query database (get-tables-sql nil nil)))))

(defmethod driver/describe-database :materialize
 [_driver database]
  ;; TODO: change this to return a reducible so we don't have to hold 100k tables in memory in a set like this
  {:tables (into #{} (describe-database-tables database))})

;; Overriding the default implementation to exclude the usage of the `format` function as it's not supported in Materialize
(defmethod sql-jdbc.sync/describe-fields-sql :materialize
  [driver & {:keys [schema-names table-names]}]
  (sql/format
   {:select [[:c.column_name :name]
             [:c.data_type :database-type]
             [[:- :c.ordinal_position [:inline 1]] :database-position]
             [:c.table_schema :table-schema]
             [:c.table_name :table-name]
             [[:not= :pk.column_name nil] :pk?]
             ;; Materialize doesn't support column comments
             [nil :field-comment]
             ;; Materialize doesn't enforce NOT NULL constraints
             [false :database-required]
             ;; Materialize doesn't support auto-increment
             [false :database-is-auto-increment]]
    :from [[:information_schema.columns :c]]
    :left-join [[{:select [:tc.table_schema
                           :tc.table_name
                           :kc.column_name]
                  :from [[:information_schema.table_constraints :tc]]
                  :join [[:information_schema.key_column_usage :kc]
                         [:and
                          [:= :tc.constraint_name :kc.constraint_name]
                          [:= :tc.table_schema :kc.table_schema]
                          [:= :tc.table_name :kc.table_name]]]
                  :where [:= :tc.constraint_type [:inline "PRIMARY KEY"]]}
                 :pk]
                [:and
                 [:= :c.table_schema :pk.table_schema]
                 [:= :c.table_name :pk.table_name]
                 [:= :c.column_name :pk.column_name]]]
    :where [:and
            [:raw "c.table_schema NOT IN ('mz_catalog', 'mz_internal', 'pg_catalog', 'information_schema')"]
            (when (seq schema-names)
              [:in :c.table_schema schema-names])
            (when (seq table-names)
              [:in :c.table_name table-names])]
    :order-by [[:c.table_schema :asc]
               [:c.table_name :asc]
               [:c.ordinal_position :asc]]}
   :dialect (sql.qp/quote-style driver)))
