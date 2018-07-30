(ns fc4c.integrations.structurizr.express.clipboard-test
  (:require [clojure.test                                     :as ct
             :refer [deftest testing is]]
            [fc4c.integrations.structurizr.express.clipboard  :as c])
  (:import [java.awt.datatransfer StringSelection]))

(defn- faux-spit
  "Used as a replacement for c/spit because when running tests in our CI
  environment there’s no clipboard available and c/spit just raises an exception
  to that effect.

  It’d probably be better to install X11 or Xorg and run them headless so that a
  clipboard would be available and we could test with the actual function, but I
  don’t have time to figure that out right now."
  [faux-clipboard s]

  ; Attempt to create a StringSelection with the supplied value because this
  ; happens in the real c/spit, and it’s a potential failure point, as this can
  ; only succeed if the supplied value is actually a string; if the supplied
  ; value is _not_ a string and cannot be automatically cast to a string (by
  ; the Clojure runtime) then a ClassCastException will be thrown. This did in
  ; fact actually happen.
  (StringSelection. s)

  ; Now “write” to the supplied faux-clipboard
  (reset! faux-clipboard s))

(deftest pcb
  (let [faux-clipboard (atom nil)]
    (with-redefs [c/slurp #(deref faux-clipboard)
                  c/spit  (partial faux-spit faux-clipboard)]
      (testing "happy path"
        (testing "processing a file that’s valid and already formatted"
          (let [fp  "test/data/structurizr/express/diagram_valid_formatted.yaml"
                in  (slurp fp)
                _   (c/spit in)
                out (c/pcb)]
            (is (= in out))))))))

(deftest try-process
  (testing "happy path"
    (testing "processing a file that’s valid and already formatted"
      (let [fp  "test/data/structurizr/express/diagram_valid_formatted.yaml"
            in  (slurp fp)
            out (binding [*out* (java.io.StringWriter.)]
                  (c/try-process in))]
        (is (= in out))))))
