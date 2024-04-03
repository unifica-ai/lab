(ns dates
  (:require [guides.lab.functions :as lf])
  (:import
   (java.time LocalDate)))

^{:kindly/hide-code true}
(comment
  ;; This is how we add and subtract days in Java 8
  ;; https://docs.oracle.com/javase/8/docs/api/java/time/LocalDate.html
  (-> (LocalDate/of 2023 3 1) (.plusDays 1))

  ;; This is how we compare days
  (LocalDate/.isBefore (LocalDate/of 2023 3 1) (LocalDate/of 2023 5 1))
  (LocalDate/.isAfter (LocalDate/of 2023 3 1) (LocalDate/of 2023 5 1))

  ;; To find a date in a range, do this
  (let [start-date (LocalDate/of 2022 2 1)
        end-date (LocalDate/of 2022 2 28)
        rng (comp (lf/is-after start-date) (lf/is-before end-date) :purchase_date)]
    (rng {:purchase_date (LocalDate/of 2022 1 31)})))
