(ns guides.lab
  (:require
   [ai.unifica.lab.config :as cfg]
   [ai.unifica.gcloud.bigquery :as bq]))

(defonce system (atom {}))

(defn use-bigquery [ctx]
  (let [service (bq/service ctx)]
    (-> ctx (assoc :gcloud.bigquery/service service))))

(def components
  [cfg/use-aero
   use-bigquery])

(def initial-system {})

(defn start! []
  (let [new-system (reduce (fn [system component]
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    new-system))
