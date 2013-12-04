(ns caribou.plugin.state
  (:refer-clojure :exclusions [new])
  (:require [caribou.plugin.protocol :as plugin]
            [caribou.hooks :as hooks]
            [caribou.migration :as migration]
            [caribou.db :as db]
            [caribou.core :as caribou]
            [caribou.model :as model]))

(defn new
  "create a new plugin state"
  []
  [])

(defn register
  "register a new plugin with this state"
  [state plugin]
  (conj state plugin))

(defn update-config
  [config state]
  (reduce #(plugin/update-config %2 %) config state))

(defn apply-config
  [config state]
  ;(map #(or (plugin/apply-config % config) %) state)
  state)

(defn migrate
  [config plugins]
  (caribou/with-caribou config
    (model/init)
    (doseq [plugin plugins]
      (doseq [{name :name migration :migration rollback :rollback
               :as migration-data} (plugin/migrate plugin config)]
        (when (and  migration name (empty? (migration/get-migration name)))
            (migration/migrate name migration rollback))))))

(defn add-hooks
  [config plugins]
  (caribou/with-caribou config
    (doseq [plugin plugins]
      (doseq [[model time key action
               :as hook] (plugin/provide-hooks plugin config)]
        (hooks/add-hook model time key action)))))

(defn get-helpers
  [plugins]
  (reduce merge (map plugin/provide-helpers plugins)))

(defn get-handlers
  [plugins]
  (reduce merge (map plugin/provide-handlers plugins)))

(defn get-pages
  [plugins config]
  (reduce merge (map plugin/provide-pages plugins config)))

(defn init
  "Initialize all the plugins."
  [state config]
  (let [updated-config (update-config config state)
        plugins (apply-config config state)]
    (migrate updated-config plugins)
    (add-hooks updated-config plugins)
    ;; this map is the plugin-map referenced below
    {:config updated-config
     :plugins plugins
     :helpers (get-helpers plugins)
     :handlers (get-handlers plugins)
     :pages (get-pages plugins updated-config)}))

;; this uses the plugin map as returned from init
(defn omni-handler
  "Construct one \"big handler\", for when their relative order is unimportant."
  [plugin-map]
  (apply comp (vals (:handlers plugin-map))))

;; this uses the plugin map as returned from init
(defn all-pages
  "Construct one big page map structure out of the individual contributions."
  [plugin-map]
  (apply concat (vals (:pages plugin-map))))
