(ns ai.unifica.gsheets
  (:require [ai.unifica.gsheets.config :as config]
            [ai.unifica.gsheets.v4 :as sheets-v4])
  (:import [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp]
           [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets GoogleAuthorizationCodeFlow$Builder]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.gson GsonFactory]
           [com.google.api.client.util.store DataStoreFactory]
           [com.google.api.services.sheets.v4 Sheets$Builder]))

(defn google-service
  [{:gsheets/keys [^String application-name ^DataStoreFactory tokens-directory
            credentials authorize scopes json-factory access-type port]
    :or {scopes config/scopes
         credentials (config/credentials)
         tokens-directory (config/tokens)
         json-factory (GsonFactory/getDefaultInstance)}}]
  (let [http-transport (GoogleNetHttpTransport/newTrustedTransport)
        client-secrets (GoogleClientSecrets/load json-factory credentials)
        flow (-> (GoogleAuthorizationCodeFlow$Builder.
                   http-transport
                   json-factory
                   client-secrets
                   scopes)
                 (.setDataStoreFactory tokens-directory)
                 (.setAccessType access-type)
                 .build)
        receiver (sheets-v4/receiver port)]
    (-> (Sheets$Builder.
          http-transport json-factory
          (-> (AuthorizationCodeInstalledApp. flow receiver)
              (.authorize authorize)))
        (.setApplicationName application-name)
        .build)))

(defn component
  "A lab component which lods a Google service"
  [ctx]
  (let [service (google-service ctx) ]
    (assoc ctx :gsheets/google-service service)))
