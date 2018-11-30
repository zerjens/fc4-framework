(ns fc4.integrations.structurizr.express.util
  (:require [clojure.string  :as str :refer [includes?]]))

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
