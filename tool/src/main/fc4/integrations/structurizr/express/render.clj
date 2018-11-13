(ns fc4.integrations.structurizr.express.render
  (:require [clojure.java.io    :as io    :refer [file]]
            [clojure.java.shell :as shell :refer [sh]]
            [clojure.spec.alpha :as s]))

(defn renderer-command
  []
  (let [relative-paths ["render"
                        "renderer/render.js"
                        "target/pkg/renderer/render-macos"
                        "target/pkg/renderer/render-linux"]
        hopefully-on-path "fc4-render"]
    (or (some #(when (.canExecute (file %)) %) relative-paths)
        hopefully-on-path)))

(defn render
  "Renders a Structurizr Express diagram as a PNG file, returning a PNG
  bytearray. Not entirely pure; spawns a child process to perform the rendering.
  FWIW, that process is stateless and ephemeral."
  [diagram-yaml]
  ;; TODO: some way to pass options to the renderer (--debug, --quiet, --verbose)
  ;; TODO: use ProcessBuilder (or some Clojure wrapper for such) rather than sh
  ;; so we can stream output from stderr to stderr so we can display progress as
  ;; it happens, so the user knows that something is actually happening!
  ;; TODO: should this really throw an exception on any and all errors? Since
  ;; we’re returning a map in the success case, how about instead we return an
  ;; “anomaly” map?
  (let [result (sh (renderer-command)
                   :in diagram-yaml
                   :out-enc :bytes)
        {:keys [exit out err]} result]
    (if (zero? exit)
      {::png-bytes out
       ::stderr    err}
      (throw (Exception. err)))))

(s/def ::yaml-string string?)
(s/def ::png-bytes bytes?)
(s/def ::stderr string?)
(s/def ::result (s/keys :req [::png-bytes ::stderr]))

; This spec is here mainly for documentation and instrumentation. I don’t
; recommend using it for generative testing, mainly because rendering is
; currently extremely slow (~12s on my system).
(s/fdef render
        :args ::yaml-string
        :ret  ::result)

(comment
  (use 'clojure.java.io 'clojure.java.shell 'fc4.io)
  (require :reload '[fc4.integrations.structurizr.express.render :as r])
  (in-ns 'fc4.integrations.structurizr.express.render)

  ; diagram-yaml
  (def dy (slurp "test/data/structurizr/express/diagram_valid_cleaned.yaml"))

  ; png-bytes
  (def result (render dy))
  (def pngb (::png-bytes result))

  (binary-spit "/tmp/diagram.png" pngb))
