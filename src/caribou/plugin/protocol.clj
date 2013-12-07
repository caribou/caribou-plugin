(ns caribou.plugin.protocol)

(defprotocol CaribouPlugin
  (update-config [this config]
    "A function that returns an updated config map.")
  (apply-config [this config]
    "A function that receives the final config and returns a replacement.")
  (migrate [this config]
    "Runs your migrations and rollbacks on the database in config.")
  (provide-hooks [this config]
    "Returns the hooks your plugin requires on models.")
  (provide-helpers [this]
    "Returns a map of keyword to template helper.")
  (provide-handlers [this]
    "Returns a map of keyword to ring request handler.")
  (provide-pages [this config]
    "Returns a nested page structure inside a map.")
  (run [this config]
    "Run this plugin, should return a handle that can access the process."))

;;; The identity implementation of each method on the protocol,
;;; so you need only implement the ones reflecting the features you need.

(def null-implementation
  {:update-config (fn [this config] config)
   :apply-config (fn [this config] this)
   :migrate (fn [this config] [{:name nil :migration nil :rollback nil}])
   :provide-hooks (fn [this config] {})
   :provide-helpers (fn [this] {})
   :provide-handlers (fn [this] {})
   :provide-pages (fn [this config] {})
   :run (constantly nil)})

(comment
  (extend java.lang.Object
    CaribouPlugin
    (assoc null-implementation
      :migrate (fn [this config] (println "no migration for object") []))))

(defn make
  [type impl]
  (extend type
    CaribouPlugin
    (merge null-implementation impl)))
