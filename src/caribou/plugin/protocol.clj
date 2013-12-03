(ns caribou.plugin.protocol)

(defprotocol CaribouPlugin
  (update-config [this config]
    "A function that returns an updated config map.")
  (apply-config [this config]
    "A function the receives the final config and returns a replacement object.")
  (migrate [this config]
    "Runs your migration on the database in config.")
  (provide-hooks [this config]
    "Returns the hooks your plugin requires on models.")
  (provide-helpers [this]
    "Returns a map of keyword to template helper.")
  (provide-handlers [this]
    "Returns a map of keyword to ring request handler.")
  (provide-pages [this config]
    "Returns a nested page structure inside a map."))

;;; the identity implementation of each method on the protocol
(extend java.lang.Object
  CaribouPlugin
  {:update-config (fn [this config] config)
   :apply-config (fn [this config] this)
   :migrate (fn [this config] nil)
   :provide-hooks (fn [this config] {})
   :provide-helpers (fn [this] [])
   :provide-handlers (fn [this] {})
   :provide-pages (fn [this] {})})
