(ns net.zalando.nakadi-mock-clj.subscriptions-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [net.zalando.nakadi-mock-clj.subscriptions :as subscriptions]))


(def ^:const uuid-regex
  #"\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+\-\p{XDigit}+")

(def ^:const date-time-regex
  #"\d+{4}\-\d+{2}\-\d+{2}T\d+{2}\:\d+{2}\:\d+{2}\+\d+{4}")

(deftest util-funcs
  (fact "UUID v4 Generator"
        (subscriptions/uuid-v4)
        => uuid-regex)
  (fact "Local datetime string"
        (subscriptions/local-date-time-str-now)
        => date-time-regex)
  (fact "With own subscriptions"
        (let [before-subscriptions @subscriptions/subscriptions
              some-stupid-example {:name "Max" :age 990}]
          (subscriptions/with-own-subscriptions
            (fact "..own new subscriptions should be empty"
                  (count @subscriptions/subscriptions) => 0)
            (subscriptions/append-to-subscriptions some-stupid-example)
            (fact "..now we got an item in subscriptions"
                  (count @subscriptions/subscriptions) => 1)
            (fact "..also it must equal with this seq"
                  @subscriptions/subscriptions => [some-stupid-example]))
          (fact "..after with-own-subscriptions, it come back to
                before state" @subscriptions/subscriptions =>
                before-subscriptions))))

(deftest basic-subscriptions
  (fact "just a dumb append one"
        (subscriptions/with-own-subscriptions
          (subscriptions/append-to-subscriptions {:name "Max" :age 42})
          (count @subscriptions/subscriptions) => 1))
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
           (subscriptions/with-own-subscriptions
             (subscriptions/append-to-subscriptions s1)
             (fact (str "should contains only " s1)
                   (subscriptions/in-subscriptions? s1) => s1)
             (fact (str "shouldn't contains " s2)
                   (subscriptions/in-subscriptions? s2)
                   => #(or (nil? %) (false? %)))
             (fact (str "also shouldn't contains " s3)
                   (subscriptions/in-subscriptions? s3)
                   => #(or (nil? %) (false? %))))))
  (facts "find missing fields"
         (let [s-ok {"event_types" [:e1 :e2]
                     "owning_application" :a1
                     "consumer_group" :g1}
               s-missing {"foobar" [:e2 :e3]
                          "owning_application" :a2
                          "consumer_group" :g2}]
           (fact "should be empty missing fields"
                 (subscriptions/find-missing-fields s-ok) => #{})
           (fact "should be one 'foobar' missing field"
                 (subscriptions/find-missing-fields s-missing)
                 => #{"event_types"})))                                     
  (facts "find unknown fields"
         (let [s-ok {"event_types" [:e1 :e2]
                     "owning_application" :a1
                     "consumer_group" :g1}
               s-unknown {"foobar" [:e2 :e3]
                          "owning_application" :a2
                          "consumer_group" :g2}]
           (fact "should be empty unknown fields"
                 (subscriptions/find-unknown-fields s-ok) => #{})
           (fact "should be one 'foobar' unknown field"
                 (subscriptions/find-unknown-fields s-unknown)
                 => #{"foobar"}))) 
  (facts "decorate subscription"
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

(deftest subscriptions-membership
  (let [s {"event_types" [:e1 :e2]
           "owning_application" :a1
           "consumer_group" :g1}]
    (facts (subscriptions/in-subscriptions? s [])
           => #(or (false? %) (nil? %)))
    (facts (subscriptions/in-subscriptions? s [s]) => s)))

(deftest subscription-equality
  (let [s {"event_types" [:e1 :e2]
           "owning_application" :a1
           "consumer_group" :g1}
        t1 {"event_types" [:e1 :e3]
            "owning_application" :a1
            "consumer_group" :g1}
        t2 {"event_types" [:e1 :e2]
            "owning_application" :a2
            "consumer_group" :g1}
        t3 {"event_types" [:e1 :e2]
            "owning_application" :a1
            "consumer_group" "g1"}
        s2 (subscriptions/decorate-subscription s)]
    (facts (subscriptions/subscription=? s t1) => false)
    (facts (subscriptions/subscription=? s s2) => true)
    (facts (subscriptions/subscription=? s t2) => false)
    (facts (subscriptions/subscription=? s t3) => false)))

(defn correctly-appended-subscript-facts
  [added? s s2 expect-added?]
  (fact added? => expect-added?)
  (fact (s2 "event_types") => (s "event_types"))
  (fact (s2 "owning_application") => (s "owning_application"))
  (fact (s2 "consumer_group") => (s "consumer_group"))
  (fact (s2 "start_from") => "end")
  (fact (s2 "id") => uuid-regex)
  (fact (s2 "created_at") => date-time-regex))

(defmacro ExceptionInfo-throwing-append+-facts
  [e-i-type-expect e-i-fields-expect & body]
  `(facts (try
            ~@body
            (catch clojure.lang.ExceptionInfo e#
              (let [i# (ex-data e#)]
                (fact (format "..should be a %s typed exception"
                              ~e-i-type-expect)
                      (:type i#) => ~e-i-type-expect)
                (fact (format "..should contains %s in :fields"
                              ~e-i-fields-expect)
                      (:fields i#) => ~e-i-fields-expect)
                (throw e#)))) => (throws clojure.lang.ExceptionInfo)))

(deftest the-ultimate-append+-function
  (subscriptions/with-own-subscriptions
    (facts "Try append item with some missing fields should fail with
    specific exception"
           (ExceptionInfo-throwing-append+-facts
            :missing-fields #{"event_types"}
            (subscriptions/append-to-subscriptions+ {"foobar" [:e2 :e3]
                                                     "owning_application" :a2
                                                     "consumer_group" :g2})))
    (facts "..should be empty now"
           (empty? @subscriptions/subscriptions) => true)
    (facts "Try append item with some unknown fields should fail with
    specific exception"
           (ExceptionInfo-throwing-append+-facts
            :unknown-fields #{"my-name-is-Max"}
            (subscriptions/append-to-subscriptions+ {"my-name-is-Max" 42
                                                     "event_types" [:e2 :e3]
                                                     "owning_application" :a2
                                                     "consumer_group" :g2})))
    (facts "..should be empty now"
           (empty? @subscriptions/subscriptions) => true)
    (let [s {"event_types" [:e2 :e3]
             "owning_application" :a2
             "consumer_group" :g2}]
      (facts "Try append item with correct fields should be alright
      and returns [true decorated-item]"
             (let [[added? s2] (subscriptions/append-to-subscriptions+ s)]
               (correctly-appended-subscript-facts added? s s2 true)))
      (facts "Try append item with already appended item should
      returns [false item-in-subscriptions]"
             (let [[added? s2] (subscriptions/append-to-subscriptions+ s)]
               (correctly-appended-subscript-facts added? s s2 false))))))
