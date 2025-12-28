(ns com.atd.mm.grand-central.model.api
  (:require
   [com.atd.mm.grand-central.resolver :as resolver]))

(defn process-raw-video
  [src-path & {:keys [xtdb-node]}]

  (let [xtdb-node (or xtdb-node (resolver/get-xtdb-node))]
    xtdb-node)
  #_(xt/submit-tx my-node [[:put-docs :files {:xt/id 1
                                              :src "example.mp4"
                                              :size 10000
                                              :duration 1002021}]]))


(comment

  (process-raw-video 1 (resolver/get-xtdb-node))

  ;;Keep from folding
  )