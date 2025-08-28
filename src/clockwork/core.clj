(ns clockwork.core
  (:use [clockwork.gears.guard :only [let?]]))

#_(defn list-gear
    ([v] (g/simple v))
    ([v f] (reduce (fn [acc x]
                     (let [res (f x)]
                       (if (sequential? res)
                         (into acc res)
                         (conj acc res))))
                   [] v)))

;; Here will be general, user-facing utilities. The refer above will be in the ns form
;; Maybe hofs that use mainsprings to do other things: compose, most probably.

;; stream (zip)
;; for-mimic (and let-mimic)
;; input, output, middleware
;; sprocket
;; ring-plus, other sugar bombs

;; tease macro-trace and ns-hydration and tape-optimizer
;; possibly implement the generic vm

;; pretty-print something, or do a swappable clockwork and show us "mock" something with no sweat? the sky's the limit.
