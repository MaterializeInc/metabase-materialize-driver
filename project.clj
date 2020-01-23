(defproject metabase/materialize-driver "0.0.1-SNAPSHOT"
  :description "Metabase Materialized Driver"

  :plugins [[reifyhealth/lein-git-down "0.3.5"]]
  :middleware [lein-git-down.plugin/inject-properties]

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [metabase-core "1.0.0-SNAPSHOT"]
   [org.clojure/java.classpath "0.3.0"]                               ; examine the Java classpath from Clojure programs
   [org.clojure/java.jdbc "0.7.9"]                                    ; basic JDBC access from Clojure
   [clojure.java-time "0.3.2"]
   [mzjdbc "4b28590f9795c717e246cd5a84cacca5f8198993"]]

  :repositories [["public-github" {:url "git://github.com"}]]
  :git-down {mzjdbc {:coordinates MaterializeInc/pgjdbc}}

  :profiles
  {:provided
   {:uberjar
    {:auto-clean     true
     :aot            :all
     :javac-options  ["-target" "1.8", "-source" "1.8"]
     :target-path    "target/%s"
     :uberjar-name   "materialize.metabase-driver.jar"}}})
