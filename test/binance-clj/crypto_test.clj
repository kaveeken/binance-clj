(ns binance-clj.core-test
  (:require [clojure.test :refer :all]
            [binance-clj.core :refer :all]))

(defn test-apikey-header []
  (let [apikey-header (build-apikey-header "this is an API key")]
    (= apikey-header {"X-MBX-APIKEY" "this is an API key"})))

(defn test-signature []
  (let [pas "NhqPtmdSJYdKjVHjA7PZj4Mge3R5YNiP1e3UZjInClVN65XAbvqqM6A7H5fATj0j" 
        params "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559"
        reference-sig "&signature=c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71"
        sig (build-signature pas params)]
    (do nil;(print (str sig "\n" reference-sig "\n"))
      (= reference-sig sig))))
