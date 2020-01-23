(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [metabase.driver :as driver]
            [metabase.driver.sql-jdbc
             [connection :as sql-jdbc.conn]]))

(driver/register! :materialize, :parent :postgres)

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                         metabase.driver.sql-jdbc impls                                         |
;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod sql-jdbc.conn/connection-details->spec :materialize
  [_ {:keys [host port db], :as opts}]
  (merge
   {:classname                     "org.postgresql.Driver"
    :subprotocol                   "pgwire"
    :subname                       (str "//" host ":" port "/" db)
    :ssl                           false
    :OpenSourceSubProtocolOverride false}
   (dissoc opts :host :port :db)))

