^:kindly/hide-code
(require '[ai.unifica.lab.notebooks :refer [logo]])

logo

;; # Let

(ns let)

;; Con 'let', podemos declarar una lista de valores para reutilizar más
;; adelante. Aqui `a` tiene el valor 3, y `b` el valor 5.

(let [a 3
      b 2]
  (+ a b))

;; A diferencia de `def`, esta asignación es temporal. (Afuera del `let`, estos
;; valores ya no están asignados a nada.)

;; Frecuentemente se usa en funciones

(defn stats
  "Return statistics for a list of values"
  [& values]
  (let [sum (apply + values)
        mean (/ sum (count values))]
    {:sum sum
     :mean mean}))

(stats 1 2 3)
