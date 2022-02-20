(ns lastfm-fun.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))
(defn -main [& args])

;; This is "Eager", so we load it all into memory @ like, 2s.
(defn take-csv [fname] (with-open [file (io/reader fname)] (doall (csv/read-csv file))))
;(def hist ()) 
(def hist (rest (take-csv "spotify-history-xl.csv"))) ; Jan 2014 to Dec 2021
;(def hist (rest (take-csv "spotify-history-sm.csv")))) ; 2000 scrobbles
(def playlist (rest (take-csv "boaty-playlist.csv")))

; https://clojuredocs.org/clojure.core/transduce
;; Count our artists by plays, xl hist takes 175ms
(time (transduce
 (partition-by first)
 (fn
   ([] [])
   ([acc] (nthrest (sort-by last acc) (- (count acc) 10))) ;; Return top 10 artists (~25ms)
   ([acc e] (conj acc 
                  {(first (first e))
                   (+ (or
                        (acc (first (first e))) ;; Increment existing playcount
                        0)                      ;; Initialize new artist at + 0 1
                      1)})))
 {}
hist))

;; Using transient data structures
;; xl hist takes 110ms
(time 
  (let [artists (persistent! (transduce
   (partition-by first)
   (fn
     ([] [])
     ([acc] acc)
     ([acc e] (assoc! acc 
                     (first (first e))
                     (+
                       (or (acc (first (first e))) 0)    
                       1))))
   (transient {})
   hist))]
  (prn (nthrest (sort-by last artists) (- (count artists) 10)))))


(defn get-artists []
  "Returns set of artists"
  (into #{} (for [p hist] (first p))))

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

