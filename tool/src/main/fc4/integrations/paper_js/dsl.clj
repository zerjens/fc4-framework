(ns fc4.integrations.paper-js.dsl)

(defn layer
  [first-child & more-children]
  [:Layer {:children (concat [first-child] more-children)}])

(defn group
  [{:keys [name matrix]} first-child & more-children]
  [:Group (merge {:children (concat [first-child] more-children)}
                 (when name {:name name})
                 (when matrix {:matrix matrix}))])

(defn path
  [m]
  [:Path m])

(defn color
  [red green blue]
  ["rgb" red green blue])

(defn shape [TBD] TBD)

(defn system
  [sys]
  (shape )
  
  )
