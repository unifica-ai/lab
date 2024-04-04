(ns tasks
  (:require [ai.unifica.lab.tasks :as tasks]))

(def custom-tasks {})

(def tasks (merge tasks/tasks custom-tasks))
