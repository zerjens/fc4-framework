(ns fc4.io.render
  "Functions for rendering Structurizr Express diagrams into PNG image files.
  NB: because these functions are specifically intended for implementing CLI
  commands, some of them write to stdout/stderr and may call fc4.cli.util/fail
  (which calls System/exit unless fc4.cli.util/*exit-on-fail* is rebound)."
  (:require [cognitect.anomalies :as anom]
            [clojure.java.io :as io :refer [file output-stream]]
            [clojure.string :as str :refer [split]]
            [fc4.cli.util :as cu :refer [debug]]
            [fc4.io :refer [binary-spit]]
            [fc4.integrations.structurizr.express.render :as r])
  (:import [java.io FileNotFoundException]))

(defn fail
  [file-path msg]
  (cu/fail (str "Error rendering " file-path ": " msg)))

(defn read-text-file
  [path]
  (try (slurp path)
       (catch FileNotFoundException _ (fail path "file not found"))
       (catch Exception e (fail path (.getMessage e)))))

(defn validate
  [yaml path]
  (let [valid (r/valid? yaml)]
    (when-not (true? valid)
      (fail path (::anom/message valid)))))

(defn render
  [yaml path]
  (println (str path "..."))
  (let [result (r/render yaml)]
    (debug (::r/stderr result))
    result))

(defn tmp-png-file
  [path]
  (-> (file path) (.getName)
      (split #"\." 3) (first) ; remove “extension”
      (java.io.File/createTempFile ".maybe.png")))

(defn check
  [result path]
  (debug "checking result for errors...")
  (condp #(contains? %2 %1) result
    ::anom/message (fail path (::anom/message result))
    ::r/png-bytes :all-good
    (fail path (str "Internal error: render result invalid (has neither"
                    " ::anom/message nor ::r/png-bytes)")))

  (debug "checking PNG data size...")
  (when (< (count (::r/png-bytes result))
           ; arbitrary number is arbitrary. That said, according to my gut, less
           ; data is likely to be invalid, and more has a chance of being valid.
           1024)
    (let [tmpfile (tmp-png-file path)]
      (with-open [out (output-stream tmpfile)]
        (.write out (::r/png-bytes result)))
      (fail path (str "PNG data is <1K so it’s likely invalid. It’s been"
                      " written to " tmpfile " for debugging."))))

  (debug "rendering seems to have succeeded!"))

(defn get-out
  [in-path]
  (str/replace in-path #"\.ya?ml$" ".png"))

(defn render-diagram-file
  [in-path]
  (let [yaml     (read-text-file in-path)
        _        (validate yaml in-path)
        result   (render yaml in-path)
        _        (check result in-path)
        out-path (get-out in-path)]
    (binary-spit out-path (::r/png-bytes result))))
