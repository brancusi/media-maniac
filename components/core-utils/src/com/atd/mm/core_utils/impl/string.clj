(ns com.atd.mm.core-utils.impl.string)

(defn extract-email-from-str
  [str]
  (when str
    (let [email-regex #"([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+)"
          matches (re-seq email-regex str)]
      (ffirst matches))))

(defn force-double
  [str-or-number]
  (if (string? str-or-number)
    (parse-double str-or-number)
    str-or-number))

(defn force-int
  [str-or-int]
  (cond
    (string? str-or-int) (if (re-matches #"\d+\.\d+" str-or-int)
                           (int (parse-double str-or-int))
                           (int (parse-long str-or-int)))
    :else                (int str-or-int)))

(comment

  (force-double "4")
  (force-double "4.0")
  (force-double 4)
  (force-double 4.0)

  (force-int "4")
  (force-int "4.0")
  (force-int 4)
  (force-int 4.0)

  ;;Keep from folding
  )