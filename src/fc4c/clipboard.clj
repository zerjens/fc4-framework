(ns fc4c.clipboard
  "A few useful funcs lifted from https://gist.github.com/Folcon/1167903"
  (:import [java.awt Toolkit]
           [java.awt.datatransfer DataFlavor StringSelection])
  (:refer-clojure :exclude [slurp spit]))

; Suppress the Java icon from popping up and grabbing focus on MacOS.
; Found in a comment to this answer: https://stackoverflow.com/a/17544259/7012
(System/setProperty "apple.awt.UIElement" "true")

(def clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit)))

(def string-flavor (DataFlavor/stringFlavor))

(defn slurp []
  (try
    ;; We’re gonna check twice for the contents being a string because of
    ;; possible race conditions.
    (when (.isDataFlavorAvailable clipboard string-flavor)
      (let [transferable (.getContents clipboard nil)]
        (when (.isDataFlavorSupported transferable string-flavor)
          (.getTransferData transferable string-flavor))))
    (catch java.lang.NullPointerException e nil)))

(defn spit [text]
  (.setContents clipboard (StringSelection. text) nil))
