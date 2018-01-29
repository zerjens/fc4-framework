(ns restructurizr.repl
  "Some useful funcs for using Minimayaml from the REPL."
  (:require [clojure.repl :as cr]
            [clojure.core.async :as ca :refer [chan go-loop offer! poll!]]
            [clojure.string :as str :refer [includes?]]
            [restructurizr [core :as rc]
                           [clipboard :as cb]
                           [files :as rf :refer [process-dir]]]))
                          
(def stop-chan (chan 1))

(defn pcb
  "Process Clipboard — process the contents of the clipboard and write the results back to the
  clipboard. If the contents of the clipboard are not a Structurizr diagram, a RuntimeException is
  thrown."
  []
  (let [contents (cb/slurp)]
    (if (and (string? contents)
             (includes? contents "type")
             (includes? contents "scope"))
      (-> contents
          rc/process-file
          cb/spit)
      (throw (RuntimeException. "Not a Structurizr diagram.")))))

(defn cpcb
  "Continuously Process Clipboard — call pcb every second. Stop the routine by calling stop.
   TODO: there’s a huge chance for a race condition here — need to compare clipboard contents
   with prior output and only process if it’s changed."
  []
  ;; Just in case stop was accidentally called twice, in which case there’d be a superfluous value
  ;; in the channel, we’ll remove a value from the channel just before we get started.
  (poll! stop-chan)

  (go-loop []
    (let [stop? (poll! stop-chan)]
      (when-not stop?
        (try
          (pcb)
          (print ".")
          (flush)
          (catch Exception err
            (println (-> err class .getSimpleName) "->" (.getMessage err))
            (flush)))
        (Thread/sleep 1000)
        (recur))))
  nil)

(defn stop
  "Stop the goroutine started by cpcb"
  []
  (offer! stop-chan true))

(cr/doc pcb)
(cr/doc cpcb)
(cr/doc stop)
(cr/doc process-dir)
