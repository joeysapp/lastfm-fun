(ns lastfm-fun.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))
(defn -main [& args])

;; This is eager, so we load it all into memory at around 2s
(defn take-csv [fname] (with-open [file (io/reader fname)] (doall (csv/read-csv file))))
(def hist (rest (take-csv "spotify-history-xl.csv"))) ; Jan 2014 to Dec 2021
;(def hist (rest (take-csv "spotify-history-sm.csv"))) ; 2000 scrobbles

;(def playlist (rest (take-csv "boaty-playlist.csv")))

;; We've gone 300s -> 175ms -> 70ms. Faster possible?
(defn get-artist-plays []
  "Returns artists sorted descending by playcount 2015-2022"
  (let [artists (persistent!
                  (transduce
                    identity
                    (fn
                      ([] [])
                      ([acc] acc)
                      ([acc [artist album track ts date]] (assoc! acc artist (+ 1 (or (acc artist) 0)))))
                    (transient {})
                    hist))]
    artists))

(def t (time (get-artist-plays)))
;(def t (time (sort-by last (get-artist-plays)))) ; 65ms

;; 90ms
(defn get-artists []
  "Returns set of artists"
  (into #{} (for [p hist] (first p))))

;; 60ms
(defn get-artists []
  (persistent! (transduce
                (partition-by first)
                (fn
                  ([] [])
                  ([acc] acc)
                  ([acc [e [artist]]] (conj! acc artist)))
                (transient #{})
                hist)))

;(defn get-artists []
;  (persistent! (transduce
;                (partition-by first)
;                (transient #{})
;                hist)))

(defn get-artist-plays [a]
  "Returns ([plays]) by artist"
  (filter (fn [p] (= (first p) a)) hist))

;; Notes

;; Lazy
;; (defn take-csv [fname] (with-open [file (io/reader fname)] (csv/read-csv (slurp file))))
;; (def hist (rest (take-csv "spotify-history-xl.csv"))) ; Jan 2014 to Dec 2021

;; https://clojuredocs.org/clojure.core/reduce  
;; (reduce
;;   (fn [primes number]
;;     (if (some zero? (map (partial mod number) primes))
;;       primes
;;       (conj primes number)))
;;   [2]
;;   (take 1000 (iterate inc 3)))

; It follows from this that at any given time, a snapshot can be taken of an entity’s properties defining its state.
; This notion of state is an immutable one because it’s not defined as a mutation in the entity itself,
; but only as a manifestation of its properties at a given moment in time.
  
;; thread-last vs thread-first
; user=> (->> "hello" (str " jmd"))   "thread-last"
; =>s " jmdhello"
; user=> (-> "hello" (str " jmd"))    "thread-first"
; => "hello jmd"

