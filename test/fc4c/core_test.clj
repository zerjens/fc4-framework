(ns fc4c.core-test
  (:require [fc4c.core :as rc]
            [clojure.test :refer [deftest]]
            [fc4c.test-utils :refer [check]]))

;; temporarily break the tests to ensure that CI builds will fail when the tests fail
(deftest whatever (clojure.test/is (= 1 2)))

(deftest blank-nil-or-empty? (check `rc/blank-nil-or-empty?))
(deftest parse-coords (check `rc/parse-coords))
(deftest round-to-closest (check `rc/round-to-closest))
(deftest snap-coords (check `rc/snap-coords))
(deftest shrink (check `rc/shrink 300))
(deftest process (check `rc/process 300))
