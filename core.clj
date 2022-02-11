(ns proj-002.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))
(defn -main [& args])

(defn take-csv [f] (with-open [r (io/reader f)] (doall (csv/read-csv r))))
;(def hist (rest (take-csv "spotify-history-2k.csv"))) ;([artist,album,track,track-id,date])
(def hist (rest (take-csv "spotify-history-300k.csv")))) ; Jan 2014 to Dec 2021

(defn get-artists []
  "Returns set of artists"
  (into #{} (for [p hist] (first p))))

(defn get-artist-plays [a]
  "Returns ([plays]) by artist"
  (filter (fn [p] (= (first p) a)) hist))

; Get a map of {:artist play count} ;; {:Coldplay 420 :Washed Out 69}
(into {} (for [a (get-artists)] {(keyword a) (count (get-artist-plays a))}))  

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

