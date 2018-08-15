(ns fc4c.yaml
  (:require [clj-yaml.core           :as yaml :refer [generate-string]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string          :as string :refer [includes? join]]
            [fc4c.spec               :as fs]))

(defn split-file
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a map containing
  ::front and ::main, wherein the value of each will be either nil or a string;
  said string might contain a valid YAML document."
  [file-contents]
  (let [matcher (re-matcher #"(?ms)((?<front>.+)\n---\n)?(?<main>.+)\Z"
                            file-contents)
        _ (.find matcher)
        front (.group matcher "front")
        main (.group matcher "main")]
    {::front front
     ::main  main}))

(def ^:private doc-separator "\n---\n")

(s/def ::yaml-file-string
  (let [sg (s/gen ::fs/non-blank-str)
        dsg (gen/return doc-separator)]
    (s/with-gen
      ::fs/non-blank-str
      #(gen/one-of [sg
                    (gen/fmap join (gen/tuple sg dsg sg))]))))

(s/def ::front (s/nilable string?))
(s/def ::main string?)

(s/fdef split-file
        :args (s/cat :file-contents ::yaml-file-string)
        :ret  (s/keys :req [::front ::main])
        :fn (fn [{{:keys [file-contents]} :args, ret :ret}]
              (and (not (nil? (::main ret)))
                   (if (includes? file-contents doc-separator)
                     (not (nil? (::front ret)))
                     (nil? (::front ret))))))

(defn stringify
  "Accepts a map, converts it to a YAML string with a certain flow-style."
  [m]
  (generate-string m :dumper-options {:flow-style :block}))
