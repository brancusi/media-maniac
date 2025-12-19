(ns com.atd.mm.core-utils.impl.predicates)

(defn matches-any?
  "Checks if the given unit of measure (uom) is present in the provided sequence.

  Example usage:
  (matches-any? \"days\" [\"days\" \"day\" :days :day])
  => true"
  [k col]
  (some #(= k %) col))