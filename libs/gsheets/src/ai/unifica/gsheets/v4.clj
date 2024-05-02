(ns ai.unifica.gsheets.v4
  (:import [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver$Builder]
           [com.google.api.services.sheets.v4 Sheets]
           [com.google.api.services.sheets.v4.model ValueRange
                                                    CellData
                                                    ExtendedValue
                                                    RowData]))

(set! *warn-on-reflection* true)

(defn receiver [port]
  (-> (LocalServerReceiver$Builder.)
      (.setPort port)
      .build))

(defn get-values
  [^Sheets gservice id]
  (let [^ValueRange response (-> gservice
                                 .spreadsheets
                                 .values
                                 (.get id range)
                                 .execute)]
    (.getValues response)))

(defn row->row-data
  "turns a row (list of columns) into data"
  [row]
  (-> (RowData.)
      (.setValues (map #(-> (CellData.)
                            (.setUserEnteredValue
                             (-> (ExtendedValue.)
                                 (.setStringValue %)))) row))))
