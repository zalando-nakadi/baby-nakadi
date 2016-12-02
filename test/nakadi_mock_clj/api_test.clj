(ns nakadi-mock-clj.api-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clj-http.client :as http-client]
            [clojure.string :as s]
            [cheshire.core :as json]
            [nakadi-mock-clj.test-utils :as test-utils]
            [nakadi-mock-clj.api :refer :all]))

(defn decode-some-json-plz []
  (http-client/json-decode
   "{\"name\": \"foo\", \"age\": 42, \"grades\":{\"English\": \"A\", \"Korean\":\"F\"}}"))

(defonce decoded-json-value-plz
  {"name" "foo", "age" 42,
   "grades" {"English" "A", "Korean" "F"}})

(deftest my-scratch-tests
  (facts "JSON"
         (facts "Decoding JSON and Deep equality check"
                (decode-some-json-plz)
                => decoded-json-value-plz))
  (facts "Deep equality"
         (= (decode-some-json-plz) decoded-json-value-plz) => true))

(defn api-url [port args]
  (format "http://127.0.0.1:%s/%s"
          port (s/join "/" args)))
  
(deftest subscriptions-facts
  (facts "GET /subscriptions"
         (facts "displays an empty subscription list"
                (clear-subscriptions)
                (test-utils/with-server http-port
                  (let [resp (http-client/get (api-url http-port ["subscriptions"]))
                        resp-json (json/parse-string (:body resp))
                        resp-status (:status resp)
                        resp-content-type ((:headers resp) "Content-Type")]
                    (fact "expect http status 200" resp-status => 200)
                    (fact "expect content-type =~ /json/" resp-content-type => #"json")
                    (fact "empty subscription list" resp-json => []))))
         (facts "retrieves registered subscriptions"
                (clear-subscriptions)
                (test-utils/with-server http-port
                  (let [url (api-url http-port ["subscriptions"])
                        orig-doc {"owning_application" "nakadi-mock"
                                  "event_types" ["event1"]
                                  "consumer_group" "slurper"}
                        json-doc (json/encode orig-doc)]
                    ;; POST /subscriptions
                    (println (http-client/post url {:body json-doc :content-type "application/json"}))
                    )))
         )
  (facts "POST /subscriptions"
         (facts "registers a valid subscription")
         (facts "returns the same subscription using the same identity properties")
         (facts "returns a Bad Request error on unknown fields")
         (facts "returns an Unprocessable Entity error on missing fields")))
  ;;        (let 
  ;; (is (= (get-hello {:configuration {:example-param "foo"}} {:name "Friboo"} nil)
  ;;        {:status  200
  ;;         :headers {}
  ;;         :body    {:message "Hello Friboo" :details {:X-friboo "foo"}}})))


