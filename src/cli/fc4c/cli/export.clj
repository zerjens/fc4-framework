(ns fc4c.cli.export
  "Exports (converts) an FC4 view to a Structurizr Express diagram. Currently
  supports only System Context diagrams, but more types are coming soon."
  (:require
   [clojure.pprint                               :as pp :refer [pprint]]
   [clojure.spec.alpha                           :as s]
   [clojure.string                               :as str :refer [join]]
   [clojure.tools.cli                            :as cli :refer [parse-opts]]
   [cognitect.anomalies                          :as anom]
   [expound.alpha                                :as ex :refer [expound-str]]
   [fc4c.cli.util                                :as cu :refer [exit]]
   [fc4c.integrations.structurizr.express.export :as se]
   [fc4c.integrations.structurizr.express.yaml   :as sy]
   [fc4c.io                                      :as io]))

(def opts
  ;; TODO: add --loop and --output
  [["-v" "--view PATH" "Path to FC4 view file (required)"]
   ["-m" "--model PATH" "Path to FC4 model directory (required)"]
   ["-s" "--styles PATH" "Path to FC4 styles file (required)"]
   ["-d" "--debug" "Print out lots of debugging data to stderr (optional)"]
   ["-h" "--help" "Print this summary (optional)"]])

(def required-opts #{:view :model :styles})

(def missing-required? #(cu/missing? required-opts %))

(defn- read-or-exit
  "Reads the value at the specified path using the supplied read-fn. If the
  result seems valid, returns it. Otherwise, exits the JVM with exit code 1 and
  prints the error message."
  [read-fn path desc]
  (let [res (read-fn path)]
    (if (and (map? res) ;; read-styles returns a seq when successful and a map when unsuccessful
             (contains? res ::anom/category))
      (exit 1 (str "invalid " desc " at " path ":\n" (::anom/message res)))
      res)))

(defn- print-debug-data
  [dd]
  (binding [*out* *err*]
    (println "\n\n\n\n")
    (doseq [[k v] dd]
      (println (str k ":\n\n"))
      (pprint v)
      (println "\n\n\n")))
  (println "\n\n"))

(defn- export-or-exit
  [view-path model-path styles-path debug]
  (let [view   (read-or-exit io/read-view   view-path   "view")
        model  (read-or-exit io/read-model  model-path  "model")
        styles (read-or-exit io/read-styles styles-path "styles")
        system-context-diagram (se/view->system-context view model styles)
        debug-data {:system-context-diagram system-context-diagram
                    :model model
                    :view view
                    :styles styles}]
    (when debug (print-debug-data debug-data))
    (when-not (s/valid? :structurizr/diagram system-context-diagram)
      (exit 1 (expound-str :structurizr/diagram system-context-diagram)))
    ;; Happy path!
    ;; Using print rather than println because stringify returns a string that
    ;; already ends with newline.
    (print (sy/stringify system-context-diagram))))

(defn -main
  [& args]
  (let [{:keys [options summary errors]}       (parse-opts args opts)
        {:keys [view model styles debug help]} options
        missing                                (missing-required? options)]
    (cond
      missing (exit 1 summary)
      errors  (exit 1 (join "\n" (conj errors summary)))
      help    (exit 0 summary))
    (export-or-exit view model styles debug)))
