(ns clj-yaml.core
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)))

(def ^{:dynamic true} *keywordize*
  "If *keywordize* is set to true (and it is by default),
   turn all keys into Clojure keywords. If set to false,
   keys will be unconverted strings."
  true)

(def flow-styles
  "Map of :auto, :block, and :flow mapped to relevant
   FlowStyles."
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  "Return a new initialized DumperOptions object with the
   flow style passed, if passed."
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  "Make a new Yaml object based on the dumper options passed."
  [& {:keys [dumper-options]}]
  (if dumper-options
    (Yaml. (apply make-dumper-options
                  (mapcat (juxt key val)
                          dumper-options)))
    (Yaml.)))

(defprotocol YAMLCodec
  "A protocol for encoding and decoding YAML."
  (encode [data] "Encode YAML.")
  (decode [data] "Decode YAML."))

(defn decode-key
  "If *keywordize* is true, call keyword on k otherwise just
   return k."
  [k]
  (if *keywordize* (keyword k) k))


;; Extend YAMLCodec to various Clojure objects so that our YAML
;; encoder/decoder knows what to do with our data structures
;; and such.
(extend-protocol YAMLCodec

  clojure.lang.IPersistentMap
  (encode [data]
    (into {}
          (for [[k v] data]
            [(encode k) (encode v)])))

  clojure.lang.IPersistentCollection
  (encode [data]
    (map encode data))

  clojure.lang.Keyword
  (encode [data]
    (name data))

  java.util.LinkedHashMap
  (decode [data]
    (into {}
          (for [[k v] data]
            [(decode-key k) (decode v)])))

  java.util.LinkedHashSet
  (decode [data]
    (into #{} data))

  java.util.ArrayList
  (decode [data]
    (map decode data))

  Object
  (encode [data] data)
  (decode [data] data)

  nil
  (encode [data] data)
  (decode [data] data))

(defn generate-string
  "Generate a YAML string from some data."
  [data & opts]
  (.dump (apply make-yaml opts)
         (encode data)))

(defn parse-string
  "Parse YAML from a string and return a Clojure
   representation of it."
  ([string keywordize]
     (binding [*keywordize* keywordize]
       (parse-string string)))
  ([string]
     (decode (.load (make-yaml) string))))
