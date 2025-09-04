(ns clockwork.workshop.log
  (:require [clockwork.foundry :as foundry]))

(defrecord Log [value notes])

(defn with-note [value note]
  (->Log value [note]))

(defn append
  [log notes]
  (if-not (instance? Log log)
    (->Log log notes)
    (update log :notes (partial into notes))))

(def notes
  (fn
    ([->next logged]
     (if-let [{:keys [value notes]} (and (instance? Log logged) logged)]
       (append (->next value) notes)
       (->next logged)))
    ([embedded]
     (->Log embedded []))))

(defmacro let-notes
  "Like `let` but captures notes submitted with `` and outputs them at the end. It's just fun!"
  [bindings & body]
  `(let [log# (foundry/with notes
                  ~bindings
                ~@body)]
     (println "Notes collected:"
            (:notes log#))
     (:value log#)))
