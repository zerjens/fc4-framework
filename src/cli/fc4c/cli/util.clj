(ns fc4c.cli.util)

(defn println-err
  [& vs]
  (binding [*out* *err*]
    (apply println vs)))

(defn exit
  [status msg]
  (println-err msg)
  (System/exit status))

(defn missing?
  "Returns true if supplied is missing any of the opts in required."
  {:source "https://github.com/clojure-cookbook/clojure-cookbook/blob/master/03_general-computing/3-07_parse-command-line-arguments.asciidoc"}
  [required supplied]
  (not-every? supplied required))
