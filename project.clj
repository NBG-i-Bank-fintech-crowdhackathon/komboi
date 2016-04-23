(defproject nbg_cbank "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.draines/postal "1.11.3"]
                 [honeysql "0.6.3"]
                 [org.clojure/java.jdbc "0.5.8"]
                 [com.stuartsierra/component "0.3.1"]
                 ;;[mount "0.1.10"]
                 [compojure "1.5.0"]
                 [http-kit "2.1.18"]
                 [ring/ring-json "0.4.0"]
                 [org.clojure/data.json "0.2.6"]
                 ;;
                 [buddy/buddy-core "0.12.0"]
                 [buddy/buddy-auth "0.12.0"]
                 [buddy/buddy-hashers "0.12.0"]
                 [buddy/buddy-sign "0.12.0"]
                 [com.zaxxer/HikariCP "2.4.5"]
                 ;;
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;;[org.apache.logging.log4j/log4j-core "2.5"]]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [org.clojure/core.async "0.2.374"]]

  :main ^:skip-aot nbg-cbank.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
