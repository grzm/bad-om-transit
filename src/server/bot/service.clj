(ns bot.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.helpers :refer [on-response]]
            [io.pedestal.log :as log]
            [ring.util.response :as ring-resp]
            [om.next.server :as om]
            [om.tempid :as tempid]
            [cognitect.transit :as transit])
  (:import [java.io OutputStream ByteArrayOutputStream]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response "Hello World!!"))

(defn body-writer [body]
  (fn [^OutputStream output-stream]
    (transit/write (om/writer output-stream) body)
    (.flush output-stream)))

(defn om-transit-json-body-fn
  [response]
  (let [body (:body response)
            content-type (get-in response [:headers "Content-Type"])]
        (log/info :fn ::om-transit-json-body :body body)
        (if (and (coll? body) (not content-type))
          (let [response'
                (-> response
                    (ring-resp/content-type "application/transit+json;charset=UTF-8")
                    (assoc :body (body-writer body)))]
            (log/info :fn ::om-transit-json-body :body (:body response'))
            response')
          response)))

(def om-transit-json-body
  "https://gist.github.com/rauhs/c137c0518cb7067f58ee"
  (on-response ::om-transit-json-body om-transit-json-body-fn))

(defn writer-to-string [writer]
  (let [baos (ByteArrayOutputStream.)]
    (writer baos)
    (.toString baos)))

(def edn-body
  `{todo/new-item
    {:tempids
     {~(tempid/tempid "2e486bfc-aacb-4736-8aa2-155411274e84") 852154481843896390}}})

(defn om-str-page
  "Uses body-writer directly to return a string.
   Route does not include transit encoder."
  [request]
  (let [bw (body-writer edn-body)
        body (writer-to-string bw)]
    {:status 200
     :headers {"Content-Type" "application/transit+json;charset=UTF-8"}
     :body body}))

(defn om-bw-page
  "Passes a writer function as the body. Route does not include transit encoder."
  [request]
  (let [bw (body-writer edn-body)]
    {:status 200
     :headers {"Content-Type" "application/transit+json;charset=UTF-8"}
     :body bw}))


(defn om-interceptor-page
  "Passes edn as the body. Uses the Transit encoding interceptor."
  [request]
  (ring-resp/response edn-body))

(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/om-str" :get (conj common-interceptors `om-str-page)]
              ["/om-bw" :get (conj common-interceptors `om-bw-page)]
              ["/om-interceptor" :get (conj common-interceptors
                                            `om-transit-json-body
                                            `om-interceptor-page)]})

(def service {:env :prod
              ::http/routes routes
              ::http/resource-path "/public"
              ::http/type :jetty
              ::http/port 8083
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})
