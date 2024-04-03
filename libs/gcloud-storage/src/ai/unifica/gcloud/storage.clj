(ns ai.unifica.gcloud.storage
  (:require
   [clojure.core.protocols :as p]
   [clojure.string :as str])
  (:import
   (com.google.cloud.storage
    BlobId
    BlobInfo
    Storage$BlobWriteOption
    StorageOptions)
   (java.nio.file Paths)))

;; https://cloud.google.com/storage/docs/uploading-objects#storage-upload-object-client-libraries

(defn service [{:gcloud/keys [project-id]}]
  (-> (StorageOptions/newBuilder)
      (.setProjectId project-id)
      .build
      .getService))

(defn with-service
  "Runs cmd with an authenticated connection"
  [ctx cmd]
  (cmd (service ctx)))

(extend-protocol p/Datafiable
  com.google.cloud.storage.Blob
  (datafy [o]
    (let [i (.asBlobInfo o)
          endpoint "https://storage.cloud.google.com"
          bucket (.getBucket i)
          name (.getName i)]
      {:gcloud.storage/uri (str/join "/" [endpoint bucket name])})))

(defn create
  "Creates an object from a file-path in the target bucket."
  [ctx bucket file-path obj]
  (let [id (BlobId/of bucket, obj)
        blob (-> (BlobInfo/newBuilder id) (.build))]
    (with-service ctx
      (fn [service]
        (-> service (.createFrom blob
                                 (Paths/get file-path (into-array String []))
                                 (into-array Storage$BlobWriteOption [])))))))
