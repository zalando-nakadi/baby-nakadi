(defproject nakadi-mock-clj "0.0.1-SNAPSHOT"
  :description "Nakadi-mock rewritten in Clojure/Friboo"
  :url "https://github.bus.zalan.do/jyun/nakadi-mock-clj"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.zalando.stups/friboo "2.0.0-beta5"]]
  :main ^:skip-aot nakadi-mock-clj.core
  :uberjar-name "nakadi-mock-clj.jar"
  :target-path "target/%s"
  :manifest {"Implementation-Version" ~#(:version %)}
  :plugins [[lein-cloverage "1.0.9"]
            [lein-set-version "0.4.1"]]
  :aliases {"cloverage" ["with-profile" "test" "cloverage"]}
  :profiles {:uberjar {:aot :all}
             :dev     {:repl-options {:init-ns user}
                       :source-paths ["dev"]
                       :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      [org.clojure/java.classpath "0.2.3"]
                                      [midje "1.8.3"]]}})
