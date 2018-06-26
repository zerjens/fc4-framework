(ns fc4c.io-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [fc4c.io :as io]
            [fc4c.model :as m]))

(deftest read-model (is (s/valid? ::m/model (io/read-model "test/data/model"))))
