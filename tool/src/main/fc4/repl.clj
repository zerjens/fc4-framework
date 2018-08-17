(ns fc4.repl
  "Import some useful funcs for using fc4-tool from the REPL into this single
  namespace, for convenience."
  (:require [clojure.repl :as cr]
            [fc4.files :as rf]
            [fc4.integrations.structurizr.express.clipboard :as cb]
            [fc4.io :as fio]
            [potemkin :refer [import-vars]]))

;; Make convenience functions readily accessible.
(import-vars [fc4.integrations.structurizr.express.clipboard pcb wcb stop]
             [fc4.io process-dir])

;; Print docs for the most handy-dandy funcs
(doseq [s ['pcb 'wcb 'stop 'process-dir]]
  (#'cr/print-doc (meta (resolve s))))
