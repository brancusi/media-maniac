(ns com.atd.mm.core-utils.interface
  (:require
   [com.atd.mm.core-utils.impl.base-64 :as base-64-utils]
   [com.atd.mm.core-utils.impl.class :as class-utils]
   [com.atd.mm.core-utils.impl.string :as string-utils]
   [com.atd.mm.core-utils.impl.predicates :as predicates-utils]
   [com.atd.mm.core-utils.impl.threading :as threading-utils]
   [com.atd.mm.core-utils.impl.url :as url-utils]
   [com.atd.mm.core-utils.impl.file :as file-utils]))

(defn assoc-conditional
  "Associates key-value pairs with a map based on predicates.
    Takes a map and a series of triples: predicate, key, value.
    If the predicate is true, associates the key with the value in the map.
  
    Example:
    (assoc-conditional {} 
      true :a 1
      false :b 2
      (> 3 2) [:c :d] 3) ;=> {:a 1, :c 3}
  
    Args:
    - m: Initial map.
    - conditions: A series of triples (predicate, key, value)."
  [m & conditions]
  (apply threading-utils/assoc-conditional m conditions))

(defn transform-conditional
  "Conditionally transforms new values to a map based on the result of predicate functions. This function takes a map and a series of condition-function pairs. If the condition (a predicate) evaluates to true, the associated function is applied to the map.
  
  Parameters:
  
  - `m`: The initial map.
  - `conditions`: A variadic series of pairs, where each pair is a predicate and a function. The function takes the current map as an argument and returns an updated map.
  
  Each predicate is evaluated in order, and if it returns true, the associated function is applied to the current map. This process continues with the potentially updated map for each predicate-function pair.
  
  **Example Usage**
  
  ```clojure
  (transform-conditional {}
                        true (fn [acc] (assoc acc :name \"Bob\"))
                        false (fn [acc] (assoc acc :last-name \"Jerry\")))
  ;; => {:name \"Bob\"}
  
  (transform-conditional {}
                        (> 1 0) (fn [acc] (assoc acc :name \"Bob\"))
                        (< 1 0) (fn [acc] (assoc acc :last-name \"Jerry\")))
  ;; => {:name \"Bob\"}
  ```"
  [m & conditions]
  (apply threading-utils/transform-conditional m conditions))

(defn decode-url-encoded-base64
  "Decodes a base64-encoded string to a regular string.

  Example:
  ```clojure
  (decode-base64 \"SGVsbG8gd29ybGQ=\")
  ;=> \"Hello world\"
  ```"
  [base64-encoded-text]
  (base-64-utils/decode-url-encoded-base64 base64-encoded-text))

(defn string->url-encoded-base64
  [s]
  (base-64-utils/string->url-encoded-base64 s))

(defn get-extention-by-type
  [type]
  (file-utils/get-extention-by-type type))

(defn get-folder-path
  "Takes a file path and returns the folder portion, stripping the file name.
   If the file name pattern (a forward slash followed by any characters except a forward slash, a dot, and then any characters except a forward slash) is not found, it returns the original path.

   Example:
   (get-folder-path \"/path/to/your/file.txt\")
   => \"/path/to/your\"

   (get-folder-path \"/path/to/your/directory\")
   => \"/path/to/your/directory\""
  [path]
  (file-utils/get-folder-path path))

(defn get-path-parts
  "Parses a file path to return the directory and filename separately."
  [path]
  (file-utils/get-path-parts path))

(defn get-file-name
  "Extracts the file name without extension from a file path."
  [file-path]
  (file-utils/get-file-name file-path))

(defn ensure-dir-exists!
  "Ensures that the directory portion of the file path exists. Create all missing dirs along the path"
  [file-path]
  (file-utils/ensure-dir-exists! file-path))

(defn ensure-file-exists!
  "Ensures that the file exists. Create all missing dirs along the path and the file."
  [file-path]
  (file-utils/ensure-file-exists! file-path))

(defn path-exists?
  "Does the path exists. Checks in the resources path."
  [file-path]
  (file-utils/path-exists? file-path))

(defn spit-with-dirs!
  [file-path content]
  (file-utils/spit-with-dirs file-path content))

(defn resource-exsists?
  [resource-path]
  (file-utils/resource-exsists? resource-path))

(defn file-exsists?
  [file-path]
  (file-utils/file-exsists? file-path))

(defn call-fn-in-ns [classpath fn-name & args]
  (class-utils/call-fn-in-ns classpath fn-name args))

(defn extract-email-from-str
  [str]
  (string-utils/extract-email-from-str str))

(defn matches-any?
  "Checks if the given unit of measure (uom) is present in the provided sequence.

  Example usage:
  (matches-any? \"days\" [\"days\" \"day\" :days :day])
  => true"
  [k col]
  (predicates-utils/matches-any? k col))

(defn force-double
  [str-or-number]
  (string-utils/force-double str-or-number))

(defn force-int
  [str-or-int]
  (string-utils/force-int str-or-int))


(defn get-url-host
  "Extracts the host from a given URL string.
  
   url - The URL string from which the host is to be extracted.
 
   Example:
   (get-url-host \"https://www.example.com/path\")
   => \"www.example.com\""
  [url]
  (url-utils/get-url-host url))

(defn get-url-path
  "Extracts the path from a given URL string.
  
   url - The URL string from which the path is to be extracted.
 
   Example:
   (get-url-path \"https://www.example.com/path\")
   => \"/path\""
  [url]
  (url-utils/get-url-path url))

(defn get-url-schema
  "Extracts the scheme (protocol) from a given URL string.
  
   url - The URL string from which the scheme is to be extracted.
 
   Example:
   (get-url-schema \"https://www.example.com/path\")
   => \"https\""
  [url]
  (url-utils/get-url-schema url))

(defn get-url-base
  "Constructs the base URL (scheme and host) from a given URL string.
  
   url - The URL string from which the base URL is to be constructed.
 
   Example:
   (get-url-base \"https://www.example.com/path\")
   => \"https://www.example.com\""
  [url]
  (url-utils/get-url-base url))

(defn build-url-with-query-params
  "Constructs a URL with query parameters.

  Args:
  - base-url: A string representing the base URL.
  - args: A map of query parameters.

  Returns:
  A string representing the URL with query parameters.

  Examples:
  (build-url-with-query-params \"http://example.com\" {:foo \"bar\" :baz \"qux\"})
  ;; => \"http://example.com?foo=bar&baz=qux\"

  (build-url-with-query-params \"http://example.com\" {})
  ;; => \"http://example.com?\"

  (build-url-with-query-params \"http://example.com\" {:a 1 :b 2})
  ;; => \"http://example.com?a=1&b=2\""
  [base-url args]
  (url-utils/build-url-with-query-params base-url args))

(defn strip-trailing-slash
  "Removes all trailing slashes from a URL.

  Args:
  - url: A string representing the URL.

  Returns:
  A string representing the URL without trailing slashes.

  Examples:
  (strip-trailing-slash \"http://example.com/\")
  ;; => \"http://example.com\"

  (strip-trailing-slash \"http://example.com//\")
  ;; => \"http://example.com\"

  (strip-trailing-slash \"http://example.com\")
  ;; => \"http://example.com\""
  [url]
  (url-utils/strip-trailing-slash url))


(comment



  ;;Keep from folding
  )