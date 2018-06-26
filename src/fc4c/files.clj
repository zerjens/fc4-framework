(ns fc4c.files
  (:require [clojure.string :as str :refer [ends-with? starts-with?]]))

(defn relativize
  "Accepts two absolute paths. If the first is a “child” of the second, the
  first is relativized to the second and returned as a string. If it is not,
  returns nil."
  [path parent-path]
  (let [[p pp]
        (map str [path parent-path])] ; coerce to strings in case they’re Files
    (when (starts-with? p pp)
      (subs p (if (ends-with? pp "/")
                (count pp)
                (inc (count pp)))))))
