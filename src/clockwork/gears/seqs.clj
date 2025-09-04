(ns clockwork.gears.seqs
  "Seq utilitites, like zip and each"
  (:require [clockwork.foundry :as foundry]))

;; a utility fn
(defn zip
  "Zip a stream of streams (a \"complected stream\"!) together"
  [complected-stream]
  (when-not (empty? complected-stream)
    (lazy-seq (cons (ffirst complected-stream)
                    (zip (map rest (rest complected-stream)))))))

(def zipping
  "Appropriate for use in a `with` or `let-` block"
  (foundry/create repeat (comp zip (partial map))))

(defmacro let-zip
  "Zips streams together, possibly infite. It picks the `nth` item per stream
  Look in the `clockwork.workshop` namespace for some examples"
  [bindings & body]
  `(foundry/with zipping ~bindings ~@body))

(def each
  "A `for`-mimic gear driver. For fun"
  (foundry/create #(cons % nil) mapcat))

(defn- for-each
  "Run your gear's driver (the `mainspring`) through a series of computations"
  [gear]
  (fn
    ([embedding] (each (gear embedding)))
    ([->next coll]
     (let [->step #(gear ->next %)]
       (each ->step coll)))))
