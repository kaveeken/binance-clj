(in-ns 'binance-clj.core)

(load "crypto")
(load "pricing")

(defn cancel-orders-symbol
  "Cancel all orders for a specific trading pair (`symbol`)."
  [key-pas symbol]
  (let [now (System/currentTimeMillis)
        api-url "https://api.binance.com/api/v3/openOrders?"
        params (str "symbol=" symbol "&timestamp=" now)
        signature (build-signature (second key-pas) params)
        url (str api-url params signature)]
    (send-request client/delete url (first key-pas))))

(defn get-orders
  ([key-pas]
   (let [now (System/currentTimeMillis)
         api-url "https://api.binance.com/api/v3/openOrders?"
         params (str "&timestamp=" now)
         signature (build-signature (second key-pas) params)
         header (build-apikey-header (first key-pas))
         url (str api-url params signature)]
     (send-request client/get url (first key-pas)))))

(defn post-limit-order
  "limit order
  `key-pas` api key and passkey
  `symbol` trading pair. string
  `side` SELL or BUY. string
  `quantity` quantity in base currency (BTC BTCUSD)
  `price` price
  `time-in-force` sets expiry conditions. default GTC (good til canceled).
  alternatively IOC (immediate or cancel: in case of partial fill cancel rest)
  or FOK (fill or kill: in case of partial fill cancel entire order)."
  ([key-pas symbol side quantity price]
   (post-limit-order key-pas symbol side quantity price "GTC"))
  ([key-pas symbol side quantity price time-in-force]
   (let [delay (do (Thread/sleep 500)
                   "this is to keep under 50 orders / 10 seconds")
         now (System/currentTimeMillis)
         api-url "https://api.binance.com/api/v3/order?"
         params (str "symbol=" symbol "&side=" side
                     "&quantity=" quantity "&type=LIMIT"
                     "&price=" price "&timeInForce=" time-in-force
                     "&timestamp=" now)
         signature (build-signature (second key-pas) params)
         url (str api-url params signature)]
     (send-request client/post url (first key-pas)))))

(defn post-limit-spread
  "post a limit order for each pair of quantity and price in `qty-price-list`.
  `key-pas`, `symbol`, `side` and `time-in-force` like `post-limit-order`."
  ([key-pas symbol side qty-price-list]
   (post-limit-spread key-pas symbol side qty-price-list "GTC"))
  ([key-pas symbol side qty-price-list time-in-force]
   (loop [qty-price-list qty-price-list
          order-ids []]
     (let [qty-price (first qty-price-list)
           qty (first qty-price)
           price (second qty-price)
           order (post-limit-order key-pas symbol side qty price time-in-force)
           order-ids (conj order-ids (:orderId order))
           qty-price-rest (rest qty-price-list)]
       (if (empty? qty-price-rest)
         order-ids
         (recur qty-price-rest order-ids))))))

(defn get-single-order
  [key-pas symbol id]
  (let [now (System/currentTimeMillis)
        api-url "https://api.binance.com/api/v3/order?"
        params (str "symbol=" symbol "&orderId=" id
                    "&timestamp=" now)
        signature (build-signature (second key-pas) params)
        url (str api-url params signature)]
    (send-request client/get url (first key-pas))))

(defn get-trade-list
  [key-pas symbol]
  (let [now (System/currentTimeMillis)
        api-url "https://api.binance.com/api/v3/myTrades?"
        params (str "symbol=" symbol "&timestamp=" now)
        signature (build-signature (second key-pas) params)
        url (str api-url params signature)]
    (send-request client/get url (first key-pas))))
        
