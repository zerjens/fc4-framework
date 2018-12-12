(ns fc4.io.render-test
  (:require [clojure.spec.alpha   :as s]
            [clojure.string :as str :refer [includes?]]
            [clojure.test         :as ct :refer [deftest is testing]]
            [cognitect.anomalies  :as anom]
            [fc4.io               :as io]
            [fc4.io.render        :as r]
            [fc4.test-utils       :as tu :refer [check]])
  (:import [java.io FileNotFoundException]))

(deftest err-msg (check `r/err-msg))

(deftest read-text-file
  (let [existant     "test/data/styles (valid).yaml"
        non-existant "test/data/does not exist"
        not-text     "test/data/structurizr/express/diagram_valid_cleaned_expected.png"]
    (is (includes? (r/read-text-file existant) "The FC4 Framework"))
    (is (thrown-with-msg? Exception #"file not found" (r/read-text-file non-existant)))
    ; read-text-file is a thin wrapper for slurp; as such it behaves the same as
    ; slurp when passed the path to a non-text file: reads the contents of the file
    ; as a String and returns that String. The String is non-sensical but so be it.
    (is (string? (r/read-text-file not-text)))))

(deftest validate
  (binding [r/*throw-on-fail* false]
    (check `r/validate)))

(deftest tmp-png-file (check `r/tmp-png-file))

(deftest check-fn
  (binding [r/*throw-on-fail* false]
    (check `r/check)))
