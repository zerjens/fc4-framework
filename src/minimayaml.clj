#!/usr/local/bin/clojure

(ns minimayaml
  (:require [clj-yaml.core :as yaml :refer [parse-string generate-string]]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str :refer [blank? join]]
            [clojure.walk :as walk :refer [postwalk]]
            [clojure.set :refer [difference intersection]]))

(defn split-file
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

(defn reorder
  "Accepts a seq of keys and a map; returns a new ordered map containing the
  specified keys and their corresponding values from the input map, in the same
  order as the specified keys. If any keys present in the input map are omitted
  from the seq of keys, the corresponding k/v pairs will be sorted “naturally”
  after the specified k/v pairs."
  [ks m]
  (let [specified-keys (set ks)
        present-keys (set (keys m))
        specified-and-present-keys (intersection specified-keys present-keys)
        unspecified-but-present-keys (difference present-keys specified-keys)
        all-keys-in-order (concat specified-and-present-keys (sort-by identity unspecified-but-present-keys))]
    (into (ordered-map)
          (map #(vector % (get m %))
              all-keys-in-order))))

(defn join-juxt-fn [& ks]
  (let [jfn (apply juxt ks)]
    (fn [item] (join (jfn item)))))

(defn sort-structurizr
  "Accepts a map representing a parsed Structurizr YAML document, as parsed by
  clj-yaml. Returns the same map with its top-level kv-pairs sorted with a
  custom sort, and second-level nodes sorted alphabetically by the names of the
  things they describe. e.g. for elements, by their type then name; for
  relationships, by the source and then destination."
  [doc]
  (as-> doc d
     (reorder [:type :scope :description :elements :relationships :styles :size] d)
     (update-in d [:elements] #(sort-by (join-juxt-fn :type :name) %))
     (update-in d [:elements] #(map (partial reorder [:type :name :description :tags :position])
                                    %))
     (update-in d [:relationships] #(sort-by (join-juxt-fn :source :destination) %))
     (update-in d [:relationships] #(map (partial reorder [:source :description :destination :technology :vertices :order])
                                         %))))     

(defn fixup-structurizr [s]
  (-> s
    (str/replace #"(\d+,\d+)" "'$1'")
    (str/replace #"(elements|relationships|styles|size):" "\n$1:")
    (str/replace #"(description): Uses\n" "$1: uses\n")
    ;; The below approach is brittle/iffy; TODO: look into using a custom SnakeYAML formatter instead.
    (str/replace #"\n- (.+)\n" "\n-\n  $1\n")))

(defn process-structurizr-doc-string [s]
  (-> s
      parse-string
      shrink
      sort-structurizr
      (generate-string :dumper-options {:flow-style :block})
      fixup-structurizr))

(defn process-file [s]
  (let [[front main] (split-file s)
        main-processed (process-structurizr-doc-string main)]
    (str front "\n---\n" main-processed)))

(defn -main []
  (->> (slurp *in*)
       process-file
       print)
  (flush)
  (Thread/sleep 10))
