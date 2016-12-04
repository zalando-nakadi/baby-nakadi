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
  (let [req-json (request-json request)
        missing-fields (subscriptions/find-missing-fields req-json)
        unknown-fields (subscriptions/find-unknown-fields req-json)]
    (cond
      ;; check mandatory fields?
      (not (empty missing-fields))
      ;; TODO: 422
      (throw (format "Missing Field (%s)" (s/join ", " missing-fields)))
      ;; check unknown fields?
      (not (empty unknown-fields))
      ;; TODO: 400
      (throw (format "Unknown Field (%s)" (s/join ", " unknown-fields)))
      ;; check is it already in subscriptions?
      :else
      (do
        (if (subscriptions/in-subscriptions? req-json)
          ;; status = 200. ok but not saved/updated.
          200
          ;; status = 201, created!
          (do
            (subscriptions/append-to-subscriptions req-json)
            201))
        (r/response {:message (str "Hello ")})))))
               



