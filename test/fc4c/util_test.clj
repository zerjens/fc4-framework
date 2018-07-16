(ns fc4c.util-test
  (:require [clojure.test :refer [deftest is]]
            [fc4c.util :as u]
            [fc4c.test-utils :refer [check]]))

(deftest add-ns       (check `u/add-ns))
(deftest qualify-keys (check `m/qualify-keys))
(deftest update-all   (check `m/update-all))
