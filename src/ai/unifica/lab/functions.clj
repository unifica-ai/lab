(ns ai.unifica.lab.functions
  (:require
   [ai.unifica.gcloud.bigquery :as bq]
   [clojure.core.protocols :as p]
   [tablecloth.api :as tc]))

(defn fetch
  "Fetch a dataset from BigQuery and transform it to a dataset"
  [fn ctx & args]
  (let [{:keys [ds-opts sql post] :or {post identity}} (apply fn ctx args)
        ds (-> (bq/query ctx sql)
               (bq/->dataset ds-opts))]
    (if (tc/empty-ds? ds) ds (post ds))))

(defn dry-run
  [fn ctx & args]
  (let [{:keys [sql]} (apply fn ctx args)]
    (-> (bq/query ctx sql {:dry-run true})
        (p/datafy))))
