(ns com.atd.mm.core-utils.interface-test
  (:require [clojure.test :as test :refer :all]
            [com.sajb.core-utils.interface :as i]))

(deftest assoc-when-tests
  (is (= (i/assoc-conditional
          {:a 10}
          true [:b :x] 1
          true :c 2)
         {:a 10, :b {:x 1}, :c 2})
      "Can associate deeply")

  (is (= (i/assoc-conditional
          {}
          true [:x :y :z] "hello"
          false :c 2)
         {:x {:y {:z "hello"}}})
      "Properly omits fals branches")

  (is (= (i/assoc-conditional
          {}
          (> 3 2) :c 2)
         {:c 2})
      "Handles single condition")

  (is (= (i/assoc-conditional
          {:a 1})
         {:a 1})
      "Handles empty conditions")

  (is (= (i/assoc-conditional
          nil
          true :a 1)
         {:a 1})
      "Nil starting map is replaced with new map if true conditions exists")

  (is (= (i/assoc-conditional
          nil
          false :a 1)
         nil)
      "Nil starting map is not replaced with new map if all conditions are false"))


