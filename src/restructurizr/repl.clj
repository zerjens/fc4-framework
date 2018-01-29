(ns restructurizr.repl
  "Some useful funcs for using Minimayaml from the REPL."
  (:require [clojure.repl :as cr]
            [clojure.core.async :as ca :refer [chan go-loop offer! poll!]]
            [clojure.string :as str :refer [includes?]]
            [restructurizr [core :as rc]
                           [clipboard :as cb]
                           [files :as rf :refer [process-dir]]]))
                          
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

(def current-local-time-format (java.text.SimpleDateFormat. "HH:mm:ss"))

(defn current-local-time-str [] (.format current-local-time-format (java.util.Date.)))

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

(defn cpcb
  "Continuously Process Clipboard — call pcb every second, if the contents of the clipboard has
  changed since the prior call. Stop the routine by calling stop."
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
          (Thread/sleep 1000)
          (recur contents)))))
  nil)

(defn stop
  "Stop the goroutine started by cpcb."
  []
  (offer! stop-chan true))

(cr/doc pcb)
(cr/doc cpcb)
(cr/doc stop)
(cr/doc process-dir)
