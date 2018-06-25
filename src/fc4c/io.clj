(ns fc4c.io
  "Provides all I/O facilities so that the other namespaces can be pure. The
  function specs are provided as a form of documentation and for instrumentation
  during development. They should not be used for generative testing."
  (:require [clojure.java.io         :as io]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string          :as str :refer [ends-with?]]
            [cognitect.anomalies     :as anom]
            [expound.alpha           :as expound :refer [expound-str]]
            [fc4c.files              :as files :refer [relativize]]
            [fc4c.model              :as m :refer [elements-from-file]]
            [fc4c.util               :as u :refer [lookup-table-by]]))

;; TODO: I think it’s kinda confusing for :dir-path to be in fc4c.model and :dir-file to be here.
(s/def ::dir-path-file
  (s/with-gen
    (partial instance? java.io.File)
    #(gen/fmap io/file (s/gen ::m/dir-path-str))))

(s/def ::dir-path
  (s/or ::m/dir-path-str ::dir-path-file))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File objects for
  all the YAML files in that dir or in any of its child dirs (recursively) to an unlimited depth."
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(or (ends-with? % ".yaml")
                    (ends-with? % ".yml")))))

(s/fdef yaml-files
        :args (s/cat :dir ::dir-path)
        :ret  (s/coll-of (partial instance? java.io.File)))

(defn process-dir
  "Accepts a directory path as a string, finds all the YAML files in that dir or
  in any of its child dirs (recursively) to an unlimited depth, and applies f to
  the contents of each file, overwriting its current contents. Prints out the
  path of each file before processing it. If an error occurs, it is thrown
  immediately, aborting the work."
  [dir-path f]
  (doseq [file (yaml-files dir-path)]
    (binding [*out* *err*]
      (println (relativize (str file) dir-path)))
    (->> (slurp file)
         (f)
         (spit file))))

(s/fdef process-dir
        :args (s/cat :dir-path ::dir-path
                     :f        (s/fspec :args (s/cat :file-contents string?)
                                        :ret  string?))
        :ret  nil?)

(defn- read-model-elements
  "Recursively find and read all elements from all YAML files under a directory
  tree."
  [root-path]
  (->> (yaml-files root-path)
       (map (juxt slurp identity))
       (mapcat (fn [[file-contents file-path]]
                 (elements-from-file file-contents file-path root-path)))
       ((partial lookup-table-by ::m/name))))

(s/fdef read-model-elements
        :args (s/cat :root-path ::m/dir-path-or-file)
        :ret  (s/map-of ::m/name ::m/element))

(defn read-model
  "Pass the path of a dir (must have trailing slash) that contains the dirs
  \"systems\" and \"users\"."
  [root-path]
  (let [model {::m/systems (read-model-elements (io/file root-path "systems"))
               ::m/users (read-model-elements (io/file root-path "users"))}]
    (if (s/valid? ::m/model model)
      model
      {::anom/category ::anom/fault
       ::anom/message (expound-str ::m/model model)
       ::m/model model})))

(s/fdef read-model
        :args (s/cat :root-path ::dir-path)
        :ret  (s/or :success ::m/model
                    :error   (s/merge ::anom/anomaly
                                      (s/keys :req [::m/model]))))
