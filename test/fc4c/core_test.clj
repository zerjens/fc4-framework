(ns fc4c.core-test
  (:require [fc4c.core :as rc]
            [clojure.test :refer [deftest testing is]]
            [clojure.test.check.generators :as gen]
            [clojure.spec.alpha :as s]
            [fc4c.test-utils :refer [check]]))

(deftest blank-nil-or-empty? (check `rc/blank-nil-or-empty?))
(deftest parse-coords (check `rc/parse-coords))
(deftest round-to-closest (check `rc/round-to-closest))
(deftest snap-coords (check `rc/snap-coords))
(deftest shrink (check `rc/shrink 300))
(deftest process (check `rc/process 300))

(deftest process-file
  (check `rc/process-file 200)
  (testing "when the front matter has an extra newline at the end"
    (let [d (-> (s/gen :fc4c/diagram) gen/generate rc/stringify)
          yf (str rc/default-front-matter "\n\n---\n" d)
          [_ str-result] (rc/process-file yf)]
      (is (not (re-seq #"\n\n---\n" str-result))))))
