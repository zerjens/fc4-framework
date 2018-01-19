(ns minimayaml.clipboard
  "A few useful funcs lifted from https://gist.github.com/Folcon/1167903"
  (:refer-clojure :exclude [get slurp spit]))

(defn get []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn slurp []
  (try
    (.getTransferData (.getContents (get) nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn spit [text]
  (.setContents (get) (java.awt.datatransfer.StringSelection. text) nil))
