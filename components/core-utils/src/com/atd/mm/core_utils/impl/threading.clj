(ns com.atd.mm.core-utils.impl.threading)

(defn divisible-by?
  [num divisor]
  (zero? (mod num divisor)))

(defn assoc-conditional
  [m & conditions]
  (assert (divisible-by? (count conditions) 3) "assoc-when requires arguments in groups of three (predicate, key, value).")

  (let [pairs (partition 3 conditions)]
    (reduce (fn [acc [pred k v]]
              (if pred
                (assoc-in acc (if (vector? k) k [k]) v)
                acc))
            m
            pairs)))

(defn transform-conditional
  [m & conditions]
  (assert (divisible-by? (count conditions) 2) "assoc-when requires arguments in groups of 2 (predicate, assoc function).")

  (let [pairs (partition 2 conditions)]
    (reduce (fn [acc [pred f]]
              (if pred
                (f acc)
                acc))
            m
            pairs)))


(comment

  (assoc-conditional
   {}
   true [:a :x] 1
   true :b 2)

  (transform-conditional
   {}
   true (fn [acc] (assoc acc :hey 1)))
  ;; => {:hey 1}

  ;; => {}


  ;;Keep from folding
  )