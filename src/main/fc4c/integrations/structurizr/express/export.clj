(ns fc4c.integrations.structurizr.express.export
  "Functions concerned with exporting FC4 views as Structurizr Express diagrams."
  (:require [cognitect.anomalies  :as anom]
            [clj-yaml.core        :as yaml]
            [clojure.walk         :as walk :refer [postwalk]]
            [clojure.spec.alpha   :as s]
            [clojure.string       :as string :refer [includes? join]]
            [fc4c.integrations.structurizr.express.spec] ;; for side fx
            [fc4c.io              :as io]
            [fc4c.model           :as m]
            [fc4c.styles          :as st]
            [fc4c.util            :as u :refer [update-all]]
            [fc4c.view            :as v]
            [fc4c.yaml            :as fy :refer [split-file]]))

(defn- position
  [sys-name view]
  (get-in view  (if (= sys-name (::v/system view))
                  [::v/positions ::v/subject]
                  [::v/positions ::v/other-systems sys-name])))

(defn- maybe-add-in-house-tag
  [tags]
  (if (contains? tags :external)
    tags
    (conj tags :in-house)))

(s/fdef maybe-add-in-house-tag
        :args (s/cat :tags ::m/tags)
        :ret  ::m/tags)

(defn- replace-internal-tag
  "The tag “internal” is a special reserved tag for Structurizr Express; for
  diagrams that indicate an “inside” and an “outside”, such as System Landscape
  diagrams and Container diagrams, the set of elements that have “internal”
  (case insensitive) is used to draw the boundary box. So when exporting an FC4
  view+model+styles as Structurizr Express diagram, we need to transform the
  tag :interal to something that is _not_ reserved, so we use :in-house."
  [tags]
  (if-not (contains? tags :internal)
    tags
    (-> tags
        (disj :internal)
        (conj :in-house))))

(s/fdef replace-internal-tag
        :args (s/cat :tags ::m/tags)
        :ret  ::m/tags)

(defn- tags
  [elem]
  (->> (::m/tags elem)
       (replace-internal-tag)
       (maybe-add-in-house-tag)
       (map name) ; converts set to a seq but in this case that’s OK as we then convert it to a str
       (join ",")))

(defn- sys-elem
  ;; TODO: this should *maybe* be combined with user-elem
  "Constructs a Structurizr Express \"Software System\" element for the named
  system for the given view and model. For now, excludes containers."
  [sys-name view model]
  (let [system (get-in model [::m/systems sys-name])]
    (merge (select-keys system [::m/name ::m/description ::m/tags])
           {:type "Software System"
            :position (position sys-name view)
            :tags (tags system)})))

(defn- user-elem
  ;; TODO: this should *maybe* be combined with sys-elem
  "Constructs a Structurizr Express \"Person\" element for the named
  user for the given view and model."
  [user-name view model]
  (let [user (get-in model [::m/users user-name])]
    (merge (select-keys user [::m/name ::m/description ::m/tags])
           {:type "Person"
            :position (get-in view [::v/positions ::v/users user-name])
            :tags (tags user)})))

(defn- deps-of
  "Returns the systems that the subject system uses — its dependencies."
  ;; TODO: maybe should be merged with users-of?
  ;; TODO: this should handle the case of a system that the subject uses >1 ways
  [system model]
  (some->>
   (::m/containers system)
   (mapcat ::m/uses)
   (filter ::m/system)
   (map (fn [{sys-name ::m/system :as dep}]
          (assoc dep ::m/system ; “overwrite” the value of :system with the system
                 (get-in model [::m/systems sys-name] {:missing sys-name})))))) ;; TODO: maybe return an anomaly instead?

(defn- users-of
  "Returns the systems that use the subject system."
  ;; TODO: maybe should be merged with deps-of?
  [subject-name model]
  (some->>
   (merge (::m/systems model) (::m/users model))
   (vals)
   (filter (fn [elem]
             (seq (->> (concat (::m/uses elem)
                               (map ::m/uses (::m/containers elem)))
                       (filter (fn [dep] (= (::m/system dep) subject-name)))))))))

(defn- dep->relationship
  ;; TODO: maybe should be merged with user->relationship?
  ;; TODO: this should handle the case of a system that the subject uses >1 ways
  [dep subject-name]
  (merge (select-keys dep [::m/description ::m/technology])
         {:source subject-name
          :destination (get-in dep [::m/system ::m/name])}))

(defn- user->relationships
  "User here means a system, person, or user that uses the subject system."
  ;; TODO: maybe should be merged with dep->relationship?
  [{user-name ::m/name :as user} subject-name]
  (let [rel {:source user-name, :destination subject-name}]
    (->> (::m/uses user)
         (map (fn [use]
                (merge rel (select-keys use [::m/description ::m/technology])))))))

(defn- get-subject
  [{subject-name ::v/system :as view} model]
  (get-in model [::m/systems subject-name]))

(defn- elements
  [view model]
  ;; TODO: clean this up; it’s ugly and hard to follow
  (let [other-systems-names (-> view ::v/positions ::v/other-systems keys)
        subject-name (::v/system view)
        sys-names (conj other-systems-names subject-name)
        sys-elems (map #(sys-elem % view model) sys-names)
        user-names (-> view ::v/positions ::v/users keys)
        user-elems (map #(user-elem % view model) user-names)]
    (concat user-elems sys-elems)))

(defn- relationship-with
  "Given a relationship and the subject name, returns the name of the other side
  of the relationship, regardless of the directionality of the relationship."
  [subject-name
   {:keys [source destination] :as rel}]
  (condp = subject-name
    source destination
    destination source
    (throw (ex-info "Relationship does not include subject!"
                    {:relationship rel
                     :subject-name subject-name}))))

(defn- inject-control-points
  "Given the set of relationships with a single system, and potentially a set of
  point-groups (might be nil) for those relationships, injects those control
  points into the relationships."
  [rels point-groups]
  (->> rels
       (map-indexed (fn [i rel]
                      (if-let [points (nth point-groups i nil)]
                        (assoc rel :vertices points)
                        rel)))))

(defn- add-control-points
  "Add control points to relationships, when they’re specified in the view."
  [rels
   {subject-name ::v/system
    {point-groups ::v/system-context} ::v/control-points
    :as view}]
  (->> rels
       (group-by (partial relationship-with subject-name))
       (mapcat (fn [[other-side-name these-rels]]
                 (->> (get point-groups other-side-name)
                      (inject-control-points these-rels))))))

(defn- relationships
  [view model]
  (let [{subject-name ::m/name :as subject} (get-subject view model)
        deps (deps-of subject model)
        users (users-of subject-name model)]
    (-> (map #(dep->relationship % subject-name) deps)
        (concat (mapcat #(user->relationships % subject-name) users))
        (add-control-points view)
        ;; In the model, two different containers of the same system can have
        ;; relationships to the same *other* system. In a container diagram
        ;; those relationships may be meaningfully different. In a System
        ;; Context diagram, however, those relationships are essentially
        ;; duplicates and can and should be collapsed down to a single
        ;; relationship.
        ;; Using distinct to do this is a bit of a hack, because it’ll only work
        ;; if the relationship maps returned by dep->relationship are equal. And
        ;; while that happens to be the case with the system I’m working with
        ;; right now, there’s no guarantee that that’ll be the case with all
        ;; systems going forward. So eventually this simplistic call to distinct
        ;; will probably need to be replaced with something more sophisticated.
        ;; Perhaps that routine will merge the relationships together,
        ;; concatenating the descriptions if they differ — TBD.
        (distinct))))

(defn dequalify-keys
  "Given a nested map, removes the namespaces from any keys that are qualified
  keywords."
  {:fork-of 'clojure.walk/stringify-keys}
  [m]
  (update-all
   (fn [[k v]]
     (if (qualified-keyword? k)
       [(keyword (name k)) v]
       [k v]))
   m))

(defn- rename-internal-tag
  "Please see docstring of replace-internal-tag."
  [styles]
  (map
   (fn [style]
     (if-not (= (::st/tag style) "internal")
       style
       (update style ::st/tag (fn [_] "in-house"))))
   styles))

(s/fdef rename-internal-tag
        :args (s/cat :styles ::st/styles)
        :ret  ::st/styles)

(defn view->system-context
  "Converts an FC4 view to a Structurizr Express System Context diagram."
  [view model styles]
  (dequalify-keys
   {:type "System Context"
    :scope (::v/system view)
    :elements (elements view model)
    :relationships (relationships view model)
    :styles (rename-internal-tag styles)
    :size (::v/size view)}))

(s/fdef view->system-context
        :args (s/cat :view ::v/view
                     :model ::m/model
                     :styles ::st/styles)
        :ret :structurizr/diagram)
