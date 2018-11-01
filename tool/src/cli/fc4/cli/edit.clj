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
    [hawk.core :as hawk])
  (:import [java.time Instant]
           [java.time.temporal ChronoUnit]))

;; The context values are a map of file path to the timestamp of the most recent
;; occasion wherein we processed that file.

(def seconds (ChronoUnit/SECONDS))
(def min-secs-between-changes 2)

(defn since
  [inst]
  (.between seconds inst (Instant/now)))

(defn process?
  [context {:keys [kind file] :as event}]
  (and (#{:create :modify} kind)
       (yaml-file? file)
       (or (not (contains? context file))
           (let [last-processed (get context file)]
             (>= (since last-processed) min-secs-between-changes)))))

(defn process
  [context {:keys [file] :as event}]
  (println "BEGIN" (str file))
  (let [process-result (se-edit/process-file (slurp file))
        ;; TODO: error handling!
        yaml-out (::se-edit/str-processed process-result)
        inst-written (do (cb/spit yaml-out)
                         (spit file yaml-out)
                         (Instant/now))
        _ (println "processed YAML written to file and clipboard.\nstarting rendering...")
        stderr (render-diagram-file file)]
    (println (str stderr "rendering complete.\nEND " (.getName file) "\n"))
    ; Return an updated context value so that process? will be able to filter out
    ; the fs modify event that will be dispatched immediately because we wrote to
    ; the YAML file.
    (assoc context file inst-written)))

(defn start-watch [paths]
  (let [watch (hawk/watch! [{:paths paths
                             :context (constantly {}) ; used only for initial context value
                             :filter process?
                             :handler process}])]
    (println "now watching for changes to YAML files under specified paths...")
    watch))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& paths]
  (-> (start-watch paths)
      (get :thread)
      (.join)))
