(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require [fc4.io :as fio :refer [render-diagram-file]]))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& in-paths]
  (doseq [in-path in-paths]
    (println "invoking renderer..."
             (render-diagram-file in-path))))
