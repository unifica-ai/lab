(ns com.example
  (:require [ai.unifica.lab.config :as cfg]
            [nrepl.cmdline :as nrepl-cmd]
            [clojure.tools.logging :as log]))

(def modules [])

(def initial-system
  {:modules #'modules})

(defonce system (atom {}))

(def components
  [cfg/use-aero])

(defn start! []
  (let [new-system (reduce (fn [system component]
                             (log/info "starting:" (str component))
                             (component system))
                           initial-system
                           components)]
    (reset! system new-system)
    (log/info "System started.")
    new-system))

(defn -main []
  (let [{:keys [lab.nrepl/args]} (start!)]
    (apply nrepl-cmd/-main args)))
