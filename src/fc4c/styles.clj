(ns fc4c.styles
  (:require [clj-yaml.core           :as yaml]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            ; This ns is required solely for the side fx of loading the file:
            ; registering the desired specs in the spec registry.
            [fc4c.integrations.structurizr.express.spec]
            [fc4c.spec               :as fs]
            [fc4c.util               :as util]))

(s/def ::background :structurizr.style/background)
(s/def ::border :structurizr.style/border)
(s/def ::color :structurizr.style/color)
(s/def ::dashed :structurizr.style/dashed)
(s/def ::height :structurizr.style/height)
(s/def ::shape :structurizr.style/shape)
(s/def ::tag :structurizr.style/tag)
(s/def ::type :structurizr.style/type)
(s/def ::width :structurizr.style/width)

(s/def ::style
  (s/keys
   :req [::type ::tag]
   :opt [::background ::border ::color ::dashed ::height ::shape ::width]))

(s/def ::styles (s/coll-of ::style :min-count 1))

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn styles-from-file
  "Parses the contents of a YAML file, then processes those contents such that
  they conform to ::style."
  [file-contents]
  (-> (yaml/parse-string file-contents)
      (util/qualify-keys this-ns-name)))

(s/def ::yaml-file-contents
  (s/with-gen
    ::fs/non-blank-str
    #(gen/fmap yaml/generate-string (s/gen ::styles))))

(s/fdef styles-from-file
        :args (s/cat :file-contents ::yaml-file-contents)
        :ret  ::styles
        :fn   (fn [{{:keys [file-contents]} :args, ret :ret}]
                (= file-contents
                   (yaml/generate-string ret))))
