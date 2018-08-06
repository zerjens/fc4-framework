(ns fc4c.integrations.structurizr.express.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str :refer [blank?]]
            [com.gfredericks.test.chuck.generators :refer [string-from-regex]]
            [fc4c.model :as m]
            [fc4c.spec :as fs]
            [fc4c.util :as fu]))

(def ^:private aliases-to-namespaces
  {'st 'structurizr
   'sy 'structurizr.system
   'sp 'structurizr.person
   'sc 'structurizr.container
   'se 'structurizr.element
   'sr 'structurizr.relationship
   'ss 'structurizr.style
   'sd 'structurizr.diagram})

(doseq [[alias-sym ns-sym] aliases-to-namespaces]
  (fu/ns-with-alias ns-sym alias-sym))

(s/def ::st/name ::fs/non-blank-simple-str)
(s/def ::st/description ::fs/non-blank-simple-str)

(def ^:private comma-delimited-simple-strs-pattern #"[A-Za-z\-_,0-9]+")

(s/def :st/comma-delimited-simple-strings
  (s/with-gen
    (s/and string? (partial re-matches comma-delimited-simple-strs-pattern))
    #(string-from-regex comma-delimited-simple-strs-pattern)))

(s/def ::st/tags :st/comma-delimited-simple-strings)

(s/def ::st/position ::fs/coord-string)

(s/def ::st/foo string?)

(def ^:private int-pattern #"\d{1,4}")
(s/def ::st/int-in-string
  (s/with-gen (s/and string? (partial re-matches int-pattern))
    #(string-from-regex int-pattern)))

;;;;; Elements (TODO: maybe should be a multispec?)

;;;; Base element

(s/def ::st/base-elem
  (s/keys :req-un [::st/name]
          :opt-un [::st/description ::st/tags]))

;;;; Container

(s/def ::sc/type #{"Container"})
(s/def ::sc/technology string?)

(s/def ::st/container
  (s/merge ::st/base-elem
           (s/keys :req-un [::sc/type ::st/position]
                   :opt-un [::sc/technology])))

;;;; Person

(s/def ::sp/type #{"Person"})

(s/def ::st/person
  (s/merge ::st/base-elem
           (s/keys :req-un [::sp/type ::st/position])))

;;;; System

(s/def ::sy/type #{"Software System"})
(s/def ::sy/containers (s/coll-of ::st/container :min-count 1))

(s/def ::st/system
  (s/merge ::st/base-elem
           (s/keys :req-un [::sy/type
                            ; The subject of a container diagram must contain
                            ; :containers; in all other cases system elements
                            ; must contain :position.
                            (or ::st/position ::sy/containers)])))

;;;; Element (an abstraction)

(s/def ::st/element (s/or :system    ::st/system
                          :person    ::st/person
                          :container ::st/container))

;;;; Relationships

; These specs use the generator of :fc4c.model/name so that the values generated
; when generating instances of ::st/relationship-without-vertices will match
; values generated in :fc4c.model/model and :fc4c.view/view, which are the main
; inputs into the export feature defined in export.clj.
(s/def ::sr/source (s/with-gen ::st/name #(s/gen ::m/name)))
(s/def ::sr/destination (s/with-gen ::st/name #(s/gen ::m/name)))
(s/def ::sr/order ::st/int-in-string)
(s/def ::sr/vertices (s/coll-of ::st/position :min-count 1))

;; This is useful for an interim stage in the export process
(s/def ::st/relationship-without-vertices
  (s/keys :req-un [::sr/source ::sr/destination]
          :opt-un [::st/description ::st/tags ::sr/order]))

(s/def ::st/relationship
  (s/merge ::st/relationship-without-vertices
           (s/keys :opt-un [::sr/vertices])))

;;;; Styles

(s/def ::ss/type #{"element" "relationship"})

(s/def ::ss/tag
  (s/with-gen ::fs/non-blank-simple-str
              ;; This generator helps test ...express.export/rename-internal-tag
    #(gen/one-of [(s/gen ::fs/non-blank-simple-str)
                  (gen/return "internal")])))

(s/def ::ss/width ::fs/coord-int)
(s/def ::ss/height ::fs/coord-int)
(s/def ::ss/color ::fs/non-blank-simple-str) ;;; TODO: Make this more specific
(s/def ::ss/shape #{"Box" "RoundedBox" "Circle" "Ellipse" "Hexagon" "Person"
                    "Folder" "Cylinder" "Pipe"})
(s/def ::ss/background ::fs/non-blank-simple-str) ;;; TODO: Make this more specific
(s/def ::ss/dashed #{"true" "false"})
(s/def ::ss/border #{"Dashed" "Solid"})

(s/def ::st/style
  (s/keys :req-un [::ss/type ::ss/tag]
          :opt-un [::ss/color ::ss/shape ::ss/background ::ss/dashed ::ss/border
                   ::ss/width ::ss/height]))

;;;; Diagrams

(s/def ::sd/type #{"System Landscape" "System Context" "Container"})
(s/def ::sd/scope ::st/name)
(s/def ::sd/size #{"A2_Landscape" "A3_Landscape"}) ;;; TODO: Add the rest of the options
(s/def ::sd/elements (s/coll-of ::st/element :min-count 1))
(s/def ::sd/relationships (s/coll-of ::st/relationship))
(s/def ::sd/styles (s/coll-of ::st/style))

(s/def ::st/diagram
  (s/keys :req-un [::sd/type ::sd/scope ::sd/elements ::sd/size]
          :opt-un [::st/description ::sd/relationships ::sd/styles]))
