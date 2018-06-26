(ns fc4c.model
  (:require [clj-yaml.core           :as yaml]
            [clojure.set                       :refer [union]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string                    :refer [includes? split]]
            [fc4c.files                        :refer [relativize]]
            [fc4c.spec               :as fs]
            [fc4c.util               :as util  :refer [lookup-table-by]]))

;; Less generic stuff:
(s/def ::name
  (s/with-gen
    ::fs/short-non-blank-simple-str
    ;; This needs to generate a small and stable set of names so that the
    ;; generated relationships have a chance of being valid — or at least useful.
    #(gen/elements ["Front" "Middle" "Back" "Internal" "External" "Mobile"])))

(s/def ::description ::fs/non-blank-str) ;; Could reasonably have linebreaks.

;; Non-generic stuff:

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
    ;;
    ;; FYI, the generator doesn’t currently respect the `or` below; a fix for
    ;; this has been contributed to core.spec but not yet released:
    ;; https://dev.clojure.org/jira/browse/CLJ-2046
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

(s/def ::element
  (s/keys
   :req [::name]
   :opt [::description ::uses ::tags]))

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

(defn- get-tags-from-path
  "Given a path to a file (as a String) and a path to an ancestor root directory
  (as a String), extracts a set of tags from set of directories that are
  descendants of the ancestor root dir. If the file path includes “external”
  then the tag :external will be added to the returned set; if not then the tag
  :internal will be added.

  For example:
  => (get-tags-from-path
       \"/docs/fc4/model/systems/uk/compliance/panopticon.yaml\"
       \"/docs/fc4/model/systems/\")
  #{:uk :compliance :internal}"
  [file-path relative-root]
  (as-> (or (relativize file-path relative-root) file-path) v
    (split v #"/")
    (map keyword v)
    (drop-last v)
    (set v)
    (conj v (if (includes? file-path "external")
              :external
              :internal))))

;; TODO: for this spec to be truly useful in QA terms, it really needs an fspec
;; and better generators (the generators will need to create two paths that are
;; usefully and realistic related).
(s/fdef get-tags-from-path
        :args (s/cat :file-path     ::fs/file-path-str
                     :relative-root ::fs/dir-path-str)
        :ret  ::tags)

(defn- to-set-of-keywords
  [xs]
  (-> (map keyword xs) set))

(s/fdef to-set-of-keywords
        :args (s/cat :xs (s/coll-of string?))
        :ret  (s/coll-of keyword? :kind set?)
        :fn (fn [{{:keys [xs]} :args, ret :ret}]
              (= (count (distinct xs)) (count ret))))

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn- fixup-element
  [tags-from-path elem]
  (-> elem
      (util/qualify-keys this-ns-name)
      (update ::repos to-set-of-keywords)
      (update ::tags to-set-of-keywords)
      (update ::tags (partial union tags-from-path))))

(s/def ::simple-strings
  (s/coll-of ::fs/short-non-blank-simple-str))

(s/def ::proto-element
  (s/with-gen
    (s/map-of ::fs/unqualified-keyword (s/or :name    ::name
                                             :strings ::simple-strings))
    #(gen/hash-map :name  (s/gen ::name)
                   :repos (s/gen ::simple-strings)
                   :tags  (s/gen ::simple-strings))))

(s/fdef fixup-element
        :args (s/cat :tags-from-path ::tags
                     :proto-element  ::proto-element)
        :ret ::element)

;; A file might contain a single element (as a map), or an array containing
;; multiple elements.
(defn elements-from-file
  "Parses the contents of a YAML file, then processes those contents such that
  each element conforms to ::element. The file-path and root-path are used to
  generate tags from the file’s path relative to the root path."
  [file-contents file-path root-path]
  (let [parsed (yaml/parse-string file-contents)
        elems (if (associative? parsed) [parsed] parsed)
        tags-from-path (get-tags-from-path file-path root-path)]
    (map (partial fixup-element tags-from-path)
         elems)))

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

(s/fdef elements-from-file
        :args (s/cat :file-contents ::yaml-file-contents
                     :file-path     ::fs/dir-path-str
                     :root-path     ::fs/dir-path-str)
        :ret  (s/coll-of ::element))
