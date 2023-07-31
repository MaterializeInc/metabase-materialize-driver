(ns metabase.driver.materialize-test
  (:require [clojure.test :as t]
            [metabase.driver.materialize :as materialize]))

(t/deftest a-true-test
  (t/testing "A test that should always pass"
    (t/is (= true true))))
