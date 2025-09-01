(ns clockwork.gears.stream
  "As `for` generates list comprehension, so the stream gear produces stream comprehensions
  I'm calling a (possibly infinite) list a stream"
  (:require [clockwork.foundry :as foundry]))

;; a utility fn
(defn zip
  "Zip a stream of streams (a \"complected stream\"!) together"
  [complected-stream]
  (lazy-seq (cons (ffirst complected-stream)
                  (zip (rest (map rest complected-stream))))))

(def zipping
  "Appropriate for use in a `with` or `let-` block"
  (foundry/create repeat (comp zip (partial map))))


