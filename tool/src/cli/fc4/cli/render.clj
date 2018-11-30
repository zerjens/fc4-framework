(ns fc4.cli.render
  "CLI subcommand that renders Structurizr Express diagrams into PNG image
  files."
  (:require
   [cognitect.anomalies  :as anom]
   [clojure.java.io                             :as io :refer [file output-stream]]
   [clojure.string                              :as string]
   [fc4.integrations.structurizr.express.render :as r]))

(defn println-stderr [& vs]
  (binding [*out* *err*]
    (apply println vs)))

;; Feel free to override when testing.
(def ^:dynamic *debug* false) ;; TODO: add a command-line flag that sets this
(def ^:dynamic *exit-on-fail* true)

(defn debug [& vs]
  (when *debug*
    (apply println-stderr vs)))

(defn fail [file-path msg]
  (println-stderr (str "Error rendering " file-path ": " msg))
  (if *exit-on-fail*
    (System/exit 1)
    (throw (Exception. "Normally the program would have exited at this point!"))))

(defn render [yaml]
  (debug "invoking renderer...")
  (let [result (r/render yaml)]
    (debug (::r/stderr result))
    result))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  ;;
  ;; TODO: Error handling!!!!!
  ;;
  ;; TODO: Check that png-bytes is not empty and exit with an error code if it
  ;; is.
  ;; TODO: output more error details if --debug flag set.
  ;; New workflow:
  ;; for each file:
  ;;    - check that the file exists, if not exit with error message and code 1
  ;;    - read the file contents
  ;;    - pass the file contents to valid and if invalid, exit with error message and code 1
  ;;    - pass the file contents to render
  ;;    - check the result of render for success/failure, and either write the PNG file (happy path) or exit with error message and code 1
  [& in-paths]
  (doseq [in-path in-paths]
    (let [fail! (partial fail in-path)
          yaml (try (slurp in-path)
                    (catch java.io.FileNotFoundException _ (fail! "file not found"))
                    (catch Exception e (fail! (.getMessage e))))
          valid (r/valid? yaml)
          _ (when-not (true? valid) (fail! (::anom/message valid)))
          result (render yaml)
          _ (condp (partial contains? result)
                   ::anom/message (fail! (::anom/message result))
                   ::r/png-bytes :no-worries
                   (fail! (str "Internal error: render result invalid (has neither"
                               "  ::anom/message nor ::r/png-bytes)")))
          {:keys [::r/png-bytes ::r/stderr]} result
          out-path  (string/replace in-path #"\.ya?ml$" ".png")]
      (with-open [out (output-stream (file out-path))]
        (.write out png-bytes)))))
