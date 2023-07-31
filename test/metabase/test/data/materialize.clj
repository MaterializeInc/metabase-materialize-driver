(ns metabase.test.data.materialize
  "Test extensions for the Materialize driver.  Includes logic for creating/destroying test datasets, building
  the connection specs from environment variables, etc."
  (:require [metabase.test.data.interface :as tx]
            [metabase.test.data.sql :as sql.tx]
            [metabase.test.data.sql-jdbc :as sql-jdbc.tx]
            [metabase.test.data.sql-jdbc.load-data :as load-data]
            [metabase.test.data.sql.ddl :as ddl]))

(sql-jdbc.tx/add-test-extensions! :materialize)

(defmethod sql.tx/pk-sql-type :materialize [_] "SERIAL")

(defmethod tx/aggregate-column-info :materialize
  ([driver ag-type]
   ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type))

  ([driver ag-type field]
   (merge
    ((get-method tx/aggregate-column-info ::tx/test-extensions) driver ag-type field)
    (when (= ag-type :sum)
      {:base_type :type/BigInteger}))))

(doseq [[base-type db-type] {:type/BigInteger     "BIGINT"
                             :type/Boolean        "BOOL"
                             :type/Date           "DATE"
                             :type/DateTime       "TIMESTAMP"
                             :type/DateTimeWithTZ "TIMESTAMP WITH TIME ZONE"
                             :type/Decimal        "DECIMAL"
                             :type/Float          "FLOAT"
                             :type/Integer        "INTEGER"
                             :type/IPAddress      "INET"
                             :type/Text           "TEXT"
                             :type/Time           "TIME"
                             :type/TimeWithTZ     "TIME WITH TIME ZONE"
                             :type/UUID           "UUID"}]
  (defmethod sql.tx/field-base-type->sql-type [:materialize base-type] [_ _] db-type))

(defmethod tx/dbdef->connection-details :materialize
  [_ context {:keys [database-name]}]
  (merge
   {:host     (tx/db-test-env-var-or-throw :materialize :host "localhost")
    :port     (tx/db-test-env-var-or-throw :materialize :port 6875)
    :timezone :America/Los_Angeles}
   (when-let [user (tx/db-test-env-var :materialize :user)]
     {:user user})
   (when-let [password (tx/db-test-env-var :materialize :password)]
     {:password password})
   (when (= context :db)
     {:db database-name})))

(defmethod ddl/drop-db-ddl-statements :materialize
  [driver {:keys [database-name], :as dbdef} & options]
  (when-not (string? database-name)
    (throw (ex-info (format "Expected String database name; got ^%s %s"
                            (some-> database-name class .getCanonicalName) (pr-str database-name))
                    {:driver driver, :dbdef dbdef})))
  ;; add an additional statement to the front to kill open connections to the DB before dropping
  (cons
   (kill-connections-to-db-sql database-name)
   (apply (get-method ddl/drop-db-ddl-statements :sql-jdbc/test-extensions) :materialize dbdef options)))

(defmethod load-data/load-data! :materialize [& args]
  (apply load-data/load-data-all-at-once! args))

(defmethod sql.tx/standalone-column-comment-sql :materialize [& args]
  (apply sql.tx/standard-standalone-column-comment-sql args))

(defmethod sql.tx/standalone-table-comment-sql :materialize [& args]
  (apply sql.tx/standard-standalone-table-comment-sql args))
