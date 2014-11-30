(ns mulligan.core
  (:require [clojure.math.numeric-tower :as math])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def default-opts {:tries            3
                   :retry-on-nil     false
                   :retry-on-false   false
                   :on-success       nil
                   :on-failure       nil
                   :on-final-failure nil
                   :wait             0
                   :backoff          nil})

(defn- parse-args [args]
  (let [possible-opts (->> default-opts keys (into #{}))]
    (loop [args args
           opts default-opts]
      (if (possible-opts (first args))
        (recur (rest (rest args))
               (assoc opts
                      (first args)
                      (second args)))
        {:opts opts
         :body args}))))

(defrecord Success         [result])
(defrecord NilResult       [])
(defrecord FalseResult     [])

(defmacro try-body [opts body]
  (let [tries        (:tries opts)
        rty-on-nil   (:retry-on-nil opts)
        rty-on-false (:retry-on-false opts)]
    `(try+
       (let [result# (do ~@body)]
         (cond
           (and ~rty-on-nil (= result# nil))
           (throw+ (NilResult.))

           (and ~rty-on-false (not result#))
           (throw+ (FalseResult.))

           :else (Success. result#)))
       (catch Object e# e#))))

(defn- calculate-wait [attempts opts]
  (let [wait    (:wait opts)
        backoff (:backoff opts)]
    (cond
      (= backoff :linear) (* wait (+ attempts 1))
      (= backoff :double) (* wait (math/expt 2 attempts))
      :default wait)))

(defn handle-failure [opts attempts failure]
  (let [on-failure (:on-failure opts)]
    (when (not (= nil on-failure))
      (on-failure failure))
    (Thread/sleep (calculate-wait attempts opts))))

(defn handle-final-failure [opts failure]
  (let [on-failure (:on-final-failure opts)]
    (when (not (= nil on-failure))
      (on-failure failure)))
  (throw+ failure))

(defn handle-success [opts success]
  (let [result     (:result success)
        on-success (:on-success opts)]
    (when (not (= nil on-success))
      (on-success result))
    result))

(defmacro retry
  "Retry a block of code several times"
  [& args]
  (let [parsed-args  (parse-args args)
        opts         (:opts parsed-args)
        tries        (:tries opts)
        body         (:body parsed-args)]
    `(loop [tries#    (dec ~tries)
            attempts# 0]
       (let [result# (try-body ~opts ~body)]
         (if (= (type result#) Success)
           (handle-success ~opts result#)
           (if (zero? tries#)
             (handle-final-failure ~opts result#)
             (do (handle-failure ~opts attempts# result#)
                 (recur (dec tries#) (inc attempts#)))))))))
