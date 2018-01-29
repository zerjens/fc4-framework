(ns restructurizr.files
  (:require [restructurizr.core :as rc]
            [clojure.java.io :as io]
            [clojure.string :as str :refer [ends-with?]]))

(defn yaml-files
  "Accepts a directory path as a string, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir-path]
  (->> dir-path
       io/file
       file-seq
       (filter #(or (ends-with? % ".yaml")
                    (ends-with? % ".yml")))))

(defn relativize [path parent-path]
  (.substring path (if (ends-with? parent-path "/")
                       (count parent-path)
                       (inc (count parent-path)))))

(defn process-dir
  "Accepts a directory path as a string, finds all the YAML files in that dir or in any of its
  child dirs (recursively) to an unlimited depth, and processes each one with
  restructurizr.core/process-file, overwriting its current contents. Prints out the path of each
  file before processing it. If an error occurs, it is thrown immediately, aborting the work."
  [dir-path]
  (doseq [file (yaml-files dir-path)]
    (println (relativize (str file) dir-path))
    (->> (slurp file)
         rc/process-file
         second
         (spit file))))
