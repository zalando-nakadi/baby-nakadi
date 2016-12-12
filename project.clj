(defproject net.zalando.baby-nakadi "0.0.1-SNAPSHOT"
  :description "Nakadi-mock rewritten in Clojure/Friboo"
  :url "https://github.bus.zalan.do/jyun/baby-nakadi"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.zalando.stups/friboo "2.0.0-beta5"]
                 [danlentz/clj-uuid "0.1.6"]
                 [clj-time "0.12.2"]
                 [org.flatland/useful "0.11.5"]
                 [grimradical/clj-semver "0.3.0-SNAPSHOT"]
                 ]
  :main ^:skip-aot net.zalando.baby-nakadi.core
  :uberjar-name "baby-nakadi.jar"
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
