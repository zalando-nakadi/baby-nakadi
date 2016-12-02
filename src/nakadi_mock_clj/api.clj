(ns nakadi-mock-clj.api
  (:require [org.zalando.stups.friboo.ring :refer :all]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :refer [require-config]]
            [com.stuartsierra.component :as component]
            [ring.util.response :as r]
            [clojure.set :as set]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clj-uuid :as uuid]
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

(def subscriptions (ref []))

(defn clear-subscriptions []
  (dosync (ref-set subscriptions [])))

(defn append-to-subscriptions [item]
  (letfn [(decor [s] (merge s {"start_from" "end",
                               "id" (uuid-v4),
                               "created_at" (local-date-time-str-now)}))]
    (dosync (alter subscriptions conj (decor item)))))

(defn list-subscription
  "Returns every subscriptions"
  [this params request]
  (r/response @subscriptions))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def ^:const required-keys #{"event_types" "owning_application" "consumer_group"})

(def ^:const known-keys (set/union required-keys #{"start_from"}))

(defn- find-missing-fields [s]
  (let [ks (set (keys s))]
    (set/difference required-keys ks)))

(defn- find-unknown-fields [s]
  (let [ks (set (keys s))]
    (set/difference ks known-keys)))

(defn- request-json [request]
  (let [body-reader (io/reader (:body request))
        body-str (slurp body-reader)]
       (json/decode body-str)))

(defn save-subscription
  "Save given subscription"
  [this params request]
  (let [req-json (request-json request)]
    ;; TODO: check mandatory fields?
    ;; TODO: check unknown fields?
    ;; check is it already in subscriptions?
    (if (in-subscriptions? req-json)
      ;; status = 200. ok but not saved/updated.
      (do)
      ;; status = 201, created!
      (do
        (append-to-subscriptions req-json)
        (r/response {:message (str "Hello ")})))))
               

(defn- in-subscriptions? [s]
  (letfn [(subscription-eq? [a b]
            (and (= (a "owning_application") (b "owning_application"))
                 (= (a "event_types") (b "event_types"))
                 (= (a "consumer_group") (b "consumer_group"))))]
    (some #(subscription-eq? s %) @subscriptions)))



;; TODO: unknown-field ==> 400 / detail: 'Unrecognized field "' +
;; unknown[0] + '" (' + knownKeys.length + ' known properties: ' +
;; knownKeys.join(', ') + ')'


;; TODO: missing-field ==> 422
;;                 detail: 'Field "' + missing[0] + '" may not be null'



(defn- uuid-v4 []
  (str (uuid/v4)))

(defonce created-at-formatter
  (t-fmt/formatter-local "yyyy-MM-dd'T'HH:mm:ssZ"))

(defn- local-date-time-str-now []  
  (t-fmt/unparse created-at-formatter (t-local/local-now)))
   


