(ns fc4c.view-test
  (:require [clojure.test :refer [deftest is]]
            [fc4c.view :as v]
            [fc4c.test-utils :refer [check]]))

(deftest view-from-file (check `v/view-from-file 1000))
(deftest fixup-keys (check `v/fixup-keys 1000))
