(ns fc4.dsl
  (:require [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]
            [fc4.model :as m]
            [fc4.util :as u :refer [fault fault? qualify-keys]]
            [fc4.yaml :as fy :refer [split-file]]
            [medley.core :refer [map-vals]])
  (:import [org.yaml.snakeyaml.parser ParserException]))

;;;; Keys that may appear at the root of the YAML files:

; Singular — these are unique to the DSL
(s/def ::system     (s/map-of ::m/name ::m/system-map    :min-count 1 :max-count 1))
(s/def ::user       (s/map-of ::m/name ::m/user-map      :min-count 1 :max-count 1))
(s/def ::datastore  (s/map-of ::m/name ::m/datastore-map :min-count 1 :max-count 1))

; Plural — these are nearly identical to the corresponding keys in fc4.model;
; the only differences are cardinalities.
(s/def ::systems    (s/map-of ::m/name ::m/system-map    :min-count 2 :gen-max 3))
(s/def ::users      (s/map-of ::m/name ::m/user-map      :min-count 2 :gen-max 3))
(s/def ::datastores (s/map-of ::m/name ::m/datastore-map :min-count 2 :gen-max 3))

;;;; “Root map” of model YAML files:
(s/def ::file-map
  (s/and (s/keys :req-un [(or (or ::system    ::systems)
                              (or ::user      ::users)
                              (or ::datastore ::datastores))]
                 :opt-un [::system    ::systems
                          ::user      ::users
                          ::datastore ::datastores])
         (fn [v]
           (let [has? (partial contains? v)]
             (and (not-every? has? #{:system    :systems})
                  (not-every? has? #{:user      :users})
                  (not-every? has? #{:datastore :datastores}))))))

(defn parse-model-file
  "Given a YAML model file as a string, parses it, and qualifies all map keys
  except those at the root so that the result has a chance of being a valid
  ::file-map. If a file contains “top matter” then only the main document is
  parsed. Performs very minimal validation. If the file contains malformed YAML,
  or does not contain a map, an anomaly will be returned."
  [file-contents]
  (try
    (let [parsed (-> (split-file file-contents)
                     (::fy/main)
                     (yaml/parse-string))]
      (if (associative? parsed)
        (map-vals #(qualify-keys % "fc4.model") parsed)
        (fault "Root data structure must be a map (mapping).")))
    (catch ParserException e
      (fault (str "YAML could not be parsed: error " e)))))

(s/fdef parse-model-file
  :args (s/cat :file-contents string?)
  :ret  (s/or :success ::file-map
              :failure ::anom/anomaly))

(defn validate-model-file
  "Returns either an error message as a string or nil."
  [parsed-file-contents]
  (cond
    (s/valid? ::file-map parsed-file-contents)
    nil

    (fault? parsed-file-contents)
    (::anom/message parsed-file-contents)

    :else
    (expound-str ::file-map parsed-file-contents)))

(s/fdef validate-model-file
  :args (s/cat :file-map ::file-map)
  :ret  (s/or :valid   nil?
              :invalid string?))
