(ns fc4.yaml
  (:require [clj-yaml.core           :as yaml :refer [generate-string]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string          :as string :refer [blank? includes? join]]
            [fc4.spec                :as fs]))

(defn split-file
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a map containing
  ::front and ::main, wherein the value of each will be either nil or a string;
  said string might contain a valid YAML document. If the input value is an
  empty string, the value of ::main will be an empty string."
  [file-contents]
  (let [matcher (re-matcher #"(?ms)((?<front>.+)\n---\n)?(?<main>.+)\Z"
                            file-contents)
        has-matches (.find matcher)
        [front main] (if has-matches
                       [(.group matcher "front") (.group matcher "main")]
                       [nil ""])]
    {::front front
     ::main  main}))

(def doc-separator "\n---\n")

(s/def ::yaml-file-string
  (s/with-gen ::fs/non-blank-str
    (let [sg (s/gen ::fs/non-blank-str)
          dsg (gen/return doc-separator)]
      #(gen/one-of [sg
                    (gen/fmap join (gen/tuple sg dsg sg))]))))

(s/def ::front (s/nilable string?))
(s/def ::main string?)

(s/fdef split-file
  :args (s/cat :v (s/alt :non-blank ::yaml-file-string
                         :blank     ::fs/blank-str))
  :ret  (s/keys :req [::front ::main])
  :fn (fn [{{[arg-tag arg-val] :v} :args, ret :ret}]
        (case arg-tag
          :non-blank
          (and (not (nil? (::main ret)))
               (if (includes? arg-val doc-separator)
                 (not (nil? (::front ret)))
                 (nil? (::front ret))))

          :blank
          (= ret {::front nil ::main ""}))))

(defn stringify
  "Accepts a map, converts it to a YAML string with a certain flow-style."
  [m]
  (generate-string m :dumper-options {:flow-style :block}))
