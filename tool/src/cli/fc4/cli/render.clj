(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require [fc4.cli.util :as cu :refer [debug fail]]
            [fc4.io.render :refer [render-diagram-file]]))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  ;;
  ;; TODO: add a command-line flag that sets cu/*debug* to true
  [& paths]
  (try
    (doseq [path paths]
      (print (str path "..."))
      (flush)
      (render-diagram-file path)
      (println "✅"))
    (catch Exception e
      ; TODO: maybe use cu/debug print out stack trace and ex-data if present?
      (fail (.getMessage e)))))
