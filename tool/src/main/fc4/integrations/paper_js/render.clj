(ns fc4.integrations.paper-js.render
  (:require [fc4.integrations.paper-js.dsl :refer [layer group system]]
            [fc4.model :as m]
            [fc4.view :as v]))

(defn get-sys [model sys-name]
  (get-in model [::m/systems sys-name]))

(defn view->shapes
  [view model]
  (for [{:keys [::v/name ::v/coord-string]}
        (get-in view [::v/positions ::v/other-systems])]
    ;;;;;;;;;;;;; TODO: finish this!
    (system (get-sys model name) coord-string)
  )
  )

(defn view->paper
  "Converts a view on a model to the paper.js data format, effectively rendering
   the view as data. Pure."
  [view model]
  (let [shapes (view->shapes view model)]
    (layer (group {:name "svg"}
                  (group nil shapes)))
  )

(defn paper->pdf
  "Renders a paper.js data structure into a PDF, returning the PDF bytearray.
  Not pure; spawns a child process to perform the rendering."
  [paper]
  )

(defn render
  "Renders an FC4 view on an FC4 model, returning a PDF bytearray. Not pure;
  spawns a child process to perform the rendering."
  [view model]
  (-> (view->paper view model)
      (paper->pdf)))
