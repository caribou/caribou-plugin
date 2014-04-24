(ns caribou.plugin.state
  (:refer-clojure :exclusions [new])
  (:require [clojure.string :as string]
            [caribou.plugin.protocol :as plugin]
            [caribou.hooks :as hooks]
            [caribou.migration :as migration]
            [caribou.db :as db]
            [caribou.core :as caribou]
            [caribou.model :as model]
            [caribou.app.pages :as pages]))

(defn new
  "Create a new plugin state."
  []
  [])

(defn register
  "Register a new plugin with this state."
  [state plugin]
  (conj state plugin))

(defn update-config
  "Give each plugin a chance to update the config map."
  [config state]
  (reduce #(plugin/update-config %2 %) config state))

(defn apply-config
  "Give each plugin a chance to return a configured replacement for itself."
  [config state]
  (map #(or (plugin/apply-config % config) %) state))

(defn migrate
  "Migrate each of the plugins for each migration that is not yet registered."
  [config plugins]
  (caribou/with-caribou config
    (model/init)
    (doseq [plugin plugins]
      (doseq [{name :name migration :migration rollback :rollback
               :as migration-data} (plugin/migrate plugin config)]
        (when (and  migration name (empty? (migration/get-migration name)))
            (migration/migrate name migration rollback))))))

(defn add-hooks
  "Gather all the hooks provided by plugins and add them to the models."
  [config plugins]
  (caribou/with-caribou config
    (doseq [plugin plugins]
      (doseq [[model time key action
               :as hook] (plugin/provide-hooks plugin config)]
        (hooks/add-hook model time key action)))))

(defn get-helpers
  [config plugins]
  (reduce merge {} (map #(plugin/provide-helpers % config) plugins)))

(defn get-handlers
  [config plugins]
  (let [helpers (get-helpers config plugins)
        inject-helpers (fn inject-helpers [handler]
                         (fn helpers-injected [request]
                           (handler (merge request helpers))))]
    (reduce merge
            {:inject-helpers inject-helpers}
            (map #(plugin/provide-handlers % config) plugins))))

(defn get-pages
  [plugins config]
  (reduce merge (map plugin/provide-pages plugins config)))

(defn init
  "Initialize all the plugins."
  [state config & [migrate?]]
  (let [updated-config (update-config config state)
        plugins (apply-config updated-config state)
        pages (get-pages plugins updated-config)]
    (when migrate (migrate updated-config plugins))
    (add-hooks updated-config plugins)
    ;; this map is the plugin-map referenced below
    {:config updated-config
     :plugins plugins
     :helpers (get-helpers updated-config plugins)
     :handlers (get-handlers updated-config plugins)
     :pages pages}))

(defn unparse
  "Unpack a var or a string/symbol for a var into the legacy format needed
   by a not-yet-upgraded caribou-frontend."
  [controller-var]
  (let [v-name (str controller-var)
        decomposed (re-matches #"(#')?(.*)" v-name)
        var-name (last decomposed)
        analyzed (string/split var-name #"\.")
        controller-ns (string/join \. (butlast analyzed))
        [controller action] (string/split (last analyzed) #"/")]
    [controller-ns controller action]))

(defn run-all
  "Creates a vector of the futures for running each plugin."
  [plugins config]
  ;; mapv since it is eager, and we want all results
  (mapv (juxt identity #(future (plugin/run % config))) plugins))

(defn stop-all
  "Takes a sequence of instance / run pairs, and stops each run."
  [all]
  (doseq [[instance running] all]
    (plugin/stop instance running)))

;; this uses the plugin map as returned from init
(defn omni-handler
  "Construct one \"big handler\", for when their relative order is unimportant."
  [plugin-map]
  (let [handlers (vals (:handlers plugin-map))]
    (apply comp handlers)))

(defn provide-handler
  "Convenience to extract a handler in a middleware."
  [base-handler plugin-handlers keyname args]
  {:pre [(or (contains? plugin-handlers keyname)
             (println "Did not find" keyname "in" (keys plugin-handlers)))]}
  (apply (get plugin-handlers keyname) base-handler args))

(defn provider
  "Convenience function to thread handlers into your middleware."
  [handler-map]
  (println "available handlers:" (keys handler-map))
  (fn [handler key-wanted & args]
    (provide-handler handler handler-map key-wanted args)))

;; this uses the plugin map as returned from init
(defn all-pages
  "Construct one big page map structure out of the individual contributions."
  [plugin-map]
  (apply concat (vals (:pages plugin-map))))

(defn all-routes
  "Construct the newer version of the Caribou data based routes from plugins."
  [plugin-map]
  (for [[name pages] (:pages plugin-map)
        page pages]
    [(:path page)
     (keyword (:controller page))
     {(:method page :ALL) (:action page)}
     ()]))
