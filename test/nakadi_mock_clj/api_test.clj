(ns nakadi-mock-clj.api-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clj-http.client :as http-client]
            [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.pprint :refer :all]
            [flatland.useful.seq :as useful-seq]
            [nakadi-mock-clj.test-utils :as test-utils]
            [nakadi-mock-clj.api :refer :all]
            [nakadi-mock-clj.subscriptions :as subscriptions]
            [nakadi-mock-clj.subscriptions-test :as subscriptions-test]))

(defn decode-some-json-plz []
  (http-client/json-decode
   "{\"name\": \"foo\", \"age\": 42, \"grades\":{\"English\": \"A\", \"Korean\":\"F\"}}"))

(defonce decoded-json-value-plz
  {"name" "foo", "age" 42,
   "grades" {"English" "A", "Korean" "F"}})

(deftest my-scratch
  (facts "JSON"
         (facts "Decoding JSON and Deep equality check"
                (decode-some-json-plz)
                => decoded-json-value-plz))
  (facts "Deep equality"
         (= (decode-some-json-plz) decoded-json-value-plz) => true))

(defn api-url [port args]
  (format "http://127.0.0.1:%s/%s"
          port (s/join "/" args)))

(defn api-list-subscriptions [port]
  (let [resp (http-client/get (api-url port ["subscriptions"]))]
    (merge resp {:body-json (json/parse-string (:body resp))})))

(defn api-post-subscriptions [port s]
  (let [s-json (json/encode s)
        resp (try (http-client/post (api-url port ["subscriptions"])
                                    {:body s-json :content-type "application/json"})
                  (catch clojure.lang.ExceptionInfo e
                    (ex-data e)))]
    (merge resp {:body-json (json/parse-string (:body resp))})))

(deftest list-subscriptions
  (facts "GET /subscriptions"
         (facts "displays an empty subscription list"
                (subscriptions/clear-subscriptions)
                (test-utils/with-server http-port
                  (let [resp (api-list-subscriptions http-port)]
                    (fact "expect http status 200" (:status resp) => 200)
                    (fact "expect content-type =~ /json/" ((:headers resp) "Content-Type") => #"json")
                    (fact "empty subscription list" (:body-json resp) => []))))))

(deftest post-subscriptions
  (test-utils/with-server http-port
    (facts "POST /subscriptions"
           (facts "registers a valid subscription"
                  (subscriptions/clear-subscriptions)
                  (facts "..should be empty"
                         (let [resp (api-list-subscriptions http-port)]
                           (fact (:body-json resp) => [])))
                  (let [s {"owning_application" "nakadi-mock"
                           "event_types" ["event1"]
                           "consumer_group" "slurper"}
                        resp-post (api-post-subscriptions http-port s)]
                    (facts "..returned value should be decorated"
                           (fact (subscriptions/subscription=? s (:body-json resp-post)) => true))   
                    (facts "..shouldn't be empty"
                           (let [resp-list (api-list-subscriptions http-port)
                                 result-list (:body-json resp-list)]
                             (fact (count result-list) => 1)
                             (fact (subscriptions/subscription=? s (first result-list)) => true))))
                  (subscriptions/clear-subscriptions))
           (facts "returns the same subscription using the same identity properties"
                  (let [s {"owning_application" "nakadi-mock"
                           "event_types" ["event1"]
                           "consumer_group" "slurper"}
                        resp-post1 (api-post-subscriptions http-port s)
                        body-post1 (:body-json resp-post1)
                        resp-post2 (api-post-subscriptions http-port s)
                        body-post2 (:body-json resp-post2)]
                    (facts "..first post should be appended"
                           (fact (:status resp-post1) => 201)                    
                           (fact (subscriptions/subscription=? s body-post1) => true)
                           (fact (body-post1 "id") => subscriptions-test/uuid-regex)
                           (fact (body-post1 "created_at") => subscriptions-test/date-time-regex)
                           (fact (body-post1 "start_from") => "end"))
                    (facts "..second post should not be appended"
                           (fact (:status resp-post2) => 200)
                           (fact (subscriptions/subscription=? s body-post2) => true)
                           (fact (body-post1 "id") => subscriptions-test/uuid-regex)
                           (fact (body-post1 "created_at") => subscriptions-test/date-time-regex)
                           (fact (body-post1 "start_from") => "end"))))
           (facts "returns a Bad Request error on unknown fields"
                  (let [s {"owning_application" "nakadi-mock"
                           "event_types" ["event1"]
                           "consumer_group" "slurper"
                           "FOO" "BAR"}
                        resp (api-post-subscriptions http-port s)
                        body (:body-json resp)]
                    (fact (:status resp) => 400)
                    (fact (body "type") => "unknown-fields")
                    (fact (body "message") => #(and (string? %) (not (empty? %))))
                    (fact (body "fields") => ["FOO"])))
           (facts "returns an Unprocessable Entity error on missing fields"
                  (let [s {"application" "nakadi-mock"
                           "event_types" ["event1"]
                           "consumer_group" "slurper"}
                        resp (api-post-subscriptions http-port s)
                        body (:body-json resp)]
                    (fact (:status resp) => 422)
                    (fact (body "type") => "missing-fields")
                    (fact (body "message") => #(and (string? %) (not (empty? %))))
                    (fact (body "fields") => ["owning_application"]))))))



