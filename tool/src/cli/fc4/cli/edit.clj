(ns fc4.cli.edit
  "CLI subcommand that watches Structurizr Express diagram files; when changes
  are observed to a YAML file, the YAML in the file is cleaned up, the diagram
  is rendered to an image file, and the cleaned up YAML is pasted to the
  clipboard."
  (:require
   [clojure.core.async :as ca :refer [<! chan dropping-buffer go]]
   [clojure.java.io :as io]
   [clojure.string :as cstr :refer [ends-with?]]
   [fc4.integrations.structurizr.express.clipboard :as cb]
   [fc4.integrations.structurizr.express.edit :as se-edit]
   [fc4.io :as fio :refer [process-text-file render-diagram-file yaml-file?]]
   [hawk.core :as hawk])
  (:import [java.time Instant]
           [java.time.temporal ChronoUnit]
           (java.io File)))

;; The context values are a map of file path to the timestamp of the most recent
;; occasion wherein we processed that file.

(def seconds (ChronoUnit/SECONDS))
(def min-secs-between-changes 1)

(defn since
  [inst]
  (.between seconds inst (Instant/now)))

(defn process-fs-event?
  [context {:keys [kind file] :as _event}]
  (and (#{:create :modify} kind)
       (yaml-file? file)
       (or (not (contains? context file))
           (let [last-processed (get context file)]
             ; (println "it’s been" (since last-processed) "seconds since" (.getName file) "was last changed...")
             (>= (since last-processed) min-secs-between-changes)))))

(defn process-fs-event
  [context {:keys [file] :as _event}]
  (println "BEGIN" (str file))
  (let [process-result (se-edit/process-file (slurp file))
        ;; TODO: error handling!
        yaml-out (::se-edit/str-processed process-result)]
    (cb/spit yaml-out)
    (spit file yaml-out)
    (print "processed YAML written to file and clipboard.\nrendering...")
    (print (render-diagram-file file))
    (println "rendering complete.\nEND" (.getName file) "\n")
    ; Return an updated context value so that process? will be able to filter out
    ; the fs modify event that will be dispatched immediately because we wrote to
    ; the YAML file.
    (assoc context file (Instant/now))))

(defn start-fs-watch
  [paths]
  (let [watch (hawk/watch! [{:paths   paths
                             :context (constantly {}) ; used only for initial context value
                             :filter  process-fs-event?
                             :handler process-fs-event}])]
    (println "now watching for changes to YAML files under specified paths...")
    watch))

(defn block-on-fs-watch
  [{:keys [thread]}]
  (.join thread))

(defn start-cb-watch
  [^File file]
  (let [output-chan (chan 10)
        stop-chan (chan (dropping-buffer 1))]
    (go
      (cb/watch output-chan stop-chan)
      (loop [processed (<! output-chan)]
        (spit file processed)
        (println "Clipboard contents processed and written to" (.getName file))
        (when-let [processed-next (<! output-chan)]
          (recur processed-next)))
    (stop-chan))))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& paths]
  (when (and (= (count paths) 1)
             (yaml-file? (first paths)))
    (start-cb-watch (io/file (first paths))))
  (block-on-fs-watch (start-fs-watch paths)))
