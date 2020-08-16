(ns jackdaw.client
  (:require ["sinek" :refer (NConsumer NProducer)]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]] ))

(def topic "foo")

(def partition-count 1)

(def conf
  (clj->js
   {"noptions"
    {"metadata.broker.list" "localhost:9092"
     "group.id" "goop7"
     "client.id" "goop7"
     "enable.auto.commit" false
     "socket.keepalive.enable" true
     "api.version.request" true
     "socket.blocking.max.ms" 5}
    "tconf"
    {"auto.offset.reset" "earliest"}}))

(def prod-conf
  (clj->js
   {"noptions"
    {"metadata.broker.list" "localhost:9092"
     "client.id" "example-client"
     "compression.codec" "none"
     "socket.keepalive.enable" true
     "batch.num.messages" 500}
    "tconf"
    {"request.required.acks" 1}}))

(comment
  (go
    (let [consumer (NConsumer. topic conf)]
      (.on consumer "error" (fn [e] (prn "consumer error:" e)))
      (try
        (<p! (.connect consumer))
        (.consume consumer
                  (go
                    (fn [messages ]
                      (doseq [m messages]
                        (prn m))
                      )))
        (catch js/Error err (prn (ex-cause err))))
      (prn "here")
      (.close consumer)
      ))

  (def consu (NConsumer. topic conf))

  (-> (js/Promise.all [(.connect consu)])
      (.then (fn [foo]
               (.consume consu
                         (fn [messages]
                           (doseq [m messages]
                             (prn m))))))
      (.catch #(js/console.log %))
      (.finally #(js/console.log "cleanup")))

  (.close consu)

  (go
    (let [producer (NProducer. prod-conf nil partition-count)]
      (.on producer "error" (fn [error] (prn "producer error:" error)))
      (try
        (<p! (.connect producer))
        (<p! (.send producer topic "bar" 0 "my-key" "partkey"))
        (catch js/Error err (prn (ex-cause err))))
      (.close producer)
      )
    )
  

  )
