(ns minimayaml.repl
  "Some useful funcs for using Minimayaml from the REPL."
  (:require [minimayaml.core :as c]
            [minimayaml.clipboard :as cb]))

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
          c/process-file
          cb/spit)
      (throw (RuntimeException. "Not a Structurizr diagram.")))))
