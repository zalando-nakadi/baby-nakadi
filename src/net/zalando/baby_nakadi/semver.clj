(ns net.zalando.baby-nakadi.semver
  (:require [clojure.string :as str]))

(defn semver-str
  [{:keys [major minor patch pre-release build] :as m}]
  (let [s (atom (str/join "." [major minor patch]))]
    (when-not (nil? pre-release)
      (swap! s str (format "-%s" pre-release)))
    (when-not (nil? build)
      (swap! s str (format "+%s" build)))
    ;; result.
    @s))
  


