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

;; SHARED STATE: map of file path to the timestamp of the most recent occasion
;; wherein we processed that file, either because of a clipboard event or a
;; filesystem event.
(def when-processed (atom {}))

(def seconds (ChronoUnit/SECONDS))
(def min-secs-between-changes 1)

(defn since
  [inst]
  (.between seconds inst (Instant/now)))

(defn process-fs-event?
  [_ {:keys [kind file] :as _event}]
  (and (#{:create :modify} kind)
       (yaml-file? file)
       (or (not (contains? @when-processed file))
           (let [last-processed (get @when-processed file)]
             ; (println "it’s been" (since last-processed) "seconds since" (.getName file) "was last changed...")
             (>= (since last-processed) min-secs-between-changes)))))

(defn process-file
  [^File file]
  (println "BEGIN" (str file))
  (let [result (se-edit/process-file (slurp file))
        ;; TODO: error handling!
        yaml-out (::se-edit/str-processed result)]
    (cb/spit yaml-out)
    (spit file yaml-out))
  (print "processed YAML written to file and clipboard.\nrendering... ")
  (render-diagram-file file)
  (println "done.\nEND " (.getName file) "\n")
  ; Update our shared state value so the process? fns will be able to filter
  ; out events that are dispatched in rapid succession and would otherwise result
  ; in infinite loops.
  (swap! when-processed assoc file (Instant/now))
  ; No need to return a value for Hawk to maintain as context, as we’re using our own state.
  nil)

(defn process-fs-event
  [_ {:keys [file] :as _event}]
  (process-file file))

(defn start-fs-watch
  [paths]
  (let [watch (hawk/watch! [{:paths   paths
                             :filter  process-fs-event?
                             :handler process-fs-event}])]
    (println "📣 Now watching for changes to"
             (if (= (count paths) 1)
               (str (first paths) " and the clipboard...\n")
               "YAML files under specified paths...\n"))
    watch))

(defn block-on-fs-watch
  [{:keys [thread]}]
  (.join thread))

(defn process-cb-change?
  [file _old _new]
  (or (not (contains? @when-processed file))
      (let [last-processed (get @when-processed file)]
        ; (println "it’s been" (since last-processed) "seconds since" (.getName file) "was last changed...")
        (>= (since last-processed) min-secs-between-changes))))

(defn start-cb-watch
  [^File file]
  (let [output-chan (chan 10)
        stop-chan (chan (dropping-buffer 1))]
    (go
      (cb/watch output-chan stop-chan {:filter (partial process-cb-change? file)})
      (loop [processed (<! output-chan)]
        (swap! when-processed assoc file (Instant/now))
        (spit file processed)
        (println "Clipboard contents processed and written to" (.getName file))
        (when-let [processed-next (<! output-chan)]
          (recur processed-next))))
    stop-chan))

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
  (let [ffile (io/file (first paths))]
    (when (and (= (count paths) 1)
               (yaml-file? ffile))
      (process-file ffile) ; clean up the file immediately, as that’s what the user would expect; it’s just intuitive.
      (start-cb-watch ffile)))
  (block-on-fs-watch (start-fs-watch paths)))
