# bot - Bad Om Transit

Test case for Om-Transit encoding issue with Reloaded Pedestal-backed Om Next
stack.

### Goal

Create an [Om Next][om-next] application with a [Pedestal][pedestal] remote.
The Pedestal server uses Stuart Sierra's [Component][component] for system
management and the [Reloaded][reloaded] pattern for server development.
Use [Figwheel][figwheel] for client development.

Transit encoding for Om is handled by an onresponse interceptor from
[a gist by Andre R.][transit-om-json-body]

```clojure
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
```

[om-next]: https://github.com/omcljs/om/wiki/Quick-Start-(om.next)
[pedestal]: http://pedestal.io
[component]: https://github.com/stuartsierra/component
[reloaded]: http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded
[figwheel]: https://github.com/bhauman/lein-figwheel
[transit-om-json-body]: https://gist.github.com/rauhs/c137c0518cb7067f58ee

### Issue

When client code is compiled by Figwheel and server code is run in a repl,
after reloading the server using `(reset)`,  `#om/id` tagged
literals are no longer transit encoded correctly.

To test the Transit encoding, the server serves a hard-coded edn value body
which is encoded by the interceptor. The edn value is:

    (def edn-body
      `{some/new-item
        {:tempids
         {~(tempid/tempid "2e486bfc-aacb-4736-8aa2-155411274e84") 852154481843896390}}})

### To replicate

1. Start Figwheel (which compiles the client code).

    rlwrap lein run -m clojure.main script/figwheel.clj

2. Start a server repl (using `lein repl` or [CIDER][cider]):

    lein repl

3. Start the server via the repl

    (go)

4. Confirm server requests properly Transit encode the `#om/id`
   tagged literal.


The `/om-str` endpoint encodes the EDN directly in the handler, and its
interceptor chain *does not* include the `om-transit-json-body` interceptor.

    curl localhost:8083/om-str
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["~#om/id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

The `/om-bw` endpoint passes a function that accepts the output stream to which
it will write the Transit-encoded body. This also does *not* include the
`om-transit-json-body` interceptor in the interceptor chain.

    curl localhost:8083/om-bw
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["~#om/id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

The `/om-interceptor` endpoint passes the edn as the body and includes the
`om-transit-json-body` interceptor in the interceptor chain.

    curl localhost:8083/om-interceptor
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["~#om/id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

Note all three encoded the `#om/id` tagged literal correctly.


5. Reload the server via the repl

    (reset)
    ;; :reloading (om.util om.tempid om.transit bot.service bot.service-test cljs.stacktrace om.next.impl.parser bot.server user om.next.protocols)

6. Confirm `#om/id` tagged literal is no longer being properly encoded.

    curl localhost:8083/om-str
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["^ ","~:id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

    curl localhost:8083/om-bw
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["^ ","~:id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

    curl localhost:8083/om-interceptor
    ["^ ","~$some/new-item",["^ ","~:tempids",["~#cmap",[["^ ","~:id","2e486bfc-aacb-4736-8aa2-155411274e84"],"~i852154481843896390"]]]]

All three no longer encode the `#om/id` tagged literal correctly.

### Notes

 1. Starting Figwheel/compiling the client code is necessary for issue to occur,
    though Figwheel does not need to be running at the time requests are made
    to the server. The repl must be started (or restarted) *after* figwheel
    compiled the client code.

 2. The issue only occurs after reloading the server code via `(reset)`.


[cider]: http://cider.readthedocs.io
