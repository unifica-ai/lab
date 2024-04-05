^:kindly/hide-code
(require '[ai.unifica.lab.notebooks :refer [logo]])

logo

;; # Mapas

(ns map)

;; Ver [libro][libro]
;;
;; [libro]: https://www.braveclojure.com/do-things/
;;
;; Un mapa es un tipo de dato basico en Clojure, que expresa definiciones como en un diccionario.
;;
;; En un diccionario, vamos de la palabra a su definicion.
;; En Clojure, vamos de un `key` a su `value`.
;;
;; Ejemplo de un mapa:

^:kindly/hide-code
{
 "perro" "Mamífero carnívoro doméstico..."
 "gato" "Mamífero felino de tamaño generalmente pequeño..."
 }

;; Podemos guardar este mapa en una variable

(def map {
 "perro" "Mamífero carnívoro doméstico..."
 "gato" "Mamífero felino de tamaño generalmente pequeño..."
 })

;; Los **keys** son "perro" y "gato", y los **values** son sus definiciones.
;;
;; Y luego retirar solo un valor

(get map "perro")

;; o simplemente

(map "perro")

;; En conclusion, un mapa es algo que podemos usar para tener mas de un valor en una variable.
;;
;; ## Keywords

;; En general, se usan keywords (ver misma pagina en Brave and True) para los **keys**:

(def map2 {
 :perro "Mamífero carnívoro doméstico..."
 :gato "Mamífero felino de tamaño generalmente pequeño..."
 })

;; Entonce podemos retirar un valor asi

(map2 :perro)

;; pero tambien asi!

(:perro map2)

;; Esto segundo se usa muy frecuentemente, y vale la pena aprenderlo.

;; Frecuentemente se usa para pasar opciones a una funcion
;; Por ejemplo, aqui hay una funcion para saludar en ingles o español,
;; donde también podemos solicitar slang o no-slang

(defn saludar [opts]
  (if (= (:language opts) "english")
    (if (:slang opts) "wassup" "hi")
    (if (:slang opts) "quiubo" "hola")))

(saludar {})

(saludar {:language "english"})

(saludar {:language "english" :slang true})

(saludar {:slang true})
