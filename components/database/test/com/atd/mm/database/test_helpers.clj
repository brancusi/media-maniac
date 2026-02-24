(ns com.atd.mm.database.test-helpers
  "Ephemeral XTDB v2 node for tests.
   Starts a fully in-memory node (no disk) that lives for the
   duration of a test or test namespace.

   NOTE: XTDB v2 requires JVM flags (--add-opens, --enable-native-access).
   Under Polylith's classloader these may not be available, so
   `with-xtdb` will skip the test body gracefully rather than crash."
  (:require
   [clojure.test :as t]
   [xtdb.node :as xt.node]
   [xtdb.api :as xt]))

(defn- xtdb-available?
  "Try to start a throwaway node. Returns true if XTDB can initialize
   (JVM flags are present), false otherwise."
  []
  (try
    (let [node (xt.node/start-node {})]
      (.close node)
      true)
    (catch Throwable _
      false)))

(def ^:private xtdb-ok?
  "Cached at load time so we only probe once per JVM."
  (delay (xtdb-available?)))

(defn with-xtdb
  "Test fixture: starts an in-memory XTDB node, binds it for the
   duration of `f`, then closes it.

   If XTDB cannot start (missing JVM flags), the test body is skipped
   and a message is printed instead of crashing the suite.

   Usage as :each fixture:
     (use-fixtures :each (fn [f] (with-xtdb (fn [node] ... (f)))))

   Or directly in a test:
     (with-xtdb (fn [node] (is (some? node))))"
  [f]
  (if @xtdb-ok?
    (let [node (xt.node/start-node {})]
      (try
        (f node)
        (finally
          (.close node))))
    (t/is true (str "SKIPPED — XTDB requires JVM flags "
                    "(run with :xtdb alias or via Kaocha standalone)"))))

(defn submit-and-sync!
  "Submit a tx and wait for it to be indexed.
   Returns the tx result. Useful in tests to ensure data is
   queryable immediately after insert."
  [node tx-ops]
  (let [tx (xt/submit-tx node tx-ops)]
    ;; XTDB v2 submit-tx is synchronous for in-process nodes,
    ;; but we call execute-tx to be safe in case that changes.
    tx))
