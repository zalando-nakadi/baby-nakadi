(ns net.zalando.baby-nakadi.validators-test
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [net.zalando.baby-nakadi.validators :as validators]))

(deftest generic-validators
  (let [m {"name" "Polster"
           "age" 1942
           "gender" :manly-man}]
    (facts "About required fields"
           (fact (validators/got-all-required? m ["name" "age"])
                 => [true #{}])
           (fact (validators/got-all-required? m ["name" "age" "hobby" "job"])
                 => [false #{"hobby" "job"}]))
    (facts "About enums"
           (fact (validators/within-enum? m "gender" [:manly-man :nice-guy :stupid-myself])
                 => true)
           (fact (validators/within-enum? m "gender" [:elephant :squid-fish])
                 => false))))

(deftest check-requireds
  (let [m {"name" "Polster"
           "age" 1942
           "gender" :manly-man}]
    (facts (validators/check-requireds m #{"name"}) => true)
    (facts (validators/check-requireds m #{"name" "hobby" "job"})
           => (throws ExceptionInfo))))
  
(deftest check-nil
  (facts (validators/check-nil true) => true)
  (facts (validators/check-nil nil) => (throws ExceptionInfo)))

(deftest check-with-pred
  (let [m {"name" "Polster"
           "email" "polster@of-the-heaven.org"
           "age" 1942
           "completely-optional" nil
           "gender" :manly-man}]
    (facts (validators/check-with-pred
            m integer? #{"age"} "Not an integer?" :integer-value-only)
           => true)
    (facts (validators/check-with-pred
            m integer? #{"name" "age"} "Not an integer?" :integer-value-only)
           => (throws ExceptionInfo))
    (facts (validators/check-with-pred
            m string? #{"name" "email"} "Not a string?" :string-value-only)
           => true)
    (facts (validators/check-with-pred
            m integer? #{"age" "completely-optional"}
            "Not an integer?" :integer-value-only
            #{"completely-optional"})
           => true)))
    
(deftest check-EventTypeStatistics
  (let [m-all-ok {"messages_per_minute" 250
                  "message_size" 1024
                  "read_parallelism" 3
                  "write_parallelism" 5}
        m-missing-required {"messages_per_minute" 250
                            "message_size" 1024}     
        m-non-int-fields {"messages_per_minute" 250
                          "message_size" "BIG"
                          "read_parallelism" 3
                          "write_parallelism" 5}]
    (facts "should ok with"
           (validators/check-EventTypeStatistics m-all-ok) => true)
    (facts "should throw an exc with missing field"
           (validators/check-EventTypeStatistics m-missing-required)
           => (throws ExceptionInfo))
    (facts "should throw an exc with non-int-field"
           (validators/check-EventTypeStatistics m-non-int-fields)
           => (throws ExceptionInfo))))

(deftest check-EventTypeOptions
  (let [m-all-ok {"retention_time" 1234}
        m-missing-some-but-ok {}]
    (facts (validators/check-EventTypeOptions m-all-ok) => true)
    (facts (validators/check-EventTypeOptions m-missing-some-but-ok) => true)))
        
                                          
                                          
                                              
