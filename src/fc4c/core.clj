#!/usr/local/bin/clojure

(ns fc4c.core
  (:require [clj-yaml.core :as yaml]
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str :refer [blank? join]]
            [clojure.walk :as walk :refer [postwalk]]
            [clojure.set :refer [difference intersection]]))

(def default-front-matter
  (str "link__for_use_with: https://structurizr.com/express\n"
       "link__diagram_scheme_description: https://c4model.com/"))

(defn split-file
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a seq like
  [front? main] wherein front? may be nil if the input string does not contain
  front matter, or does not contain a valid separator. In that case main may or
  may not be a valid YAML document, depending on how mangled the document
  separator was."
  [s]
  (let [matcher (re-matcher #"(?ms)((?<front>.+)---\n)?(?<main>.+)\Z" s)
        _ (.find matcher)
        front (.group matcher "front")
        main (.group matcher "main")]
    [front main]))

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
  "Reorder a map as per a seq of keys.
  
  Accepts a seq of keys and a map; returns a new ordered map containing the
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
        all-keys-in-order (concat ks (sort unspecified-but-present-keys))]
    (into (ordered-map)
          (map (juxt identity (partial get m))
               all-keys-in-order))))

(def desired-order
  {:root          {:sort-keys nil
                   :key-order [:type :scope :description :elements
                               :relationships :styles :size]}
   :elements      {:sort-keys [:type :name]
                   :key-order [:type :name :description :tags :position
                               :containers]}
   :relationships {:sort-keys [:source :destination]
                   :key-order [:source :description :destination :technology
                               :vertices :order]}
   :styles        {:sort-keys [:type :tag]
                   :key-order [:type :tag]}})

(defn reorder-structurizr
  "Apply desired order/sort to diagram keys and values.
  
  Accepts a map representing a parsed Structurizr YAML document, as parsed by
  clj-yaml. Returns the same map with custom ordering/sorting applied to the
  root-level key-value pairs and many of the nested sequences of key-value
  pairs as per desired-order."
  [diagram]
  (reduce
    (fn [d [key {:keys [sort-keys key-order]}]]
      (if (= key :root)
          (reorder key-order d)
          (update-in d [key]
            #(->> (sort-by (comp join (apply juxt sort-keys)) %)
                  (map (partial reorder key-order))))))
    diagram
    desired-order))

(defn parse-coords [s]
  (some->> s
           (re-find #"^(-?\d+), ?(-?\d+)$")
           (drop 1)
           (map #(Integer/parseInt %))))

(defn round-to-closest [target n]
  (-> (/ n (float target))
      Math/round
      (* target)))

(def elem-offsets
  {"Person" [25, -50]})

(defn snap-coords
  "Accepts a seq of X and Y numbers, and config values and returns a string in
  the form \"x,y\"."
  ([coords to-closest min-margin]
   (snap-coords coords to-closest min-margin (repeat 0)))
  ([coords to-closest min-margin offsets]
   (->> coords
        (map (partial round-to-closest to-closest))
        (map (partial max min-margin)) ; minimum left/top margins
        (map + offsets)
        (join ","))))

(defn snap-elem-to-grid
  "Accepts an ordered map representing an element (a software system, person, container, or
  component) and snaps its position (coords) to a grid using the specified values."
  [e to-closest min-margin]
  (let [coords (parse-coords (:position e))
        offsets (get elem-offsets (:type e) (repeat 0))
        new-coords (snap-coords coords to-closest min-margin offsets)]
    (assoc e :position new-coords)))

(defn snap-vertices-to-grid
  "Accepts an ordered-map representing a relationship, and snaps its vertices, if any, to a grid
  using the specified values."
  [e to-closest min-margin]
  (assoc e :vertices
    (map #(snap-coords (parse-coords %) to-closest min-margin)
         (:vertices e))))

(def elem-types
  #{"Person" "Software System" "Container" "Component"})

(defn snap-to-grid
  "Accepts a parsed structurizr doc, a grid-size number, and a min-margin number. Searches the doc
  for elements and adjusts their positions so as to effectively “snap” them to a virtual grid of
  the specified size, and to ensure that each coord is no “smaller” than the min-margin number.
  Accounts for a quirk of Structurizr Express wherein elements of type “Person” need to be offset
  from other elements in order to align properly with them."
  [d to-closest min-margin]
  (postwalk
    #(cond
       (and (contains? elem-types (:type %))
             ; Checking for :position alone wouldn’t be sufficient; relationships can also have it
             ; and it means something different for them.
            (:position %))
       (snap-elem-to-grid % to-closest min-margin)
 
       (:vertices %)
       (snap-vertices-to-grid % (/ to-closest 2) min-margin)
 
       :else
       %)
    d))

(defn fixup-structurizr [s]
  (-> s
    (str/replace #"(\d+,\d+)" "'$1'")
    (str/replace #"(elements|relationships|styles|size):" "\n$1:")
    (str/replace #"(description): Uses\n" "$1: uses\n")))

(defn process-structurizr-doc [d]
  (-> (reorder-structurizr d)
      (snap-to-grid 100 50)
      shrink)) ; must follow reorder-structurizr because that tends to introduce new keys with nil values
      
(defn stringify-structurizr-doc [d]
   (-> (yaml/generate-string d :dumper-options {:flow-style :block})
       fixup-structurizr))

(defn process-file
  "Accepts a string containing either a single YAML document, or a YAML document and front matter
  (which itself is a YAML document). Returns a seq containing in the first position the fully
  processed main document as an ordered-map, and in the second a string containing first some front
  matter, the front matter separator, and then the fully processed main document."
  [s]
  (let [[front? main] (split-file s)
        main-processed (-> main
                           yaml/parse-string
                           process-structurizr-doc)
        str-output (str (or front? default-front-matter)
                        "\n---\n"
                        (stringify-structurizr-doc main-processed))]
    [main-processed str-output]))

(defn -main []
  (-> (slurp *in*)
      process-file
      second
      print)
  (flush)
  (Thread/sleep 10))
