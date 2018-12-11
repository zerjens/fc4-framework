(ns fc4.integrations.structurizr.express.util-test
  (:require [fc4.integrations.structurizr.express.util :as u]
            [clojure.test :refer [deftest]]
            [fc4.test-utils :refer [check]]))

(deftest probably-diagram-yaml? (check `u/probably-diagram-yaml?))
