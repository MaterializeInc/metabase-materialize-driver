(ns metabase.driver.materialize
  "Metabase Materialize Driver."
  (:require [clojure
              [set :as set]]
            [metabase.db.spec :as db.spec]
            [metabase.driver :as driver]
            [metabase.driver.sql-jdbc
             [common :as sql-jdbc.common]
             [connection :as sql-jdbc.conn]]))

(driver/register! :materialize, :parent :postgres)

; ;;; +----------------------------------------------------------------------------------------------------------------+
; ;;; |                                         metabase.driver.sql-jdbc impls                                         |
; ;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod sql-jdbc.conn/connection-details->spec :materialize [_ {ssl? :ssl, :as details-map}]
  (-> details-map

      (set/rename-keys {:dbname :db})
      db.spec/postgres
      (sql-jdbc.common/handle-additional-options details-map)))
