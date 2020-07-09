(defproject ovotech/kafka-clj-test-utils "2.1.0-2"
  :description "Companion test utility library for `ovotech/kafka-clj-utils`"
  :url "https://github.com/ovotech/kafka-clj-test-utils"
  :license {:name "Eclipse Public License"
            :url  "https://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ovotech/kafka-avro-confluent "2.4.1-1"]]

  :profiles {:dev {:resource-paths ["dev/resources" "test/resources"]
                   :dependencies [[vise890/zookareg "2.4.1-1"]
                                  [ch.qos.logback/logback-classic "1.2.3"]
                                  [ch.qos.logback/logback-core "1.2.3"]]}
             :ci  {:deploy-repositories
                   [["clojars" {:url           "https://clojars.org/repo"
                                :username      :env ;; LEIN_USERNAME
                                :password      :env ;; LEIN_PASSWORD
                                :sign-releases false}]]}})
