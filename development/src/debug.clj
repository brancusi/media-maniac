(ns debug)

(defonce store (atom {}))

(defn spy
  ([v]
   (tap> {:spied v})
   v)
  ([key v]
   (tap> {key v})
   v))

(defn spy-when
  [v key pred]
  (when (pred v)
    (spy key v))
  v)

(defn spy-present
  ([v]
   (spy-present v :spied-present))
  ([v key]
   (when (boolean v)
     (spy v key))
   v))

(defn peek-stash
  ([] (-> @store :default peek))
  ([key]  (peek (get @store key))))

(defn stash
  ([val] (stash :default val))
  ([key val]
   (stash key val {:freeze? false})
   val)
  ([key val {:keys [freeze?]}]
   (let [frozen? (:frozen? (meta (peek-stash key)))]
     (if (not frozen?)
       (swap! store update-in [key] conj (with-meta val {:frozen? freeze?}))
       (tap> (str "Attempted to stash " key " but it was frozen"))))
   val))


(comment

  (stash :hey {:a 1 :b 6} {:freeze? true})

  (stash {:a 1 :b 6})
  ;; => {:a 1, :b 3}

  ;; => {:a 1, :b 2}

  (peek-stash :what)
  (peek-stash :hey)

  (peek-stash)

  (clear-all)


  ;;Keep from folding
  )

(defn get-keys
  []
  (keys @store))

(defn all
  []
  @store)

(defn clear-key
  [key]
  (swap! store dissoc key)
  nil)

(defn clear-all
  []
  (reset! store {}))

(defn inject-debug-middleware
  "Stash requests for debugging"
  [handler stash-key]
  (fn [request]
    (stash stash-key request)
    (handler request)))

(comment

  @store
  (get-keys)

  ;;Keep from folding
  )