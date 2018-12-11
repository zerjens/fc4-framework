(ns fc4.integrations.structurizr.express.render
  (:require [clojure.java.io      :as io      :refer [file]]
            [clojure.java.shell   :as shell   :refer [sh]]
            [clojure.data.json    :as json]
            [clojure.spec.alpha   :as s]
            [clojure.string       :as str     :refer [ends-with? includes? split trim]]
            [cognitect.anomalies  :as anom]
            [expound.alpha        :as expound :refer [expound-str]]
            [fc4.integrations.structurizr.express.spec :as ss]
            [fc4.util             :as fu    :refer [namespaces]]))

(namespaces '[structurizr :as st])

(defn jar-dir
  "Utility function to get the path to the dir in which the jar in which this
  function is invoked is located.
  Adapted from https://stackoverflow.com/a/13276993/7012"
  []
  ;; The .toURI step is vital to avoid problems with special characters,
  ;; including spaces and pluses.
  ;; Source: https://stackoverflow.com/q/320542/7012#comment18478290_320595
  (-> (class *ns*)
      .getProtectionDomain .getCodeSource .getLocation .toURI .getPath
      file .toPath .getParent .toFile))

(defn renderer-command
  []
  (let [possible-paths [; This first one must be first so it’s preferred to an
                        ; “installed” executable when running tests from source
                        "renderer/render.js"
                        ; Used when the tool is packaged in a jar
                        (str (file (jar-dir) "fc4-render"))
                        "render"
                        "target/pkg/renderer/render-mac"
                        "target/pkg/renderer/render-macos"
                        "target/pkg/renderer/render-linux"]
        hopefully-on-path "fc4-render"]
    (or (some #(if (.canExecute (file %)) %)
              possible-paths)
        hopefully-on-path)))

(defn get-fenced
  "If fence is found, returns the fenced string; if not, throws."
  [s sep]
  (or (some-> (split s (re-pattern sep) 3)
              (second)
              (trim))
      (throw (Exception. (str "Error finding fenced segments in error output: "
                              s)))))

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn parse-json-err
  [js]
  (try
    (json/read-str js :key-fn (partial keyword this-ns-name))
    (catch Exception e
      (throw (if (includes? (.getMessage e) "JSON error")
               (Exception. (str "Error while parsing JSON fenced by 🤖🤖🤖: " js)
                           e)
               e)))))

(defn parse-stderr-err
  "Parses the contents of stderr, presumably the output of a failed invocation
  of the renderer, into a structured value."
  [stderr]
  {::human-output (get-fenced stderr "🚨🚨🚨")
   ::error        (parse-json-err (get-fenced stderr "🤖🤖🤖"))})

(s/def ::stderr string?)
(s/def ::human-output string?)
(s/def ::message string?)
(s/def ::errors (s/coll-of ::error))
(s/def ::error (s/keys :req [::message]
                       :opt [::errors]))

(s/fdef parse-stderr-err
        :args ::stderr
        :ret  (s/keys :req [::error ::human-output]))

(defn render
  "Renders a Structurizr Express diagram as a PNG file, returning a PNG
  bytearray on success. Not entirely pure; spawns a child process to perform the rendering.
  FWIW, that process is stateless and ephemeral."
  [diagram-yaml]
  ;; Protect developers from themselves
  {:pre [(not (ends-with? diagram-yaml ".yaml"))
         (not (ends-with? diagram-yaml ".yml"))]}
  ;; TODO: some way to pass options to the renderer (--debug, --quiet, --verbose)
  ;; TODO: use ProcessBuilder (or some Clojure wrapper for such) rather than sh
  ;; so we can stream output from stderr to stderr so we can display progress as
  ;; it happens, so the user knows that something is actually happening!
  (let [command (renderer-command)
        result (sh command
                   :in diagram-yaml
                   :out-enc :bytes)
        {:keys [exit out err]} result]
    (if (zero? exit)
      {::png-bytes out
       ::stderr    err}
      (let [{:keys [::human-output ::error]} (parse-stderr-err err)]
        {::anom/category ::anom/fault
         ::anom/message  human-output
         ::stderr        err
         ::error         error}))))

(s/def ::png-bytes (s/and bytes? #(> (count %) 0)))
(s/def ::result (s/keys :req [::png-bytes ::stderr]))

(s/def ::failure
  (s/merge ::anom/anomaly (s/keys :req [::stderr ::error])))

; This spec is here mainly for documentation and instrumentation. I don’t
; recommend using it for generative testing, mainly because rendering is
; currently quite slow (~2s on my system).
(s/fdef render
        :args (s/cat :diagram ::st/diagram-yaml-str)
        :ret  (s/or :success ::result
                    :failure ::failure))

(comment
  (use 'clojure.java.io 'clojure.java.shell 'fc4.io.util)
  (require :reload '[fc4.integrations.structurizr.express.render :as r])
  (in-ns 'fc4.integrations.structurizr.express.render)

  ; diagram-yaml
  (def dy (slurp "test/data/structurizr/express/diagram_valid_cleaned.yaml"))

  ; png-bytes
  (def result (render dy))
  (def pngb (or (::png-bytes result)
                (::anom/message result)
                "WTF"))

  (binary-spit "/tmp/diagram.png" pngb))
