(ns net.zalando.baby-nakadi.api-schema-registry
  (:require [taoensso.timbre :as timbre
             :refer [log  trace  debug  info  warn  error  fatal  report
                     logf tracef debugf infof warnf errorf fatalf reportf
                     spy get-env]]))

;; TODO:
(defn get-event-types
  [this params request]
  nil)

(defn post-event-type
  [this params request]
  (info "params = " params)
  {:msg :wtf})

(defn get-event-type
  [this params request]
  nil)

(defn delete-event-type
  [this params request]
  nil)

(defn update-event-type
  [this params request]
  nil)




