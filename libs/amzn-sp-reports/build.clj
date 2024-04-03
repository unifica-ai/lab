(ns build
  "Tasks for building this dependency"
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile [_]
  (b/javac {:src-dirs ["java"]
            :class-dir class-dir
            :basis @basis
            :javac-opts ["--release" "17"]}))
