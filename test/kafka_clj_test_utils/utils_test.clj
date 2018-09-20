(ns kafka-clj-test-utils.utils-test
  (:require [clojure.test :refer :all]
            [kafka-clj-test-utils.core :as co]
            [kafka-clj-test-utils.consumer :as ktc]
            [kafka-clj-test-utils.producer :as ktp]
            [zookareg.core :as zkr]
            [integrant.core :as ig]))


;; Taken from https://gitlab.com/vise890/multistub with permission
(defn with-around-fns
  "Executes `f` within the context of `around-fns`. `around-fns` is a (sorted!)
  sequence of functions that take a no arg function.

  E.g.:

  (with-around-fns [with-zookeeper-fn
                    with-kafka-fn]
                   produce-some-stuff-on-kafka-fn)"
  [around-fns f]
  (cond
    (empty? around-fns) (f)
    (= 1 (count around-fns)) ((first around-fns) f)
    :else (with-around-fns (butlast around-fns)
                           (fn [] ((last around-fns) f)))))

(defn with-ig-sys
  [ig-config f]
  (let [_       (ig/load-namespaces ig-config)
        system  (ig/init ig-config)]
    (try
      (f)
      (finally (ig/halt! system)))))

(def topic-a "test.topic.a")
(def topic-b "test.topic.b")
(def ig-config {:kafka-clj-utils.schema-registry/client {:base-url "http://localhost:8081"}})
(def config (assoc-in ig-config
                      [:kafka-config :bootstrap.servers] "127.0.0.1:9092"))


(use-fixtures :each (partial with-around-fns [zkr/with-zookareg-fn
                                                 (partial co/with-topic topic-a)
                                                 (partial co/with-topic topic-b)
                                                 (partial with-ig-sys ig-config)]))



(def test-schema
  {:namespace "kafkaCljTestUtils"
   :type      "record"
   :name      "TestRecord"
   :fields    [#_{:name "metadata"
                :type {:type   "record"
                       :name   "metaV1"
                       :fields [{:name "eventId" :type "string"}
                                {:name "createdAt" :type "long" :logicalType "timestamp-millis"}
                                {:name "traceToken" :type "string"}]}}
               {:name "foo" :type "string"}
               {:name "bar" :type "string"}]})

(def event1 {:foo "FOO" :bar "BAR"})
(def event2 {:foo "BAZ" :bar "QUX"})

(deftest produce-consume-test

  (ktp/produce config topic-a test-schema event1)

  (ktc/with-consumer config
    (fn [consumer]
      (ktc/assign-partition-0 consumer topic-a)
      (ktc/seek-to-end consumer)

      (let [msg (ktc/poll* consumer)]
        (is (empty? msg)))

      (ktp/produce config topic-a test-schema event2)

      (let [[msg :as msgs] (ktc/poll* consumer)]
        (is (= 1 (count msgs)))
        (is (= event2 msg)))

      (ktc/seek-to-beginning consumer)

      (let [[msg1 msg2 :as msgs] (ktc/poll* consumer)]
        (is (= 2 (count msgs)))
        (is (= event1 msg1))
        (is (= event2 msg2)))))

  (let [[msg1 msg2 :as msgs] (ktc/consume config topic-a)]
    (is (= 2 (count msgs)))
    (is (= event1 msg1))
    (is (= event2 msg2))))

(deftest subscribe-unsubscribe-test

  (ktc/with-consumer config
    (fn [consumer]

      (ktc/subscribe consumer topic-a topic-b)
      (ktc/seek-to-end consumer)

      (ktp/produce config topic-a test-schema event1)
      (ktp/produce config topic-b test-schema event1)

      (let [[msg1 msg2 :as msgs] (ktc/poll* consumer :expected-msgs 2)]
        (is (= 2 (count msgs)))
        (is (= event1 msg1))
        (is (= event1 msg2)))

      (ktc/unsubscribe consumer)

      (ktp/produce config topic-a test-schema event2)

      (ktc/subscribe consumer topic-a)
      (ktc/seek-to-beginning consumer)

      (let [[msg1 msg2 :as msgs] (ktc/poll* consumer :expected-msgs 2)]
        (is (= 2 (count msgs)))
        (is (= event1 msg1))
        (is (= event2 msg2))))))