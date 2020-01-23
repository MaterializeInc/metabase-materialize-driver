(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [metabase.driver :as driver]
            [metabase.driver.sql-jdbc
             [connection :as sql-jdbc.conn]]))

(driver/register! :materialize, :parent :postgres)

; ;;; +----------------------------------------------------------------------------------------------------------------+
; ;;; |                                         metabase.driver.sql-jdbc impls                                         |
; ;;; +----------------------------------------------------------------------------------------------------------------+
;
(defmethod sql-jdbc.conn/connection-details->spec :materialize
  [_ {:keys [host port user password], :as opts}]
  (println "in connection details")

  (merge
   {:classname                     "org.postgres.Driver"
    :subprotocol                   "postgres"
    :subname                       (str "//" host ":" port "/")
    :ssl                           false
    :use_server_time_zone_for_dates true}
   (dissoc opts :host :port :user :password)))
