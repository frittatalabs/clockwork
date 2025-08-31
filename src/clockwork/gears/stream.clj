(ns clockwork.gears.stream
  "As `for` generates list comprehension, so the stream gear produces stream comprehensions
  I'm calling a (possibly infinite) list a stream"
  (:require [clockwork.foundry :as foundry]))

;; let's define some "gear algebra" - because I'm liking how this file is turning out

;; just a utility fn
(defn zip
  "Zip a stream of streams (a \"complected stream\"!) together"
  [complected-stream]
  (lazy-seq (cons (ffirst complected-stream)
                  (zip (rest (map rest complected-stream))))))

;; a "Mainspring"-like fn we can use to decomplect
(defn drive
  "Zip a stream-generating fn down a (possibly infinite) stream. Returns a (possibly infinite) stream,
  as you might guess. So, you know. Be ready for that"
  [stream ->next]
  (zip (map ->next stream)))

;; an appropriately named driver for e.g. composing or in a `with` form
(def zipping
  "A mainspring for infinite streams, appropriate for use in a `with` or `let` block
  Be sure to return an infinite stream from the body so that it can be properly zipped
  You can use the driver to do this for simple value expressions: `(with driver [x (range)] (driver x))`"
  (foundry/create repeat drive))

;; a mainspring-generating fn, for wrapping another mainspring. Used to compose different behaviours together
;; arbitrarily, but if you don't need to enable mixing behaviours, this might be extra.
;; This would enable us to compose e.g. a guard/nilsafe -> stream so that your stream-generating fn is protected
;; from being invoked with nil. Kind of a higher-level gear fn. Which means it produces higher level gears!
(defn create
  [mainspring]
  (fn ([embedding] (mainspring (repeat embedding)))
    ([stream ->gears]
     ;; This thing is giving me some trouble! Kind of a brain twister, even with my working ref :P
     (let [recalibrate #(mainspring % ->gears)]
       (letfn [(decomplect [complected-stream]
                 (zip complected-stream))
               (zip [complected-stream]
                 (lazy-seq
                  (cons (ffirst complected-stream)
                        (decomplect (map rest complected-stream)))))]
         (decomplect (map recalibrate stream)))))))

