(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [clojure
             [set :as set]]
            [metabase.db.spec :as db.spec]
            [metabase.driver :as driver]
            [metabase.driver.sql-jdbc
             [common :as sql-jdbc.common]
             [connection :as sql-jdbc.conn]
             [sync :as sql-jdbc.sync]]))

(driver/register! :materialize, :parent :postgres)

; ;;; +----------------------------------------------------------------------------------------------------------------+
; ;;; |                                         metabase.driver.sql-jdbc impls                                         |
; ;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod sql-jdbc.conn/connection-details->spec :materialize
  [_ {:keys [host port db], :as opts}]
  (sql-jdbc.common/handle-additional-options
   (merge
    {:classname                     "org.postgresql.Driver"
     :subprotocol                   "postgresql"
     :subname                       (str "//" host ":" port "/" db)
     :ssl                           true
     :OpenSourceSubProtocolOverride false}
    (dissoc opts :host :port :db))))

(defmethod driver/describe-table :materialize
  [driver database table]
  (sql-jdbc.sync/describe-table driver database table))
