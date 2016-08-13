(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :as repl]
            [clojure.java.classpath :as cp]
            [clojure.java.io :refer [resource]]

            [bot.server :as server]
            [bot.service :as service]
            [figwheel-sidecar.repl-api :as ra]
            [figwheel-sidecar.config :as fw-config])
  (:import [java.io File]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (let [config-fn (server/configure-dev-pedestal service/service)]
    (alter-var-root #'system
                    (constantly (server/system {:pedestal {:configure-pedestal
                                                           config-fn}})))))

(defn start
  "Starts the current (initialized) development system"
  []
  (alter-var-root #'system component/start))

(defn stop
  "Shuts down and destroys the current development system"
  []
  (alter-var-root #'system (fn [sys]
                             (when sys
                               (component/stop sys)
                               nil))))
(defn go
  "Initializes and starts the current development system"
  []
  (if system
    "system not nil. Use (reset) ?"
    (do (init)
        (start))))

(defn refresh-dirs
  "Remove `resource` path from refresh-dirs"
  ([] (refresh-dirs repl/refresh-dirs))
  ([dirs]
   (let [resources-path (-> "public" resource .getPath File. .getParent)
         exclusions #{resources-path}
         ds (or (seq dirs) (cp/classpath-directories))]
     (remove #(contains? exclusions (.getPath %)) ds))))

(defn reset
  "Destroys, initializes, and starts the current development system"
  []
  (stop)
   (apply repl/set-refresh-dirs (refresh-dirs))
  (repl/refresh :after 'user/go))



(def figwheel-config
  {:figwheel-options {:server-port 3456
                      :css-dirs ["resources/public/css"]}
   :build-ids ["dev"]
   :all-builds (fw-config/get-project-builds)})

(defn start-figwheel
  "Start Figwheel for the given builds. Defaults to build-ids in
   `figwheel-config`"
  ([]
   (let [props (System/getProperties)
         all-builds (->> figwheel-config :all-builds (mapv :id))]
     (start-figwheel (keys (select-keys props all-builds)))))
  ([build-ids]
   (let [default-build-ids (:build-ids figwheel-config)
         build-ids (if (empty? build-ids)
                     default-build-ids
                     build-ids)]
     (println "Starting Figwheel on builds: " build-ids)
     (ra/start-figwheel! (assoc figwheel-config :build-ids build-ids))
     (ra/cljs-repl))))
