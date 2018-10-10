(ns fc4.render
  (:require [fc4.integrations.structurizr.express.edit :refer [parse-coords]]
            [clojure.string :refer [includes?]]
            [seesaw.core :as sc]
            [clojure.java.io :as io])
  (:import [java.awt Color]
           [javax.imageio ImageIO]
           [com.mxgraph.view mxGraph]
           [com.mxgraph.swing mxGraphComponent]
           [com.mxgraph.util mxCellRenderer mxPoint]))

(def scale (float 1/5))
(def vertex-width 85)
(def vertex-height 60)

(defn add-vertices [d graph]
  (into {}
    (for [e (:elements d)]
      (let [name (:name e)
            root (.getDefaultParent graph)
            [x y] (->> (parse-coords (:position e))
                       (map (partial * scale)))]
        [name (.insertVertex graph root name name x y vertex-width vertex-height "fontSize=10;whiteSpace=wrap;")]))))

(defn add-edges [d graph vertices]
  (doseq [r (:relationships d)]
    (let [source (get vertices (:source r))
          destination (get vertices (:destination r))
          root (.getDefaultParent graph)
          edge (.insertEdge graph root nil (:description r) source destination "fontSize=8;whiteSpace=wrap;")
          geo (.getGeometry edge)]
      (->> (:vertices r) ; Structurizr Express calls these “vertices” but “control points” or “waypoints” would be less confusing
           (map #(let [[x y] (->> (parse-coords %)
                                  (map (partial * scale)))]
                   (mxPoint. x y)))
           java.util.ArrayList.
           (.setPoints geo)))))

(defn doc->graph [d]
  (let [graph (mxGraph.)
        root (.getDefaultParent graph)
        vertices (add-vertices d graph)]
    (add-edges d graph vertices)
    graph))

(defn render-image [graph]
  (mxCellRenderer/createBufferedImage graph nil 5.0 Color/WHITE true nil))

(defn save-png [image]
  ; Suppress the Java icon from popping up and grabbing focus on MacOS.
  ; Found in a comment to this answer: https://stackoverflow.com/a/17544259/7012
  (System/setProperty "apple.awt.UIElement" "true")
  (ImageIO/write image "PNG" (io/file "/tmp/graph.png")))

(defn render-swing [d]
  (System/setProperty "apple.awt.UIElement" "false")
  (let [graph (doc->graph d)
        {:keys [:type :scope]} d
        title (str type " for " scope)
        frame (sc/frame :title title)]
    (->> (mxGraphComponent. graph)
         sc/scrollable
         (sc/config! frame :content))
    (sc/invoke-later
      (-> frame sc/pack! sc/show!))))

(comment
  (use 'fc4.render)
  (require '[fc4.integrations.structurizr.express.edit :as se :refer [process-file]])
  (def fc (slurp "examples/internet_banking_context.yaml"))
  (-> fc process-file ::se/main-processed render-swing)
  (-> fc process-file ::se/main-processed doc->graph render-image save-png)
  )
