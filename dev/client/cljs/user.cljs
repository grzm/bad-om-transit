(ns cljs.user
  (:require [cljs.pprint :refer [pprint]]
            [devtools.core :as devtools]
            [om.next :as om]))

(enable-console-print!)

(defonce cljs-build-tools
  (do (devtools/install! [:sanity-hints])))
