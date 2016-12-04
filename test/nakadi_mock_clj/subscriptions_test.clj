(ns nakadi-mock-clj.subscriptions-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [nakadi-mock-clj.subscriptions :as subscriptions]))


(def ^:const uuid-regex
  #"\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+")

(def ^:const date-time-regex
  #"\d+{4}\-\d+{2}\-\d+{2}T\d+{2}\:\d+{2}\:\d+{2}\+\d+{4}")

(facts "Utility functions"
       (fact "UUID v4 Generator"
             (subscriptions/uuid-v4)
             => uuid-regex)
       (fact "Local datetime string"
             (subscriptions/local-date-time-str-now)
             => date-time-regex))

(facts "Subscriptions"
       (fact "dumb clear, append one, and clear again"
             (subscriptions/clear-subscriptions)
             (subscriptions/append-to-subscriptions {:name "Max" :age 42})
             (count @subscriptions/subscriptions) => 1
             (subscriptions/clear-subscriptions))
       (facts "cleared, append two, membership tests"
              (let [s1 {"event_types" [:e1 :e2]
                        "owning_application" :a1
                        "consumer_group" :g1}
                    s2 {"event_types" [:e2 :e3]
                        "owning_application" :a2
                        "consumer_group" :g2}
                    s3 {"event_types" [:e3 :e4]
                        "owning_application" :a3
                        "consumer_group" :g3}]
                (subscriptions/clear-subscriptions)
                (subscriptions/append-to-subscriptions s1)
                (fact (str "should contains only " s1)
                      (subscriptions/in-subscriptions? s1) => s1)
                (fact (str "shouldn't contains " s2)
                      (subscriptions/in-subscriptions? s2) => #(or (nil? %) (false? %)))
                (fact (str "also shouldn't contains " s3)
                      (subscriptions/in-subscriptions? s3) => #(or (nil? %) (false? %)))
                (subscriptions/clear-subscriptions)))
       (fact "find missing fields"
             (let [s-ok {"event_types" [:e1 :e2]
                         "owning_application" :a1
                         "consumer_group" :g1}
                   s-missing {"foobar" [:e2 :e3]
                              "owning_application" :a2
                              "consumer_group" :g2}]
               (fact "should be empty missing fields"
                     (subscriptions/find-missing-fields s-ok) => #{})
               (fact "should be one 'foobar' missing field"
                     (subscriptions/find-missing-fields s-missing) => #{"event_types"})))                                     
       (fact "find unknown fields"
             (let [s-ok {"event_types" [:e1 :e2]
                         "owning_application" :a1
                         "consumer_group" :g1}
                   s-unknown {"foobar" [:e2 :e3]
                              "owning_application" :a2
                              "consumer_group" :g2}]
               (fact "should be empty unknown fields"
                     (subscriptions/find-unknown-fields s-ok) => #{})
               (fact "should be one 'foobar' unknown field"
                     (subscriptions/find-unknown-fields s-unknown) => #{"foobar"}))) 
       (fact "decorate subscription"
             (let [s-before {"event_types" [:e1 :e2]
                             "owning_application" :a1
                             "consumer_group" :g1}
                   s-after (subscriptions/decorate-subscription s-before)]
               (fact (s-after "event_types")
                     => (s-before "event_types"))
               (fact (s-after "owning_application")
                     => (s-before "owning_application"))
               (fact (s-after "consumer_group")
                     => (s-before "consumer_group"))
               (fact (s-after "start_from") => "end")
               (fact (s-after "id") => uuid-regex)
               (fact (s-after "created_at") => date-time-regex))))
                     

(facts "Subscriptions: my ultimate append+ function."
       (fact "Cleared & empty"
             (subscriptions/clear-subscriptions))
       (fact "Try append item with some missing fields should fail with specific exception"
             (try
               (subscriptions/append-to-subscriptions+ {"foobar" [:e2 :e3]
                                                        "owning_application" :a2
                                                        "consumer_group" :g2})
               (catch clojure.lang.ExceptionInfo e
                 (let [i (ex-data e)]
                   (fact "..should be a :missing-fields typed exception"
                         (:type i) => :missing-fields)
                   (fact "..should contains 'event_types' in :fields"
                         (:fields i) => #{"event_types"})
                   (throw e)))) => (throws clojure.lang.ExceptionInfo))
       (fact "..should be empty now"
             (empty? @subscriptions/subscriptions) => true)
       (fact "Try append item with some unknown fields should fail with specific exception"
             (try
               (subscriptions/append-to-subscriptions+ {"my-name-is-Max" 42
                                                        "event_types" [:e2 :e3]
                                                        "owning_application" :a2
                                                        "consumer_group" :g2})
               (catch clojure.lang.ExceptionInfo e
                 (let [i (ex-data e)]
                   (fact "..should be a :unknown-fields typed exception"
                         (:type i) => :unknown-fields)
                   (fact "..should contains 'my-name-is-Max' in :fields"
                         (:fields i) => #{"my-name-is-Max"})
                   (throw e)))) => (throws clojure.lang.ExceptionInfo))
       (fact "..should be empty now"
             (empty? @subscriptions/subscriptions) => true)
       (let [s {"event_types" [:e2 :e3]
                "owning_application" :a2
                "consumer_group" :g2}]
         (fact "Try append item with correct fields should be alright and returns [true decorated-item]"
               (let [[added? s2] (subscriptions/append-to-subscriptions+ s)]
                 (fact added? => true)
                 (fact (s2 "event_types") => (s "event_types"))
                 (fact (s2 "owning_application") => (s "owning_application"))
                 (fact (s2 "consumer_group") => (s "consumer_group"))
                 (fact (s2 "start_from") => "end")
                 (fact (s2 "id") => uuid-regex)
                 (fact (s2 "created_at") => date-time-regex)))
         (fact "Try append item with already appended item should returns [false item-in-subscriptions]"
               (let [[added? s2] (subscriptions/append-to-subscriptions+ s)]
                 (fact added? => false)
                 (fact (s2 "event_types") => (s "event_types"))
                 (fact (s2 "owning_application") => (s "owning_application"))
                 (fact (s2 "consumer_group") => (s "consumer_group"))
                 (fact (s2 "start_from") => "end")
                 (fact (s2 "id") => uuid-regex)
                 (fact (s2 "created_at") => date-time-regex)))))
