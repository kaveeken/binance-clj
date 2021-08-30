(ns binance-clj.core
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))


; (def key-pas
;   "Vector containing API key and passphrase."
;   [(slurp "resources/key") (slurp "resources/pas")])

(load "crypto")
(load "pricing")
(load "klines")
(load "account")
(load "market_info")

;(get-price "BTCEUR")
