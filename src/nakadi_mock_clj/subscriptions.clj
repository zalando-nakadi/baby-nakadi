(ns nakadi-mock-clj.subscriptions
  (:require [clojure.set :as set]
            [clj-uuid :as uuid]
            [clojure.string :as s]
            [clj-time.local :as t-local]
            [clj-time.format :as t-fmt]))

(defn uuid-v4 []
  (str (uuid/v4)))

(defonce created-at-formatter
  (t-fmt/formatter-local "yyyy-MM-dd'T'HH:mm:ssZ"))

(defn local-date-time-str-now []  
  (t-fmt/unparse created-at-formatter (t-local/local-now)))   

(def subscriptions (ref []))

(defn clear-subscriptions []
  (dosync (ref-set subscriptions [])))

(defn append-to-subscriptions [item]
  (dosync (alter subscriptions conj item)))

(defn decorate-subscription [s]
  (merge s {"start_from" "end",
            "id" (uuid-v4),
            "created_at" (local-date-time-str-now)}))

(def ^:const required-keys #{"event_types" "owning_application" "consumer_group"})

(def ^:const known-keys (set/union required-keys #{"start_from"}))

(defn find-missing-fields [s]
  (let [ks (set (keys s))]
    (set/difference required-keys ks)))

(defn find-unknown-fields [s]
  (let [ks (set (keys s))]
    (set/difference ks known-keys)))

(defn in-subscriptions? [s]
  (letfn [(subscription-eq? [a b]
            (and (= (a "owning_application") (b "owning_application"))
                 (= (a "event_types") (b "event_types"))
                 (= (a "consumer_group") (b "consumer_group"))))]
    (some #(subscription-eq? s %) @subscriptions)))


;; TODO: master append function?
