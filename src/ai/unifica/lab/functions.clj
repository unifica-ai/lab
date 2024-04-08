(ns ai.unifica.lab.functions
  (:require
   [ai.unifica.gcloud.bigquery :as bq]
   [clojure.core.protocols :as p]))

(defn fetch
  "Fetch a dataset from BigQuery and transform it to a dataset"
  [fn ctx & args]
  (let [{:keys [name sql post] :or {post identity}} (apply fn ctx args)]
    (-> (bq/query ctx sql)
        (bq/->dataset {:dataset-name name})
        post)))

(defn dry-run
  [fn ctx & args]
  (let [{:keys [sql]} (apply fn ctx args)]
    (-> (bq/query ctx sql {:dry-run true})
        (p/datafy))))
