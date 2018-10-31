(ns fc4.cli.edit
  "CLI subcommand that watches Structurizr Express diagram files; when changes
  are observed to a YAML file, the YAML in the file is cleaned up, the diagram
  is rendered to an image file, and the cleaned up YAML is pasted to the
  clipboard."
  (:require
    [clojure.string :as cstr :refer [ends-with?]]
    [fc4.integrations.structurizr.express.clipboard :as cb]
    [fc4.integrations.structurizr.express.edit :as se-edit]
    [fc4.io :as fio :refer [process-text-file render-diagram-file yaml-file?]]
    [hawk.core :as hawk]))

(defn fs-event-filter
  [_context {:keys [kind file] :as event}]
  (and (#{:create :modify} kind)
       (yaml-file? file)))

(defn on-diagram-file-change
  [_context {:keys [kind file] :as event}]
  (println "processing" (str file))
  (let [process-result (se-edit/process-file (slurp file))
        ;; TODO: error handling!
        yaml-out (::se-edit/str-processed process-result)]
    (spit file yaml-out)
    (cb/spit yaml-out)
    (println (render-diagram-file file)
             "...done. Processed YAML written to file and clipboard.")))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& paths]
  (hawk/watch! [{:paths paths
                 :filter fs-event-filter
                 :handler on-diagram-file-change}]))
