(in-ns 'binance-clj.core)

(defn get-price [symbol]
  (read-string
   (:price
    (json/read-str
     (:body
      (client/get
       (str "https://api.binance.com/api/v3/ticker/price?symbol="
            symbol)))
     :key-fn keyword))))
