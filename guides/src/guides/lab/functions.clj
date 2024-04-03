(ns guides.lab.functions)

(defn is-before [date]
  (let [d (-> date (.plusDays 1))]
    #(when-not (nil? %) (when (LocalDate/.isBefore % d) %))))

(defn is-after [date]
  (let [d (-> date (.minusDays 1))]
    #(when-not (nil? %) (when (LocalDate/.isAfter % d) %))))
