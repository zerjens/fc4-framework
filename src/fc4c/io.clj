(ns fc4c.io
  "Provides all I/O facilities so that the other namespaces can be pure. The
  function specs are provided as a form of documentation and for instrumentation
  during development. They should not be used for generative testing."
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]
            [fc4c.files :refer [yaml-files]]
            [fc4c.model :as m :refer [elements-from-file]]
            [fc4c.util :refer [lookup-table-by]]))

(s/def ::dir-file
  (s/with-gen
    (partial instance? java.io.File)
    #(gen/fmap io/file (s/gen ::m/dir-path))))

(s/def ::dir-path-or-file
  (s/or ::m/dir-path ::dir-file))

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
        :args (s/cat :root-path ::dir-path-or-file)
        :ret  (s/or :success ::m/model
                    :error   (s/merge ::anom/anomaly
                                      (s/keys :req [::m/model]))))
