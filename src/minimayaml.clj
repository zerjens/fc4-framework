#!/usr/local/bin/clojure

(ns minimayaml
  (:require [clj-yaml.core :as yaml :refer [parse-string generate-string]]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str :refer [blank? join]]
            [clojure.walk :as walk :refer [postwalk]]))

(defn split-front-matter
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a seq containing 2
  strings. If the input string does not contain front matter, or does not
  contain a valid separator, the first string will be blank. In that case the
  second string may or may not be a valid YAML document, depending on how
  mangled the document separator was."
  [s]
  (let [split (str/split s #"\n[-]{3}\n" 2)]
     (if (= (count split) 2)
         split
         ["" (first split)])))

(defn blank-nil-or-empty? [v]
  (or (nil? v)
      (and (coll? v)
           (empty? v))
      (and (string? v)
           (blank? v))))

(defn shrink
  "Remove key-value pairs wherein the value is blank, nil, or empty from a
  (possibly nested) map. Also transforms maps to nil if all of their values are
  nil, blank, or empty.
  
  Adapted from https://stackoverflow.com/a/29363255/7012"
  [nm]
  (postwalk (fn [el]
              (if (map? el)
                  (let [m (into {} (remove (comp blank-nil-or-empty? second) el))]
                    (when (seq m) m))
                  el))
            nm))

(defn sort-by-specified-keys
  "Accepts an ordered map and a seq of keys; returns a new ordered map
  containing the specified keys and their corresponding values from the input
  map, in the same order as the specified keys."
  [m ks]
  (into (ordered-map)
        (map #(vector % (get m %))
             ks)))

(defn sort-structurizr
  "Accepts a map representing a parsed Structurizr YAML document, as parsed by
  clj-yaml. Returns the same map with its top-level kv-pairs sorted with a
  custom sort, and second-level nodes sorted alphabetically by the names of the
  things they describe. e.g. for elements, by their name. For relationships, by
  the source and then destination."
  [d]
  ;; TODO: NOT CURRENTLY WORKING
  ;; TODO: sort elements and relationships
  (-> d))
    ;  (sort-by-specified-keys [:type :scope :description :elements :relationships :styles])))

(defn fixup [s]
  (-> s (str/replace #"(\d+,\d+)" "'$1'")))

(defn process-structurizr-doc-string [s]
  (-> s
      parse-string
      shrink
      sort-structurizr
      (generate-string :dumper-options {:flow-style :block})
      fixup))

(defn process-file [s]
  (let [[front main] (split-front-matter s)
        main-processed (process-structurizr-doc-string main)]
    (str front "\n---\n" main-processed)))

(defn -main []
  (->> (slurp *in*)
       process-file
       print)
  (flush)
  (Thread/sleep 10))
