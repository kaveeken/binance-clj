(in-ns 'bbb.core)

(defn parse-kline
  "Takes a `kline` as received from the REST API and returns it as a keyed map.
  Maybe consider [[json/read-str]] instead."
  [kline]
  {:time-open (first kline)
   :time-close (get kline 6)
   :open (read-string (second kline))
   :high (read-string (get kline 2))
   :low (read-string (get kline 3))
   :close (read-string (get kline 4))
   :volume (read-string (get kline 5))})

(defn get-klines
  "Takes url parameters `params`, sends a get request,
  and returns the received klines parsed with [[parse-kline]] in a vector."
  [params]
  (let [url (str "https://api.binance.com/api/v3/klines" params)
        message (client/get url)]
        (map parse-kline (read-string (:body message)))))
  
(defn get-klines-full
  "Sends a kline request with [[get-klines]] specifying all possible parameters.
  Arguments:
  `symbol`; string describing trading pair, e.g. \"BTCUSDT\",
  `interval`; string describing kline interval, e.g. \"1d\",
  `start-time`; timestamp in milliseconds from where to first gather data,
  `endtime`; timestamp in milliseconds up to where to gather data,
  `limit`; maximum number of klines to request."
  ([symbol interval start-time end-time limit]
   (let [params (str "?symbol=" symbol "&interval=" interval
                     "&startTime=" start-time "&endTime=" end-time
                     "&limit=" limit)]
     (get-klines params))))

(defn get-last-n-klines
  "Sends a kline request with [[get-klines]]
  for the most recent `limit` number of klines with specified `interval`."
  [symbol interval limit]
  (get-klines (str "?symbol=" symbol
                   "&interval=" interval
                   "&limit=" limit)))

(defn get-klines-from
  "Sends a kline request with [[get-klines]] for klines starting from
  the timestamp `start-time`.
  Does not specify a limit parameter, so returns the default 500 klines."
  [symbol interval start-time]
  (get-klines (str "?symbol=" symbol
                   "&interval=" interval
                   "&startTime=" start-time)))

(defn gen-empty-price-data
  "Makes an empty datastructure to track trading pair `symbol` price data
  with specified `interval` and (optionally) `start-time`."
  ([symbol interval]
   (gen-empty-price-data symbol interval (long 1e12))) 
  ([symbol interval start-time]   ; no real reason why 1e12
   {:pair symbol                  ; binance has data from ~1.5e12ms since epoch
    :interval interval
    :next-kline start-time
    :current-price nil
    :klines []}))

(defn add-to-price-data
  "Returns an updated `price-data` map by adding klines to the `:klines` vector
  from the `:next-kline` timestamp onward, using [[get-klines-from]].
  Also updates `:next-kline` and `:current-price`."
  [price-data]
  (let [latest-kline (if (empty? (:klines price-data))
                       (:next-kline price-data)
                       (:time-open (last (:klines price-data))))
        gotten-klines (get-klines-from (:pair price-data)
                                       (:interval price-data)
                                       latest-kline)
        next-kline (:time-close (last gotten-klines))
        current-price (:close (last gotten-klines))
        new-klines (vec (drop-last gotten-klines))]
    (-> price-data
        (assoc :next-kline next-kline)
        (assoc :current-price current-price)
        (assoc :klines (apply conj (:klines price-data) new-klines)))))

(defn complete-price-data
  "Iterates a `price-data` map with [[add-to-price-data]]
  such that the data is up to date."
  [price-data]
  (let [now (System/currentTimeMillis)]
    (loop [price-data price-data]
      (if (>= (:next-kline price-data) now)
        price-data
        (recur (add-to-price-data price-data))))))

(defn spit-closes [price-data]
  (let [selector (fn [kline-map] (:close kline-map))
        closes (vec (map selector (:klines price-data)))
        path (str "resources/closes/"
                  (:pair price-data) "-" (:interval price-data))
        spitter (fn [close] (spit path (str close "\n") :append true))]
    (do (println (spit path "")) (map spitter closes))))
