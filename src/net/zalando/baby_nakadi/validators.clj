(ns net.zalando.baby-nakadi.validators)

(defn within-enum? [m k enum-vals]
  (let [v (get m k)]
    (if (nil? v)
      false
      (contains? (set enum-vals) v))))

(defn filter-keys-by-val [m pred? ks optionals]
  (let [trues! (transient #{})]
    (doseq [k ks]
      (when (and (pred? (get m k))
                 (not (contains? optionals k)))
        (conj! trues! k)))
    (persistent! trues!)))

(defn filter-keys-by-not-val [m pred? ks optionals]
  (filter-keys-by-val m (comp not pred?) ks optionals))
  
(defn got-all-required? [m requireds]
  (let [s (filter-keys-by-val m nil? requireds #{})]
    (if (empty? s)
      [true #{}]
      [false s])))

(defn check-requireds [m ks]
  (let [[got-all? missings] (got-all-required? m ks)]
    (when-not got-all?
      (throw (ex-info "Missing required field(s)"
                      {:type :missing-required-field
                       :fields missings})))
    true))

(defn check-nil [subject]
  (when (nil? subject)
    (throw (ex-info "Cannot be nil" {:type :cannot-be-nil})))
  true)

(defn check-with-pred
  ([m pred? ks msg exc-type]
   (check-with-pred m pred? ks msg exc-type #{}))
  ([m pred? ks msg exc-type optionals]
   (let [fails (filter-keys-by-not-val m pred? ks optionals)]
     (when-not (empty? fails)
       (throw (ex-info msg
                       {:type exc-type
                        :fields fails})))
     true)))

(defn check-EventTypeSchema [m]
  ;; TODO:
  false)


(comment
 
  
  )

(defn check-EventTypeOptions [m]
  (check-nil m)   
  ;; retention_time / optional & integer?
  (check-with-pred m integer?
                   #{"retention_time"}                     
                   "Not an integer(s)" :integer-value-only
                   #{"retention_time"})
  ;;
  true)

(defn check-EventTypeStatistics [m]
  (check-nil m)   
  ;; requireds.
  (check-requireds m #{"messages_per_minute" "message_size"
                       "read_parallelism" "write_parallelism"})
  ;; integer?
  (check-with-pred m integer?
                   #{"messages_per_minute" "message_size"
                     "read_parallelism" "write_parallelism"}
                   "Not an integer(s)" :integer-value-only)
  ;;
  true)

(defn check-EventType [m]
  ;; TODO:
  false)

