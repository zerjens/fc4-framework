(ns fc4.io.render
  "Functions for rendering Structurizr Express diagrams into PNG image files.
  NB: because these functions are specifically intended for implementing CLI
  commands, some of them write to stdout/stderr and may call fc4.cli.util/fail
  (which calls System/exit unless fc4.cli.util/*exit-on-fail* is rebound)."
  (:require [cognitect.anomalies :as anom]
            [clojure.java.io :as io :refer [file]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str :refer [ends-with? includes? split]]
            [fc4.io :refer [binary-spit]]
            [fc4.integrations.structurizr.express.render :as r]
            [fc4.spec :as fs])
  (:import [java.io File FileNotFoundException]))

; Feel free to change for development or whatever.
; This is an atom rather than a dynamic var because the functions in this
; namespace are often called from background threads (e.g. a filesystem
; watching thread) and it’s quite annoying to rebind it in such a situation.
(def debug? (atom false))

(defn debug
  [& vs]
  (when @debug?
    (apply println vs)))

(defn err-msg
  [file-path msg]
  (str "Error rendering [" file-path "]: " msg))

(s/fdef err-msg
        :args (s/cat :file ::fs/non-blank-str
                     :msg  ::fs/non-blank-str)
        :ret  ::fs/non-blank-str
        :fn   (fn [{:keys [args ret]}]
                (every? (partial includes? ret)
                        (vals args))))

; rebind for testing
(def ^:dynamic *throw-on-fail* true)

(defn fail
  ([path msg]
   (fail path msg {} nil))
  ([path msg data]
   (fail path msg data nil))
  ([path msg data cause]
   (let [e (if cause
             (ex-info (err-msg path msg) data cause)
             (ex-info (err-msg path msg) data))]
     (if *throw-on-fail*
       (throw e)
       e))))

(defn read-text-file
  [path]
  (try (slurp path)
       (catch FileNotFoundException _ (fail path "file not found"))
       (catch Exception e (fail path (.getMessage e) {} e))))

(defn validate
  [yaml path]
  (let [result (r/valid? yaml)]
    (when-not (true? result)
      (fail path (::anom/message result)))))

(s/fdef validate
        :args (s/cat :yaml (s/or :valid :structurizr/diagram-yaml-str
                                 :invalid string?)
                     :path ::fs/non-blank-simple-str)
        :ret  (s/or :valid   nil?
                    :invalid (partial instance? Exception))
        :fn   (fn [{{:keys [yaml path]} :args, ret :ret}]
                (and (= (first yaml) (first ret))
                     (or (= (first yaml) :valid)
                         (includes? (.getMessage (second ret)) path)))))

(defn remove-filename-extension
  "fp should be a string containing either a filename or a path ending with a
  filename."
  [fp]
  (first (split fp #"\." 3)))

(defn tmp-png-file
  [path]
  (-> (file path)
      (.getName)
      (remove-filename-extension)
      (File/createTempFile ".maybe.png")))

(s/fdef tmp-png-file
        :args (s/cat :path (s/and ::fs/file-path-str
                                  #(>= (count (.getName (file %))) 3)))
        :ret  (s/and (partial instance? File)
                     #(.canWrite %)
                     #(ends-with? % ".maybe.png"))
        :fn   (fn [{:keys [args ret]}]
                (includes? (str ret) (-> (:path args)
                                         (file)
                                         (.getName)
                                         (split #"\." 3)
                                         (first)))))

; Arbitrary number is arbitrary. That said, according to my gut, less
; data is likely to be invalid, and more has a chance of being valid.
(def min-valid-png-size 1024)

(defn check
  [result path]
  (debug "checking result for errors...")

  ;; In dev/test fail will return an exception rather than throw it, so we use
  ;; or here to ensure the function exits when a failure condition is
  ;; encountered, because we can’t count on fail throwing and thus forcing the
  ;; function to exit at that point.
  (or
   (condp #(contains? %2 %1) result
     ::anom/message (fail path (::anom/message result))
     ::r/png-bytes nil
     (fail path (str "Internal error: render result invalid (has neither"
                     " ::anom/message nor ::r/png-bytes)")))

   (debug "checking PNG data size...")

   (when (< (count (::r/png-bytes result)) min-valid-png-size)
     (let [tmpfile (tmp-png-file path)]
       (binary-spit tmpfile (::r/png-bytes result))
       (fail path (str "PNG data is <1K so it’s likely invalid. It’s been"
                       " written to " tmpfile " for debugging."))))

   (debug "rendering seems to have succeeded!")
   nil))

(s/fdef check
        :args (s/cat :result (s/or :success ::r/result
                                   :failure ::r/failure)
                     :path   ::fs/file-path-str)
        :ret  (s/or :success nil?
                    :failure (partial instance? Exception))
        :fn   (fn [{{[result-tag result-val] :result
                     path                    :path}  :args
                    [ret-tag ret-val]                :ret}]
                (case result-tag
                  :failure
                  (and (= ret-tag :failure)
                       (includes? (.getMessage ret-val) path))

                  ; this case can still fail the check if png-bytes is too small
                  :success
                  (cond
                    (>= (count (::r/png-bytes result-val)) min-valid-png-size)
                    (= ret-tag :success)

                    (< (count (::r/png-bytes result-val)) min-valid-png-size)
                    (and (= ret-tag :failure)
                         (includes? (.getMessage ret-val) path)))

                  false)))

(defn yaml-path->png-path
  [in-path]
  (str/replace in-path #"\.ya?ml$" ".png"))

(defn render-diagram-file
  "Self-contained workflow for reading a YAML file containing a Structurizr
  Express diagram definition, rendering it to an image, and writing the image to
  a file in the same directory as the YAML file. Returns the path of the PNG
  file that was written (as a string) or throws an Exception."
  [in-path]
  (let [yaml     (read-text-file in-path)
        _        (validate yaml in-path)
        result   (r/render yaml)
        _        (debug (::r/stderr result))
        _        (check result in-path)
        out-path (yaml-path->png-path in-path)]
    (binary-spit out-path (::r/png-bytes result))
    out-path))
