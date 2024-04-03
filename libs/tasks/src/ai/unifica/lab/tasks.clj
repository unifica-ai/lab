(ns ai.unifica.lab.tasks
  (:require [com.biffweb.task-runner :refer [run-task]]
            [ai.unifica.lab.tasks.lazy.ai.unifica.lab.config :as config]
            [ai.unifica.lab.tasks.lazy.clojure.string :as str]
            [ai.unifica.lab.tasks.lazy.babashka.fs :as fs]
            [ai.unifica.lab.tasks.lazy.babashka.process :as process]
            [ai.unifica.lab.tasks.lazy.clojure.java.shell :as sh]
            [ai.unifica.lab.tasks.lazy.clojure.stacktrace :as st]))

(def ^:dynamic *shell-env* nil)

(defn- sh-success? [& args]
  (try
    (= 0 (:exit (apply sh/sh args)))
    (catch Exception _
      false)))

(defn- get-env-from [cmd]
  (let [{:keys [exit out]} (sh/sh "sh" "-c" (str cmd "; printenv"))]
    (when (= 0 exit)
      (->> out
           str/split-lines
           (map #(vec (str/split % #"=" 2)))
           (filter #(= 2 (count %)))
           (into {})))))

(defn- shell [& args]
  (apply process/shell {:extra-env *shell-env*} args))

(defn- with-ssh-agent* [{:keys [lab.tasks/skip-ssh-agent]} f]
  (if-let [env (and (not skip-ssh-agent)
                    (fs/which "ssh-agent")
                    (not (sh-success? "ssh-add" "-l"))
                    (nil? *shell-env*)
                    (get-env-from "eval $(ssh-agent)"))]
    (binding [*shell-env* env]
      (try
        (try
          (shell "ssh-add")
          (println "Started an ssh-agent session. If you set up `keychain`, you won't have to enter your password"
                   "each time you run this command: https://www.funtoo.org/Funtoo:Keychain")
          (catch Exception e
            (binding [*out* *err*]
              (st/print-stack-trace e)
              (println "\nssh-add failed. You may have to enter your password multiple times. You can avoid this if you set up `keychain`:"
                       "https://www.funtoo.org/Funtoo:Keychain"))))
        (f)
        (finally
          (sh/sh "ssh-agent" "-k" :env *shell-env*))))
    (f)))

(defmacro with-ssh-agent [ctx & body]
  `(with-ssh-agent* ~ctx (fn [] ~@body)))

(def ^:private config (delay (config/use-aero {})))

(defn- push-files-rsync [{:lab.tasks/keys [server deploy-untracked-files]}]
  (let [files (->> (:out (sh/sh "git" "ls-files"))
                   str/split-lines
                   (map #(str/replace % #"/.*" ""))
                   distinct
                   (concat deploy-untracked-files)
                   (filter fs/exists?))]
    (when (fs/exists? "config.env")
      (fs/set-posix-file-permissions "config.env" "rw-------"))
    (->> (concat ["rsync" "--archive" "--verbose" "--relative" "--include='**.gitignore'"
                  "--exclude='/.git'" "--filter=:- .gitignore" "--delete-after"]
                 files
                 [(str "app@" server ":")])
         (apply shell))))

(defn- push-files [ctx]
  (push-files-rsync ctx))

(defn- ssh-run [{:keys [lab.tasks/server]} & args]
  (apply shell "ssh" (str "root@" server) args))

(defn restart
  "Restarts the app processs via `systemctl restart app` (on the server)."
  []
  (ssh-run @config "systemctl reset-failed app.service; systemctl restart app"))

(defn logs
  "Tails the application's logs"
  ([]
   (logs "300"))
  ([n-lines]
   (ssh-run @config "journalctl" "-u" "app" "-f" "-n" n-lines)))

(defn deploy
  "Pushes code to the server and restarts the app.

   Uploads config and code to the server, using `rsync` if it's available, and
   `git push` otherwise. Then restarts the app."
  []
  (with-ssh-agent @config
    (push-files @config)
    (run-task "restart")))

(defn prod-repl
  "Opens an SSH tunnel so you can connect to the server via nREPL."
  []
  (let [{:keys [lab.tasks/server lab.nrepl/port]} @config]
    (println "Connect to nrepl port" port)
    (spit ".nrepl-port" port)
    (shell "ssh" "-NL" (str port ":localhost:" port) (str "root@" server))))

(defn dev
  []
  (let [{:keys [lab.nrepl/port lab.tasks/main-ns]} @config]
    (spit ".nrepl-port" port)
    ((requiring-resolve (symbol (str main-ns) "-main")))))

(def tasks
  {"deploy" #'deploy
   "restart" #'restart
   "logs" #'logs
   "prod-repl" #'prod-repl
   "dev" #'dev})
