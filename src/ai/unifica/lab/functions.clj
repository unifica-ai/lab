(ns ai.unifica.lab.functions
  (:require
   [ai.unifica.gcloud.bigquery :as bq]))

(defn fetch
  "Fetch a dataset from BigQuery and transform it to a dataset"
  [fn ctx & args]
  (let [{:keys [sql post] :or {post identity}} (apply fn ctx args)]
    (-> (bq/query ctx sql)
        bq/->dataset
        post)))
