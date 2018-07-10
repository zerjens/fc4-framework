(ns fc4c.model
  (:require [clj-yaml.core           :as yaml]
            [clojure.set                       :refer [union]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string                    :refer [includes? split]]
            [fc4c.files                        :refer [relativize]]
            [fc4c.spec               :as fs]))

(load "model_specs")

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
  (as-> (or (relativize file-path relative-root)
            (str file-path)) v
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
        :args (s/cat :file-path     ::fs/file-path
                     :relative-root ::fs/dir-path)
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
      (update ::tags (partial union tags-from-path))
      (update ::containers (fn [containers]
                             (map #(update % ::tags to-set-of-keywords)
                                  containers)))))

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

(s/fdef elements-from-file
        :args (s/cat :file-contents ::yaml-file-contents
                     :file-path     ::fs/file-path
                     :root-path     ::fs/dir-path)
        :ret  (s/coll-of ::element))
