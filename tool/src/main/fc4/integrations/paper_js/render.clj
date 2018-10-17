(ns fc4.integrations.paper-js.render
  (:require [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [fc4.integrations.paper-js.dsl :refer [project layer group system]]
            [fc4.model :as m]
            [fc4.view :as v]))

(defn get-sys [model sys-name]
  (get-in model [::m/systems sys-name]))

;;;;; START TEMP

(def coord-pattern-base "(\\d{1,4}), ?(\\d{1,4})")
(def coord-pattern (re-pattern (str "^" coord-pattern-base "$")))
(defn parse-coords [s]
  (some->> s
           (re-find coord-pattern)
           (drop 1)
           (map #(Integer/parseInt %))))

;;;;; END TEMP

(defn view->shapes
  [view model]
  (for [[sys-name coord-string]
        (get-in view [::v/positions ::v/other-systems])
        :let [sys (get-sys model sys-name)
              {:keys [::m/name ::m/description]} sys
              [x y] (parse-coords coord-string)]]
    (system name description x y)))

(defn view->paper
  "Converts a view on a model to the paper.js data format, effectively rendering
   the view as data. Pure."
  [view model]
  (let [shapes (view->shapes view model)]
    (project
      (layer
        (group nil shapes)))))

(defn paper->pdf
  "Renders a paper.js data structure into a PDF, returning the PDF bytearray.
  Not pure; spawns a child process to perform the rendering. If the child
  process exits with an error code, an exception will be thrown."
  [paper]
  (let [json-str (json/write-str paper)
        result (sh "renderer/render.js"
                   :in json-str
                   :out-enc :bytes)
        {:keys [exit out err]} result]
    (if (zero? exit)
        out
        (throw (Exception. err)))))

(defn render
  "Renders an FC4 view on an FC4 model, returning a PDF bytearray. Not pure;
  spawns a child process to perform the rendering."
  [view model]
  (-> (view->paper view model)
      (paper->pdf)))

(comment
  (use 'fc4.io)
  (use 'clojure.java.io)
  (use :reload 'fc4.integrations.paper-js.dsl)
  (use :reload 'fc4.integrations.paper-js.render)
  (def m (read-model "test/data/model (valid)"))
  (def v (read-view "test/data/views/middle (valid).yaml"))
  (view->shapes v m)
  (view->paper v m)
  (require '[clojure.data.json :as j])
  (j/pprint (view->paper v m))
  (def pdfb (render v m))
  (with-open [out (output-stream (file "/tmp/fc4-paper.pdf"))]
     (.write out pdfb))
)
