(ns minimayaml.repl
  "Some useful funcs for using Minimayaml from the REPL."
  (:require [minimayaml.core :as c]
            [minimayaml.clipboard :as cb]))

(defn pcb
  "Process the contents of the clipboard and write the results back to the clipboard."
  []
  (-> (cb/slurp)
      c/process-file
      cb/spit))
