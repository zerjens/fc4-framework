(ns fc4c.integrations.structurizr.express.edit-test
  (:require [fc4c.integrations.structurizr.express.edit :as e]
            [fc4c.integrations.structurizr.express.yaml :as y]
            [clojure.test :refer [deftest testing is]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [fc4c.test-utils :refer [check]]))

(deftest blank-nil-or-empty? (check `e/blank-nil-or-empty?))
(deftest parse-coords (check `e/parse-coords))
(deftest process (check `e/process 300))
(deftest reorder (check `e/reorder))
(deftest round-to-closest (check `e/round-to-closest))
(deftest snap-coords (check `e/snap-coords))
(deftest shrink (check `e/shrink 300))

(deftest process-file
  (check `e/process-file 200)

  (testing "happy paths"
    (testing "processing a file that’s valid but is messy and needs cleanup"
      (let [dir "test/data/structurizr/express/"
            in  (slurp (str dir "diagram_valid_messy.yaml"))
            {out ::e/str-processed} (e/process-file in)
            expected (slurp (str dir "diagram_valid_cleaned.yaml"))]
         (is (= expected out)))))

  (testing "when the front matter has an extra newline at the end"
    (let [d (-> (s/gen :structurizr/diagram) gen/generate y/stringify)
          yf (str e/default-front-matter "\n\n---\n" d)
          {str-result ::e/str-processed} (e/process-file yf)]
      (is (not (re-seq #"\n\n---\n" str-result))))))
