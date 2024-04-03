(ns ai.unifica.amzn-sp.reports
  (:refer-clojure :exclude [read get])
  (:require
   [ai.unifica.plumbing :refer [try3 wait-for]]
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as cheshire]
   [clojure.core :as c]
   [clojure.core.protocols :as p]
   [clojure.datafy :refer [datafy]]
   [clojure.java.io :as io]
   [plumbing.core :refer [?> map-keys]])
  (:import
   (com.amazon.SellingPartnerAPIAA LWAAuthorizationCredentials)
   (io.swagger.client.api ReportsApi ReportsApi$Builder)
   (io.swagger.client.model CreateReportSpecification ReportOptions)
   (org.threeten.bp
    LocalDate
    LocalTime
    OffsetDateTime
    ZoneOffset)))

(set! *warn-on-reflection* true)  ;; turn on *warn-on-reflection*

(def ^:const marketplace-usa "ATVPDKIKX0DER")

(def types
  "Types as defined in

  https://developer-docs.amazon.com/sp-api/docs/report-type-values
  "
  {:sales-and-traffic "GET_SALES_AND_TRAFFIC_REPORT"})

(extend-protocol p/Datafiable
  io.swagger.client.model.CreateReportResponse
  (datafy [o]
    {:amzn-sp.reports/id (.getReportId o)}))

(extend-protocol p/Datafiable
  io.swagger.client.model.Report
  (datafy [o]
     #:amzn-sp.reports{:id (.getReportId o)
                       :status (-> o .getProcessingStatus .toString)
                       :doc-id (.getReportDocumentId o)}))

(extend-protocol p/Datafiable
  io.swagger.client.model.ReportDocument
  (datafy [o]
    #:amzn-sp.reports{:doc-id (.getReportDocumentId o)
                      :doc-url (.getUrl o)
                      :compression (-> o .getCompressionAlgorithm .toString)
                      ;; :encryption (-> o .getEncryptionDetails datafy)
                      }))

(extend-protocol p/Datafiable
  io.swagger.client.model.ReportDocumentEncryptionDetails
  (datafy [o]
    {:standard (-> o .getStandard .toString)
     :init-vector (.getInitializationVector o)
     :key (.getKey o)}))

(defn with-conn
  "Runs cmd with an authenticated connection"
  [{:keys [:lab/secret] :as ctx} cmd]
  (let [^LWAAuthorizationCredentials creds
        (-> (LWAAuthorizationCredentials/builder)
             (.clientId (:amzn-sp/client-id ctx))
             (.clientSecret (secret :amzn-sp/client-secret))
             (.refreshToken (secret :amzn-sp/refresh-token))
             (.endpoint (:amzn-sp/auth-endpoint ctx))
             .build)
        ^ReportsApi api
        (-> (ReportsApi$Builder.)
            (.lwaAuthorizationCredentials creds)
            (.endpoint (:amzn-sp/endpoint ctx))
            .build)]
    (cmd api)))

;; TODO solve reflection warning
(defn- ->opts
  "Returns options in the format expected by the SP API"
  [m]
  (let [h (->> m
               (map-keys (comp name csk/->camelCase))
               (java.util.HashMap.))
        o (ReportOptions.)]
    (-> o (.putAll h))
    o))

(defn create
  "Creates a report. Dates are YYYY-MM-DD. Options vary per report type."
  [ctx {:amzn-sp.reports/keys [marketplace-ids type date-from date-to opts]
        :or {marketplace-ids (list marketplace-usa)}}]
  {:pre [(not (empty? type))]}
  (let [zone-offset "-08"] ;; seems to go unused?
    (with-conn ctx
      (fn [^ReportsApi api]
        (let [h (LocalTime/of 0 0 0)
              z (ZoneOffset/of zone-offset)
              d0 (when date-from (LocalDate/parse date-from))
              ;; Even though the java API takes a date time object, the time
              ;; portion does not seem to be used.
              d1 (when date-to (-> (LocalDate/parse date-to) #_(.plusDays 1)))
              ->offset #(OffsetDateTime/of % h z)
              ^CreateReportSpecification params
              (-> (CreateReportSpecification.)
                  (.reportType type)
                  (.marketplaceIds marketplace-ids)
                  (?> opts (.reportOptions (->opts opts)))
                  (?> d0 (.dataStartTime (->offset d0)))
                  (?> d1 (.dataEndTime (->offset d1))))
              response (.createReport api params)]
          (-> response datafy))))))

(defn get
  "Gets a report"
  [ctx {:keys [:amzn-sp.reports/id]}]
  {:pre [(not (empty? id))]}
  (with-conn ctx
    (fn [^ReportsApi api]
      (let [response (.getReport api id)]
        (datafy response)))))

(defn get-doc
  "Gets a report document from a report document ID"
  [ctx {:keys [:amzn-sp.reports/doc-id]}]
  (with-conn ctx
    (fn [^ReportsApi api]
      (let [response (.getReportDocument api doc-id)]
        (datafy response)))))

(defn done?
  "Returns a report if its processing status is DONE, and nil otherwise"
  [r]
  (when (= (:amzn-sp.reports/status r) "DONE") r))

(defn download
  "Downloads a report, returning a map"
  [{:amzn-sp.reports/keys [doc-url compression]}]
  (let [stream (-> doc-url
                   io/input-stream
                   (?> (= compression "GZIP")(java.util.zip.GZIPInputStream.)))
        contents (slurp stream)]
    (cheshire/parse-string contents)))

(defn datasets
  "Get lists of nested data maps from a report map, depending on report type" [m]
  (let [subs (->> m ((juxt #(c/get % "salesAndTrafficByAsin")
                           #(c/get % "salesAndTrafficByDate"))))
        [byasin bydate] subs]
    {:asin byasin
     :date bydate}))

(defn fetch
  "Workflow which performs the sequence of calls necessary to get a report."
  [ctx {:amzn-sp.reports/keys [marketplace-ids type date-from date-to opts] :as params}]
  (let [create-result (try3 (create ctx params))
        report (wait-for #(done? (try3 (get ctx create-result))))
        doc (try3 (get-doc ctx report))
        data (try3( download doc))]
    {
     :create-result create-result
     :report report
     :doc doc
     :data data}))
