(ns minimayaml
  (:require [clj-yaml.core :as yaml :refer [parse-string generate-string]]
            [clojure.string :as str :refer [blank? join]]
            [clojure.walk :as walk :refer [postwalk]]))

(def doc-separator "\n---\n")

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

; (defn non-empty-coll? [v]
;   (and (coll? v)
;        (not (empty? v))))

(defn blank-nil-or-empty? [v]
  (or (nil? v)
      (and (coll? v)
           (empty? v))
      (and (string? v)
           (blank? v))))

(defn shrink2
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil" 
  [nm]
  (postwalk (fn [el]
              (if (map? el)
                (let [m (into (sorted-map) (remove (comp blank-nil-or-empty? second) el))]
                  (when (seq m)
                    m))
                el))
            nm))

(defn shrink
  "Accepts a map representing a parsed YAML document, as parsed by clj-yaml.
  Returns the same map with all empty/blank nodes/entries removed."
  [d]
  (postwalk #(when-not
               (and (coll? %)
                    (= (count %) 2)
                    (or (empty? (second %))
                        (and (string? (second %))
                             (blank? (second %)))))
               %)               
            d))

(def clean identity)

(defn sort-structurizr
 "Accepts a map representing a parsed Structurizr YAML document, as parsed by clj-yaml.
  Returns the same map with its second-level nodes sorted alphabetically."
 [d]
 d) ;;TODO

(defn clean-and-shrink [yaml-doc-str]
  (-> yaml-doc-str
      parse-string 
      shrink2
      clean
      sort-structurizr
      generate-string))

(defn process [s]
  (->> s
    split-front-matter
    (map clean-and-shrink)
    (join doc-separator)))

(defn -main []
  (->> (slurp *in*)
       process
       print)
  (flush)
  (Thread/sleep 100))
