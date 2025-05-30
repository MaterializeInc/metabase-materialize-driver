(ns metabase.test.data.materialize
  "Test extensions for the Materialize driver.  Includes logic for creating/destroying test datasets, building
  the connection specs from environment variables, etc."
  (:require
   [clojure.string :as str]
   [metabase.config :as config]
   [metabase.driver :as driver]
   [metabase.driver.ddl.interface :as ddl.i]
   [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
   [metabase.driver.sql-jdbc.execute :as sql-jdbc.execute]
   [metabase.driver.sql-jdbc.sync.describe-table-test :as describe-table-test]
   [metabase.query-processor-test.alternative-date-test :as alternative-date-test]
   [metabase.query-processor-test.date-bucketing-test :as date-bucketing-test]
   [metabase.test.data.interface :as tx]
   [metabase.test.data.sql :as sql.tx]
   [metabase.test.data.sql-jdbc :as sql-jdbc.tx]
   [metabase.test.data.sql-jdbc.execute :as execute]
   [metabase.test.data.sql-jdbc.load-data :as load-data]
   [metabase.test.data.sql.ddl :as ddl]
   [metabase.util.log :as log]
   [metabase.util.malli :as mu]))

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

(defmethod load-data/row-xform :materialize
  [_driver _dbdef tabledef]
  (load-data/maybe-add-ids-xform tabledef))

(defmethod tx/dataset-already-loaded? :materialize
  [driver dbdef]
  (let [tabledef    (first (:table-definitions dbdef))
        schema-name "public"
        table-name  (:table-name tabledef)]
    (sql-jdbc.execute/do-with-connection-with-options
     driver
     (sql-jdbc.conn/connection-details->spec driver (tx/dbdef->connection-details driver :db dbdef))
     {:write? false}
     (fn [^java.sql.Connection conn]
       (with-open [rset (.getTables (.getMetaData conn)
                                  nil          ; catalog
                                  schema-name  ; schema
                                  table-name   ; table
                                  (into-array String ["TABLE"]))]
         (.next rset))))))

(defmethod load-data/chunk-size :materialize
  [_driver _dbdef _tabledef]
  400)

(defmethod tx/sorts-nil-first? :materialize
  [_driver _base-type]
  false)

(defmethod driver/database-supports? [:materialize :test/time-type]
  [_driver _feature _database]
  false)

(defmethod driver/database-supports? [:materialize :test/timestamptz-type]
  [_driver _feature _database]
  false)

(defmethod driver/database-supports? [:materialize ::describe-table-test/describe-materialized-view-fields]
  [_driver _feature _database]
  false)

(defmethod driver/database-supports? [:materialize ::describe-table-test/describe-view-fields]
  [_driver _feature _database]
  false)

(defmethod driver/database-supports? [:materialize :test/creates-db-on-connect]
  [_driver _feature _database]
  true)

(defmethod driver/database-supports? [:materialize ::alternative-date-test/yyyymmddhhss-binary-timestamps]
  [_driver _feature _database]
  false)

(defmethod  alternative-date-test/yyyymmddhhmmss-binary-dates-expected-rows :materialize
  [_driver]
  [[1 "foo" #t "2019-04-21T16:43"]
   [2 "bar" #t "2020-04-21T16:43"]
   [3 "baz" #t "2021-04-21T16:43"]])

(defmethod  alternative-date-test/yyyymmddhhmmss-dates-expected-rows :materialize
  [_driver]
  [[1 "foo" #t "2019-04-21T16:43"]
   [2 "bar" #t "2020-04-21T16:43"]
   [3 "baz" #t "2021-04-21T16:43"]])


(defmethod date-bucketing-test/group-by-default-test-expected-rows :materialize
  [_driver]
  [["2015-06-01T10:31:00Z" 1]
   ["2015-06-01T16:06:00Z" 1]
   ["2015-06-01T17:23:00Z" 1]
   ["2015-06-01T18:55:00Z" 1]
   ["2015-06-01T21:04:00Z" 1]
   ["2015-06-01T21:19:00Z" 1]
   ["2015-06-02T02:13:00Z" 1]
   ["2015-06-02T05:37:00Z" 1]
   ["2015-06-02T08:20:00Z" 1]
   ["2015-06-02T11:11:00Z" 1]])

(defmethod date-bucketing-test/group-by-default-test-2-expected-rows :materialize
  [_driver]
  [["2015-06-01T10:31:00Z" 1]
   ["2015-06-01T16:06:00Z" 1]
   ["2015-06-01T17:23:00Z" 1]
   ["2015-06-01T18:55:00Z" 1]
   ["2015-06-01T21:04:00Z" 1]
   ["2015-06-01T21:19:00Z" 1]
   ["2015-06-02T02:13:00Z" 1]
   ["2015-06-02T05:37:00Z" 1]
   ["2015-06-02T08:20:00Z" 1]
   ["2015-06-02T11:11:00Z" 1]])

(defmethod date-bucketing-test/group-by-week-database-timezone-override-test-expected-rows :materialize
  [_driver]
  [["2015-05-30T17:00:00-07:00" 46]
   ["2015-06-06T17:00:00-07:00" 47]
   ["2015-06-13T17:00:00-07:00" 40]
   ["2015-06-20T17:00:00-07:00" 60]
   ["2015-06-27T17:00:00-07:00" 7]])
