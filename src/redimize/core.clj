(ns redimize.core
  (:require [clojure.tools.logging :as log]
            [taoensso.carmine :as car]
            [clojure.core.memoize :as cm]))

(defn check-opts [{:keys [pool host port spec]}]
  {:pool (or pool (car/connection-pool {}))
   :spec (or spec {:host (or host (:host (car/make-conn-spec)))
                   :port (or port (:port (car/make-conn-spec)))})})

(def ret (atom nil))

(defn to-redis
  [my-wcar-opts f & {:keys [keyprefix expire]}]
  (fn [& args]
    (let [expire (if (not expire) 60 expire)
          _ (reset! ret nil)
          my-wcar-opts (check-opts my-wcar-opts)]
      (let [memo-key (str (pr-str (type f)) ":" (pr-str args))
            memo-key (if keyprefix (str keyprefix ":" memo-key) memo-key)]
        (try
          (if-let [val (car/wcar my-wcar-opts (car/get memo-key))]
            val
            (let [retv (apply f args)
                  _ (reset! ret retv)]
              (car/wcar my-wcar-opts (car/set memo-key retv))
              (when (not= -1 expire) (car/wcar my-wcar-opts (car/expire memo-key expire)))
              retv))
          (catch Exception e
            (log/error e)
            (if @ret @ret (apply f args))))))))

(defn dual-memo
  [my-wcar-opts f & {:keys [keyprefix expire]}]
  (let [expire (if (not expire) 60 expire)]
    (if (= 0 expire)
      f
      (if (= -1 expire)
        (cm/memo (to-redis my-wcar-opts f :keyprefix keyprefix :expire expire))
        (cm/ttl
          (to-redis my-wcar-opts f :keyprefix keyprefix :expire expire)
          :ttl/threshold (* expire 1000))))))



(defn get-key
  [my-wcar-opts key]
  (try
    (car/wcar my-wcar-opts (car/get key))
    (catch Exception e
      (log/error e (str "Problem retrieving credentials for " key " from cache")))))

(defn set-key
  [my-wcar-opts key value expires]
  (try
    (car/wcar my-wcar-opts (car/set key value :ex expires))
    (catch Exception e
      (log/error e (str "Problems caching credentials for " key)))))




;(let [my-wcar-opts {:spec {:host "redis-cache.idacxb.0001.euw1.cache.amazonaws.com" :port 6379}}
;      key (str "analytics:services:" "849fe9ae7536460a8b40d40ad7498edb" ":" "qobuz")
;      val {:userid      "849fe9ae7536460a8b40d40ad7498edb",
;           :service     "qobuz",
;           :created     "2025-06-13T14:51:50.399Z",
;           :credentials {:userid     2528872,
;                         :token_type "bearer"},
;           :oem         "bowerswilkins",
;           :updated     "2025-06-30T10:20:43.523Z",
;           :writeregion "eu-west-1"}
;      expires 60]
;
;    (prn (set-key my-wcar-opts key val expires))
;
;    (prn (get-key my-wcar-opts key))
;
;    (prn (get-key (assoc-in my-wcar-opts [:spec :port] 1234) key))
;
;    (Thread/sleep 60000)
;
;    (prn (get-key my-wcar-opts key))
;
;  )