
(post-limit-order key-pas "XMRETH" "BUY" 1 0.05)
(def gorder (get-orders key-pas))
(get-orders key-pas)
(cancel-orders-symbol key-pas "XMRETH")

(def spread '((0.5 0.06) (0.5 0.05)))
(post-limit-spread key-pas "XMRETH" "BUY" spread)
