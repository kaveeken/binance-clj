(ns bbb.core
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))


(def key-pas [(slurp "resources/key") (slurp "resources/pas")])
(load "crypto")
(load "pricing")
(load "klines")
(load "account")
(get-price "BTCUSDT")

(def product-data
  "vector of maps with information about trading pairs.
  Map keys:
  `:s`: pair string
  `:q`: quote asset
  `:cs`: number in circulation(? maybe a proxy)
  `:c`: asset price
  "
  (:data
   (send-unsigned-request
    client/get
    "https://www.binance.com/exchange-api/v2/public/asset-service/product/get-products")))

(def usdt-product-data
  (filterv
   (fn [data-map]
     (and
      (= "USDT" (:q data-map))
      (some #(= "mining-zone" %1) (:tags data-map))))
   product-data))

(def usdt-pd-plus-cap
  (mapv #(if (:cs %1)
           (assoc %1 :cap (* (:cs %1) (read-string (:c %1))))
           (assoc %1 :cap 0))
       usdt-product-data))
(def cap-sorted-product-data
  (reverse (sort-by :cap usdt-pd-plus-cap)))

(def top 30)
(def selection (take top cap-sorted-product-data))
(def cap-sum (apply + (map #(:cap %1) selection)))
(def selection-plus-mfrac
  (mapv #(assoc %1 :mfrac (/ (:cap %1) cap-sum))
        selection))
