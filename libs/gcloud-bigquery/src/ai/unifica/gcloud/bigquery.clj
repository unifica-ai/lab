(ns ai.unifica.gcloud.bigquery
  (:refer-clojure :exclude [load])
  (:require
   [clojure.string :as str]
   [tablecloth.api :as tc]
   [clojure.core.protocols :as p])
  (:import
   (com.google.cloud RetryOption)
   (com.google.cloud.bigquery
    BigQuery$JobOption
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
  "Executes a query in BigQuery"
  [ctx query]
  (with-service ctx
    (fn [service]
      (let [^QueryJobConfiguration query-config
            (-> (QueryJobConfiguration/newBuilder query)
                .build)
            opts (into-array BigQuery$JobOption [])]
        (.query service query-config opts)))))

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
  [m]
  (let [types {"STRING" StandardSQLTypeName/STRING
               "NUMERIC" StandardSQLTypeName/NUMERIC
               "INTEGER" StandardSQLTypeName/INT64
               "DATE" StandardSQLTypeName/DATE}
        fs (map #(Field/of (:name %) (get types (:type %)) (into-array Field [])) m)]
    (Schema/of (into-array Field fs))))

(comment
  (schema [{:name "foo" :type "STRING"}]))

(defn load
  "Loads a dataset + table from a gs:// uri, with an explicit schema"
  [ctx ds tbl uri schema]
  (with-service ctx
    (fn [service]
      (let [opts (-> (CsvOptions/newBuilder)
                     (.setSkipLeadingRows 1)
                     .build)
            tid (TableId/of ds tbl)
            config
            (-> (LoadJobConfiguration/newBuilder tid uri opts)
                (.setSchema schema)
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
     (let [stats (-> o .getStatistics)]
       #:google.bigquery.job.stats{:bad-records (.getBadRecords stats)
                                   :creation-time (.getCreationTime stats)
                                   :start-time (.getStartTime stats)
                                   :end-time (.getEndTime stats)
                                   :output-rows (.getOutputRows stats)}))))

(extend-protocol p/Datafiable
  com.google.cloud.bigquery.TableResult
  (datafy [o]
    {:gcloud.bigquery.job/status (-> o .getJobId .getJob)
     :gcloud.bigquery.result/total-rows (-> o .getTotalRows)}))

(comment

  (let [ds "amazon_sp_reports"
        tbl "st_sales_and_traffic"
        uri "https://storage.cloud.google.com/unifica-luum-datalake/a/luum/2024/03/sales-and-traffic/amz-sp-sales-and-traffic-report-2024-03-12.csv"
        schema (schema (vector {:name "traffic_by_asin_mobile_app_session_percentage_b2b", :type "NUMERIC"} {:name "sales_by_asin_units_ordered_b2b", :type "INTEGER"} {:name "traffic_by_asin_unit_session_percentage", :type "NUMERIC"} {:name "traffic_by_asin_page_views_b2b", :type "INTEGER"} {:name "traffic_by_asin_page_views_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_browser_page_views", :type "INTEGER"} {:name "traffic_by_asin_buy_box_percentage", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_page_views_percentage", :type "NUMERIC"} {:name "traffic_by_asin_browser_session_percentage", :type "NUMERIC"} {:name "traffic_by_asin_unit_session_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_session_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_browser_session_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_page_views", :type "INTEGER"} {:name "traffic_by_asin_mobile_app_session_percentage", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_page_views", :type "INTEGER"} {:name "traffic_by_asin_browser_sessions_b2b", :type "INTEGER"} {:name "traffic_by_asin_browser_page_views_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_page_views_percentage", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_page_views_b2b", :type "INTEGER"} {:name "traffic_by_asin_browser_sessions", :type "INTEGER"} {:name "sales_by_asin_ordered_product_sales_amount", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_sessions", :type "INTEGER"} {:name "traffic_by_asin_buy_box_percentage_b2b", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_page_views_percentage_b2b", :type "NUMERIC"} {:name "sales_by_asin_ordered_product_sales_b2b_currency_code", :type "STRING"} {:name "traffic_by_asin_sessions_b2b", :type "INTEGER"} {:name "sales_by_asin_total_order_items_b2b", :type "INTEGER"} {:name "sales_by_asin_total_order_items", :type "INTEGER"} {:name "traffic_by_asin_browser_page_views_percentage", :type "NUMERIC"} {:name "traffic_by_asin_mobile_app_sessions_b2b", :type "INTEGER"} {:name "sales_by_asin_units_ordered", :type "INTEGER"} {:name "child_asin", :type "STRING"} {:name "parent_asin", :type "STRING"} {:name "sales_by_asin_ordered_product_sales_currency_code", :type "STRING"} {:name "traffic_by_asin_session_percentage", :type "NUMERIC"} {:name "traffic_by_asin_sessions", :type "INTEGER"} {:name "sales_by_asin_ordered_product_sales_b2b_amount", :type "NUMERIC"} {:name "traffic_by_asin_browser_page_views_b2b", :type "INTEGER"} {:name "date", :type "DATE"} {:name "marketplace_id", :type "STRING"}))
        svc (service {:gcloud/project-id "luum-413618"})]
    (def schema* schema)
    (load {:gcloud.bigquery/service svc} ds tbl uri schema))

)
