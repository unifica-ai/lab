(ns ai.unifica.lab.notebooks
  (:require
   [scicloj.kindly.v4.api :as kindly]
   [scicloj.kindly.v4.kind :as kind]))


(def md
  (comp kindly/hide-code kind/md))

(def code
  (comp kindly/hide-code kind/code))

(def html
  (comp kindly/hide-code kind/hiccup))

(def logo (html [:img
 {:style {:width "200px"}
  :src "https://raw.githubusercontent.com/unifica-ai/lab/main/resources/logo.svg"
  :alt "Unifica logo"}]))
