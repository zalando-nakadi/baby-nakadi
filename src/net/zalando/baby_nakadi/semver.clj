(ns net.zalando.baby-nakadi.semver
  (:require [clojure.string :as str]))

(defn semver-str
  [{:keys [major minor patch pre-release build] :as m}]
  (let [sbuf (StringBuffer. (str/join "." [major minor patch]))]
    (when-not (nil? pre-release)
      (.append sbuf (format "-%s" pre-release)))
    (when-not (nil? build)
      (.append sbuf (format "+%s" build)))
    ;; result.
    (.toString sbuf)))
  


