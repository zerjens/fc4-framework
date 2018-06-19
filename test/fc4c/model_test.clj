(ns fc4c.model-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer [deftest]]
            [expound.alpha :as expound :refer [explain-results]]
            [fc4c.model :as m]
            [fc4c.test-utils :refer [check]]))

(deftest add-ns (check `m/add-ns 100))
(deftest elements-from-file (check `m/elements-from-file 100))
(deftest fixup-element (check `m/fixup-element 100))
(deftest get-tags-from-path (check `m/get-tags-from-path 100))
(deftest qualify-keys (check `m/qualify-keys 100))
(deftest to-set-of-keywords (check `m/to-set-of-keywords 100))
(deftest update-all (check `m/update-all 100))
