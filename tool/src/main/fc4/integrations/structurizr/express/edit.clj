(ns fc4.integrations.structurizr.express.edit
  "Functions that assist with editing Structurizr Express diagrams, which are
  serialized as YAML documents."
  (:require [fc4.integrations.structurizr.express.spec :as ss]
            [fc4.integrations.structurizr.express.yaml :as sy :refer [stringify]]
            [fc4.spec :as fs]
            [fc4.util :as fu :refer [namespaces]]
            [fc4.yaml :as fy :refer [split-file]]
            [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.gfredericks.test.chuck.generators :as gen']
            [flatland.ordered.map :refer [ordered-map]]
            [clojure.string :as str :refer [blank? ends-with? includes? join
                                            split trim]]
            [clojure.walk :as walk :refer [postwalk]]
            [clojure.set :refer [difference intersection]])
  (:import [flatland.ordered.map OrderedMap]))

(namespaces '[structurizr :as st])

(def default-front-matter
  (str "links:\n"
       "  The FC4 Framework: https://fundingcircle.github.io/fc4-framework/\n"
       "  Structurizr Express: https://structurizr.com/express"))

(defn blank-nil-or-empty? [v]
  (or (nil? v)
      (and (coll? v)
           (empty? v))
      (and (string? v)
           (blank? v))))

(s/fdef blank-nil-or-empty?
        :args (s/cat :v (s/or :nil nil?
                              :coll (s/coll-of any?
                                         ; for this fn it matters only if the coll is empty or not
                                               :gen-max 1)
                              :string string?))
        :ret boolean?
        :fn (fn [{:keys [args ret]}]
              (let [[which v] (:v args)]
                (case which
                  :nil
                  (= ret true)

                  :coll
                  (if (empty? v)
                    (= ret true)
                    (= ret false))

                  :string
                  (if (blank? v)
                    (= ret true)
                    (= ret false))))))

(defn shrink
  "Recursively remove map entries with a blank, nil, or empty value.

  Also replaces maps with nil if all of their values are nil, blank, or empty;
  the traversal is depth-first, so if that nil is the value of a map entry then
  that map entry will then be removed.

  Adapted from https://stackoverflow.com/a/29363255/7012"
  [diagram]
  (postwalk (fn [el]
              (if (map? el)
                (let [m (into (empty el) ; using empty to preserve ordered maps 
                              (remove (comp blank-nil-or-empty? second) el))]
                  (when (seq m) m))
                el))
            diagram))

(s/fdef shrink
        :args (s/cat :in ::st/diagram)
        :ret  ::st/diagram
        :fn (s/and
             (fn [{{in :in} :args, ret :ret}] (= (type in) (type ret)))
             (fn [{{in :in} :args, ret :ret}]
               (let [all-vals #(if (map? %) (vals %) %) ; works on maps or sequences
                     leaf-vals #(->> (tree-seq coll? all-vals %)
                                     (remove coll?)
                                     flatten)
                     in-vals (->> (leaf-vals in) (filter (complement blank-nil-or-empty?)) set)
                     ret-vals (->> (leaf-vals ret) set)]
                 (= in-vals ret-vals)))))

(defn reorder
  "Reorder a map as per a seq of keys.
  
  Accepts a seq of keys and a map; returns a new ordered map containing the
  specified keys and their corresponding values from the input map, in the same
  order as the specified keys. If any keys present in the input map are omitted
  from the seq of keys, the corresponding k/v pairs will be sorted “naturally”
  after the specified k/v pairs."
  [m ks]
  ; You might think it’d be reasonable to also have a precondition like
  ; `(>= (count m) 2)` because why bother reordering the keys of an empty map,
  ; or one with 1 key? But no, I don’t think that’d be a good idea, because I
  ; suspect there’s a good chance that all sorts of instances of various maps
  ; might be passed to this function — maps that we can’t know about ahead of
  ; time. In other words, I suspect the data will vary at runtime; some maps
  ; might have a dozen keys, and some might have only 1. As long as the maps are
  ; valid according to their own criteria, I think this function should pass
  ; them through unchanged.
  {:pre [(seq ks)]}
  (let [specified-keys (set ks) ; reminder: this set is unordered.
        present-keys (set (keys m)) ; reminder: this set is unordered.
        unspecified-but-present-keys (difference present-keys specified-keys)
        ; The below starts with ks because the above sets don’t retain order. I
        ; tried using flatland.ordered.set but the difference and intersection
        ; functions from clojure.set did not work as expected with those.
        all-keys-in-order (concat ks (sort unspecified-but-present-keys))]
    (->> all-keys-in-order
         (map (juxt identity (partial get m)))
         ; we want the output to contain the same keys as the input
         (filter (fn [[k _]] (present-keys k)))
         (into (ordered-map)))))

(s/fdef reorder
        :args (s/cat :m  (s/map-of simple-keyword? any?)
                     :ks (s/coll-of simple-keyword?
                                    :min-count 2
                                    :distinct true))
        :ret  (s/and (s/map-of simple-keyword? any?)
                     (partial instance? OrderedMap))
        :fn   (fn [{{:keys [m ks]} :args, ret :ret}]
                (let [kss (set ks)]
                  (and
                    ; Yeah, OrderedMaps are equal to maps with the same entries
                    ; regardless of order — surprised me too!
                   (= m ret)
                   (= (or (keys ret) []) ; keys on empty map returns nil
                      (concat (filter (partial contains? m) ks)
                              (sort (remove (partial contains? kss)
                                            (keys m)))))))))

(def desired-order
  {:root          {:sort-keys nil
                   :key-order [:type :scope :description :elements
                               :relationships :styles :size]}
   :elements      {:sort-keys [:type :name]
                   :key-order [:type :name :description :tags :position
                               :containers]}
   :relationships {:sort-keys [:order :source :destination]
                   :key-order [:order :source :description :destination
                               :technology :vertices]}
   :styles        {:sort-keys [:type :tag]
                   :key-order [:type :tag]}})

(defn reorder-diagram
  "Apply desired order/sort to diagram keys and values.
  
  Accepts a diagram as a map. Returns the same map with custom ordering/sorting applied to the
  root-level key-value pairs and many of the nested sequences of key-value
  pairs as per desired-order."
  [diagram]
  (reduce
   (fn [d [key {:keys [sort-keys key-order]}]]
     (if (= key :root)
       (reorder d key-order)
       (update-in d [key]
                  (fn [v] (->> (sort-by (comp join (apply juxt sort-keys)) v)
                               (map #(reorder % key-order)))))))
   diagram
   desired-order))

(def coord-pattern (re-pattern (str "^" fs/coord-pattern-base "$")))

(defn parse-coords [s]
  (some->> s
           (re-find coord-pattern)
           (drop 1)
           (map #(Integer/parseInt %))))

(s/fdef parse-coords
        :args (s/cat :s ::fs/coord-string)
        :ret (s/coll-of ::fs/coord-int :count 2)
        :fn (fn [{:keys [ret args]}]
              (= ret
                 (->> (split (:s args) #",")
                      (map trim)
                      (map #(Integer/parseInt %))))))

(defn round-to-closest
  "Accepts any natural int n and rounds it to the closest target. If the rounded
  value exceeds the max coord value of fs/max-coord-int then the max value will
  be returned."
  [target n]
  (if (or (zero? target) (zero? n))
    0
    (min fs/max-coord-int
         (-> (/ n (float target))
             (Math/round)
             (* target)))))

(s/def ::snap-target #{10 25 50 75 100})

(s/fdef round-to-closest
        :args (s/cat :target ::snap-target
                     :n      ::fs/coord-int)
        :ret ::fs/coord-int
        :fn (fn [{{:keys [target n]} :args
                  ret :ret}]
              (or (zero? ret)
                  (zero? (rem ret target))
                  (= ret fs/max-coord-int))))

(def elem-offsets
  {"Person" [25 -50]
   :default [0 0]})

(defn get-offsets
  [elem-type]
  (get elem-offsets elem-type (:default elem-offsets)))

(defn snap-coords
  "Accepts a seq of X and Y numbers, and config values and returns a string in
  the form \"x,y\"."
  ; TODO: it’s inconsistent that the min-margin is passed in as an arg but the
  ; max margin is referenced from a var in the spec namespace. (My idea to fix
  ; this is to define a new map named something like ::snap-config that would
  ; contain the target, margins, and offsets — and this single value could then
  ; be threaded through, rather than threading a bunch of scalar values through
  ; (see below how often e.g. min-margin is threaded through various function
  ; calls).
  ([coords to-closest min-margin]
   (snap-coords coords to-closest min-margin (repeat 0)))
  ([coords to-closest min-margin offsets]
   (->> coords
        (map (partial round-to-closest to-closest))
        (map + offsets)
        (map (partial max min-margin))       ; minimum left/top margins
        (map (partial min fs/max-coord-int)) ; maximum right/bottom margins
        (join ","))))

(s/fdef snap-coords
        :args (s/cat :coords     (s/coll-of ::fs/coord-int :count 2)
                     :to-closest ::snap-target
                     :min-margin ::fs/coord-int
                     :offsets    (s/? (s/coll-of (s/int-in -50 50) :count 2)))
        :ret ::fs/coord-string
        :fn (fn [{:keys [ret args]}]
              (let [parsed-ret (parse-coords ret)
                    {:keys [:to-closest :min-margin]} args]
                (every? #(>= % min-margin) parsed-ret))))

(defn snap-elem-to-grid
  "Accepts an element (a software system, person, container, or
  component) as a map and snaps its position (coords) to a grid using the
  specified values."
  [elem to-closest min-margin]
  (update elem :position
          #(let [coords (parse-coords %)
                 offsets (get-offsets (:type elem))]
             (snap-coords coords to-closest min-margin offsets))))

(defn- snap-elem-to-grid-fdef-pred
  "This is in a var because it’s just too big+long to inline."
  [{{elem-in-conformed :elem
     min-margin        :min-margin} :args
    elem-out-conformed              :ret}]
  (let [elem-in (s/unform ::st/element-with-position elem-in-conformed)
        elem-out (s/unform ::st/element-with-position elem-out-conformed)
        out-coords (parse-coords (:position elem-out))]
    (and (= (keys elem-out) (keys elem-in))
         (every? #(or (= % min-margin)
                      (= % fs/max-coord-int)
                      (= % (+ fs/max-coord-int (-> (get-offsets (:type elem-in))
                                                   (second))))
                      (zero? (rem % 5)))
                 out-coords))))

(s/fdef snap-elem-to-grid
        :args (s/cat :elem       ::st/element-with-position
                     :to-closest ::snap-target
                     :min-margin (s/int-in 0 500))
        :ret  ::st/element-with-position
        :fn   snap-elem-to-grid-fdef-pred)

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
  "Accepts a diagram as a map, a grid-size number, and a min-margin number. Searches the doc
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

(defn probably-diagram-yaml? [v]
  (and (string? v)
       (includes? v "type")
       (includes? v "scope")))

(defn process
  "Accepts a diagram as a map; reorders everything, snaps all coordinates to a
  virtual grid, and removes all empty/blank nodes."
  [d]
  (-> (reorder-diagram d)
      (snap-to-grid 100 50)
      shrink)) ; must follow reorder-diagram because that tends to introduce new keys with nil values

(s/fdef process
        :args (s/cat :in ::st/diagram)
        :ret  ::st/diagram)

(defn process-file
  "Accepts a string containing either a single YAML document, or a YAML document and front matter
  (which itself is a YAML document). Returns a map containing:

  * ::main-processed: the fully processed main document as an ordered-map
  * ::str-processed: a string containing first some front matter, then the front
                     matter separator, then the fully processed main document"
  [s]
  (let [{:keys [::fy/front ::fy/main]} (split-file s)
        main-processed (process (yaml/parse-string main))]
    {::main-processed main-processed
     ::str-processed (str (trim (or front default-front-matter))
                          "\n---\n"
                          (stringify main-processed))}))

(defmacro sometimes [body]
  `(when (< (rand) 0.5)
     ~body))

(s/def ::diagram-yaml-str
  (s/with-gen
    (s/and string?
           #(not (re-seq #"\n\n---\n" %)) ; prevent extra blank line
           (fn [s]
             (let [parsed (-> s split-file ::fy/main yaml/parse-string)]
               (every? #(contains? parsed %) [:type :scope :description
                                              :elements :size]))))
    #(gen/fmap
      (fn [diagram]
        (str (sometimes (str default-front-matter "\n---\n"))
             (stringify diagram)))
      (s/gen ::st/diagram))))

(s/def ::main-processed ::st/diagram)
(s/def ::str-processed ::diagram-yaml-str)

(s/fdef process-file
        :args (s/cat :in ::diagram-yaml-str)
        :ret  (s/keys :req [::main-processed ::str-processed]))
