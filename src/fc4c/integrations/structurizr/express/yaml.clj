(ns fc4c.integrations.structurizr.express.yaml
  (:require [clojure.string :as str]
            [fc4c.yaml :as fy]))

(defn fixup
  "Accepts a diagram as a YAML string and applies some custom formatting rules."
  [s]
  (-> s
      (str/replace #"(\d+,\d+)" "'$1'")
      (str/replace #"(elements|relationships|styles|size):" "\n$1:")
      (str/replace #"(description): Uses\n" "$1: uses\n")))

(def stringify (comp fixup fy/stringify))
