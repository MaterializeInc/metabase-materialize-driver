(defproject metabase/materialize-driver "0.0.5-SNAPSHOT"
  :description "Metabase Materialized Driver"

  :dependencies
  [[clojure.java-time "0.3.2"]]

  :aot :all     ; Checks for compile-time failures when building the uberjar
  :profiles
  {:provided
   {:dependencies [[metabase-core "1.0.0-SNAPSHOT"]]

   :uberjar
    {:auto-clean     true
     :aot            :all
     :javac-options  ["-target" "1.8", "-source" "1.8"]
     :target-path    "target/%s"
     :uberjar-name   "materialize.metabase-driver.jar"}}})
