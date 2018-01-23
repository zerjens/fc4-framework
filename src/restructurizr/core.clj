#!/usr/local/bin/clojure

(ns restructurizr.core
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
  (let [specified-keys (set ks) ; reminder: this set is unordered.
        present-keys (set (keys m)) ; reminder: this set is unordered.
        unspecified-but-present-keys (difference present-keys specified-keys)
        ; The below starts with ks because the above sets don’t retain order. I
        ; tried using flatland.ordered.set but the difference and intersection
        ; functions from clojure.set did not work as expected with those. This
        ; means this function won’t filter out keys that are specified but not
        ; present, and therefore those keys will be present in the output map with
        ; nil values. This is acceptable to me; I can work with it.
        all-keys-in-order (concat ks (sort-by identity unspecified-but-present-keys))]
    (into (ordered-map)
          (map #(vector % (get m %))
               all-keys-in-order))))

(defn join-juxt-fn [& ks]
  (let [jfn (apply juxt ks)]
    (fn [item] (join (jfn item)))))

(defn reorder-structurizr
  "Accepts a map representing a parsed Structurizr YAML document, as parsed by
  clj-yaml. Returns the same map with its top-level kv-pairs sorted with a
  custom sort, and second-level nodes sorted alphabetically by the names of the
  things they describe. e.g. for elements, by their type then name; for
  relationships, by the source and then destination."
  [doc]
  (as-> doc d
     (reorder [:type :scope :description :elements :relationships :styles :size] d)
     ;; TODO: this calls for a more declarative style; some kind of “spec” data structure that declares all this
     (update-in d [:elements] #(sort-by (join-juxt-fn :type :name) %))
     (update-in d [:elements] #(map (partial reorder [:type :name :description :tags :position :containers]) %))
     (update-in d [:relationships] #(sort-by (join-juxt-fn :source :destination) %))
     (update-in d [:relationships] #(map (partial reorder [:source :description :destination :technology :vertices :order]) %))
     (update-in d [:styles] #(sort-by (join-juxt-fn :type :tag) %))
     (update-in d [:styles] #(map (partial reorder [:type :tag]) %))))

(defn round-to-closest [target n]
  (-> (/ n (float target))
      Math/round
      (* target)))

; ;; TODO: This was superceded by snap-to-grid but keeping it around because we might be able to use
; ;; this to “snap” the relationship vertices.
; (defn round-coords [d target min-margin]
;   (postwalk
;     (fn [e]
;       (if-let [[_ x y] (when (string? e)
;                          (re-find #"^(-?\d+), ?(-?\d+)$" e))]
;         (->> [x y]
;              (map #(Integer/parseInt %))
;              (map (partial round-to-closest target))
;              (map (partial max min-margin)) ; minimum left/top margins
;              (join ","))
;         e))
;     d))

(def person-offsets {:x 25, :y -50})

(defn snap-to-grid
  "Accepts a parsed structurizr doc, a grid-size number, and a min-margin number. Searches the doc
  for elements and adjusts their positions so as to effectively “snap” them to a virtual grid of
  the specified size, and to ensure that each coord is no “smaller” than the min-margin number.
  Accounts for a quirk of Structurizr Express wherein elements of type “Person” need to be offset
  from other elements in order to align properly with them."
  ;; TODO: this does not currently “snap” the coords of relationship vertices
  [d to-closest min-margin]
  (postwalk
    (fn [e]
      ; not all values of :position have an x and a y — relationship positions have a single value
      (if-let [coords (when-let [pos (:position e)]
                         (re-find #"^(-?\d+), ?(-?\d+)$" pos))]
        (let [[_ x y] coords
              offsets (case (:type e) "Person" (vals person-offsets) [0 0])
              new-coords (->> [x y]
                              (map #(Integer/parseInt %))
                              (map (partial round-to-closest to-closest))
                              (map (partial max min-margin)) ; minimum left/top margins
                              (map (partial +) offsets)
                              (join ","))]
          (assoc e :position new-coords))
        e))
    d))

(defn fixup-structurizr [s]
  (-> s
    (str/replace #"(\d+,\d+)" "'$1'")
    (str/replace #"(elements|relationships|styles|size):" "\n$1:")
    (str/replace #"(description): Uses\n" "$1: uses\n")))

(defn process-structurizr-doc-string [s]
  (-> s
      parse-string
      reorder-structurizr
      (snap-to-grid 100 50)
      shrink ; must follow reorder-structurizr because that tends to introduce new keys with nil values
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
