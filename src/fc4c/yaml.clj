(ns fc4c.yaml
  (:require [clojure.spec.alpha  :as s]))

(defn split-file
  "Accepts a string containing either a single YAML document, or a YAML document
  and front matter (which itself is a YAML document). Returns a seq like
  [front? main] wherein front? may be nil if the input string does not contain
  front matter, or does not contain a valid separator. In that case main may or
  may not be a valid YAML document, depending on how mangled the document
  separator was."
  [s]
  (let [matcher (re-matcher #"(?ms)((?<front>.+)\n---\n)?(?<main>.+)\Z" s)
        _ (.find matcher)
        front (.group matcher "front")
        main (.group matcher "main")]
    [front main]))

(s/fdef split-file
  :args (s/cat :s string?)
  :ret  (s/tuple #(or (string? %) (nil? %))
                 string?))
