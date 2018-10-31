(ns fc4.cli.edit
  "CLI subcommand that watches Structurizr Express diagram files; when changes
  are observed the YAML in the files is cleaned up and the diagram is rendered
  to an image."
  (:require
    [clojure.string :as cstr :refer [ends-with?]]
    [fc4.integrations.structurizr.express.edit :as se-edit]
    [fc4.io :as fio :refer [process-text-file render-diagram-file yaml-file?]]
    [hawk.core :as hawk]))

(defn fs-event-filter
  [_context {:keys [kind file] :as event}]
  (and (hawk/file? _context event)
       (#{:create :modify} kind)
       yaml-file?))

(defn on-diagram-file-change
  [_context {:keys [kind file] :as event}]
  (println "processing" file)
  (process-text-file file se-edit/process-file)
  (println (render-diagram-file file)))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& paths]
  (hawk/watch! [{:paths paths
                 :filter hawk/file?
                 :handler on-diagram-file-change}]))
