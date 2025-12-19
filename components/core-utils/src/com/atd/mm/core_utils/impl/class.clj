(ns com.atd.mm.core-utils.impl.class)

(defn call-fn-in-ns [classpath fn-name args]
  (let [function-symbol (symbol (str classpath "/" fn-name))
        function-to-call (resolve function-symbol)]
    (apply function-to-call args)))