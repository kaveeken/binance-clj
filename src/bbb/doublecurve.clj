(in-ns 'bbb.core)

(use '[clojure.java.shell :only [sh]])

(defn compute-target-position [center close k factor]
  (let [location (- close (* factor center))
        arg (/ (* -1 k location) center)
        denominator (+ 1 (Math/exp arg))]
    (- 1 (/ 1 denominator))))

(defn determine-action [center close k factor position]
  (let [target-buy (compute-target-position center close k 1)
        target-sell (compute-target-position center close k factor)
        delta-buy (max 0 (- target-buy position))
        delta-sell (max (- position target-sell))
        buy (> delta-buy delta-sell)]
    {:what (if buy :buy :sell)
     :delta (if buy delta-buy delta-sell)}))

(defn optimize-params [price-data]
  (let [fname (str (:pair price-data) "-" (:interval price-data))
        ; spat? (do (spit-closes price-data) "done") cannot get this to work
        ; file does not get written as expected but is made empty
        ; so file needs to be present before this is ran
        opt-string (:out (sh "resources/opt" (str "resources/closes/" fname)))
        nothing (do (println opt-string) "AAA")
        opt-split (-> opt-string
                      (str/split #"\n")
                      (first)
                      (str/split #", "))]
    {:period (read-string (second opt-split))
     :k (read-string (get opt-split 2))
     :factor (read-string (get opt-split 3))}))
