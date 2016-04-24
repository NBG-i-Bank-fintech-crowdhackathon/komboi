(ns nbg-cbank.core
  (:require [com.stuartsierra.component :as mc])
  (:require [compojure.core :as cc]
            [compojure.route :as cr])
  (:require [org.httpkit.server :as wsrv])
  (:require [postal.core :as pc])
  (:require [honeysql.core :as hc]
            [honeysql.format :as hf]
            [honeysql.helpers :as hh])
  (:require [clojure.java.jdbc :as j])
  (:require [buddy.sign.jws :as sjws])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.core.async :as asnc])
  (:use [ring.middleware.json :only [wrap-json-response wrap-json-body]])
  (:import [com.zaxxer.hikari HikariConfig
            HikariDataSource])
  (:require [nbg-cbank.mock :as mock])
  (:gen-class))

;; (pc/send-message {:host "localhost"}
;;                  {:from "nvlass@megacorp1.eu"
;;                   :to "giorgospapa86@gmail.com"
;;                   :subject "Testing Clojure email subsystems #2"
;;                   :body "Should you receive this email, then all systems are nominal"})

(def demo-user-id #uuid "f3872015-ee21-4c33-9bc2-e1ac80073665")

(declare system)

(defn make-hikari-datasource [props]
  (let [hc (HikariConfig. (:hikari-props props))]
    {:datasource (HikariDataSource.  hc)}))

(defn stop-hikari-datasource [ds]
  (.close {:datasource ds}))

(defn cors-stuff [h]
  (fn [req]
    (let [resp (h req)]
      (assoc-in resp [:headers "Access-Control-Allow-Origin"] "*"))))

(defn wrap-db [h sys]
  (fn [req]
    (let [nreq (assoc req :db (:db @sys))]
      (h nreq))))


(defn- put-cookie [resp user-id]
  (assoc-in resp [:headers "set-cookie"]
            (sjws/sign {"user_id" user-id} "superduper")))
  
(defn wrap-ck
  "wrap with a fixed jwt (already logged in as demo-user-id)"
  [h]
  (fn [req]
    (let [tt (get-in req [:headers "cookie"])]
      (if (nil? tt)
        (let [resp (h req)]
          (put-cookie resp demo-user-id))
        (try
          (h (assoc req :user-id
                    (java.util.UUID/fromString
                     (get (sjws/unsign tt "superduper") :user_id))))
          (catch Exception ex
            ;; robust cookie login
            (log/info (str ex))
            (log/info "resetting cookie")
            (put-cookie (h (assoc req :user-id demo-user-id)) demo-user-id)))))))

(defn has-upvote [db user-id project-id]
  (> 
   (count (j/with-db-connection [c (:connection db)]
            (j/query
             c
             (-> (hh/select :*)
                 (hh/from :investor_greenlight)
                 (hh/where [:and [:= :user_id user-id]
                            [:= :project_id project-id]])
                 (hc/format)))))
   0))

(defn del-vote [db user-id project-id]
  (j/with-db-connection [c (:connection db)]
    (j/execute!
     c
     (-> (hh/delete-from :investor_greenlight)
         (hh/where [:and [:= :user_id user-id]
                    [:= :project_id project-id]])
         (hc/format)))))

(defn add-vote [db user-id project-id]
  (j/with-db-connection [c (:connection db)]
    (j/execute!
     c
     (-> (hh/insert-into :investor_greenlight)
         (hh/values [{:user_id user-id :project_id project-id}])
         (hc/format)))))

(defn swap-vote [db user-id project-id]
  (if (has-upvote db user-id project-id)
    (do (del-vote db user-id project-id) false)
    (do (add-vote db user-id project-id) true)))


(defn view-projects [db user-id prop]
  (j/with-db-connection [c (:connection db)]
    (j/query
     c
     ;; this should inner join in credibility
     (-> (hh/select :p.project_id :p.project_name
                    :p.required_amount :p.current_amount
                    :p.estimated_risk_factor 
                    :u.user_id :u.name
                    :c.required_credibility :c.current_credibility)
         (hh/from [:projects :p] [:investors :u] [:project_investor_credibility :c])
         (hh/where [:and [:= :p.creating_user_id :u.user_id]
                    [:= :p.state prop]
                    [:= :c.project_id :p.project_id]])
         (hc/format)))))

;; FIXME: merge with above form and build select on the spot
(defn view-project-details [db user-id project-id]
  (first
   (j/with-db-connection [c (:connection db)]
    (j/query
     c
     ;; this should inner join in credibility
     (-> (hh/select :p.project_id :p.project_name
                    :p.required_amount :p.current_amount
                    :p.estimated_risk_factor 
                    :u.user_id :u.name
                    :c.required_credibility :c.current_credibility)
         (hh/from [:projects :p] [:investors :u] [:project_investor_credibility :c])
         (hh/where [:and [:= :p.creating_user_id :u.user_id]
                    [:= :c.project_id :p.project_id]
                    [:= :p.project_id project-id]])
         (hc/format))))))

(defn view-project-details-upvotes [db user-id project-id]
  (assoc (view-project-details db user-id project-id)
         :upvote (has-upvote db user-id project-id)))

(defn view-portfolio [db user-id]
  (j/with-db-transaction [c (:connection db)]
    (j/query
     c
     (-> (hh/select :ip.amount :p.*)
         (hh/from [:investor_portfolio :ip]
                  [:projects :p])
         (hh/where [:and [:= :ip.user_id user-id]
                    [:= :ip.project_id :p.project_id]])
         (hc/format)))))

(defn view-triumphs [db]
  (j/with-db-connection [c (:connection db)]
    (j/query
     c
     (-> (hh/select :*)
         (hh/from :triumphs)
         (hh/where [:= :user_id demo-user-id])
         (hh/order-by [:date_unlocked :desc])
         (hc/format)))))

;; this should be a sinngle query
(defn view-projects-with-upvotes [db user-id prop]
  (doall
   (map (fn [p] (assoc p :upvote (has-upvote db user-id (:project_id p))))
        (view-projects db user-id prop))))

(cc/defroutes cbank-routes
  (cc/GET "/" [] {:status 200
                  :body "the empty route"})

  ;; (cc/POST "/login" {body :body})
  ;; (cc/POST "/logout" [])
  ;; (cc/GET "/projects" [] {:status 200 :body {:a 1 :b 2 :c 3}})

  (cc/GET "/projects/approved" {dbreq :db user-id :user-id}
    (log/info dbreq)
    {:status 200
     :body (view-projects dbreq user-id "approved")})

  (cc/GET "/projects/greenlight" {dbreq :db user-id :user-id}
    {:status 200
     :body (view-projects-with-upvotes dbreq user-id "pending_greenlight")})

  (cc/GET "/projects/:project-id" {dbreq :db user-id :user-id
                                   {:keys [project-id]} :params}
    {:status 200
     :body (view-project-details-upvotes
            dbreq user-id (java.util.UUID/fromString project-id))})
  (cc/GET "/portfolio" {dbreq :db user-id :user-id}
    {:status 200
     :body (view-portfolio dbreq user-id)})

  (cc/GET "/triumphs" {dbreq :db user-id :user-id} ;;/:user-id/triumphs
    {:status 200
     :body (view-triumphs dbreq)})

  (cc/POST "/projects/:project-id/swapvote" {dbreq :db user-id :user-id
                                            {:keys [project-id]} :params}
    {:status 200
     :body {:result
            (swap-vote dbreq user-id (java.util.UUID/fromString project-id))}}))
    
    ;;(log/info (str "project-id " project-id))))


(def wroutes
  (-> cbank-routes
      wrap-json-body
      wrap-json-response
      cors-stuff
      wrap-ck
      (wrap-db #'system)
      ))

(defn start-webserver [prop-map]
  (wsrv/run-server #'wroutes
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
      (.close (:datasource connection))
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


