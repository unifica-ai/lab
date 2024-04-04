(ns guides.lab
  (:require
   [ai.unifica.lab.config :as cfg]))

(defonce system (atom {}))

(def components
  [cfg/use-aero])

(def initial-system {})

(defn start! []
  (let [x #{:lab.tasks}
        new-system (reduce (fn [system component]
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    (-> (filter (fn [[k]] (not (x ((comp keyword namespace) k)))) new-system)
        (update-vals (fn [v] (cond (fn? v) "******"
                                   (not (string? v)) "#"
                                   :else v))))))
