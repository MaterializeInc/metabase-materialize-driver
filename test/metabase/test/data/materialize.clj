(ns metabase.test.data.materialize
  "Test extensions for the Materialize driver.  Includes logic for creating/destroying test datasets, building
  the connection specs from environment variables, etc."
  (:require
   [clojure.string :as str]
   [metabase.config :as config]
   [metabase.driver :as driver]
   [metabase.driver.ddl.interface :as ddl.i]
   [metabase.test.data.interface :as tx]
   [metabase.test.data.sql :as sql.tx]
   [metabase.test.data.sql-jdbc.execute :as execute]
   [metabase.test.data.sql-jdbc.load-data :as load-data]
   [metabase.test.data.sql.ddl :as ddl]
   [metabase.util.log :as log]))

(set! *warn-on-reflection* true)

(defmethod ddl/drop-db-ddl-statements :materialize
  [& args]
  (apply (get-method ddl/drop-db-ddl-statements :sql-jdbc/test-extensions) args))

(defmethod tx/aggregate-column-info :materialize
  ([driver ag-type]
   ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type))

  ([driver ag-type field]
   (cond-> ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type field)
    (= ag-type :sum) (assoc :base_type :type/BigInteger))))

(doseq [[base-type db-type] {:type/BigInteger     "BIGINT"
                             :type/Boolean        "BOOL"
                             :type/Date           "DATE"
                             :type/DateTime       "TIMESTAMP"
                             :type/Decimal        "DECIMAL"
                             :type/Float          "FLOAT"
                             :type/Integer        "INTEGER"
                             :type/Text           "TEXT"
                             :type/JSON           "JSON"
                             :type/Time           "TIME"
                             :type/UUID           "UUID"}]
  (defmethod sql.tx/field-base-type->sql-type [:materialize base-type] [_ _] db-type))

(defmethod tx/dbdef->connection-details :materialize
  [_ context {:keys [database-name]}]
  (merge
   {:host     (tx/db-test-env-var-or-throw :materialize :host "localhost")
    :ssl      (tx/db-test-env-var :materialize :ssl false)
    :port     (tx/db-test-env-var-or-throw :materialize :port 6877)
    :cluster  (tx/db-test-env-var :materialize :cluster "quickstart")
    :user     (tx/db-test-env-var-or-throw :materialize :user "mz_system")}
   (when-let [password (tx/db-test-env-var :materialize :password)]
     {:password password})
   (when (= context :db)
     {:db database-name})))

(defmethod sql.tx/drop-table-if-exists-sql :materialize
  [driver {:keys [database-name]} {:keys [table-name]}]
  (format "DROP TABLE IF EXISTS \"%s\".\"%s\".\"%s\""
          (ddl.i/format-name driver database-name)
          "public"
          (ddl.i/format-name driver table-name)))

(defmethod sql.tx/create-db-sql :materialize
  [driver {:keys [database-name]}]
  (format "CREATE DATABASE \"%s\";" (ddl.i/format-name driver database-name)))
(defmethod sql.tx/drop-db-if-exists-sql :materialize
  [driver {:keys [database-name]}]
  (format "DROP DATABASE IF EXISTS \"%s\";" (ddl.i/format-name driver database-name)))

(defmethod sql.tx/add-fk-sql :materialize [& _] nil)

(defmethod execute/execute-sql! :materialize [& args]
  (apply execute/sequentially-execute-sql! args))

(defmethod sql.tx/pk-sql-type :materialize [_] "INTEGER")

(defmethod sql.tx/create-table-sql :materialize
  [driver dbdef tabledef]
  (let [tabledef (update tabledef :field-definitions (fn [field-defs]
                                                       (for [field-def field-defs]
                                                         (dissoc field-def :not-null?))))
        ;; strip out the PRIMARY KEY stuff from the CREATE TABLE statement
        sql      ((get-method sql.tx/create-table-sql :sql/test-extensions) driver dbdef tabledef)]
    (str/replace sql #", PRIMARY KEY \([^)]+\)" "")))

(defmethod load-data/load-data! :materialize [& args]
  (apply load-data/load-data-maybe-add-ids-chunked! args))

(defmethod tx/sorts-nil-first? :materialize
  [_driver _base-type]
  false)

(defmethod tx/supports-time-type? :materialize
  [_driver]
  false)

(defmethod tx/supports-timestamptz-type? :materialize
  [_driver]
  false)
