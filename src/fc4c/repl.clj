(ns fc4c.repl
  "Import some useful funcs for using FC4C from the REPL into this single
  namespace, for convenience."
  (:require [clojure.repl :as cr]
            [fc4c.integrations.structurizr.express.clipboard :as cb]
            [fc4c.files :as rf]
            [potemkin :refer [import-vars]]))

;; Make convenience functions readily accessible.
(import-vars [fc4c.integrations.structurizr.express.clipboard pcb wcb stop]
             [fc4c.io process-dir])

;; Print docs for the most handy-dandy funcs
(doseq [s ['pcb 'wcb 'stop 'process-dir]]
  (#'cr/print-doc (meta (resolve s))))
