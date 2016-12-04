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
                      (subscriptions/in-subscriptions? s1) => true)
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
                   s-missing {"foobar" [:e2 :e3]
                              "owning_application" :a2
                              "consumer_group" :g2}]
               (fact "should be empty missing fields"
                     (subscriptions/find-unknown-fields s-ok) => #{})
               (fact "should be one 'foobar' missing field"
                     (subscriptions/find-unknown-fields s-missing) => #{"foobar"}))) 
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
                     

