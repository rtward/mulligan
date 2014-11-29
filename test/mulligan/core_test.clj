(ns mulligan.core-test
  (:require [clojure.test :refer :all]
            [mulligan.core :refer :all]))

(deftest retries
  (testing "Default is to retry three times"
    (def retry-count 0)
    (is (thrown? Exception
                 (retry (def retry-count (inc retry-count))
                        (throw (Exception.))))
        "Exception was eventually thrown")
    (is (= retry-count 3)
        "Code was retried three times"))

  (testing "Only retries the given number of times"
    (def retry-count 0)
    (is (thrown? Exception
                 (retry :tries 2
                        (def retry-count (inc retry-count))
                        (throw (Exception.))))
        "Exception was eventually thrown")
    (is (= retry-count 2)
        "Code was retried two times")

    (def retry-count 0)
    (is (thrown? Exception
                 (retry :tries 5
                        (def retry-count (inc retry-count))
                        (throw (Exception.))))
        "Exception was eventually thrown")
    (is (= retry-count 5)
        "Code was retried five times"))

  (testing "Only runs once when successful"
    (def retry-count 0)
    (is (= (retry (def retry-count (inc retry-count)):a) :a)
        "Returns correct value when successful")
    (is (= retry-count 1)
        "Code was only run once"))

  (testing "Runs until successful"
    (def retry-count 0)
    (is (= :a
           (retry (def retry-count (inc retry-count))
                  (if (= retry-count 2)
                     :a
                     (throw (Exception.)))))
        "Returns correct value when successful")
    (is (= retry-count 2)
        "Code was only run once")))

(deftest retry-on-nil
  (testing "No failure on nil return by default"
    (is (= nil (retry nil))
        "Nil is returned from retry"))

  (testing "Failure on nil when option is given true"
    (is (thrown? Exception (retry :retry-on-nil true nil))
        "An exception is eventually thrown"))

  (testing "No failure on nil when option is given false"
    (is (= nil (retry :retry-on-nil false nil))
        "Nil is returned from retry")))

(deftest retry-on-false)
  (testing "No failure on false return by default"
    (is (= false (retry false))
        "False is returned from retry"))

  (testing "Failure on false when option is given true"
    (is (thrown? Exception (retry :retry-on-false true false))
        "An exception is eventually thrown"))

  (testing "No failure on false when option is given false"
    (is (= false (retry :retry-on-false false false))
        "False is returned from retry"))

(deftest on-success
  (testing "Success callback is called with result"
    (def result nil)
    (is (= :a (retry :on-success #(def result %) :a))
        "Correct result is returned from retry block")
    (is (= :a result)
        "Correct result is passed to the on-sucess function")))

(deftest on-failure
  (testing "Failure callback is called with the exception thrown"
    (def failures [])
    (is (thrown? Exception
                 (retry :on-failure
                        (fn [failure]
                          (def failures (cons failure failures)))
                        (throw (Exception.))))
        "Exception is eventually thrown")
    (is (= 2 (count failures))
        "We collected the correct number of failures")
    (is (instance? Exception (first failures))
        "The failures are the expected type")
    (is (instance? Exception (second failures))
        "The failures are the expected type")))

(deftest on-final-failure
  (testing "Final failure callback is called with the exception thrown"
    (def failure nil)
    (is (thrown? Exception
                 (retry :on-final-failure
                        #(def failure %)
                        (throw (Exception.))))
        "Exception is eventually thrown")
    (is (instance? Exception failure)
        "The final failure callback was called with the last exception")))
