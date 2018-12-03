(ns fc4.integrations.structurizr.express.util
  (:require [clojure.spec.alpha  :as s]
            [clojure.string      :as str  :refer [includes?]]))

;; We require this namespace for the side-effect of registering specs.
(require '[fc4.integrations.structurizr.express.spec])

;; You could make a pretty good case that this should be in
;; fc4.integrations.structurizr.express.yaml, and in fact I tried putting it
;; there but I encountered the circular dependency problem. Yay for `util`
;; namespaces! SO much better than `misc`.
(defn probably-diagram-yaml?
  "A fast and efficient but cursory check of whether a string seems likely to
  contain a Structurizr Express diagram definition."
  [s]
  (and (includes? s "type")
       (includes? s "scope")))

(s/fdef probably-diagram-yaml?
        :args (s/cat :v (s/or :is-diagram     :structurizr/diagram-yaml-str
                              :is-not-diagram string?))
        :ret  boolean?)
