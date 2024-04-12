(ns ai.unifica.gcloud.bigquery
  (:refer-clojure :exclude [load])
  (:require
   [clojure.string :as str]
   [tablecloth.api :as tc]
   [clojure.core.protocols :as p]
   [plumbing.core :refer [?>]])
  (:import
   (com.google.cloud RetryOption)
   (com.google.cloud.bigquery
    BigQuery$JobOption BigQuery$TableOption
    BigQueryOptions
    QueryJobConfiguration
    CsvOptions
    TableId
    LoadJobConfiguration
    Schema Field StandardSQLTypeName JobInfo)))

;; TODO Reflection warnings
(set! *warn-on-reflection* false)  ;; turn off *warn-on-reflection*

(defn service [{:gcloud/keys [project-id]}]
  (-> (BigQueryOptions/newBuilder)
      (.setProjectId project-id)
      .build
      .getService))

(defn with-service
  "Runs cmd with an authenticated connection"
  [ctx cmd]
  (cmd (service ctx)))

(defn query
  "Executes a query in BigQuery.
  Options:

  * dry-run: (default false) do not execute

  See also:

  https://cloud.google.com/bigquery/docs/samples/bigquery-query-dry-run
  "
  ([ctx sql]
   (query ctx sql {}))
  ([ctx sql opts]
   (with-service ctx
     (fn [service]
       (let [{:keys [dry-run] :or {dry-run false}} opts
             ^QueryJobConfiguration query-config
             (-> (QueryJobConfiguration/newBuilder sql)
                 (?> dry-run (.setDryRun true))
                 (?> dry-run (.setUseQueryCache false))
                 .build)
             info (JobInfo/of query-config)
             opts (into-array BigQuery$JobOption [])]
         (if dry-run
           (.create service info opts)
           (.query service query-config opts)))))))


;; Dataset location: https://msi.nga.mil/api/publications/download?type=view&key=16920959/SFH00000/UpdatedPub150.csv
;; (def res (query (str "SELECT * FROM unifica-ai.notebooks.test LIMIT 10")))

(defn parse-row
  "Split each FieldValueList into its components"
  [row]
  (let [indices (range (.size row))]
    (map #(.getValue (.get row %1)), indices)))

(defn column-names
  "Get column names from TableResult"
  [table-result]
  (let [field-list (.getFields (.getSchema table-result))
        list-length (.size field-list)
        list-indices (range list-length)]
    (map #(.getName (.get field-list %1)) list-indices)))

(defn ->dataset
  "Convert a TableResult into a dataset. Opts are passed to Tablecloth."
  [table-result & opts]
  (let [cols (map keyword (column-names table-result))
        data (->> table-result
                  (.iterateAll)
                  (map parse-row)
                  (map #(zipmap (partial cols) %1)))]
    (tc/dataset data opts)))

(defn tbl [{:gcloud/keys [project-id]} ds name] (str/join "." [project-id ds name]))

(defn schema
  "Return table schema for a dataset and table name. The project ID is obtained from the context."
  [{:gcloud/keys [project-id] :as ctx} ds name]
  (with-service ctx
    (fn [service]
      (let [tid (TableId/of project-id ds name)
        tbl (.getTable service tid (into-array BigQuery$TableOption []))
        def (.getDefinition tbl)
        schema (.getSchema def)
        fields (.getFields schema)]
        (map p/datafy fields)))))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.Field
  (datafy [o]
    {:name (.getName o)
     :type (-> (.getType o) p/datafy)}))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.LegacySQLTypeName
  (datafy [o]
    (.toString o)))

(defn -schema
  [m]
  (let [types {"STRING" StandardSQLTypeName/STRING
               "DECIMAL" StandardSQLTypeName/NUMERIC
               "FLOAT" StandardSQLTypeName/FLOAT64
               "NUMERIC" StandardSQLTypeName/NUMERIC
               "INTEGER" StandardSQLTypeName/INT64
               "DATE" StandardSQLTypeName/DATE}
        fs (map #(Field/of (:name %) (get types (:type %) "STRING") (into-array Field [])) m)]
    (Schema/of (into-array Field fs))))

(comment
  (-schema [{:name "foo" :type "DECIMAL"}]))

(defn load
  "Loads a dataset + table from a gs:// uri, with an explicit schema"
  [ctx ds tbl uri sch]
  (with-service ctx
    (fn [service]
      (let [opts (-> (CsvOptions/newBuilder)
                     (.setSkipLeadingRows 1)
                     .build)
            tid (TableId/of ds tbl)
            config
            (-> (LoadJobConfiguration/newBuilder tid uri opts)
                (.setSchema (-schema sch))
                .build)
            info (JobInfo/of config)
            create-opts (into-array BigQuery$JobOption [])
            job
            (-> service (.create info create-opts))
            retry-opts (into-array RetryOption [] )]
        (-> job
            (.waitFor retry-opts)
            p/datafy)))))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.Job
  (datafy [o]
    (merge
     #:gcloud.bigquery.job{:id (-> o .getJobId .getJob)
                           :status (-> o .getStatus .getState .toString)}
     (-> o .getStatistics p/datafy))))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.JobStatistics$QueryStatistics
  (datafy [o]
    #:google.bigquery.job.stats{:partitions-processed (.getTotalPartitionsProcessed o)
                                :total-processed (.getTotalBytesProcessed o)
                                :total-billed (.getTotalBytesBilled o)
                                :creation-time (.getCreationTime o)
                                :start-time (.getStartTime o)
                                :end-time (.getEndTime o)}))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.JobStatistics$LoadStatistics
  (datafy [o]
    #:google.bigquery.job.stats{:bad-records (.getBadRecords o)
                                :creation-time (.getCreationTime o)
                                :start-time (.getStartTime o)
                                :end-time (.getEndTime o)
                                :output-rows (.getOutputRows o)}))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.TableResult
  (datafy [o]
    {:gcloud.bigquery.job/id (-> o .getJobId .getJob)
     :gcloud.bigquery.result/total-rows (-> o .getTotalRows)}))
