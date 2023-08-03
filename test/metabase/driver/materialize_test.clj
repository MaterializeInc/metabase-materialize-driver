(ns metabase.driver.materialize-test
  (:require
   [clojure.test :refer :all]
   #_{:clj-kondo/ignore [:discouraged-namespace]}
   [honeysql.format :as hformat]
   [metabase.driver :as driver]
   [metabase.driver.materialize :as materialize]
   [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
   [metabase.driver.sql.query-processor :as sql.qp]
   [metabase.public-settings.premium-features :as premium-features]
   [metabase.query-processor :as qp]
   [metabase.test.fixtures :as fixtures]
   [metabase.test :as mt]
   #_{:clj-kondo/ignore [:discouraged-namespace]}
   [metabase.util.honeysql-extensions :as hx]))

(set! *warn-on-reflection* true)

(use-fixtures :once (fixtures/initialize :plugins))
(use-fixtures :once (fixtures/initialize :db))

(deftest a-true-test
  (testing "A test that should always pass"
    (is (= true true))))

(deftest expression-using-aggregation-test
  (mt/test-drivers :materialize
                   (testing "Can we use aggregations from previous steps in expressions (#12762)"
                     (is (= [["25°" 2 2 0]
                             ["33 Taps" 2 2 0]
                             ["20th Century Cafe" 2 2 0]]
                            (mt/formatted-rows [str int int int]
                                               (mt/run-mbql-query venues
                                                                  {:source-query {:source-table (mt/id :venues)
                                                                                  :aggregation  [[:min (mt/id :venues :price)]
                                                                                                 [:max (mt/id :venues :price)]]
                                                                                  :breakout     [[:field (mt/id :venues :name) nil]]
                                                                                  :limit        3}
                                                                   :expressions  {:price_range [:-
                                                                                                [:field "max" {:base-type :type/Number}]
                                                                                                [:field "min" {:base-type :type/Number}]]}})))))))
