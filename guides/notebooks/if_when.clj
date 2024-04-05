^:kindly/hide-code
(require '[ai.unifica.lab.notebooks :refer [logo]])

logo

;; # If y when

(ns if-when)

;; Ver [libro][libro] o la documentacion [oficial][if]
;;
;; [libro]: https://www.braveclojure.com/do-things/
;; [if]: https://clojuredocs.org/clojure.core/if
;;
;; **if** es una funcion donde el primer argumento es una prueba verdadero / falso
;; En Excel, Microsoft le llama "comparacion". En Clojure y otros lenguajes, se les llama [boolean][bool]
;;
;; [excel]: https://support.microsoft.com/en-us/office/if-function-69aed7c9-4e8a-4755-a9bc-aa8bbff73be2
;; [bool]: https://en.wikipedia.org/wiki/Boolean_data_type

;; Si el primer valor es verdadero, la funcion retorna el siguiente valor. Si es falso, retorna el ultimo.
;; En Excel, esta formula seria
;;
;; ```
;; =IF(TRUE,"si", "no")
;; ```
;;
;; En Clojure, es muy similar:

(if true "si" "no")

(if false "si" "no")

;; Por supuesto que se puede usar en una funcion

(defn verdad-que
  [valor]
  (if valor "si" "no"))

(verdad-que true)

(verdad-que false)

;; O le podemos pasar una forma (algo en parentesis) que evalua a verdadero o falso. Algo como en Excel:

;; ```
;; =IF(ISEVEN(2),"si", "no") => si
;; ```
;;
;; (Ver documentacion de [iseven][iseven].)
;;
;; [iseven]: https://support.microsoft.com/en-gb/office/iseven-function-aa15929a-d77b-4fbb-92f4-2f479af55356

;; En Clojure no hay una funcion 'is-even', entonces la definimos:

(defn is-even [n] (= 0 (mod n 2)))

;; (Podemos ignorar por el momento que significa `mod`, pero aqui esta la [definicion][mod]).
;;
;; [mod]: https://en.wikipedia.org/wiki/Modulo

(if (is-even 2) "si" "no")

(if (is-even 3) "si" "no")

;; Combinando con nuestra funcion

(verdad-que (is-even 2))

(verdad-que (is-even 3))

;; ## When

;; Ver [libro][libro] o documentacion [oficial][when]
;;
;; [when]: https://clojuredocs.org/clojure.core/when
;; [libro]: https://www.braveclojure.com/do-things/
;;
;; Es como un "if", pero que devuelve nulo (`nil`) si la condicion / test es falso.

(when true "si")

(when false "si")
