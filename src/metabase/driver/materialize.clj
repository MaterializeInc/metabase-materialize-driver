(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [clojure
             [set :as set]]
            [metabase.db.spec :as db.spec]
            [metabase [config :as config] [driver :as driver] [util :as u]]
            [metabase.driver.sql-jdbc.execute :as sql-jdbc.execute]
            [metabase.driver.sql.query-processor :as sql.qp]
            [metabase.util.honey-sql-2 :as h2x]
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
                              ;; Materialize defaults to UTC, and this is the only supported value
                              :set-timezone              false
                              :datetime-diff             false
                              :convert-timezone          false
                              :temporal-extract          (not config/is-test?)
                              ;; Disabling nested queries during tests as they try to use Foreign Keys
                              :nested-queries            (not config/is-test?)
                              ;; Disabling the expressions support due to the following error:
                              ;; Error executing query: ERROR: function substring(text, character varying) does not exist
                              :expressions               false
                              ;; Disabling model caching:
                              :persist-models            false
                              ;; Disable percentile aggregations due to missing support for PERCENTILE_CONT
                              :percentile-aggregations   false
                              :test/jvm-timezone-setting false}]
  (defmethod driver/database-supports? [:materialize feature] [_driver _feature _db] supported?))

(defmethod sql-jdbc.execute/set-timezone-sql :materialize
  [_]
  "SET TIMEZONE TO %s;")

; ;;; +----------------------------------------------------------------------------------------------------------------+
; ;;; |                                         metabase.driver.sql-jdbc impls                                         |
; ;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod sql-jdbc.conn/connection-details->spec :materialize
  [_ {:keys [host port db cluster], :as opts}]
  (sql-jdbc.common/handle-additional-options
   (merge
    {:classname                     "org.postgresql.Driver"
     :subprotocol                   "postgresql"
     :subname                       (str "//" host ":" port "/" db "?options=--cluster%3D" cluster)
     :OpenSourceSubProtocolOverride false}
    (dissoc opts :host :port :db :cluster))))

(defmethod driver/describe-table :materialize
  [driver database table]
  (sql-jdbc.sync/describe-table driver database table))

(defmethod sql-jdbc.sync/excluded-schemas :materialize [_driver] #{"mz_catalog" "mz_internal" "pg_catalog"})
