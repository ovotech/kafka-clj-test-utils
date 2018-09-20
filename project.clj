(defproject ovotech/kafka-clj-test-utils "0.1.0-1"
  :description "Companion test utility library for `ovotech/kafka-clj-utils`"
  :url "https://github.com/ovotech/kafka-clj-test-utils"
  :license {:name "Eclipse Public License"
            :url  "https://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.apache.kafka/kafka-clients "1.0.2" :exclusions [org.scala-lang/scala-library]]
                 [org.apache.kafka/kafka-streams "1.0.2"]
                 [org.clojure/clojure "1.9.0"]
                 [ovotech/kafka-avro-confluent "0.8.5"]
                 [vise890/zookareg "1.0.2-1"]]

  :profiles {:ci  {:deploy-repositories
                   [["clojars" {:url           "https://clojars.org/repo"
                                :username      :env ;; LEIN_USERNAME
                                :password      :env ;; LEIN_PASSWORD
                                :sign-releases false}]]}})
