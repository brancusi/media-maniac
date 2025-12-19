(ns logging
  (:require [clojure.core :as c]
            [clojure.datafy :as d]
            [clojure.instant :as i]
            [portal.api :as p]
            [donut.system :as ds]))

(add-tap #'p/submit)

(defn process-log-msg
  "Accumulate a rolling log of 100 entries."
  [log-stash msg]
  (swap! log-stash
         (fn [logs]
           (take 100 (conj logs msg)))))

(defn portal-config
  [title]
  (let [log-stash (atom '())]
    #::ds{:start (fn [{{:keys [title]} ::ds/config}]
                   {:portal (p/open {:launcher :vs-code
                                     :theme :portal.colors/nord
                                     :window-title title
                                     :value log-stash})})
          :stop (fn [{data-map ::ds/instance}]
                  (println "Attempting to close the portal")
                  (p/close (:portal data-map)))
          :config {:title title
                   :log-stash log-stash}}))

(defn system-portal-config
  [title]
  #::ds{:start (fn [_]
                 (p/open {#_#_:launcher :vs-code
                          :window-title title
                          :theme :portal.colors/gruvbox}))
        :stop (fn [{::ds/keys [instance]}]
                (println "Attempting to close the portal")
                (p/close instance))})

(comment

  (p/close)
  (tap> 3)

  ;;Keep from folding
  )