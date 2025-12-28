(ns com.atd.mm.http-client.interface
  (:require [com.atd.mm.http-client.core :as impl]
            [donut.system :as ds]))

(defn create-http-client
  []
  (impl/create-http-client))

(def system-config
  #::ds{:start (fn [_]
                 (println "Creating HTTP client")
                 (tap> :creating-http-client)
                 (create-http-client))})