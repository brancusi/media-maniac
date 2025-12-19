(ns com.atd.mm.core-utils.impl.base-64
  (:import [java.util Base64]))

(defn decode-url-encoded-base64
  "Decodes a base64-encoded string to a regular string.

  Example:
  ```clojure
  (decode-base64 \"SGVsbG8gd29ybGQ=\")
  ;=> \"Hello world\"
  ```"
  [^String base64-encoded]
  (let [decoder (Base64/getUrlDecoder)
        decoded-bytes (.decode decoder base64-encoded)
        decoded-string (String. decoded-bytes "UTF-8")]
    decoded-string))

(defn string->url-encoded-base64
  [s]
  (.encodeToString (java.util.Base64/getUrlEncoder) (.getBytes s "UTF-8")))

(comment

  (-> (string->url-encoded-base64 "Hello world")
      decode-url-encoded-base64)

  ;; 
  )