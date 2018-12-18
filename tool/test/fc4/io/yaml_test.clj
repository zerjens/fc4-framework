(ns fc4.io.yaml-test
  (:require [clojure.java.io :refer [file]]
            [clojure.test :as ct :refer [deftest is testing]]
            [fc4.integrations.structurizr.express.edit :as see :refer [process-file]]
            [fc4.io.yaml :as y]
            [fc4.test-utils :as tu :refer [check]]
            [fc4.util :as fu]))

(deftest validate
  (binding [fu/*throw-on-fail* false]
    (check `y/validate)))

(deftest process-diagram-file
  (let [valid        "test/data/structurizr/express/diagram_valid_cleaned.yaml"
        invalid_a    "test/data/structurizr/express/se_diagram_invalid_a.yaml"
        invalid_b    "test/data/structurizr/express/se_diagram_invalid_b.yaml"
        non-existant "test/data/does_not_exist"
        not-text     "test/data/structurizr/express/diagram_valid_cleaned_expected.png"]
    (testing "a YAML file containing a valid SE diagram"
      (let [expected (-> valid slurp process-file ::see/str-processed)
            _ (y/process-diagram-file valid)
            actual (slurp valid)]
        (is (= actual expected))))
    (testing "a YAML file containing a blatantly invalid SE diagram"
      (let [fp invalid_a
            before (slurp fp)]
        (is (thrown-with-msg? Exception
                              #"invalid because it is missing the root property"
                              (y/process-diagram-file fp)))
        (is (= (slurp fp) before))))
    (testing "a YAML file containing a subtly invalid SE diagram"
      (let [fp invalid_a
            before (slurp fp)]
        (is (thrown-with-msg? Exception
                              #"(?s)invalid because it is missing the root property.+scope"
                              (y/process-diagram-file fp)))
        (is (= (slurp fp) before))))
    (testing "a input file path that does not exist"
      (let [fp non-existant]
        (is (thrown-with-msg? Exception #"exist" (y/process-diagram-file fp)))
        (is (not (.exists (file fp))))))
    (testing "a input file that does not contain text"
      (let [fp not-text
            before (slurp fp)]
        (is (thrown-with-msg?
             Exception
             #"(?i)Error.+cursory check.+not a valid Structurizr Express diagram definition"
             (y/process-diagram-file fp)))
        (is (= (slurp fp) before))))))
