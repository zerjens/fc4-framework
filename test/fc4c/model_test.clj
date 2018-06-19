(ns fc4c.model-test
  (:refer-clojure :exclude [read])
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [fc4c.model :as m]
            [fc4c.test-utils :refer [check]]))

;;;; Generative Tests
(deftest add-ns (check `m/add-ns 500))
(deftest elements-from-file (check `m/elements-from-file 100))
(deftest fixup-element (check `m/fixup-element 100))
(deftest get-tags-from-path (check `m/get-tags-from-path))
(deftest qualify-keys (check `m/qualify-keys 100))
(deftest to-set-of-keywords (check `m/to-set-of-keywords))
(deftest update-all (check `m/update-all 100))

;;;; Example Tests
(deftest read (is (s/valid? ::m/model (m/read "test/data/model"))))
