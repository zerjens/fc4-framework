(ns fc4c.repl
  "Some useful funcs for using Minimayaml from the REPL."
  (:require [clojure.repl :as cr]
            [clojure.core.async :as ca :refer [<! chan go-loop offer! poll! timeout]]
            [clojure.string :as str :refer [includes?]]
            [fc4c.core :as rc]
            [fc4c.clipboard :as cb]
            [fc4c.files :as rf]
            [potemkin :refer [import-vars]]))

(def stop-chan (chan 1))

(defn probably-structurizr-doc? [v]
  (and (string? v)
       (includes? v "type")
       (includes? v "scope")))

(defn pcb
  "Process Clipboard — process the contents of the clipboard and write the results back to the
  clipboard. If the contents of the clipboard are not a Structurizr diagram, a RuntimeException is
  thrown."
  []
  (let [contents (cb/slurp)]
    (if (probably-structurizr-doc? contents)
      (-> contents
          rc/process-file
          second
          cb/spit)
      (throw (RuntimeException. "Not a Structurizr diagram.")))))

(def current-local-time-format
  (java.text.SimpleDateFormat. "HH:mm:ss"))

(defn current-local-time-str []
  (.format current-local-time-format (java.util.Date.)))

(defn try-process [contents]
  (try
     (let [[main str-result] (rc/process-file contents)
           _ (cb/spit str-result)
           {:keys [:type :scope]} main]
       (println (current-local-time-str) "-> processed" type "for" scope "with great success!")
       (flush)
       str-result)
     (catch Exception err
       ; toString _should_ suffice but some of the SnakeYAML exception classes seem to have a bug in
       ; their toString implementations wherein they don’t print their names.
       (println (-> err class .getSimpleName) "->" (.getMessage err))
       (flush)
       nil)))

(defn start
  "Start continuously processing the clipboard in the background, once a second, when the contents
  of the clipboard change. Stop the routine by calling stop."
  []
  ;; Just in case stop was accidentally called twice, in which case there’d be a superfluous value
  ;; in the channel, we’ll remove a value from the channel just before we get started.
  (poll! stop-chan)

  (go-loop [prior-contents nil]
    (let [contents (cb/slurp)
          process? (and (not= contents prior-contents)
                        (probably-structurizr-doc? contents))
          output (when process?
                   (try-process contents))]          
      (if (poll! stop-chan)
        (do (println "Stopped!") (flush))
        (let [contents (cb/slurp)]
          (<! (timeout 1000))
          (recur contents)))))
  nil)

(defn stop
  "Stop the goroutine started by start."
  []
  (offer! stop-chan true))

;; Make process-dir readily accessible
(import-vars [fc4c.files process-dir])

;; Print docs for the most handy-dandy funcs
(doseq [s ['pcb 'start 'stop 'process-dir]]
  (#'cr/print-doc (meta (resolve s))))