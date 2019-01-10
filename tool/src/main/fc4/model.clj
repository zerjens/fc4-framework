(ns fc4.model
  (:require [fc4.util :as fu :refer [qualify-keys]]))

(load "model_specs")

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn empty-model
  []
  {::systems {} ::users {} ::datastores {}})

(defn add-file-contents
  "Adds the elements from a parsed model file to a model."
  [model parsed-file-contents]
  (reduce
   (fn [model [src dest]]
     (update model dest merge (get parsed-file-contents src {})))
   model
   [[:system     ::systems]
    [:systems    ::systems]
    [:user       ::users]
    [:users      ::users]
    [:datastore  ::datastores]
    [:datastores ::datastores]]))

(defn build-model
  "Accepts a sequence of maps read from model YAML files and combines them into
  a single model map. Does not validate the result."
  [file-content-maps]
  (reduce add-file-contents (empty-model) file-content-maps))
