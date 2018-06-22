(ns fc4c.util)

(defn lookup-table-by
  "Given a function and a seqable, returns a map of (f x) to x.

  For example:
  => (lookup-table-by :name [{:name :foo} {:name :bar}])
  {:foo {:name :foo}, :bar {:name :bar}}"
  [f xs]
  (zipmap (map f xs) xs))
