(ns fc4.cli.edit
  "CLI subcommand that watches Structurizr Express diagram files; when changes
  are observed to a YAML file, the YAML in the file is cleaned up and rendered
  to an image file."
  (:require
   [clojure.core.async :as ca :refer [<! chan dropping-buffer go]]
   [clojure.java.io :as io]
   [clojure.string :as cstr :refer [ends-with?]]
   [fc4.integrations.structurizr.express.edit :as se-edit]
   [fc4.io :as fio :refer [process-text-file render-diagram-file yaml-file?]]
   [hawk.core :as hawk])
  (:import [java.time Instant]
           [java.time.temporal ChronoUnit]
           (java.io File)))

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

(defn process-file
  [context ^File file]
  (println (.getName file))
  (let [result (se-edit/process-file (slurp file))
        ;; TODO: error handling!
        yaml-out (::se-edit/str-processed result)]
    (spit file yaml-out))
  (print "processed YAML written to file.\nrendering... ")
  (render-diagram-file file)
  (println "done.\n " (.getName file) "\n")
  ; Update the state value so process-fn-event? will be able to filter out
  ; events that are dispatched in rapid succession that would otherwise result
  ; in infinite loops.
  (assoc context file (Instant/now)))

(defn process-fs-event
  [context {:keys [file] :as _event}]
  (process-file context file))

(defn start-watch
  [paths]
  (let [watch (hawk/watch! [{:paths   paths
                             :filter  process-fs-event?
                             :handler process-fs-event}])]
    (println "📣 Now watching for changes to YAML files under specified paths...")
    watch))

(defn block-on-watch
  [{:keys [thread]}]
  (.join thread))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  [& paths]
  (when (empty? paths)
    (println "usage: fc4 edit [path ...]")
    (System/exit 1))
  (block-on-watch (start-watch paths)))
