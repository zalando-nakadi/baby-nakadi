(ns net.zalando.baby-nakadi.schema-registry)

(def ^:dynamic schemas
  "Simple schema-registry"
  (ref {}))

;; TODO: validate input
(defn add-schema
  [name schema]
  (when (contains? @schemas name)
    (throw (ex-info (format "Schema found (%s)" name)
                    {:type :schema-found
                     :name name})))
  (dosync (alter schemas assoc name schema)))
  
(defn delete-schema
  [name]
  (when-not (contains? @schemas name)
    (throw (ex-info (format "Schema not found (%s)" name)
                    {:type :schema-not-found
                     :name name})))  
  (dosync (alter schemas dissoc name)))

;; TODO: validate input?
(defn update-schema
  [name schema]
  (when-not (contains? @schemas name)
    (throw (ex-info (format "Schema not found (%s)" name)
                    {:type :schema-not-found
                     :name name})))
  (dosync (alter schemas assoc name schema)))

(defn schema
  [name]
  (@schemas name))
