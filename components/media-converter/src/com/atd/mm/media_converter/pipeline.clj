(ns com.atd.mm.media-converter.pipeline
  (:require [malli.core :as m]
            [malli.generator :as mg]
            [com.rpl.specter :refer [transform MAP-VALS ALL filterer select setval]]))
(def Rule
  [:map
   [:xt/id :uuid]
   [:opts {:optional true} :map]
   [:type [:enum :media/proxy
           :media/extract-audio
           :media/extract-stills
           :media/transcribe
           :media/copy]]
   [:deps {:optional true} [:vector :uuid]]])

(def Pipeline
  [:map
   [:xt/id :uuid]
   [:src :string]
   [:rules [:vector #'Rule]]])

(defn- update-rule-deps
  [data old-id new-id]
  (setval [(filterer #(:deps %))
           ALL
           :deps
           (filterer #(= % old-id))
           ALL]
          new-id
          data))

(defn prepare-pipeline
  [data]
  (let [rules (:rules data)
        keys (select [ALL :xt/id] rules)
        updated-rules (reduce (fn [acc old-id]
                                (let [new-id (java.util.UUID/randomUUID)
                                      updated-rules (setval [(filterer #(= (:xt/id %) old-id)) ALL :xt/id]
                                                            new-id
                                                            acc)]
                                  (update-rule-deps updated-rules old-id new-id)))
                              rules
                              keys)]
    (assoc data
           :rules updated-rules
           :xt/id (java.util.UUID/randomUUID))))

(defn pipeline-valid?
  [pipeline]
  (m/validate Pipeline pipeline))

(comment

  (m/validate Pipeline [{:id #uuid "7697bd11-fab0-4240-8bde-f003e19e8518"
                         :deps [#uuid "7697bd11-fab0-4240-8bde-f003e19e8518"
                                #uuid "37ddbd0d-6475-4b6a-b61e-32cc37f13a11"]
                         :type :media/transcribe
                         :opts {:model :whisper}}])

  (->> (mg/generate Pipeline)
       #_(m/validate Pipeline))

  ;;Keep from folding
  )