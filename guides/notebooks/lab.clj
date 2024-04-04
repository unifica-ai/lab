(ns lab
  (:require
   [guides.lab :as lab]
   [ai.unifica.lab.notebooks :refer [md logo]]))

logo

(md "# Lab")

;; La función `lab/start` carga los componentes que se han definido.

(lab/start!)

;; Ya habiendo cargado el sistema, podemos verificar cualquier secreto con una
;; funcion de `@lab/system` que se llama `secret`. Utilizamos esta funcion para
;; cargar un secreto en particular.

(let [{:keys [:lab/secret]} @lab/system]
  (secret :example-secret))

