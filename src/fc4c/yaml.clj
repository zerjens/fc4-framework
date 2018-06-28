(ns fc4c.yaml
  (:require [clojure.spec.alpha  :as s]))

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

(s/def ::front (s/nilable string?))
(s/def ::main string?)

(s/fdef split-file
  :args (s/cat :file-contents string?)
  :ret  (s/keys :req [::front ::main]))
