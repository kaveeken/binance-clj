(ns binance-clj.core
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.json :as json])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))


(def key-pas
  "Vector containing API key and passphrase."
  [(slurp "resources/key") (slurp "resources/pas")])

(load "crypto")
(load "pricing")
(load "klines")
(load "account")
(load "market_info")
(get-price "BTCEUR")


(def top 30)
(def selection (take top cap-sorted-product-data))
(def cap-sum (reduce + (map #(:cap %1) selection)))
(def selection-plus-mfrac
  (mapv #(assoc %1 :mfrac (/ (:cap %1) cap-sum))
        selection))

(defn normalize
  [vec]
  (let [total (apply + vec)]
    (mapv #(/ %1 total) vec)))
(defn normalize-root
  [vec root]
  (normalize (mapv #(Math/pow %1 (/ 1 root)) vec)))

(def lots-of-empty-price-data
  (map #(gen-empty-price-data %1 "1d") (map :s selection)))

(def lots-completed-price-data
  (map complete-price-data lots-of-empty-price-data))

(map spit-closes lots-completed-price-data)


;;; this should go to other project entirely

(defn root-predict-target
  [position price-old price-new root]
  (* position (Math/pow (/ price-new price-old) (/ 1 root))))
(predict-target 0.15 10 11 2)

(defn generate-spread
  "Generates a spread of prices around a given `price` and returns a
  vector containing maps with for each of the spreads `:price`s a
  corresponding positional `:target`.
  Prices are generated from a starting (current) `price`, with a total
  spread of `depth` times 2 pricepoints above and below `price`, which
  are `grain` apart from eachother. `:target`s are predicted by
  mapping `predictor` over the price spread. `:price`s and `:target`s
  are then paired into maps and put into a vector."
  [price depth grain predictor]
  (let [up-range (map #(* %1 grain)
                      (rest (range (inc depth))))
        down-range (map #(* -1 grain %1)
                        (rest (range (inc depth))))
        prices (sort (map #(+ %1 price)
                          (reduce conj up-range down-range)))
        targets (map predictor prices)]
    (loop [prices prices
           targets targets
           result []]
      (if (empty? prices)
        result
        (recur (rest prices) (rest targets)
               (conj result {:price (first prices)
                             :target (first targets)}))))))

(defn add-size-to-delta-including-spread
  [base-spread capital]
  (loop [new-spread []
         spread base-spread]
    (if (empty? spread)
      new-spread
      (recur (conj new-spread
                   (assoc (first spread)
                          :size (* (:delta (first spread)) capital)))
             (rest spread)))))
    
  
(defn add-delta-to-spread
  ([position base-spread]
   (loop [new-spread []
          spread base-spread]
     (if (empty? spread)
       new-spread
       (recur (conj
               new-spread
               (assoc (first spread)
                      :delta (- (:target (first spread)) position)))
              (rest spread)))))
  ([position base-spread capital]
   (add-size-to-delta-including-spread
    (add-delta-to-spread position base-spread)
    capital)))
                     
  

