(ns fc4c.integrations.structurizr.express.export-test
  (:require [clojure.pprint          :as pp :refer [pprint]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.test.alpha :as st]
            [clojure.string          :as string :refer [includes?]]
            [clojure.test            :as ct :refer [deftest is testing]]
            [cognitect.anomalies     :as anom]
            [expound.alpha           :as expound]
            [fc4c.integrations.structurizr.express.export :as e]
            [fc4c.io                 :as io]
            [fc4c.test-utils :refer [opts]]))

(deftest view->system-context
  (testing "generative"
    (st/check `e/view->system-context (opts 100)))

  (testing "on-disk examples"
    (testing "happy path"
      (let [mp     "test/data/model (valid)/"
            model  (io/read-model mp)
            vp     "test/data/views/middle (valid).yaml"
            view   (io/read-view vp)
            sp     "test/data/styles (valid).yaml"
            styles (io/read-styles sp)
            _ (doseq [[n [v p]] {"model"  [model  mp]
                                 "view"   [view   vp]
                                 "styles" [styles sp]}]
                (when (and (map? v) (contains? v ::anom/category))
                  (throw (ex-info (str "invalid " n " in " p) v))))
            result (e/view->system-context view model styles)
            valid (s/valid? :structurizr/diagram result)]
        (when-not valid
          (expound/expound :structurizr/diagram result)
          (pprint result))
        (is valid)))

    (testing "sad path:"
      (testing "view references undefined system"
        (let [mp     "test/data/model (valid)/"
              model  (io/read-model mp)
              vp     "test/data/views/middle (valid, references undefined system).yaml"
              view   (io/read-view vp)
              sp     "test/data/styles (valid).yaml"
              styles (io/read-styles sp)
              _ (doseq [[n [v p]] {"model"  [model  mp]
                                   "view"   [view   vp]
                                   "styles" [styles sp]}]
                  (when (and (map? v) (contains? v ::anom/category))
                    (throw (ex-info (str "invalid " n " in " p) v))))
              result (e/view->system-context view model styles)
              valid (s/valid? :structurizr/diagram result)
              exposition (expound/expound-str :structurizr/diagram result)]
          (is (not valid))
          (is (includes? exposition "should contain key: :name")))))))
