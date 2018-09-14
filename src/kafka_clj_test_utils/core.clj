(ns kafka-clj-test-utils.core
  (:require [clojure.walk :refer [stringify-keys]]
            [kafka-avro-confluent.schema-registry-client :as schema-registry])
  (:import [java.util Properties]
           [kafka.admin AdminUtils]
           [kafka.utils ZKStringSerializer$ ZkUtils]
           [org.I0Itec.zkclient ZkClient ZkConnection]))


(defn ensure-topic
  [topic]
  (with-open [zk (ZkClient. "localhost:2181" 1000 1000 (ZKStringSerializer$/MODULE$))]
    (let [zc (ZkConnection. "localhost:2181")
          zu (ZkUtils. zk zc false)]
      (AdminUtils/createTopic zu topic 1 1
                              (Properties.)
                              (kafka.admin.RackAwareMode$Enforced$.)))))

(defn with-topic
  [topic f]
  (ensure-topic topic)
  (f))


(defn normalize-config
  "Normalise configuration map into a format required by kafka."
  [config]
  (->> config
       stringify-keys
       (map (fn [[k v]] [k (if (integer? v) (int v) v)]))
       (into {})))

(defn ->schema-registry
  [config]
  {:post [some?]}
  (-> config
      schema-registry/->schema-registry-client))