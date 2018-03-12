(ns fc4c.clipboard
  "A few useful funcs lifted from https://gist.github.com/Folcon/1167903"
  (:refer-clojure :exclude [slurp spit]))

; Suppress the Java icon from popping up and grabbing focus on MacOS.
; Found in a comment to this answer: https://stackoverflow.com/a/17544259/7012
(System/setProperty "apple.awt.UIElement" "true")

(def clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn slurp []
  (try
    (.getTransferData (.getContents clipboard nil) (java.awt.datatransfer.DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn spit [text]
  (.setContents clipboard (java.awt.datatransfer.StringSelection. text) nil))
