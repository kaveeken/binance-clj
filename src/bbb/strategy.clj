(in-ns 'bbb.core)

(defn step-down
  [x c]
  (if (< x c) 1 0))

(defn step-up
  [x c]
  (if (>= x c) 1 0))

(defn logist
  [x c k]
  (let [exponent (/ (* (- k) (- x c)) c)
        denom (+ 1 (Math/exp exponent))]
    (- 1 (/ 1 denom))))

(defn inverse-logist
  [x c k]
  (let [numerator (* c (- (Math/log x) (Math/log (- 1 x))))]
    (+ (/ numerator k) c)))

(defn generate-limit-spread
  [x c k position capital parsed-filters]
  (let [target (logist x c k)
        min-delta-position (/ min-qty capital)
        last-buy (logist min-price c k)
        last-sell (logist max-price c k)
        ]))
