(ns fc4c.io-test
  (:require [clojure.spec.alpha   :as s]
            [clojure.test         :as ct :refer [deftest is testing]]
            [cognitect.anomalies  :as anom]
            [fc4c.io              :as io]
            [fc4c.model           :as m]
            [fc4c.styles          :as st]
            [fc4c.view            :as v])
  (:import [java.io FileNotFoundException]))

(deftest read-model
  (testing "happy path"
    (is (s/valid? ::m/model (io/read-model "test/data/model (valid)"))))

  (testing "sad path:"
    (testing "files on disk contain invalid data as per the specs"
      (let [result (io/read-model "test/data/model (invalid)")]
        (is (not (s/valid? ::m/model result)))
        (is (s/valid? ::io/error result))))

    (testing "dir does not exist:"
      (doseq [[desc path]
              [["root" "foo/bar/root"]
               ; dir exists but does not contain dir "systems"
               ["systems" "test/data/"]
               ["users"
                "test/data/model (invalid)/contains systems but not users"]]]
        (testing desc
          (is (thrown-with-msg? FileNotFoundException (re-pattern desc)
                                (io/read-model path))))))

    (testing "supplied root path is to a file"
      (is (thrown-with-msg? RuntimeException #"not a dir"
                            (io/read-model "test/data/styles (valid).yaml"))))))

(deftest read-view
  (testing "happy path"
    (is (s/valid? ::v/view
                  (io/read-view "test/data/views/middle (valid).yaml"))))

  (testing "sad path:"
    (testing "file on disk contains invalid data as per the specs"
      (let [result (io/read-view "test/data/views/middle (invalid).yaml")]
        (is (not (s/valid? ::v/view result)))
        (is (s/valid? ::io/error result))))

    (testing "file does not exist"
      (is (thrown-with-msg? FileNotFoundException #"foo"
                            (io/read-view "foo"))))))

(deftest read-styles
  (testing "happy path"
    (is (s/valid? ::st/styles
                  (io/read-styles "test/data/styles (valid).yaml"))))

  (testing "sad path:"
    (testing "file on disk contains invalid data as per the specs"
      (let [result (io/read-styles "test/data/styles (invalid).yaml")]
        (is (not (s/valid? ::st/styles result)))
        (is (s/valid? ::io/error result))))

    (testing "file does not exist"
      (is (thrown-with-msg? FileNotFoundException #"foo"
                            (io/read-styles "foo"))))))
