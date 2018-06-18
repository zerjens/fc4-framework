(ns fc4c.files
  (:require [clojure.java.io :as io]
            [clojure.string :as str :refer [ends-with? starts-with?]]))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(or (ends-with? % ".yaml")
                    (ends-with? % ".yml")))))

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

(defn process-dir
  "Accepts a directory path as a string, finds all the YAML files in that dir or
  in any of its child dirs (recursively) to an unlimited depth, and applies f to
  the contents of each file, overwriting its current contents. Prints out the
  path of each file before processing it. If an error occurs, it is thrown
  immediately, aborting the work."
  [dir-path f]
  (doseq [file (yaml-files dir-path)]
    (println (relativize (str file) dir-path))
    (->> (slurp file)
         (f)
         (spit file))))
