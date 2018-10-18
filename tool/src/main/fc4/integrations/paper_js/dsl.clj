(ns fc4.integrations.paper-js.dsl)

(defn project
  [first-child & more-children]
  (concat [first-child] more-children))

(defn layer
  [children]
  [:Layer {:children children}])

(defn group
  ([{:keys [name matrix]} children]
   [:Group (merge {:children children}
                  (when name {:name name})
                  (when matrix {:matrix matrix}))]))

(defn path
  [m]
  [:Path m])

(defn shape
  ([m]
   [:Shape m])
  ([type m]
   [:Shape (merge m {:type type})]))

(defn circle
  [center radius m]
  (shape :circle (merge m {:center center
                           :radius radius})))

(defn ellipse
  [x y width height m]
  (shape :ellipse (merge m {:point [x y]
                            :size  [width height]})))

(defn rect
  [x y width height m]
  (shape :rectangle (merge m {:position [x y]
                              ;; for some reason, radius is required.
                              :radius (or (:radius m) [0 0])
                              :size  [width height]})))

(defn text
  ([content x y]
   (text content x y nil))
  ([content x y m]
   [:PointText (merge m {:content content
                         :position [x y]})]))

(defn color
  [red green blue]
  ["rgb" red green blue])

(def representations
  {:system {:width 120
            :height 80
            :corner-radius 10
            :name-font-size 12
            :desc-font-size 10
            :fill-color (color 170 74 237)}})

(defn system
  [name description x y]
  (let [{:keys [width height corner-radius name-font-size desc-font-size fill-color]}
        (:system representations)]
    (group
      {:name name}
      [(rect x y width height {:fillColor fill-color
                               :radius (repeat 2 corner-radius)
                               :strokeColor (color 1 0 0)})
       (text name x (- y 18) {:fontSize name-font-size
                              :fontWeight :bold})
       (when description
         (text description x (+ y 2) {:fontSize desc-font-size}))])))
