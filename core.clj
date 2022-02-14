(ns lastfm-fun.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))
(defn -main [& args])

(defn take-csv [fname] (with-open [file (io/reader fname)] (doall (csv/read-csv file)))))
(def hist (rest (take-csv "spotify-history-xl.csv"))) ; Jan 2014 to Dec 2021
;(def hist (rest (take-csv "spotify-history-sm.csv")))) ; 2000 scrobbles

;; Lazy
;; (defn take-csv [fname] (with-open [file (io/reader fname)] (csv/read-csv (slurp file))))
;; (def hist (rest (take-csv "spotify-history-xl.csv"))) ; Jan 2014 to Dec 2021

(defn get-artists []
  "Returns set of artists"
  (into #{} (for [p hist] (first p))))

(defn get-artist-plays [a]
  "Returns ([plays]) by artist"
  (filter (fn [p] (= (first p) a)) hist))

;; This is very slow. 
;; - Is it okay being slow?
;; - If is it something that can just stay loaded in, or do
;; - we want to re-compute? (think in lists...)
;; BENCHMARKING too
; Get a map of {:artist play count} ;; {:Coldplay 420 :Washed Out 69}
;; Non-lazy, 197 seconds... Lazy, 190 seconds
(time (into {} (for [a (get-artists)] {(keyword a) (count (get-artist-plays a))})))

(transduce
 (partition-by identity)
 (fn 
   ([] []) ;; init - returns initial value for accumulator
   ([acc] acc)    ;; completion - returns the final result, take the final accumulated value
   ([acc e] (conj acc e)))    ;; step - returns accumulated state and takes accumulated state from before and new element
 '()
[[1 "foo"] [2 "bar"] [3 "whee"] [1 "???"]])

;; Experimenting
(let [artists #{}]
  (doseq [play hist, artist (first play)]
    (if (nil? (artists artist))
        (conj artists [(keyword artist) 0]) ;; this is not re-assigning back to artists. duh. lol.
        (conj artists [(keyword artist) (+ (artists artist) 1)]))
    artists))

;; Notes

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

