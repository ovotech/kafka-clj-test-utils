(defproject kafka-clj-test-utils "0.1.0-SNAPSHOT"
  :description "FIXME: write description"

  :dependencies [[org.apache.kafka/kafka-clients "1.0.2" :exclusions [org.scala-lang/scala-library]]
                 [org.apache.kafka/kafka-streams "1.0.2"]
                 [org.clojure/clojure "1.9.0"]
                 [ovotech/kafka-avro-confluent "0.8.5"]
                 [vise890/zookareg "1.0.2-1"]]

  :profiles {:dev {:dependencies [[vise890/multistub "0.1.1"] ]}} )
