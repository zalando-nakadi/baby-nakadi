(ns nakadi-mock-clj.api-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [nakadi-mock-clj.api :refer :all]))

(deftest can-post-subscription-and-list-subscription-simply
  (facts "Post a simple subscription"))
  ;;        (let 
  ;; (is (= (get-hello {:configuration {:example-param "foo"}} {:name "Friboo"} nil)
  ;;        {:status  200
  ;;         :headers {}
  ;;         :body    {:message "Hello Friboo" :details {:X-friboo "foo"}}})))
