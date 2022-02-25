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
                              ; persistent! is the last thing we call here, it'll be on the result of below
                              ; Something in Clojure called "transient" data, when we start our transducing
                              ; with a (transient {}), there is only ever one map in memory, instead of new
                              ; accumulators/maps/etc. being created every loop in transducing.
                              ; persistent! tells us the transient map is no longer transient, and we
                              ; can use it as unmutable data then. (assigned it to artists, with let)
                              ; 
                              ; This is really cool, it's like pointer math with no work and was super easy.
                              ; It took 175ms runtime down to 70ms for a 300k line csv

                 (transduce   ; Transduce lets us create a HOF, basically. It starts with an empty vector [],
                              ;   and we'll tell it what to add and how to add it

                   identity   ; First, call identity on the element in the loop, starting kinda like
                              ;     (source)                                (end)
                              ;  1. [1, 2, 3, 4] -> identity -> function 
                              ;                                 (called) -> []
                              ;  2. [1, 2, 3, 4] -> identity -> function  
                              ;                                 (called) -> [1], etc. 

                   (fn        ; our anonymous fn that has 3 signatures
                     ([] [])  
                              ; first call, returns [] we'll be adding to
                     ([acc] acc)
                              ; last call, [accumlator-with-nothing-else], return the accumulator
                     ([acc [artist album track ts date]]
                              ; main calls, this is cool, [accumulator csv-line]
                              ; and we destructure the csv values into a coll, this saved time actually
                          (assoc! acc artist (+ 1 (or (acc artist) 0)))))
                              ; assoc! is taking our accumulator, a map, and adding a kv pair { artist scrobbles }
                              ; We get scrobbles from look at our accumulator map, see if the artist is in it, if it is
                              ; place the kv pair in with an incremented number
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

