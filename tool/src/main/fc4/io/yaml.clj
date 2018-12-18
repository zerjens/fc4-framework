(ns fc4.io.yaml
  (:require [clojure.java.io :as io :refer [file output-stream]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str :refer [ends-with?]]
            [fc4.files :as files :refer [relativize]]
            [fc4.spec :as fs])
  (:import [java.io File]))

(defn yaml-file?
  [f]
  (and (.isFile (file f))
       (or (ends-with? f ".yaml")
           (ends-with? f ".yml"))))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir]
  (->> (file dir)
       (file-seq)
       (filter yaml-file?)))

(s/fdef yaml-files
        :args (s/cat :dir ::fs/dir-path)
        :ret  (s/coll-of (partial instance? File)))
