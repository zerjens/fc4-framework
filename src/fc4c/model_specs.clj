(ns fc4c.model
  (:require [clj-yaml.core           :as yaml]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [fc4c.spec               :as fs]
            [fc4c.util               :as util  :refer [lookup-table-by]]))

(s/def ::name
  (s/with-gen
    ::fs/short-non-blank-simple-str
    ;; This needs to generate a small and stable set of names so that the
    ;; generated relationships have a chance of being valid — or at least useful.
    #(gen/elements ["Front" "Middle" "Back" "Internal" "External" "Mobile"])))

(s/def ::description ::fs/non-blank-str) ;; Could reasonably have linebreaks.

(s/def ::simple-strings
  (s/coll-of ::fs/short-non-blank-simple-str))

(s/def ::short-simple-keyword
  (s/with-gen
    (s/and keyword?
           (comp (partial s/valid? ::fs/short-non-blank-simple-str) name))
    #(gen/fmap keyword (s/gen ::fs/short-non-blank-simple-str))))

(s/def ::small-set-of-keywords
  (s/coll-of ::short-simple-keyword
             :distinct true
             :kind set?
             :gen-max 10))

(s/def ::repos ::small-set-of-keywords)
(s/def ::tags ::small-set-of-keywords)
(s/def ::system ::name)
(s/def ::container ::name)
(s/def ::technology ::fs/non-blank-simple-str)

(s/def ::system-ref
  (s/keys
    ;; Must contain *either* ::system *or* ::container, or both, so as to
    ;; support these cases:
    ;;
    ;; * a system or container might use a different system with or without
    ;;   specifying the container
    ;; * a container might use a different container of the same/current system,
    ;;   in which case the system is implicit
   :req [(or ::container ::system (and ::system ::container))]
   :opt [::technology ::description]))

;;; order doesn’t really matter here, so I guess it should be a set?
(s/def ::uses
  (s/with-gen
    (s/coll-of ::system-ref :min-count 1)
    #(gen/vector (s/gen ::system-ref) 5 10)))

(s/def ::container-map
  (s/keys
   :req [::name]
   :opt [::description ::technology ::uses]))

;;; Order doesn’t really matter here, so I guess it should be a set? Or maybe a
;;; map of container names to container-maps? That would be consistent with
;;; ::systems.
(s/def ::containers
  (s/coll-of ::container-map))

;; An element just after it’s parsed from the YAML, before any fixup.
(s/def ::proto-element
  (s/with-gen
    (s/map-of ::fs/unqualified-keyword (s/or :name    ::name
                                             :strings ::simple-strings))
    #(gen/hash-map :name  (s/gen ::name)
                   :repos (s/gen ::simple-strings)
                   :tags  (s/gen ::simple-strings))))

(s/def ::element
  (s/keys
   :req [::name]
   :opt [::description ::uses ::tags]))

(s/def ::element-yaml-string
  (s/with-gen
    ::fs/non-blank-str
    #(gen/fmap yaml/generate-string (s/gen ::element))))

(s/def ::elements-yaml-string
  (s/with-gen
    ::fs/non-blank-str
    #(gen/fmap yaml/generate-string (s/gen (s/coll-of ::element)))))

(s/def ::yaml-file-contents
  (s/with-gen
    ::fs/non-blank-str
    #(gen/one-of (map s/gen [::element-yaml-string ::elements-yaml-string]))))

(s/def ::system-map
  (s/merge ::element
           (s/keys :opt [::containers ::repos])))

(def ^:private lookup-table-by-name
  (partial lookup-table-by ::name))

(s/def ::systems
  (s/with-gen
    (s/map-of ::name ::system-map :min-count 1)
    #(gen/fmap lookup-table-by-name (s/gen (s/coll-of ::system-map)))))

(s/def ::user-map
  (s/merge ::element
           (s/keys :req [::uses])))

(s/def ::users
  (s/with-gen
    (s/map-of ::name ::user-map :min-count 1)
    #(gen/fmap lookup-table-by-name (s/gen (s/coll-of ::user-map)))))

(s/def ::model
  (s/keys :req [::systems ::users]))
