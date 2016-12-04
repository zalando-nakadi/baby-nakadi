(ns nakadi-mock-clj.api
  (:require [org.zalando.stups.friboo.ring :refer :all]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :refer [require-config]]
            [com.stuartsierra.component :as component]
            [nakadi-mock-clj.subscriptions :as subscriptions]
            [ring.util.response :as r]
            [clojure.set :as set]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clj-uuid :as uuid]
            [clojure.string :as s]
            [clj-time.local :as t-local]
            [clj-time.format :as t-fmt]))

(defrecord Controller [configuration]
  component/Lifecycle
  (start [this]
    (log/info "Starting API Controller")
    this)
  (stop [this]
    (log/info "Stopping API Controller")
    this))

(defn get-hello
  "Says hello"
  [{:as this :keys [configuration]} {:as params :keys [name]} request]
  (log/debug "API configuration: %s" configuration)
  (log/info "Hello called for %s" name)
  (r/response {:message (str "Hello " name)
               :details {:X-friboo (require-config configuration :example-param)}}))

(defn list-subscription
  "Returns every subscriptions"
  [this params request]
  (r/response @subscriptions/subscriptions))

(defn- request-json [request]
  (let [body-reader (io/reader (:body request))
        body-str (slurp body-reader)]
    (json/decode body-str)))

(defn save-subscription
  "Save given subscription"
  [this params request]
  (let [req-json (request-json request)]
    (try (let [[added? s] (subscriptions/append-to-subscriptions+ req-json)
               status-code (if added? 201 200)]
           (r/status (r/response s) status-code))
         (catch clojure.lang.ExceptionInfo e
           (let [i (ex-data e)]
             (case :type
               :unknown-fields
               (r/status (r/response
                          (format "%s -- unknown-fields=%s" (:type i) (:fields e)))
                         400)
               :missing-fields
               (r/status (r/response
                          (format "%s -- missing-fields=%s" (:type i) (:fields e)))
                         422)
               (throw e)))))))
