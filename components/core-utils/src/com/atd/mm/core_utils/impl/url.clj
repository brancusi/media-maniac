(ns com.atd.mm.core-utils.impl.url
  (:require [clojure.string])
  (:import [java.net URI]))

(defn get-url-host
  [url]
  (let [uri (URI. url)]
    (.getHost uri)))

(defn get-url-path
  [url]
  (let [uri (URI. url)]
    (.getPath uri)))

(defn get-url-schema
  [url]
  (let [uri (URI. url)]
    (.getScheme uri)))

(defn get-url-base
  [url]
  (str (get-url-schema url) "://" (get-url-host url)))

(defn strip-trailing-slash
  [url]
  (clojure.string/replace url #"/+$" ""))

(defn build-url-with-query-params
  [base-url args]
  (str base-url "?" (clojure.string/join "&" (map (fn [[k v]] (str (name k) "=" v)) args))))


(comment
  (get-url-host "https://www.bizbuysell.com/juice-bars-for-sale/")
  (get-url-path "https://www.bizbuysell.com/juice-bars-for-sale/")
  (get-url-schema "https://www.bizbuysell.com/juice-bars-for-sale/")
  (get-url-base "https://www.bizbuysell.com/juice-bars-for-sale/")
  (build-url-with-query-params "http://example.com" {:foo "bar" :baz "qux"})
  (strip-trailing-slash "http://example.com/")

  ;;Keep from folding
  )