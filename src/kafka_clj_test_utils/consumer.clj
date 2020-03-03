(ns kafka-clj-test-utils.consumer
  (:require [kafka-avro-confluent.v2.deserializer :as des]
            [kafka-clj-test-utils.core :as co])
  (:import java.util.UUID
           org.apache.kafka.clients.consumer.KafkaConsumer
           [org.apache.kafka.common PartitionInfo TopicPartition]
           org.apache.kafka.common.serialization.StringDeserializer))

(defn- PartitionInfo->TopicPartition
  [^PartitionInfo pi]
  (TopicPartition. (.topic pi) (.partition pi)))
(defn- subscribed-PartitionInfos
  [^KafkaConsumer k-consumer]
  (->> k-consumer
       .subscription
       (mapcat #(.partitionsFor k-consumer %))))
(defn- subscribed-TopicPartitions
  [^KafkaConsumer k-consumer]
  (->> k-consumer
       subscribed-PartitionInfos
       (map PartitionInfo->TopicPartition)))

(defn assign-partitions
  [^KafkaConsumer consumer topic-partitions]
  (->> topic-partitions
       (map #(TopicPartition. (:topic %) (:partition %)))
       (.assign consumer)))
(defn assign-partition-0
  [^KafkaConsumer consumer topic]
  (.assign consumer [(TopicPartition. topic 0)]))

(defn subscribe [consumer & topics] (.subscribe consumer topics))
(defn unsubscribe [consumer] (.unsubscribe consumer))

(defn- ConsumerRecord->m
  [cr]
  (-> cr
      .value
      (with-meta (merge {:kafka/offset    (.offset cr),
                         :kafka/partition (.partition cr),
                         :kafka/topic     (.topic cr),
                         :kafka/timestamp (.timestamp cr),
                         :kafka/key       (.key cr)}
                        (meta (.value cr))))))
(defn committed-info
  [consumer]
  (->> (subscribed-TopicPartitions consumer)
       (map (fn [tp]
              {:topic (.topic tp),
               :partition (.partition tp),
               :offset (.offset (.committed consumer tp))}))))

(defn seek-to-end
  [consumer]
  (.poll consumer 0)
  (.seekToEnd consumer [])
  ;; NOTE we poll to force the seek, assuming it's ok to discard any messages on
  ;; the topic when this is called
  (.poll consumer 0))
(defn seek-to-beginning
  [consumer]
  (.poll consumer 0)
  ;; NOTE we don't poll here, as we don't wanna discard the messages
  (.seekToBeginning consumer []))

(defn poll*
  [consumer &
   {:keys [expected-msgs retries poll-timeout backoff?],
    :or {expected-msgs 1, retries 200, poll-timeout 25, backoff? false}}]
  (loop [received []
         retries retries
         timeout poll-timeout]
    (if (or (>= (count received) expected-msgs) (zero? retries))
      received
      (recur (concat received
                     (map ConsumerRecord->m (.poll consumer poll-timeout)))
             (dec retries)
             (* timeout (backoff? 2 1))))))

(defn with-consumer
  ([kafka-config kafka-serde-config f]
   (let [cc                 (co/normalize-config (merge kafka-config
                                                        {:group.id (str "kafka-clj-test-utils-"
                                                                        (UUID/randomUUID))}))
         key-deserializer   (StringDeserializer.)
         value-deserializer (des/->avro-deserializer kafka-serde-config)
         k-consumer         (KafkaConsumer. cc key-deserializer value-deserializer)]
     (try (f k-consumer) (finally (.close k-consumer)))))
  ([config f]
   (with-consumer (:kafka/config config) (:kafka.serde/config config) f)))

(defn with-consumer*
  [kafka-config value-deserializer f]
  (let [cc                 (co/normalize-config (merge kafka-config
                                                       {:group.id (str "kafka-clj-test-utils-"
                                                                       (UUID/randomUUID))}))
        key-deserializer   (StringDeserializer.)
        k-consumer         (KafkaConsumer. cc key-deserializer value-deserializer)]
    (try (f k-consumer) (finally (.close k-consumer)))))

(defn consume
  ([config topic & args]
   (with-consumer config
     (fn [consumer]
       (assign-partition-0 consumer topic)
       (seek-to-beginning consumer)
       (apply poll* consumer args)))))

(defn consume*
  ([config value-deserializer topic & args]
   (with-consumer* config value-deserializer
     (fn [consumer]
       (assign-partition-0 consumer topic)
       (seek-to-beginning consumer)
       (apply poll* consumer args)))))

(defn with-1-partition-consumer-from-end
  [kafka-config kafka-serde-config topic f]
  (with-consumer kafka-config kafka-serde-config
    (fn [kc]
      (assign-partition-0 kc topic)
      (seek-to-end kc)
      ;; NOTE seek is lazy, so we force a poll here
      ;; **YOU SHOULD CALL THIS BEFORE MESSAGES ARE PRODUCED, OR
      ;; THIS POLL
      ;; WILL CONSUME THEM**
      (.poll kc 1)
      (f kc))))
