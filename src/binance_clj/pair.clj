(in-ns 'binance-clj.core)

(load "crypto")
(load "pricing")
(load "orders")

(defn round [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/floor (* d factor)) factor)))
(defn roundup [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/ceil (* d factor)) factor)))
  
(def key-pas [(slurp "resources/key") (slurp "resources/pas")])

(defn get-exchange-info
  [symbol]
  (send-unsigned-request
   client/get
   (str "https://api.binance.com/api/v3/exchangeInfo?symbol=" symbol)))

(defn filter-trades
  [trade-list order-id]
  (loop [trade-list trade-list]
    (if (= (:orderId (first trade-list)) order-id)
      (first trade-list)
      (recur (rest trade-list)))))

(defn sum-trade-effects
  "from `trade-list` (list of mapped JSON), sum up the total changes
  for the respective currencies, including commissions.
  Returns a map with keys `:base` and `:quote`, and currency
  difference values."
  [trade-list]
  (loop [trade-list trade-list
         deltas {:base 0 :quote 0}]
    (if (empty? (rest trade-list))
      deltas
      (let [trade (first trade-list)
            buy (:isBuyer trade)
            base-qty (read-string (:qty trade))
            quote-qty (read-string (:quoteQty trade))
            commission (read-string (:commission trade))
            change-base (if buy
                          (- base-qty commission)
                          (- base-qty))
            change-quote (if buy
                           (- quote-qty)
                           (- quote-qty commission))
            new-deltas {:base (+ (:base deltas) change-base)
                        :quote (+ (:quote deltas) change-quote)}]
        (recur (rest trade-list) new-deltas)))))

(defn count-filled-orders
  "For trading pair `symbol` and list of order ids `order-ids`, check
  which of the orders are filled and count up the effect on currencies
  with [[sum-trade-effects]].
  Requires api key and passkey as `key-pas` pair."
  [key-pas symbol order-ids]
  (loop [trade-list (get-trade-list key-pas symbol) ;; gets up to 500 trades
         filtered-trades []]
    (if (empty? trade-list)
      (sum-trade-effects filtered-trades)
      (let [trade (first trade-list)
            tests (map
                   (fn [id] (str/includes? (str trade) (str ":orderId " id)))
                   order-ids)
            match? (= 1 (reduce + (map (fn [bool] (if bool 1 0)) tests)))]
        (recur (rest trade-list)
               (if match? (conj filtered-trades trade) filtered-trades))))))




(defn get-filters
  [symbol]
  (let [reply (:filters (first (:symbols (get-exchange-info symbol))))]
        flatten (loop [reply reply
                       reply-dict {}]
                  (if (empty? reply)
                    reply-dict
                    (recur
                     (rest reply)
                     (conj reply-dict {(keyword (:filterType (first reply)))
                                       (dissoc (first reply) :filterType)}))))))

(defn parse-filters
  [filters price]
  (let [pf (:PRICE_FILTER filters)
        pp (:PERCENT_PRICE filters)
        min-price (min (read-string (:minPrice pf))
                       (* price (read-string (:multiplierDown
                                              pp))))
        max-price (max (read-string (:maxPrice pf))
                       (* price (read-string (:multiplierUp
                                              pp))))
        price-precision (- (Math/round
                            (/ (Math/log (read-string (:tickSize pf)))
                               (Math/log 10))))
        ls (:LOT_SIZE filters)
        min-qty (min (read-string (:minQty ls))
                     (/ (read-string (:minNotional (:MIN_NOTIONAL filters)))
                        price))
        max-qty (read-string (:maxQty ls))
        qty-precision (- (Math/round
                            (/ (Math/log (read-string (:stepSize ls)))
                               (Math/log 10))))]
    {:min-price (* min-price 1.02)
     :max-price (* max-price 0.98)
     :price-precision price-precision
     :min-qty (* min-qty 1.02)
     :max-qty (* max-qty 0.98)
     :qty-precision qty-precision}))
                       


(def example
  {:symbol "XMRETH"
   :base :XMR
   :quote :ETH
   :coins {:XMR 1
           :ETH 0.1}
   :filters {:min-price-mult 0.2 ;; "PERCENT_PRICE"
             :max-price-mult 5
             :min-price-abs 0.00001 ;; "PRICE_FILTER"
             :max-price-abs 1000
             :price-rounding 6
             :min-qty 0.001
             :max-qty 90000000
             :qty-rounding 3
             :min-not 0.005}
   :price-data {}
   :order-ids []
   :cancel-orders (fn [] (cancel-orders-symbol key-pas "XMRETH"))
   :count-fills (fn [order-ids] (count-filled-orders key-pas "XMRETH" order-ids))
   :apply-strategy (fn [] "???")
   :iterate (fn [] "???")
   })


(load "klines")

(defn init-pair
  [base quote starting-base starting-quote]
  (let [symbol (str base quote)
        coins {:base starting-base :quote starting-quote}
        capital (+ (* starting-base (get-price symbol)) starting-quote)
        filters (get-filters symbol)
        price-data (gen-empty-price-data symbol "1d")]
    {:symbol symbol :coins coins :capital capital :filters filters
     :price-data price-data}))


