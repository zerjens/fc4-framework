(ns fc4.integrations.structurizr.express.render-test
  (:require [fc4.integrations.structurizr.express.render :as r]
            [fc4.test-utils :refer [check]]
            [fc4.test-utils.image-diff :refer [bytes->buffered-image image-diff]]
            [clojure.java.io :refer [copy file input-stream output-stream]]
            [clojure.spec.alpha :as s]
            [clojure.string :refer [includes?]]
            [clojure.test :refer [deftest testing is]]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]))

; Require image-resizer.core while preventing the Java app icon from popping up
; and grabbing focus on MacOS.
; Approach found here: https://stackoverflow.com/questions/17460777/stop-java-coffee-cup-icon-from-appearing-in-the-dock-on-mac-osx/17544259#comment48475681_17544259
; This require is here rather than in the ns form at the top of the file because
; if I include this ns in the require list in the ns form, then the only way to
; suppress the app icon from popping up and grabbing focus would be to place the
; System/setProperty call at the top of the file, before the ns form, and that’d
; violate Clojure idioms. When people open a clj file, they expect to see a ns
; form right at the top declaring which namespace the file defines and
; populates.
; To be clear, calling the `require` function in a clj file, to require a
; dependency, outside of the ns form, is *also* non-idiomatic; people expect all
; of the dependencies of a file to be listed in the ns form. So I had to choose
; between two non-idiomatic solutions; I chose this one because it seems to me
; to be slightly less jarring for Clojurists.
(do
  (System/setProperty "apple.awt.UIElement" "true")
  (require '[image-resizer.core :refer [resize]]))

(defn binary-slurp
  "fp should be either a java.io.File or something coercable to such by
  clojure.java.io/file."
  [fp]
  (let [f (file fp)]
    (with-open [out (java.io.ByteArrayOutputStream. (.length f))]
      (copy f out)
      (.toByteArray out))))

(defn binary-spit
  "fp should be either a java.io.File or something coercable to such by
  clojure.java.io/file."
  [fp data]
  (with-open [out (output-stream (file fp))]
    (copy data out)))

(def max-allowable-image-difference
  ;; This threshold might seem low, but the diffing algorithm is
  ;; giving very low results for some reason. This threshold seems
  ;; to be sufficient to make the random watermark effectively ignored
  ;; while other, more significant changes (to my eye) seem to be
  ;; caught. Still, this is pretty unscientific, so it might be worth
  ;; looking into making this more precise and methodical.
  0.005)

(deftest valid?
  (testing "example test"
    (let [result (r/valid? "this is not YAML? Or I guess maybe it is?")]
      (is (s/valid? ::anom/anomaly result))
      (is (every? #(includes? (::anom/message result) %)
                  ["A cursory check"
                   "almost certainly not"
                   "valid Structurizr Express diagram definition"
                   "contain some crucial keywords"]))))
  (testing "property tests"
    (check `r/valid? 300)))

(def dir "test/data/structurizr/express/")

(defn temp-png-file
  [basename]
  (java.io.File/createTempFile basename ".png"))

(deftest render
  (testing "happy paths"
    (testing "rendering a Structurizr Express file"
      (let [yaml (slurp (file dir "diagram_valid_cleaned.yaml"))
            {:keys [::r/png-bytes ::r/stderr] :as result} (r/render yaml)
            actual-bytes png-bytes
            expected-bytes (binary-slurp (file dir "diagram_valid_cleaned_expected.png"))
            difference (->> [actual-bytes expected-bytes]
                            (map bytes->buffered-image)
                            (map #(resize % 1000 1000))
                            (reduce image-diff))]
        (is (s/valid? ::r/result result) (s/explain-str ::r/result result))
        (is (<= difference max-allowable-image-difference)
            ;; NB: below in addition to returning a message we write the actual
            ;; bytes out to the file system, to help with debugging. But
            ;; apparently `is` evaluates this `msg` arg eagerly, so it’s
            ;; evaluated even if the assertion is true. This means that even
            ;; when the test passes the “expected” file is written out to the
            ;; filesystem. So TODO: maybe we should do something about this.
            (let [expected-debug-fp (temp-png-file "rendered_expected.png")
                  actual-debug-fp (temp-png-file "rendered_actual.png")]
              (binary-spit expected-debug-fp expected-bytes)
              (binary-spit actual-debug-fp actual-bytes)
              (str stderr
                   "Images are "
                   difference
                   " different, which is higher than the threshold of "
                   max-allowable-image-difference
                   "\n“expected” PNG written to:" (.getPath expected-debug-fp)
                   "\n“actual” PNG written to:" (.getPath actual-debug-fp)))))))
  (testing "sad paths"
    ;; TODO: validate that the ::anom/message contains the required formatting (the two fenced sections)
    (testing "inputs that contain no diagram definition whatsoever"
      (doseq [input [""
                     "this is not empty, but it’s not a diagram!"]]
        (let [{:keys [::anom/message ::r/error] :as result} (r/render input)]
          (is (s/valid? ::r/failure result)
              (expound-str ::r/failure result))
          (is (every? (partial includes? message)
                      ["RENDERING FAILED"
                       "Errors were found in the diagram definition"
                       "No diagram has been defined"]))
          (is (includes? (::r/message error) "Errors were found in the diagram definition"))
          (is (includes? (-> error ::r/errors first ::r/message) "No diagram has been defined")))))
    (testing "inputs that contain invalid diagram definitions"
      (doseq [[fname-suffix expected-strings]
              {"a.yaml" ["Diagram scope" "software system named" "undefined" "could not be found"]
               "b.yaml" ["The diagram type must be" "System Landscape" "Dynamic"]
               "c.yaml" ["relationship destination element named" "Does not exist" "does not exist"]}]
        (let [path (file dir (str "se_diagram_invalid_" fname-suffix))
              input (slurp path)
              {:keys [::anom/message ::r/error] :as result} (r/render input)]
          (is (s/valid? ::r/failure result)
              (expound-str ::r/failure result))
          (is (every? (partial includes? message) expected-strings))
          (is (every? (partial includes? (-> error ::r/errors first ::r/message)) expected-strings))
          (is (includes? (::r/message error) "Errors were found in the diagram definition")))))))
