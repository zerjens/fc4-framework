(ns fc4c.io-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test       :as ct :refer [deftest is]]
            [fc4c.io            :as io]
            [fc4c.model         :as m]
            [fc4c.styles        :as st]
            [fc4c.view          :as v]))

;;; TODO: these are all happy-path tests; some sad-path tests are sorely needed!

(deftest read-model
  (is (s/valid? ::m/model (io/read-model "test/data/model"))))

(deftest read-view
  (is (s/valid? ::v/view (io/read-view "test/data/views/middle.yaml"))))

(deftest read-styles
  (is (s/valid? ::st/styles (io/read-styles "test/data/styles.yaml"))))
