^:kindly/hide-code
(require '[ai.unifica.lab.notebooks :refer [logo]])

logo

;; # Funciones

;; [Back to index](https://unifica-lab.pages.dev)

(ns funcs)

(def color "rojo")

color

(defn hola
  "Una funcion sin argumentos"
  []
  "Hola!")

(str "a" "b" 3)

(+ 1 2 3 1 3 6 7)

;; Cuando llamamos la funcion, saluda

(delay
  (hola))

;; ## Un argumento
;;
;; Una funcion que saluda a alguien con un argumento

(defn hola-vos
  "Una funcion con un argumento"
  [nombre]
  (str "Que onda " nombre "!"))

(delay
  (hola-vos "Cesar"))

(delay
  (hola-vos "Kyle"))

;; ## Con o sin nombre
;;
;; Una funcion que saluda con o sin nombre.
;;
;; Aquí, decimos que la función `saludos` está "llamando" a las funciones "hola"
;; y "hola-vos".

(defn saludos
  ([]
   (hola))
  ([nombre]
   (hola-vos nombre)))

;; Sin argumentos, se llama a `hola`

(delay
  (saludos))

;; Con argumentos, se llama a `saludos`, pasandole el nombre.
(delay
  (saludos "Cesar"))

;; ## Funcion que se llama

;; Un patron muy común es que una función se llame a si misma.
;; En este ejemplo, cuando la función se llama sin argumentos,
;; se vuelve a llamar a si misma con un nombre default.

(defn saludos-rey
  ([] (saludos-rey "de Roma"))
  ([nombre] (str "Saludos Rey " nombre)))

(delay
  (saludos-rey))

(delay
  (saludos-rey "Cesar"))

;; En Clojure, decimos que la función `hola-rey` tiene dos "formas" (forms).
;; Una forma es algo agrupado por paréntesis `()`.
;;
;; El sistema llama a la forma correcta depende de los argumentos que se le
;; envían.

;; ## Argumentos opcionales

;; Se pueden pasar argumentos opcionales con un `&` en la lista de argumentos
;; también.

(defn saludo-color
  [name & color]
  (str "Hola " name
       (when color (str ", tu color favorito es " color))))

(delay
  (saludo-color "Kyle"))

(delay
  (saludo-color "Kyle" "verde"))

;; El detalle aqui es que los argumentos aqui entran como una lista.

;; Esto lo arreglamos con la función "apply"

(defn saludo-color2
  [name & color]
  (str "Hola " name
       (when color (apply str ", tu color favorito es " color))))

(delay
  (saludo-color2 "Kyle" "verde"))
