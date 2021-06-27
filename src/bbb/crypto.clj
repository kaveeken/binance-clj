(in-ns 'bbb.core)
; (ns bbb.core
;   (:require [clj-http.client :as client]
;             [clojure.string :as str]
;             [clojure.data.json :as json])
;   (:import (javax.crypto Mac)
;            (javax.crypto.spec SecretKeySpec)))


(defn secretKeyInst [pas mac]
  (SecretKeySpec. (.getBytes pas) (.getAlgorithm mac)))

(defn sign [pas string]
  (let [mac (Mac/getInstance "HmacSHA256")
        secretKey (secretKeyInst pas mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes string)))
        .doFinal)))

(defn signature-to-string [bytes]
  (apply str (map #(format "%02x" %) bytes)))

(defn build-apikey-header [key]
  {"X-MBX-APIKEY" key})

(defn build-signature [pas params]
  (str "&signature=" (signature-to-string (sign pas params))))

(defn test-apikey-header []
  (let [apikey-header (build-apikey-header "this is an API key")]
    (= apikey-header {"X-MBX-APIKEY" "this is an API key"})))

(defn test-signature []
  (let [pas "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j" 
        params "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559"
        reference-sig "&signature=c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71"
        sig (build-signature pas params)]
    (do nil;(print (str sig "\n" reference-sig "\n"))
      (= reference-sig sig))))

(defn send-request
  "Send an api request. `type` is a function like `client/get`,
  `url` is the request url and `key` is the api key.
  returns a map of the reply JSON."
  [type url key]
  (let [header (build-apikey-header key)]
    (json/read-str
     (:body
      (type url {:headers header}))
     :key-fn keyword)))

(defn send-unsigned-request
  [type url]
  (json/read-str
   (:body
    (type url))
   :key-fn keyword))
