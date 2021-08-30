(in-ns 'binance-clj.core)

(load "crypto")
(load "pricing")

(defn build-account-request-url [pas]
  (let [now (System/currentTimeMillis)
        api-url "https://api.binance.com/api/v3/account?"
        params (str "recvWindow=60000&timestamp=" now)
        signature (build-signature pas params)
        full-url (str api-url params signature)]
    full-url))

(defn get-account-info [key-pas]
  (let [url (build-account-request-url (second key-pas))
        header (build-apikey-header (first key-pas))]
    (json/read-str
     (:body
      (client/get url {:headers header}))
     :key-fn keyword)))

(defn filter-balances [balances]
  (loop [balances balances
         my-balances {}]
    (if (empty? balances)
      my-balances
      (let [balance (first balances)
            asset-key (keyword (:asset balance))
            balance (assoc balance :free (read-string (:free balance)))
            balance (assoc balance :locked (read-string (:locked balance)))
            my-balances (if (> (:free balance) 0)
                          (assoc my-balances asset-key balance)
                          my-balances)]
        (recur (rest balances) my-balances)))))

(defn get-my-balances [key-pas]
  (filter-balances (:balances (get-account-info key-pas))))

(defn add-price-to-balance [balance base-currency-symbol]
  (let [price (get-price (str (:asset balance) base-currency-symbol))]
    (assoc balance (keyword (str "price-" base-currency-symbol)) price)))

(defn compute-currency-capital [balance base-currency-symbol]
  (let [price-key (keyword
                   (str "price-" base-currency-symbol))
        price (price-key balance)
        amount (:free balance)]
    (if price
      (* price amount)
      (let [price (get-price (str (:asset balance)
                                  base-currency-symbol))]
        (* price amount)))))

(defn sort-currency-by-capital [filtered-balances base-currency-symbol]
  (let [comparison (fn [bal1 bal2]
                     (let [bcs base-currency-symbol
                           bal1 (second bal1)
                           bal2 (second bal2)
                           cap1 (compute-currency-capital bal1 bcs)
                           cap2 (compute-currency-capital bal2 bcs)]
                       (> cap1 cap2)))]
    (sort comparison filtered-balances)))
