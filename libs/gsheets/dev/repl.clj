(ns repl
  (:require [ai.unifica.gsheets :as gsheets]
            [ai.unifica.gsheets.v4 :as sheets-v4]))

(defonce ctx
  (atom (gsheets/component
         {:gsheets/application-name "App"
          :gsheets/access-type "offline"
          :gsheets/port 8888
          :gsheets/authorize "user"})))

(delay
  (sheets-v4/get-values @ctx "1-qOXNscq19EeGYAgYXD55CRUPxgeKYh9Yc_SpcwSP-8" "Sheet1!A2:E"))
