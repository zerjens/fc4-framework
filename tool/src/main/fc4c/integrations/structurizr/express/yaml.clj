(ns fc4c.integrations.structurizr.express.yaml
  (:require [clojure.string :as str]
            [fc4c.yaml :as fy]))

(defn- wrap-coord-strings
  "If an entire value looks like a coordinate, wrap it in single quotes so as to
  force it to a string, because otherwise it might be parsed by Structurizr
  Express as a number, and therefore an invalid coordinate. e.g. `82,34` is a
  valid number in European locales, and some YAML parsers will therefore parse
  something like that as a number rather than a string."
  [s]
  (str/replace s #"([: '])(\d+,\d+)(['\s])" "$1'$2'$3"))

(defn fixup
  "Accepts a diagram as a YAML string and applies some custom formatting rules."
  [s]
  (-> s
      (wrap-coord-strings)
      (str/replace #"(elements|relationships|styles|size):" "\n$1:")
      (str/replace #"(description): Uses\n" "$1: uses\n")))

(def stringify (comp fixup fy/stringify))
