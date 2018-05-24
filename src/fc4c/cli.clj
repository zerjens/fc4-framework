(ns fc4c.cli
  (:require [fc4c.clipboard :refer [wcb]]
            [clojure.core.async :as ca :refer [<!!]]))

(defn -main []
  (let [c (wcb)]
    (println "Now watching clipboard for FC4 diagrams in YAML format. Hit ctrl-c to exit.")
    
    ;; Block until wcb’s return channel emits something. Which it won’t, so
    ;; effectively block forever, or until the user hits ctrl-c, whichever
    ;; happens first.
    (<!! c)))
