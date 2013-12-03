(ns caribou.plugin.state
  (:refer-clojure :exclusions [new])
  (:require [caribou.plugin.protocol :as plugin]
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
        plugins (map #(apply-config % updated-config) state)]
    (caribou/with-caribou updated-config
      (doseq [plugin plugins]
        (migrate plugin config)))
    ;; this map is the plugin-map referenced below
    {:config updated-config
     :plugins plugins
     :helpers (reduce merge (map provide-helpers plugins))
     :handlers (reduce merge (map provide-handlers plugins))
     :pages (reduce merge (map provide-pages plugins))}))

;; this uses the plugin map as returned from init
(defn omni-handler
  "construct one big handler, for when their relative order is unimportant"
  [plugin-map]
  (apply comp (values (:handlers plugin-map))))

;; this uses the plugin map as returned from init
(defn all-pages
  "construct one big page map structure out of the individual contributions"
  [plugin-map]
  (apply concat (values (:pages plugin-map))))
