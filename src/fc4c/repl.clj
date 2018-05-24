(ns fc4c.repl
  "Import some useful funcs for using FC4C from the REPL into this single
  namespace, for convenience."
  (:require [clojure.repl :as cr]
            [fc4c.clipboard :as cb]
            [fc4c.files :as rf]
            [potemkin :refer [import-vars]]))

;; Make process-dir readily accessible
(import-vars [fc4c.clipboard pcb wcb stop]
             [fc4c.files process-dir])

;; Print docs for the most handy-dandy funcs
(doseq [s ['pcb 'wcb 'stop 'process-dir]]
  (#'cr/print-doc (meta (resolve s))))
