(ns fc4c.test-utils
  (:require [clojure.test             :as t :refer [is]]
            [clojure.spec.alpha       :as s]
            [clojure.spec.test.alpha  :as st]
            [clojure.pprint           :as pprint]
            [expound.alpha            :as expound]))

;; Utility functions to integrate clojure.spec.test/check and expound with
;; clojure.test, originally inspired by
;;   https://gist.github.com/Risto-Stevcev/dc628109abd840c7553de1c5d7d55608

(defn opts
  [num-tests]
  {:clojure.spec.test.check/opts {:num-tests num-tests}})

;; TODO: maybe this should be a macro; might improve stack traces (which
;; currently show the `is` as being in this file, in this fn, because it
;; is.)
(defn check
  ([sym] (check sym 1000))
  ([sym num-tests]
   (let [results (st/check sym (opts num-tests))]
     (is (nil? (-> results first :failure))
         (binding [s/*explain-out* expound/printer]
           (expound/explain-results-str results))))))
