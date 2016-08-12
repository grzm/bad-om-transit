(defproject bot "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; server dependencies
                 [io.pedestal/pedestal.service "0.5.0"]
                 [io.pedestal/pedestal.jetty "0.5.0"]
                 [ch.qos.logback/logback-classic "1.1.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]

                 [com.stuartsierra/component "0.3.1"]
                 [com.cognitect/transit-clj "0.8.285"]

                 ;; client dependencies
                 [org.omcljs/om "1.0.0-alpha41"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :plugins [[info.sunng/lein-bootclasspath-deps "0.2.0"]]
  :source-paths ["src/server" "src/client"]
  :boot-dependencies [;; See: https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions
                      [org.mortbay.jetty.alpn/alpn-boot "8.1.3.v20150130"] ;; JDK 1.8.0_31/40/45
                      ]

  :profiles {:dev {:source-paths ["dev/client" "dev/server"]
                   :aliases {"run-dev" ["trampoline" "run" "-m" "bot.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.0"]
                                  [org.clojure/tools.namespace "0.3.0-alpha3"]
                                  [figwheel-sidecar "0.5.4-7" :scope "test"]
                                  [binaryage/devtools "0.6.1"]]
                   :main user}
             :uberjar {:aot [bot.server]}}

  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :main ^{:skip-aot true} bot.server
  :figwheel {}
  :cljsbuild {:builds [{:id "dev"
                        :figwheel true
                        :source-paths ["dev/client" "src/client"]
                        :compiler {:main bot.client.main
                                   :recompile-dependents true
                                   :optimizations :none
                                   :asset-path "js"
                                   :output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js"
                                   :verbose true}}]})
