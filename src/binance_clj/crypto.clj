(in-ns 'binance-clj.core)


(defn secretKeyInst [pas mac]
  (SecretKeySpec. (.getBytes pas) (.getAlgorithm mac)))

(defn sign [pas string]
  (let [mac (Mac/getInstance "HmacSHA256")
        secretKey (secretKeyInst pas mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes string)))
        .doFinal)))

(defn signature-to-string [bytes]
  (apply str (map #(format "%02x" %) bytes)))

(defn build-apikey-header [key]
  {"X-MBX-APIKEY" key})

(defn build-signature [pas params]
  (str "&signature=" (signature-to-string (sign pas params))))

(defn send-request
  "Send an api request. `type` is a function like `client/get`,
  `url` is the request url and `key` is the api key.
  returns a map of the reply JSON."
  [type url key]
  (let [header (build-apikey-header key)]
    (json/read-str
     (:body
      (type url {:headers header}))
     :key-fn keyword)))

(defn send-unsigned-request
  [type url]
  (json/read-str
   (:body
    (type url))
   :key-fn keyword))
