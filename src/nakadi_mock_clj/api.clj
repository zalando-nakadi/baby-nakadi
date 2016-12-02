(ns nakadi-mock-clj.api
  (:require [org.zalando.stups.friboo.ring :refer :all]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :refer [require-config]]
            [com.stuartsierra.component :as component]
            [ring.util.response :as r]
            [clojure.set :as set]))

(defrecord Controller [configuration]
  component/Lifecycle
  (start [this]
    (log/info "Starting API Controller")
    this)
  (stop [this]
    (log/info "Stopping API Controller")
    this))

(def subscriptions (ref []))

(defn list-subscription
  "Returns every subscriptions"
  [this params request]
  (r/response @subscriptions))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def ^:const required-keys #{:event_types :owning_application :consumer_group})

(def ^:const known-keys (set/union required-keys #{:start_from}))

(defn save-subscription
  "Save given subscription"
  [this params request]
  ;; TODO:
  (dosync (alter subscriptions conj (uuid)))
  ;; TODO:
  (r/response {:message (str "Hello ")}))
               



