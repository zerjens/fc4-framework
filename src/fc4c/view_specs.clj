(ns fc4c.view
  (:require [clojure.spec.alpha  :as s]
            ; This ns is required solely for the side fx of loading the file:
            ; registering the desired specs in the spec registry.
            [fc4c.integrations.structurizr.express.spec]
            [fc4c.model          :as m]
            [fc4c.spec           :as fs]))

(s/def ::description ::fs/non-blank-str) ;; Could reasonably have linebreaks.

;; We need that generator! See comment in definition of ::m/name.
(s/def ::name ::m/name)
(s/def ::system ::m/name)

;; You might ask: why copy specs over from a different namespace? It’s because
;; when the views are parsed from YAML files and we end up with non-namespaced
;; keyword keys, and we then post-process them to qualify them with namespaces,
;; it’s impractical to qualify them with _different_ namespaces, so we’re going
;; to qualify them all with _the same_ namespace. Thus, that namespace needs to
;; include definitions for -all- the keys that appear in the YAML files.
(s/def ::coord-string ::fs/coord-string)

(s/def ::subject ::coord-string)
(s/def ::position-map (s/map-of ::name ::coord-string :min-count 1))
(s/def ::users ::position-map)
(s/def ::containers ::position-map)
(s/def ::other-systems ::position-map)

(s/def ::positions
  (s/and
   (s/keys
    :req [::subject]
    :opt [::users ::containers ::other-systems])
   #(some (partial contains? %) [::users ::containers ::other-systems])))

(s/def ::control-point-group
  (s/map-of
   ::name
   (s/coll-of (s/coll-of ::coord-string :min-count 1 :gen-max 3)
              :min-count 1
              :gen-max 3)
   :min-count 1
   :gen-max 3))

(s/def ::system-context ::control-point-group)
(s/def ::container ::control-point-group)

(s/def ::control-points
  (s/keys
   :req [::system-context]
   :opt [::container]))

(s/def ::size :structurizr.diagram/size)

(s/def ::view
  (s/keys
   :req [::system ::description ::positions ::control-points ::size]))
