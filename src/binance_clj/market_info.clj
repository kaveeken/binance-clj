(in-ns 'binance-clj.core)

(defn get-full-market-info
  "Vector of maps with information about all(?) trading pairs.
  Map keys:
  `:s`: pair string
  `:q`: quote asset
  `:cs`: number in circulation(? maybe a proxy)
  `:c`: asset price"
  []
  (:data
   (send-unsigned-request
    client/get
    "https://www.binance.com/exchange-api/v2/public/asset-service/product/get-products")))

(defn filter-market-info-by-quote
  "FIXME: also filters by 'mining-zone', whatever that is.
  Meant to filter out unwanted coins, but also kicks out some wanted."
  [market-info quote-string]
  (filterv
   (fn [info-map]
     (and
      (= quote-string (:q info-map))
      (some #(= "mining-zone" %1) (:tags info-map))))
   market-info))

(defn add-market-cap-to-market-info
  [market-info]
  (mapv #(if (:cs %1)
           (assoc %1 :cap (* (:cs %1) (read-string (:c %1))))
           (assoc %1 :cap 0))
       market-info))

(defn sort-market-info-by-market-cap
  [capped-market-info]
  (reverse (sort-by :cap capped-market-info)))
