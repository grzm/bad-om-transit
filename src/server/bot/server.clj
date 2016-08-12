(ns bot.server
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :as log]
            [com.stuartsierra.component :as component]
            [bot.service :as service]))

(defrecord Pedestal [configure-pedestal pedestal]
  component/Lifecycle
  (start [this]
    (if pedestal
      this
      (assoc this :pedestal
             (-> (configure-pedestal)
                 http/create-server
                 http/start))))
  (stop [this]
    (when pedestal
      (http/stop pedestal)
      (assoc this :pedestal nil))))

(defn system [{:keys [pedestal]}]
  (component/system-map
    :pedestal (let [{:keys [configure-pedestal]} pedestal]
                (map->Pedestal {:configure-pedestal configure-pedestal}))))

(defn start-system [sys]
  (component/start sys)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (log/info :server "Shutting down system")
                               (component/stop sys)
                               (log/info :server "Shut down complete")))))

(defn configure-dev-pedestal
  [service-map]
  (fn []
    (-> service-map
        (merge {:env :dev
                ;; do not block thread that starts web server
                ::http/join? false
                ;; Routes can be a function that resolve routes,
                ;;  we can use this to set the routes to be reloadable
                ::http/routes #(route/expand-routes (deref #'service/routes))
                ;; all origins are allowed in dev mode
                ::http/allowed-origins {:creds true :allowed-origins (constantly true)}})
        ;; Wire up interceptor chains
        http/default-interceptors
        http/dev-interceptors)))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (log/info :run-dev "\nCreating your [DEV] server" :args args)
  (let [sys (system {:pedestal
                     {:configure-pedestal
                      (configure-dev-pedestal service/service)}})]
    (start-system sys)))

(defn configure-prod-pedestal
  [service-map]
  (fn []
    (service-map)))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (let [sys (system {:pedestal {:configure-pedestal
                                (configure-prod-pedestal service/service)}})]
    (start-system sys)))
