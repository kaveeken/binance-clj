
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


