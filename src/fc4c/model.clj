(ns fc4c.model
  (:require [clj-yaml.core           :as yaml]
            [clojure.set                       :refer [union]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string                    :refer [blank? ends-with? includes? join split]]
            [clojure.walk                      :refer [postwalk]]
            [fc4c.files                        :refer [relativize]]
            [fc4c.util                         :refer [lookup-table-by]]))

;; Fairly generic stuff:
;; TODO: these are duplicated in src/fc4c/integrations/structurizr/express/spec.clj
(s/def ::non-blank-str (s/and string? (complement blank?)))
(s/def ::no-linebreaks  (s/and string? #(not (includes? % "\n"))))
(s/def ::non-blank-simple-str (s/and ::non-blank-str ::no-linebreaks))

(defn- str-gen
  [min-length max-length]
  ;; Technique found here: https://stackoverflow.com/a/35974064/7012
  (gen/fmap (partial apply str)
            (gen/vector (gen/char-alphanumeric) min-length max-length)))

(s/def ::short-non-blank-simple-str
  (let [min 1 max 50] ;; inclusive
    (s/with-gen
      (s/and ::non-blank-simple-str
             #(<= min (count %) max))
      #(str-gen min max))))

;; Less generic stuff:
(s/def ::name
  (s/with-gen
    ::short-non-blank-simple-str
    ;; This needs to generate a small and stable set of names so that the
    ;; generated relationships have a chance of being valid — or at least useful.
    #(gen/elements ["Front" "Middle" "Back" "Internal" "External" "Mobile"])))

(s/def ::description ::non-blank-str) ;; Could reasonably have linebreaks.

;; Non-generic stuff:

(s/def ::short-simple-keyword
  (s/with-gen
    (s/and keyword?
           (comp (partial s/valid? ::short-non-blank-simple-str) name))
    #(gen/fmap keyword (s/gen ::short-non-blank-simple-str))))

(s/def ::small-set-of-keywords
  (s/coll-of ::short-simple-keyword
             :distinct true
             :kind set?
             :gen-max 10))

(s/def ::repos ::small-set-of-keywords)
(s/def ::tags ::small-set-of-keywords)
(s/def ::system ::name)
(s/def ::container ::name)
(s/def ::technology ::non-blank-simple-str)

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

(s/def ::file-path-str
  (s/with-gen
    (s/and ::non-blank-simple-str #(includes? % "/"))
    #(gen/fmap
      (fn [s] (str (->> (repeat 5 s) (join "/"))))
      (s/gen ::short-non-blank-simple-str))))

;; TODO: a version of this is also in io.clj, and they’ve drifted…
(s/def ::dir-path-str
  (s/with-gen
    (s/and ::file-path-str #(ends-with? % "/"))
    #(gen/fmap
      (fn [file-path] (str file-path "/"))
      (s/gen ::file-path-str))))

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
        :args (s/cat :file-path     ::file-path-str
                     :relative-root ::dir-path-str)
        :ret  ::tags)

(defn- add-ns
  [namespace keeword]
  (keyword (name namespace) (name keeword)))

(s/def ::keyword-or-simple-string
  (s/or :keyword keyword?
        :string  ::non-blank-simple-str))

(s/fdef add-ns
        :args (s/cat :namespace ::keyword-or-simple-string
                     :keyword   ::keyword-or-simple-string)
        :ret  qualified-keyword?)

(defn- update-all
  "Given a map and a function of entry (coll of two elems) to entry, applies the
  function recursively to every entry in the map."
  {:fork-of 'clojure.walk/stringify-keys}
  [f m]
  (postwalk
   (fn [x]
     (if (map? x)
       (into {} (map f x))
       x))
   m))

(s/def ::map-entry
  (s/tuple any? any?))

(s/fdef update-all
        :args (s/cat :fn (s/fspec :args (s/cat :entry ::map-entry)
                                  :ret  ::map-entry)
                     :map map?)
        :ret map?)

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn- qualify-keys
  "Given a nested map with keyword keys, qualifies all keys, recursively, with
  the current namespace."
  [m]
  (update-all
   (fn [[k v]]
     (if (and (keyword? k)
              (not (qualified-keyword? k)))
       [(add-ns this-ns-name k) v]
       [k v]))
   m))

(s/fdef qualify-keys
        :args (s/cat :map (s/map-of keyword? any?))
        :ret  (s/map-of qualified-keyword? any?))

(defn- to-set-of-keywords
  [xs]
  (-> (map keyword xs) set))

(s/fdef to-set-of-keywords
        :args (s/cat :xs (s/coll-of string?))
        :ret  (s/coll-of keyword? :kind set?)
        :fn (fn [{{:keys [xs]} :args, ret :ret}]
              (= (count (distinct xs)) (count ret))))

(defn- fixup-element
  [tags-from-path elem]
  (-> elem
      qualify-keys
      (update ::repos to-set-of-keywords)
      (update ::tags to-set-of-keywords)
      (update ::tags (partial union tags-from-path))))

(s/def ::simple-strings
  (s/coll-of ::short-non-blank-simple-str))

(s/def ::unqualified-keyword
  (s/and keyword? (complement qualified-keyword?)))

(s/def ::proto-element
  (s/with-gen
    (s/map-of ::unqualified-keyword (s/or :name    ::name
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
    ::non-blank-str
    #(gen/fmap yaml/generate-string (s/gen ::element))))

(s/def ::elements-yaml-string
  (s/with-gen
    ::non-blank-str
    #(gen/fmap yaml/generate-string (s/gen (s/coll-of ::element)))))

(s/def ::yaml-file-contents
  (s/with-gen
    ::non-blank-str
    #(gen/one-of (map s/gen [::element-yaml-string ::elements-yaml-string]))))

(s/fdef elements-from-file
        :args (s/cat :file-contents ::yaml-file-contents
                     :file-path     ::dir-path-str
                     :root-path     ::dir-path-str)
        :ret  (s/coll-of ::element))
