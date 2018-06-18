(ns fc4c.model-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer [deftest]]
            [expound.alpha :as expound :refer [explain-results]]
            [fc4c.model :as m]
            [fc4c.test-utils :refer [check]]))

(deftest add-ns (check `m/add-ns 10))
(deftest add-tags (check `m/add-tags 10))
(deftest qualify-keys (check `m/qualify-keys 10))

;; If you’re wondering why there’s no tests for m/read, m/read-elements, and
;; m/elements-from-file, it’s because those functions do I/O (they read YAML
;; files from disk and conver them into elements and a model) and we can’t do
;; generative testing with them. We _can_ do “example testing” with them, i.e.
;; more traditional unit tests, by simply adding a bunch of YAML files to our
;; test suite and having tests that point those functions to them, then validate
;; the results. And we probably *will* do that. But first, these functions need
;; to be refactored a bit to more properly separate I/O from computation — to
;; “move IO to the edges”.
