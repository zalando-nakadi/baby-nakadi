(ns nakadi-mock-clj.test-utils
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [nakadi-mock-clj.core :as core]
            [org.zalando.stups.friboo.system :as system]
            [com.stuartsierra.component :as component]
            [org.zalando.stups.friboo.dev :as dev]
            [org.zalando.stups.friboo.log :as log]))


(defn http-port [] (dev/get-free-port))


;; A Var containing an object representing the application under development.
(defonce system nil)

(defn start
  "Starts the system running, sets the Var #'system."
  [extra-config]
  (dev/reload-log4j2-config)
  (#'system/set-log-level! "DEBUG" :logger-name "nakadi_mock_clj")
  (#'system/set-log-level! "DEBUG" :logger-name "org.zalando.stups")
  (alter-var-root #'system (constantly (core/run (merge {:system-log-level "INFO"}
                                                        (dev/load-dev-config "./test-config.edn")
                                                        extra-config)))))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go
  "Initializes and starts the system running."
  ([extra-config]
   (start extra-config)
   :ready)
  ([]
   (go {})))

(defmacro with-server
  "Run body within HTTP server is running context. Server's randomly
  chosen port number will bound to http-port sym."
  [http-port-sym & body]
  `(let [~http-port-sym ~(http-port)]
     (log/info "Temporary HTTP port number = %s" ~http-port-sym)
     (try
       (go {:http-port ~http-port-sym})
       ~@body
       (finally
         (stop)))))
  


