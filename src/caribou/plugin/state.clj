(ns caribou.plugin.state
  (:refer-clojure :exclusions [new])
  (:require [caribou.plugin.protocol :as plugin]
            [caribou.hooks :as hooks]
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

(defn init
  "initialize all the plugins"
  [state config]
  (let [updated-config (reduce #(plugin/update-config %2 %) config state)
        plugins (map #(plugin/apply-config % updated-config) state)]
    (caribou/with-caribou updated-config
      (doseq [plugin plugins]
        (plugin/migrate plugin config)
        (doseq [[model time key action
                 :as hook] (plugin/provide-hooks plugin updated-config)]
          (hooks/add-hook model time key action))))
    ;; this map is the plugin-map referenced below
    {:config updated-config
     :plugins plugins
     :helpers (reduce merge (map plugin/provide-helpers plugins))
     :handlers (reduce merge (map plugin/provide-handlers plugins))
     :pages (reduce merge (map plugin/provide-pages plugins))}))

;; this uses the plugin map as returned from init
(defn omni-handler
  "construct one big handler, for when their relative order is unimportant"
  [plugin-map]
  (apply comp (vals (:handlers plugin-map))))

;; this uses the plugin map as returned from init
(defn all-pages
  "construct one big page map structure out of the individual contributions"
  [plugin-map]
  (apply concat (vals (:pages plugin-map))))
