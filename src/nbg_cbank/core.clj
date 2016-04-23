(ns nbg-cbank.core
  (:require [com.stuartsierra.component :as mc])
  (:require [compojure.core :as cc]
            [compojure.route :as cr])
  (:require [org.httpkit.server :as wsrv])
  (:require [postal.core :as pc])
  (:require [honeysql.core :as hc]
            [honeysql.format :as hf])
  (:require [clojure.java.jdbc :as j])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.core.async :as asnc])
  (:import [com.zaxxer.hikari HikariConfig
            HikariDataSource])
  (:gen-class))

;; (pc/send-message {:host "localhost"}
;;                  {:from "nvlass@megacorp1.eu"
;;                   :to "giorgospapa86@gmail.com"
;;                   :subject "Testing Clojure email subsystems #2"
;;                   :body "Should you receive this email, then all systems are nominal"})

(defn make-hikari-datasource [props]
  (let [hc (HikariConfig. (:hikari-props props))]
    {:datasource (HikariDataSource.  hc)}))

(defn stop-hikari-datasource [ds]
  (.close {:datasource ds}))


;; (defn ws-handler [req]
;;   (wsrv/with-channel req chan
;;     (wsrv/on-close chan (fn [status] (log/info "channel closed: " status)))
;;     (wsrv/on-receive chan (fn [data]
;;                             (log/info "received: " data)
;;                             (wsrv/send! chan data)))))

(cc/defroutes cbank-routes
  (cc/GET "/" [] {:status 200
                  :body "the empty route"})
  (cc/GET "/projects" {:status 200
                       :body "asd"}))

  ;;(cc/GET "/wsapi/" [] ws-handler))


(defn start-webserver [prop-map]
  (wsrv/run-server #'cbank-routes
                   {:port (:port prop-map)
                    :ip (:ip prop-map)
                    :thread (:thread prop-map)}))

(defn stop-webserver [stop-fn]
  (stop-fn))


(defrecord db [conf connection]
  mc/Lifecycle
  (start [component]
    (log/info "starting database")
    (when (nil? (:connection component))
      (let [conn (make-hikari-datasource conf)]
        (assoc component :connection conn))))
  (stop [component]
    (log/info "stopping database")
    (when-not (nil? (:connection component))
      (.close connection)
      (assoc component :connection nil))))


(defrecord ws [conf ws]
  mc/Lifecycle
  (start [component]
    (log/info "starting webserver")
    (when (nil? (:ws component))
      (assoc component :ws (start-webserver conf))))
  (stop [component]
    (log/info "stopping webserver")
    (when-not (nil? (:ws component))
      (let [stopfn (:ws component)]
        (stopfn)
        (assoc component :ws nil)))))

(defn new-db [conf]
  (map->db {:conf conf}))

(defn new-ws [conf]
  (map->ws {:conf conf}))

(defn make-system [conf]
  (mc/system-map
   :db (new-db conf)
   :ws (mc/using
        (new-ws conf)
        [:db])))

(defn get-connection [db]
  ((:connection db)))

(defn project-list [db]
  (j/with-db-connection [c (:connection db)]
    (j/query c "SELECT * FROM projects")))

(def app-conf {:hikari-props "resources/hikari.properties"
               :port 8989
               :ip "127.0.0.1"
               :thread 4})

(def system (make-system app-conf))

(defn start-system []
  (alter-var-root #'system mc/start))

(defn stop-system []
  (alter-var-root #'system mc/stop))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))


