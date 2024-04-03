^{:kindly/hide-code true}
(ns bigquery
  (:require [ai.unifica.gcloud.bigquery :as bq]
            [clojure.string :as str]
            [ai.unifica.lab.notebooks :refer [md]]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

^{:kindly/hide-code true
  :kindly/kind :kind/hiccup}
[:img {:src "notebooks/img/huesos.png"
       :style {:width "200px"}}]

;; # Cual Pokemon es Huesos?
;;
;; Adapted from this talk:
(md "[Link](https://github.com/ladymeyy/tablecloth-talks-and-workshop/tree/main)")

;; **bq-project**
;;
;; El **proyecto** en Google Cloud Platform (GCP)
(def bq-project "luum-413618")

;; **bq-dataset**
;;
;; El [dataset](https://cloud.google.com/bigquery/docs/datasets-intro) que
;; contiene la información de Luum para esta fuente en particular
(def bq-dataset "pokemons")

(defonce ds-pokemon
  (let [tbl (bq/tbl bq-dataset "pokemons")
        sql (str "SELECT * from `" tbl "`;")]
    (-> (bq/query sql)
        (bq/->dataset)
        (tc/convert-types {:height_m :float32
                           :weight_kg :float32}))))

(defonce ds-combat
  (let [sql (str "SELECT * from `" (bq/tbl bq-dataset "combats") "`;")]
    (-> (bq/query sql)
        (bq/->dataset))))

^{:kindly/hide-code true}
(comment
  (-> ds-combat
      (tc/head 3)))

;; ### 3 tallest pokemons
;;
;; Save a cleaned up version of pokemon, keeping just the columns we need
;; (If the last form is uncommented, then write the result to a file.)
;;
;; - Wailord and Celesteela are the tallest Pokemeons.

(-> ds-pokemon
    (tc/select-columns [:pokedex_number :name :height_m :weight_kg :generation :is_legendary])
    (tc/order-by [:height_m] :desc)
    (tc/head 3))

;; ### Total pokemons
;;
;; - There are 801 pokemons total.

(-> ds-pokemon
    (tc/row-count))

;; ### Shortest pokemons
;; Cutiefly and Cosmoem are the shortest.

(-> ds-pokemon
    (tc/select-columns [:pokedex_number :name :height_m :weight_kg :generation :is_legendary])
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
(-> ds-pokemon
    (tc/select-columns [:pokedex_number :name :height_m :weight_kg])
    (tc/select-rows (fn [row] (and (height-in-range? row)
                                   (weight-in-range? row)))))

(-> ds-combat
    (tc/group-by [:Winner])
    (tc/aggregate {:number_of_wins tc/row-count})
    (tc/rename-columns {:Winner :pokedex_number})
    (tc/order-by [:number_of_wins] :desc)
    (tc/head 3))

;; Combining the last two

(let [wins (-> ds-combat
               (tc/group-by [:Winner])
               (tc/aggregate {:number_of_wins tc/row-count})
               (tc/rename-columns {:Winner :pokedex_number})
               (tc/order-by [:number_of_wins] :desc))]
  (-> ds-pokemon
      (tc/select-columns [:pokedex_number :name :height_m :weight_kg])
      (tc/select-rows (fn [row] (and (height-in-range? row)
                                     (weight-in-range? row))))
      (tc/left-join wins :pokedex_number)))
