(ns fc4c.styles-test
  (:require [clojure.test :refer [deftest is]]
            [fc4c.styles :as st]
            [fc4c.test-utils :refer [check]]))

(deftest styles-from-file (check `st/styles-from-file 1000))
