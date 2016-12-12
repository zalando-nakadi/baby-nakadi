(ns net.zalando.baby-nakadi.semver-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [clj-semver.core :as clj-semver]
            [clojure.tools.macro :refer :all] 
            [net.zalando.baby-nakadi.semver :as my-semver]))

(deftest semver-str-test
  (macrolet [(semver-back-and-forth [s]
                                    `(fact (my-semver/semver-str (clj-semver/version ~s))
                                           => ~s))]
            (facts "simple building"
                   (semver-back-and-forth "0.1.2")
                   (semver-back-and-forth "0.1.2-foobar")
                   (semver-back-and-forth "0.1.2+2016"))))
                   

