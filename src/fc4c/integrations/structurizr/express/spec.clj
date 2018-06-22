(ns fc4c.integrations.structurizr.express.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str :refer [blank?]]
            [com.gfredericks.test.chuck.generators :as gen']))

;; TODO: these are duplicated in src/fc4c/model.clj
(s/def :structurizr/non-blank-string (s/and string? (complement blank?)))
(s/def :structurizr/name :structurizr/non-blank-string)
(s/def :structurizr/description :structurizr/non-blank-string)

(s/def :structurizr/tags string?) ;; comma-delimited TODO: use a regex

(def coord-pattern-base "(\\d{1,4}), ?(\\d{1,4})")

(s/def :structurizr/coord-string
  (s/with-gen string?
    ;; unfortunately we can’t use coord-pattern here because it has anchors
    ;; which are not supported by string-from-regex.
    #(gen'/string-from-regex (re-pattern coord-pattern-base))))

(s/def :structurizr/coord-int
  ;; The upper bound here was semi-randomly chosen; we just need a reasonable number that a real
  ;; diagram is unlikely to ever need but that won’t cause integer overflows when multiplied.
  ;; In other words, we’re using int-in rather than nat-int? because sometimes the generator for
  ;; nat-int? returns very very large integers, and those can sometimes blow up the functions
  ;; during generative testing.
  (s/int-in 0 50001))

(s/def :structurizr/position :structurizr/coord-string)

(def int-pattern #"\d{1,6}")
(s/def :structurizr/int-in-string
  (s/with-gen (s/and string? (partial re-matches int-pattern))
    #(gen'/string-from-regex int-pattern)))

;;;; Elements

(s/def :structurizr.container/type #{"Container"})
(s/def :structurizr.container/technology string?)

(s/def :structurizr/container
  (s/keys :req-un [:structurizr/name :structurizr/position :structurizr.container/type]
          :opt-un [:structurizr/description :structurizr/tags :structurizr.container/technology]))

(s/def :structurizr.element/type #{"Person" "Software System"})
(s/def :structurizr.element/containers (s/coll-of :structurizr/container :min-count 1))

(s/def :structurizr/element
  (s/keys :req-un [:structurizr/name :structurizr/position :structurizr.element/type]
          :opt-un [:structurizr/description :structurizr/tags :structurizr.element/containers]))

;;;; Relationships

(s/def :structurizr.relationship/source :structurizr/name)
(s/def :structurizr.relationship/destination :structurizr/name)
(s/def :structurizr.relationship/order :structurizr/int-in-string)
(s/def :structurizr.relationship/vertices (s/coll-of :structurizr/coord-string :min-count 1))

(s/def :structurizr/relationship
  (s/keys :req-un [:structurizr.relationship/source :structurizr.relationship/destination]
          :opt-un [:structurizr/description :structurizr/tags :structurizr.relationship/vertices
                   :structurizr.relationship/order]))

;;;; Styles

(s/def :structurizr.style/type #{"element" "relationship"})
(s/def :structurizr.style/tag :structurizr/non-blank-string)
(s/def :structurizr.style/width :structurizr/coord-int)
(s/def :structurizr.style/height :structurizr/coord-int)
(s/def :structurizr.style/color :structurizr/non-blank-string) ;;; TODO: Make this more specific
(s/def :structurizr.style/shape #{"Box" "RoundedBox" "Circle" "Ellipse" "Hexagon"
                                  "Person" "Folder" "Cylinder" "Pipe"})
(s/def :structurizr.style/background :structurizr/non-blank-string) ;;; TODO: Make this more specific
(s/def :structurizr.style/dashed #{"true" "false"})
(s/def :structurizr.style/border #{"Dashed" "Solid"})

(s/def :structurizr/style
  (s/keys :req-un [:structurizr.style/type :structurizr.style/tag]
          :opt-un [:structurizr.style/color :structurizr.style/shape :structurizr.style/background
                   :structurizr.style/dashed :structurizr.style/border :structurizr.style/width
                   :structurizr.style/height]))

;;;; Diagrams

(s/def :structurizr.diagram/type #{"System Landscape" "System Context" "Container"})
(s/def :structurizr.diagram/scope :structurizr/non-blank-string)
(s/def :structurizr.diagram/size #{"A2_Landscape" "A3_Landscape"}) ;;; TODO: Add the rest of the options
(s/def :structurizr.diagram/elements (s/coll-of :structurizr/element :min-count 1))
(s/def :structurizr.diagram/relationships (s/coll-of :structurizr/relationship :min-count 1))
(s/def :structurizr.diagram/styles (s/coll-of :structurizr/style :min-count 1))

(s/def :structurizr/diagram
  (s/keys :req-un [:structurizr.diagram/type :structurizr.diagram/scope :structurizr/description
                   :structurizr.diagram/elements :structurizr.diagram/relationships
                   :structurizr.diagram/styles :structurizr.diagram/size]))
