(ns kafka-clj-test-utils.producer
  (:require [kafka-avro-confluent.serializers :refer [->avro-serializer]]
            [kafka-clj-test-utils.core :as co])
  (:import [java.util UUID]
           [org.apache.kafka.clients.producer ProducerRecord KafkaProducer]
           [org.apache.kafka.common.serialization StringSerializer]))


(defn with-producer
  [kafka-config kafka-schema-registry-config schema f]
  (let [pc (co/normalize-config (merge kafka-config
                                    {:retries 3,
                                     :acks "all",
                                     :compression.type "gzip",
                                     :max.request.size 5242880,
                                     :request.timeout.ms 10000,
                                     :max.block.ms 10000}))
        key-serializer (StringSerializer.)
        schema-registry (co/->schema-registry kafka-schema-registry-config)
        value-serializer (->avro-serializer schema-registry schema)
        k-producer (KafkaProducer. pc key-serializer value-serializer)]
    (try (f k-producer) (finally (.close k-producer)))))


(defn produce
  ([kafka-config kafka-schema-registry-config topic schema val]
   (with-producer
     kafka-config
     kafka-schema-registry-config
     schema
     (fn [p]
       (deref (.send p (ProducerRecord. topic (str (UUID/randomUUID)) val)))
       (.flush p))))
  ([config topic schema val]
   (let [kafka-config                 (:kafka/config config)
         kafka-schema-registry-config (:kafka-clj-utils.schema-registry/client config)]
     (produce kafka-config kafka-schema-registry-config topic schema val))))
