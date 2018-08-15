(ns fc4c.yaml-test
  (:require [clojure.test :refer [deftest is]]
            [fc4c.test-utils :refer [check]]
            [fc4c.yaml :as y]))

(deftest split-file (check `y/split-file))
