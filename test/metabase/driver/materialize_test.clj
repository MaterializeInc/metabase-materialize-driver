(ns metabase.driver.materialize-test
  (:require
   [clojure.test :refer :all]
   #_{:clj-kondo/ignore [:discouraged-namespace]}
   [honey.sql :as sql]
   [metabase.driver :as driver]
   [metabase.driver.materialize :as materialize]
   [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
   [metabase.driver.sql.query-processor :as sql.qp]
   [metabase.public-settings.premium-features :as premium-features]
   [metabase.query-processor :as qp]
   [metabase.test.fixtures :as fixtures]
   [metabase.test :as mt]))

(set! *warn-on-reflection* true)

(use-fixtures :once (fixtures/initialize :plugins))
(use-fixtures :once (fixtures/initialize :db))
