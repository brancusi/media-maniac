(ns com.atd.mm.media-converter.pipeline
  (:require
   [com.rpl.specter :refer [ALL filterer select setval]]))

(defn- update-process-deps
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
  (let [pipe-line-xt-id (java.util.UUID/randomUUID) ;; Create a new pipeline uuid
        processes (:processes data)
        keys (select [ALL :xt/id] processes)
        processes-with-uuids (reduce (fn [acc old-id]
                                       (let [new-id (java.util.UUID/randomUUID)
                                             updated-processes (setval [(filterer #(= (:xt/id %) old-id)) ALL :xt/id]
                                                                       new-id
                                                                       acc)]
                                         (update-process-deps updated-processes old-id new-id)))
                                     processes
                                     keys)

        ;; Augment with additional keys

        ;; Add an open status to the process
        processes-with-open-status (mapv #(assoc % :status :open) processes-with-uuids)

        ;; Add the pipeline uuid to the process
        processes-with-pipe-line-xt-id (mapv #(assoc % :pipeline-id pipe-line-xt-id) processes-with-open-status)]

    ;; Add the pipeline uuid
    (assoc data
           :processes processes-with-pipe-line-xt-id
           :xt/id pipe-line-xt-id)))

#_(comment

    (m/validate Pipeline [{:id #uuid "7697bd11-fab0-4240-8bde-f003e19e8518"
                           :deps [#uuid "7697bd11-fab0-4240-8bde-f003e19e8518"
                                  #uuid "37ddbd0d-6475-4b6a-b61e-32cc37f13a11"]
                           :type :media/transcribe
                           :opts {:model :whisper}}])

    (->> (mg/generate Pipeline)
         #_(m/validate Pipeline))

    ;;Keep from folding
    )