(ns fc4.model
  (:require [fc4.util :as fu :refer [qualify-keys]]))

;; TODO: now that _this_ file, model.clj, is basically empty, let’s just move
; the contents of model_specs.clj over into this file.
(load "model_specs")

(defn empty-model
  []
  {::systems {} ::users {} ::datastores {}})
