(ns com.atd.mm.http-client.core
  (:require [hato.client :as hc]))

(defn create-http-client
  []
  (hc/build-http-client {:connect-timeout 10000
                         :redirect-policy :always}))

(comment

  (hc/get "https://httpbin.org/get")

  ;;Keep from folding
  )