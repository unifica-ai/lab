(ns ai.unifica.lab.tasks.lazy.babashka.process
  (:require [com.biffweb.task-runner.lazy :as lazy]))

(lazy/refer-many babashka.process [shell process])
