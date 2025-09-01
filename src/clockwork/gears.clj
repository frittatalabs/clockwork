(ns clockwork.gears
  (:require [clockwork.foundry :as foundry]))

(defrecord Clockwork [flow payload]
  clojure.lang.IFn
  #_(applyTo [this args]
      (let [arity (count args)]
          (if (= arity 1)
            ;; exactly one arg: the mainspring
            (.invoke this (first args))
            ;; anything else is a clear arity error
            (throw (clojure.lang.ArityException.
                    arity "Clockwork.applyTo")))))
  (invoke [this driver]
    (loop [{:keys [flow payload]} this
           queue []]
      (case flow
        :simple (if-let [[->next & steps] (seq queue)]
                  (do
                    (println "In a simple flow with a non-empty queue - optimization note")
                    (recur (driver ->next payload) steps))
                  (driver payload))
        ;; TODO: better, smarter queue handling instead of this manual stuff
        :complected (let [{:keys [gear chain]} payload]
                      (if-let [[->next & steps] (seq chain)]
                        (recur (driver ->next gear) (into steps queue))
                        (if-let [[->next & remaining] (seq queue)]
                          (recur (driver ->next gear) remaining)
                          (driver driver gear))))
        nil payload
        (do (println "Warning: taking `windup` path for unknown code" flow)
            (if-let [[->next & steps] (seq queue)]
              (recur (driver ->next this) steps)
              (driver driver this)))))))

(defn halt
  "Immediately exit the flow, returning `result`"
  [result]
  (->Clockwork nil result))

(defn ->clockwork
  ([embedded]
   (->Clockwork :simple embedded))
  ([any chain]
   ;; TODO: use a Record instead of a map literal for :gear :chain
   (->Clockwork :complected {:gear any :chain (vec chain)})))

;; a convenience constructor, explicitly named
(def simple
  "Embed a value in a Clockwork"
  ->clockwork)

;; a gear driver
(def ?
  "The name `?` implied \"decide later\", as this 'general' gear lets you defer deciding how to
  drive the gears until later, allowing you to try plugging different behavior"
  (foundry/create simple ->clockwork))

(defn mesh
  "Put any object through some optional ->gear functions, no constraints
  We will figure it out (read: decomplect things) later - note: this is designed to be Clockwork-specific"
  [any & steps]
  (if (instance? Clockwork any)
    (if-let [[->gear & queue] steps]
      (let [{:keys [flow payload]} any]
        (case flow
          nil any
          :simple (recur (->gear payload) queue)
          :complected (let [{:keys [gear meshed]} payload]
                        (->clockwork gear (into meshed steps)))
          (->clockwork any steps)))
      any)
    (->clockwork any steps)))
