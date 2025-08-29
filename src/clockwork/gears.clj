(ns clockwork.gears
  (:require [clockwork.foundry :as foundry]))

(defn- fuse-or-append [q f]
  (cond (nil? f) q
        (empty? q) [f]
        :else (update q (dec (count q)) (partial comp f))))

(declare ->clockwork)

(defrecord Clockwork [flow payload]
  clockwork.foundry/Gear
  (mesh* [_ ->next]
    (case flow
      :simple (->next payload)
      :complected (let [{:keys [gear chain]} payload]
                    (->clockwork gear (conj chain ->next)))
      {:unmeshable-flow flow}))
  clojure.lang.IFn
  (applyTo [this args]
    (let [arity (count args)]
        (if (= arity 1)
          ;; exactly one arg: the mainspring
          (.invoke this (first args))
          ;; anything else is a clear arity error
          (throw (clojure.lang.ArityException.
                  arity "Clockwork.applyTo")))))
  (invoke [this mainspring]
    (let [driver (foundry/->fn mainspring)]
      (loop [{:keys [flow payload]} this
             queue []]
        (case flow
          :simple (if-let [[->next & steps] (seq queue)]
                    (do
                      (println "In a simple flow with a non-empty queue - optimization note")
                      (recur (driver payload ->next) steps))
                    (driver payload))
          :complected (let [{:keys [gear chain]} payload]
                        (if-let [[->next & steps] (seq chain)]
                          (recur (driver gear ->next) (into steps queue))
                          (if-let [[->next & remaining] (seq queue)]
                            (recur (driver gear ->next) remaining)
                            (recur (driver gear ->clockwork) []))))
          {:undefined-flow this})))))

(defn ->clockwork
  ([embedded]
   (->Clockwork :simple embedded))
  ([any chain]
   (->Clockwork :complected {:gear any :chain (vec chain)})))

;; a convenience constructor, explicitly named
(def simple
  "Embed a value in a Clockwork"
  ->clockwork)

;; foundry/mesh just chains calls to mesh* - it doesn't know about anything else
;; core exports this one here, which checks for Clockworkyness, and if so, does our thing, otherwise it boots you into an instance
;; later core can have its own version that checks for Gear -> foundry else Clockwork -> here
(defn mesh
  "Put any object through some optional ->gear functions, no constraints (at this time!)
  We will figure it out (read: decomplect things) later - note: this is designed to be Clockwork-specific"
  [any & ->next]
  (if-let [[->gear & queue] (and (instance? Clockwork any) ->next)]
    (let [[_ {:keys [gear steps]}] (foundry/mesh* any ->gear)]
      (->clockwork gear (into steps queue)))
    (->clockwork any ->next)))
