(ns ai.unifica.plumbing
  (:refer-clojure :exclude [flatten])
  (:require
   [babashka.fs :as fs]
   [camel-snake-kebab.core :as csk]
   [clojure.core.memoize :as m]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [plumbing.core :refer [map-keys]]
   [plumbing.map :as plm])
  (:import
   (java.util Base64)
   (java.io File)
   (java.nio.file Files Paths)
   (java.nio.file.attribute FileAttribute)))

(defn wait-for
  "Invoke predicate every interval (default 3) seconds until it returns true,
  or timeout (default 30) seconds have elapsed. E.g.:
      (wait-for #(< (rand) 0.2) :interval 1 :timeout 10)
  Returns nil if the timeout elapses before the predicate becomes true, otherwise
  the value of the predicate on its last evaluation."
 [predicate & {:keys [interval timeout]
               :or {interval 3
                    timeout 30}}]
 (let [end-time (+ (System/currentTimeMillis) (* timeout 1000))]
   (loop []
     (if-let [result (predicate)]
       result
       (do
         (Thread/sleep (* interval 1000))
         (when (< (System/currentTimeMillis) end-time)
           (recur)))))))


(def ->kebab-case
  "Memoize since every dataset has the same columns and we do this operation repeatedly"
  (m/fifo csk/->kebab-case {} :fifo/threshold 512))

(defn flatten
  "Returns a flattened version of a nested map with keyword keys"
  [m]
  (let [f (plm/flatten m)]
    (->> f
         (map (fn [[k v]] [(->> k (map name) (interpose "-") (apply str) ->kebab-case) v]))
         (into {})
         (map-keys #(-> % name (str/replace "b-2-b" "b2b") keyword)))))

(comment
  (flatten {:foo {:b-2-b "beetoobee"}}))

;; https://ericnormand.me/article/try-three-times
(defn try-n-times [f n]
  "Pass a function and a number of times to retry it"
  (if (zero? n)
    (f)
    (try
      (f)
      (catch Throwable e
        (log/error e)
        (try-n-times f (dec n))))))

 (defmacro try3 [& body]
   `(try-n-times (fn [] ~@body) 2))

(comment
  (flatten {:a {:b "c"}}))

(defn encode [to-encode]
  (.encode (Base64/getEncoder) (.getBytes to-encode)))

(defn decode [to-decode]
  (String. (.decode (Base64/getDecoder) to-decode)))

(comment
  ;; Encode in base 64 and then decode back
  (-> (String. (encode "hi"))
      decode))

;; From https://github.com/scicloj/tempfiles/blob/main/src/scicloj/tempfiles/api.clj
(def ^:private *session-tempdir (atom  nil))

(def root
  "The root directory to hold all temporary directories
  as subdirectories"
  "/tmp/unifica-files/")

(defn- make-new-tempdir! []
  (when-not (fs/exists? root)
    (fs/create-dirs root))
  (->> (Files/createTempDirectory
        (Paths/get root (into-array String []))
        "unifica-"
        (into-array FileAttribute []))
       (reset! *session-tempdir)))

(defn session-tempdir!
  "Get the temporary directory of the current session."
  []
  (-> @*session-tempdir
      (or (make-new-tempdir!))
      str))

(defn tempfile!
  [extension]
  (let [file ^File (File/createTempFile "file-"
                                   extension
                                   (io/file (session-tempdir!)))]
    (.getPath file)))
