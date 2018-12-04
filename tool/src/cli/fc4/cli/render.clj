(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require
   [cognitect.anomalies  :as anom]
   [clojure.java.io                             :as io :refer [file output-stream]]
   [clojure.string                              :as str :refer [split]]
   [fc4.cli.util                                :as cu :refer [debug]]
   [fc4.integrations.structurizr.express.render :as r]))

(defn fail
  [file-path msg]
  (cu/fail (str "Error rendering " file-path ": " msg)))

(defn read-file
  [path]
  (try (slurp path)
       (catch java.io.FileNotFoundException _ (fail path "file not found"))
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
  (-> (str/replace in-path #"\.ya?ml$" ".png")
      (file)
      (output-stream)))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  ;;
  ;; TODO: add a command-line flag that sets cu/*debug* to true
  [& paths]
  (doseq [path paths]
    (let [yaml                               (read-file path)
          _                                  (validate yaml path)
          result                             (render yaml path)
          _                                  (check result path)
          {:keys [::r/png-bytes ::r/stderr]} result]
      (with-open [out (get-out path)]
        (.write out png-bytes)))))
