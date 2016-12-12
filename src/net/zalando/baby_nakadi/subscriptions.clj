(ns net.zalando.baby-nakadi.subscriptions
  (:require [clojure.set :as set]
            [clj-uuid :as uuid]
            [clojure.string :as s]
            [clj-time.local :as t-local]
            [clj-time.format :as t-fmt]))

(defn uuid-v4
  "Generates UUID v4 string"
  []
  (str (uuid/v4)))

(defonce created-at-formatter
  (t-fmt/formatter-local "yyyy-MM-dd'T'HH:mm:ssZ"))

(defn local-date-time-str-now
  "Formatted datetime string"
  ([] (local-date-time-str-now (t-local/local-now)))
  ([dt] (t-fmt/unparse created-at-formatter dt)))

(def ^:dynamic subscriptions
  "Subscriptions STM'd list"
  (ref []))

(defmacro with-own-subscriptions
  "Runs body within own-subscriptions binding, returns seq
  of [result-of-body own-subscriptions]."
  [& body]
  `(binding [subscriptions (ref [])]
     (let [body-result# (do ~@body)]
       [subscriptions body-result#])))

(defn clear-subscriptions
  "Empties saved subscriptions"
  []
  (dosync (ref-set subscriptions [])))

(defn append-to-subscriptions
  "Appends subscription, but never checks its' validity."
  [item]
  (dosync (alter subscriptions conj item)))

(defn decorate-subscription
  "Decorates given map with extra fields"
  [s]
  (merge s {"start_from" "end",
            "id" (uuid-v4),
            "created_at" (local-date-time-str-now)}))

(def ^:const required-keys
  "Mandatory fields for every subscription post"
  #{"event_types" "owning_application" "consumer_group"})

(def ^:const known-keys
  "Allowed fields for a subscription post"
  (set/union required-keys #{"start_from"}))

(defn find-missing-fields
  "Checks a subscription post (a map) that contains every mandatory
  fields. If there is any missing field, returns its' names set. (If
  there's no missing field, it would be a empty-set)"
  [s]
  (let [ks (set (keys s))]
    (set/difference required-keys ks)))

(defn find-unknown-fields
  "Checks a subscription post (a map) that contains any unknown
  fields. If there is any unknown field, return its' names set. (If
  there's no unknown field, it would be a empty-set)"
  [s]
  (let [ks (set (keys s))]
    (set/difference ks known-keys)))

(defn subscription=?
  "Checks equality on two maps, only in 'owning_application',
  'event_types' and 'consumer_group' fields. (Any other fields are
  ignored)"
  [a b]
  (and (= (a "owning_application") (b "owning_application"))
       (= (a "event_types") (b "event_types"))
       (= (a "consumer_group") (b "consumer_group"))))

(defn in-subscriptions?
  "Checks a subscription in a list or subscriptions STM'd list."
  ([s l]
    (first (filter #(subscription=? s %) l)))
  ([s] (in-subscriptions? s @subscriptions)))

(defn append-to-subscriptions+
  "Tries to append a subscription post into STM'd list with every
  validity checks. If there's any missing or unknown field in given
  map, it would raises clojure.lang.ExceptionInfo exception. With that
  ExceptionInfo, you could see which fields are problem (key
  ':fields') and type of exception. (key ':type', is can be one of
  ':missing-fields' or 'unknown-fields'"
  [s]
  (let [missings (find-missing-fields s)
        unknowns (find-unknown-fields s)]
    (cond
      ;; check mandatory fields?
      (not (empty? missings))
      (throw (ex-info "Missing field(s)"
                      {:type :missing-fields :fields missings}))
      ;; check unknown fields?
      (not (empty? unknowns))
      (throw (ex-info "Unknown field(s)"
                      {:type :unknown-fields :fields unknowns}))
      ;; check is it already in subscriptions?
      :else
      (if-let [found (in-subscriptions? s)]
        ;; already contains, never updates.
        [false found]
        ;; append.
        (let [s2 (decorate-subscription s)]
          (append-to-subscriptions s2)
          [true s2])))))
               




