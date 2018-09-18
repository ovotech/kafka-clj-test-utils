(ns kafka-clj-utils.schema-registry
  (:require [clojure.test :refer :all]
            [kafka-avro-confluent.schema-registry-client :as schema-registry]
            [integrant.core :as ig]))


(defmethod ig/init-key ::client
  [_ opts]
  (schema-registry/->schema-registry-client opts))