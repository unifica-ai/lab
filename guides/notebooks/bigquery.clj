^:kindly/hide-code
(require '[ai.unifica.lab.notebooks :refer [logo]])

logo

;; # BigQuery

(ns bigquery
  (:require [guides.lab :as lab]
            [clojure.string :as str]
            [ai.unifica.lab.functions :as lf]
            [ai.unifica.gcloud.bigquery :as bq]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [clojure.core.protocols :as p]))

(lab/start!)

;; El laboratorio de Unifica se conecta a BigQuery, que utilizamos para alojar
;; data de clientes.
;;
;; Cada cliente tiene su proyecto de GCP, que está guardado en `@lab/system` y
;; se usa para todas las operaciones de ese cliente.
;;
;; ## Prerequisitos
;;
;; Para conectar al data warehouse, es encesario crear un service account en GCP
;; con accesos específicos. Contactar a Kyle.
;;
;; ## Correr un query
;;
;; Podemos usar la funcion `bq/query` para ir a traer data. Si pasamos la opcion
;; `dry-run`, lo que vemos es un resumen de cuánto va a costar ese query.

(-> (bq/query @lab/system "SELECT 1=1" {:dry-run true})
    p/datafy)

;; En este caso, se procesaron 0 bytes porque el query `SELECT 1=1` no utiliza
;; datos. Para explorar datos, vamos a hacer un ejercicio.

;; ## Cual Pokemon es Huesos?

;; Huesos es mi perro. Tenemos una base de datos de Pokemons que bajamos de
;; Kaggle, y queremos averiguar cuál Pokemon es.

^{:kindly/hide-code true
  :kindly/kind :kind/hiccup}
[:img {:src "notebooks/img/huesos.jpg"
       :style {:width "200px"}}]

;; Huesos es el Pokemon con igual altura y peso, y que también ha ganado la
;; mayor cantidad de batallas contra otros pokemones.

;; Esta sección fue adaptada de esta [presentacion][talk].
;;
;; [talk]: https://github.com/ladymeyy/tablecloth-talks-and-workshop/tree/main
;;
;; **bq-dataset**
;;
;; El [dataset][dataset] es una agrupación de tablas y otros elementos en
;; BigQuery. En general se usa un dataset por cada notebook.
;;
;; [dataset]: https://cloud.google.com/bigquery/docs/datasets-intro

(def bq-dataset "pokemons")

;; Las funciones para retirar data de BigQuery tienen este formato.

(delay (bq/schema @lab/system bq-dataset "pokemons"))

;; Entre esas colunas, vamos a escoger solamente algunas por defecto.

(def ds-pokemons-cols ["pokedex_number" "name" "height_m" "weight_kg" "generation" "is_legendary"])

;; Luego armamos un dataset, extrayendo solo esas columnas de la fuente.
;; Esta es nuestra tabla de "dimensiones".

(defn ds-pokemons
  ([ctx] (ds-pokemons ctx {}))
  ([ctx {:keys [cols] :or {cols ds-pokemons-cols}}]
    (let [cols* (str/join ", " cols)
          tbl (bq/tbl ctx bq-dataset "pokemons")

          sql (str "SELECT " cols* " from `" tbl "`")

          post (fn [ds]
                 (-> ds
                     (tc/convert-types {:height_m :float32
                                        :weight_kg :float32})))]
      {:name "pokemons"
       :sql sql
       :post post})))

;; Lo mismo con combates. Esta es nuesta tabla de "facts". Contiene
;; batallas etre dos pokemones, y cual fue el ganador.

(delay (bq/schema @lab/system bq-dataset "combats"))

(def ds-combats-cols ["First_pokemon" "Second_pokemon" "Winner"])

(defn ds-combats
  "Combats dataset"
  ([ctx] (ds-combats ctx {}))
  ([ctx {:keys [cols] :or {cols ds-combats-cols}}]
   (let [cols* (str/join ", " cols)
        tbl (bq/tbl ctx bq-dataset "combats")

        sql (str "SELECT " cols* " from `" tbl "`")]
    {:name "combats"
     :sql sql})))

;; Cuando ejecutamos la función, no utilizamos BigQuery. Es mas como un "plan"
;; de que es lo que se va a hacer.

(ds-pokemons @lab/system)

;; El SQL

(:sql (ds-pokemons @lab/system))

;; Usando el SQL para obtener un dataset

(delay
  (-> (bq/query @lab/system (:sql (ds-pokemons @lab/system)) {:dry-run true})
      (p/datafy)))

;; Este query va procesar 29 MB de datos.

(ds-combats @lab/system)

(bq/schema @lab/system bq-dataset "combats")

;; ### 3 pokemones mas altos
;;
;; Save a cleaned up version of pokemon, keeping just the columns we need
;; (If the last form is uncommented, then write the result to a file.)
;;
;; - Wailord and Celesteela are the tallest Pokemeons.

(def pokemons (lf/fetch ds-pokemons @lab/system))

(-> pokemons
    (tc/order-by [:height_m] :desc)
    (tc/head 3))

;; ### Total pokemons
;;
;; - There are 801 pokemons total.

(-> pokemons
    (tc/row-count))

;; ### Shortest pokemons
;; Cutiefly and Cosmoem are the shortest.

(-> pokemons
    (tc/order-by [:height_m])
    (tc/select-rows (comp #(not= % nil) :height_m))
    (tc/head 3))

;; ## Which pokemon is Huesos?
;;
;; Huesos is the pokemon who is a winner, and has a similar height and weight
;;
;; ### Equivalent SQL

;; We want to replicate the results of this SQL query:

^{:kindly/hide-code true}
(->>
 ["SELECT pokedex_number, name, height_m, weight_kg, b.wins"
  "FROM pokemons p"
  "LEFT JOIN (select count(1) AS wins, winner"
  "FROM battles"
  "GROUP BY winner) as b"
  "ON b.winner = p.pokedex_number"
  "WHERE height_m BETWEEN 1.6 AND 1.8"
  "AND weight_kg BETWEEN 40 and 60 ORDER BY pokedex_number ASC"]
 (str/join "\n")
 kind/code)

;; ### Constants and Functions

(def huesos-height 0.5) ;; meters

(def huesos-weight 23.0) ;; kg

;; Define a function to find out if weight is in range

(defn weight-in-range? [row]
  (and (:weight_kg row)
       (> (+ huesos-weight 1) (:weight_kg row) (- huesos-weight 1))))

;; Define a funciton to find out if height is in range

(defn height-in-range? [row]
  (and (:height_m row)
       (> (+ huesos-height 0.5) (:height_m row) (- huesos-height 0.5))))

^{:kindly/hide-code true}
(comment
  (weight-in-range? {:weight_kg 5})
  (height-in-range? {:height_m nil}))

;; Pokemons in the right height and weight range
(-> pokemons
    (tc/select-columns [:pokedex_number :name :height_m :weight_kg])
    (tc/select-rows (fn [row] (and (height-in-range? row)
                                   (weight-in-range? row)))))

(-> pokemons
    (tc/group-by [:Winner])
    (tc/aggregate {:number_of_wins tc/row-count})
    (tc/rename-columns {:Winner :pokedex_number})
    (tc/order-by [:number_of_wins] :desc)
    (tc/head 3))

;; Combining the last two

(def combats (lf/fetch ds-combats @lab/system))


(let [wins (-> combats
               (tc/group-by [:Winner])
               (tc/aggregate {:number_of_wins tc/row-count})
               (tc/rename-columns {:Winner :pokedex_number})
               (tc/order-by [:number_of_wins] :desc))]
  (-> pokemons
      (tc/select-columns [:pokedex_number :name :height_m :weight_kg])
      (tc/select-rows (fn [row] (and (height-in-range? row)
                                     (weight-in-range? row))))
      (tc/left-join wins :pokedex_number)))

;; Huesos is Prinlup!!
