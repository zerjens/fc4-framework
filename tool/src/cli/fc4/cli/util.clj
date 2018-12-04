(ns fc4.cli.util)

;; Feel free to change when testing.
(def ^:dynamic *debug* false)
(def ^:dynamic *exit-on-fail* true)

(defn println-err
  "Prints values to stderr using println."
  [& vs]
  (binding [*out* *err*]
    (apply println vs)))

(defn debug
  "Prints values to stderr using println-err, if *debug* is true."
  [& vs]
  (when *debug*
    (apply println-err vs)))

(defn fail
  "Prints values to stderr using println-err then, if *exit-on-fail* is true,
  exits the JVM process with the status code 1."
  [& vs]
  (apply println-err vs)
  (if *exit-on-fail*
    (System/exit 1)
    (throw (Exception. "Normally the program would have exited at this point!"))))

(defn exit
  "Prints values to stderr using println-err then exits the JVM process with the
  specified status code."
  [status & vs]
  (apply println-err vs)
  (System/exit status))

(defn missing?
  "Returns true if supplied is missing any of the opts in required."
  {:source "https://github.com/clojure-cookbook/clojure-cookbook/blob/master/03_general-computing/3-07_parse-command-line-arguments.asciidoc"}
  [required supplied]
  (not-every? supplied required))
