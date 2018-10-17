(ns fc4.integrations.paper-js.dsl)

(defn project
  [first-child & more-children]
  (concat [first-child] more-children))

(defn layer
  [first-child & more-children]
  [:Layer {:children (concat [first-child] more-children)}])

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
  (shape :rectangle (merge m {:point [x y]
                              :size  [width height]})))

(defn text
  ([content x y]
   (text content x y nil))
  ([content x y m]
   [:Text (merge m {:content content
                    :point [x y]})]))

(defn color
  [red green blue]
  ["rgb" red green blue])

(def representations
  {:system {:width 100
            :height 80}})

(defn system
  [name description x y]
  (let [{:keys [width height]} (:system representations)]
    (group
      {:name name}
      [(rect x y width height {:strokeColor (color 1 0 0)})
       (text name x y)])))
