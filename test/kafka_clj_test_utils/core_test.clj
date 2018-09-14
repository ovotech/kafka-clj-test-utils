(ns kafka-clj-test-utils.core-test
  (:require [clojure.test :refer :all]
            #_[kafka-clj-test-utils.core :refer :all]
            [vise890.multistub.core :as ms]
            [kafka-clj-test-utils.core :as co]
            [zookareg.core :as zkr]))


(use-fixtures :once (partial ms/with-around-fns [zkr/with-zookareg-fn
                                                 (partial co/with-topic topic-a)
                                                 (partial co/with-topic topic-b)]))

(deftest not-implemented


  ;; send something to the topic using produce


  ;; seek-to-end
  ;; seek-to-beginning (but this is not used
  ;; subscribe/unsubscribe

  ;; consume it using
  ;; - with-consumer
  ;; - consume
  ;; - with-1-partition-consumer-from-end



  (is false))
