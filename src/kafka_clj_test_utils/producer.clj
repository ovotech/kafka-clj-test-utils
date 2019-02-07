(ns kafka-clj-test-utils.producer
  (:require [kafka-avro-confluent.v2.serializer :as ser]
            [kafka-clj-test-utils.core :as co])
  (:import java.util.UUID
           [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           org.apache.kafka.common.serialization.StringSerializer))

(defn with-producer
  ([kafka-config kafka-serde-config f]
   (let [pc               (co/normalize-config (merge kafka-config
                                                      {:retries            3,
                                                       :acks               "all",
                                                       :compression.type   "gzip",
                                                       :max.request.size   5242880,
                                                       :request.timeout.ms 10000,
                                                       :max.block.ms       10000}))
         key-serializer   (StringSerializer.)
         value-serializer (ser/->avro-serializer kafka-serde-config)
         k-producer       (KafkaProducer. pc key-serializer value-serializer)]
     (try (f k-producer) (finally (.close k-producer)))))
  ([config f]
   (with-producer (:kafka/config config) (:kafka.serde/config config) f)))

(defn with-producer*
  [kafka-config value-serializer f]
  (let [pc               (co/normalize-config (merge kafka-config
                                                     {:retries            3,
                                                      :acks               "all",
                                                      :compression.type   "gzip",
                                                      :max.request.size   5242880,
                                                      :request.timeout.ms 10000,
                                                      :max.block.ms       10000}))
        key-serializer   (StringSerializer.)
        k-producer       (KafkaProducer. pc key-serializer value-serializer)]
    (try (f k-producer) (finally (.close k-producer)))))

(defn produce
  ([kafka-config kafka-serde-config topic schema val]
   (with-producer
     kafka-config
     kafka-serde-config
     (fn [p]
       (deref (.send p (ProducerRecord. topic (str (UUID/randomUUID)) {:value val
                                                                       :schema schema})))
       (.flush p))))
  ([config topic schema val]
   (produce (:kafka/config config)
            (:kafka.serde/config config)
            topic
            schema
            val)))

(defn produce*
  [kafka-config kafka-serializer topic schema val]
  (with-producer*
    kafka-config
    kafka-serializer
    (fn [p]
      (deref (.send p (ProducerRecord. topic (str (UUID/randomUUID)) {:value val
                                                                      :schema schema})))
      (.flush p))))
