(ns ai.unifica.lab.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]))

;; Adapted from
;; https://github.com/jacobobryant/biff/blob/v1.8.0/libs/config/src/com/biffweb/config.clj

(defmacro catchall
  [& body]
  `(try ~@body (catch Exception ~'_ nil)))

;; Algorithm adapted from dotenv-java:
;; https://github.com/cdimascio/dotenv-java/blob/master/src/main/java/io/github/cdimascio/dotenv/internal/DotenvParser.java
;; Wouldn't hurt to take a more thorough look at Ruby dotenv's algorithm:
;; https://github.com/bkeepers/dotenv/blob/master/lib/dotenv/parser.rb
(defn parse-env-var [line]
  (let [line (str/trim line)
        [_ _ k v] (re-matches #"^\s*(export\s+)?([\w.\-]+)\s*=\s*(['][^']*[']|[\"][^\"]*[\"]|[^#]*)?\s*(#.*)?$"
                              line)]
    (when-not (or (str/starts-with? line "#")
                  (str/starts-with? line "////")
                  (empty? v))
      (let [v (str/trim v)
            v (if (or (re-matches #"^\".*\"$" v)
                      (re-matches #"^'.*'$" v))
                (subs v 1 (dec (count v)))
                v)]
        [k v]))))

(defmethod aero/reader 'lab/env
  [{:keys [profile lab.aero/env] :as opts} _ value]
  (not-empty (get env (str value))))

(defmethod aero/reader 'lab/secret
  [{:keys [profile lab.aero/env] :as opts} _ value]
  (when-some [value (aero/reader opts 'lab/env value)]
    (fn [] value)))

(defn get-env []
  (reduce into
          {}
          [(some->> (catchall (slurp "config.env"))
                    str/split-lines
                    (keep parse-env-var))
           (System/getenv)
           (keep (fn [[k v]]
                   (when (str/starts-with? k "lab.env.")
                     [(str/replace k #"^lab.env." "") v]))
                 (System/getProperties))]))

(defn use-aero [{:ai.unifica.lab.config/keys [profile] :as ctx}]
  (let [env (get-env)
        profile (some-> (or profile (get env "LAB_PROFILE"))
                        keyword)
        ctx (merge ctx (aero/read-config (io/resource "config.edn") {:profile profile :lab.aero/env env}))
        secret (fn [k]
                 (some-> (get ctx k) (.invoke)))]
    (assoc ctx :lab/secret secret)))

(comment
  ;; Get a secret
  (let [{:keys [:lab/secret] :as ctx} (use-aero {})]
    (secret :lab.sp-api/client-secret)))
