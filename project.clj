(defproject metabase/materialize-driver "0.0.1-SNAPSHOT"
  :description "Metabase Materialized Driver"

  :plugins [[reifyhealth/lein-git-down "0.3.5"]]
  :middleware [lein-git-down.plugin/inject-properties]

  :dependencies
  [[mzjdbc "4b28590f9795c717e246cd5a84cacca5f8198993"]]

  :repositories [["public-github" {:url "git://github.com"}]]
  :git-down {mzjdbc {:coordinates MaterializeInc/pgjdbc}}

  :profiles
  {:provided
   {:dependencies [[metabase-core "1.0.0-SNAPSHOT"]]

   :uberjar
    {:auto-clean     true
     :aot            :all
     :javac-options  ["-target" "1.8", "-source" "1.8"]
     :target-path    "target/%s"
     :uberjar-name   "materialize.metabase-driver.jar"}}})
