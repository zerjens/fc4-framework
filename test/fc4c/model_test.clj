(ns fc4c.model-test
  (:require [clojure.test :refer [deftest is]]
            [fc4c.model :as m]
            [fc4c.test-utils :refer [check]]))

(deftest elements-from-file (check `m/elements-from-file 100))
(deftest fixup-element (check `m/fixup-element 100))
(deftest get-tags-from-path (check `m/get-tags-from-path))
(deftest to-set-of-keywords (check `m/to-set-of-keywords))