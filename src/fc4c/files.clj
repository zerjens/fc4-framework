(ns fc4c.files
  (:require [clojure.java.io :as io]
            [clojure.string :as str :refer [ends-with?]]))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(or (ends-with? % ".yaml")
                    (ends-with? % ".yml")))))

(defn relativize [path parent-path]
  (let [p  (str path) ; coerce to string as it might be a File
        pp (str parent-path)]
    (subs p (if (ends-with? pp "/")
                (count pp)
                (inc (count pp))))))

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
