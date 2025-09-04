(ns clockwork.gears
  (:require [clockwork.foundry :as foundry]))

(defn- prime [command queue]
  {:command command
   :queue (into (clojure.lang.PersistentQueue/EMPTY) (seq queue))})

(declare ->clockwork)

(defrecord Clockwork [flow payload]
  foundry/Gear
  (mesh* [this ->gear]
    (case flow
      nil payload
      :simple (->gear payload)
      :complected (let [{:keys [command queue]} payload]
                    (->clockwork command (conj queue ->gear)))
      (->clockwork this [->gear])))
  clojure.lang.IFn
  (invoke [{:keys [flow payload]} driver]
    (case flow
      :simple (driver payload)
      :complected (let [{:keys [command queue]} payload
                        [->next & steps] queue]
                    (if (seq queue)
                      (let [recalibrate #((->next %) driver)
                            next-gear (driver recalibrate command)]
                        (if (seq steps)
                          (let [next-steps (if (instance? Clockwork next-gear)
                                            (reduce foundry/mesh* next-gear steps) ;; a ha! a reduce mesh*
                                            (->clockwork next-gear steps))]
                            (next-steps driver))
                          next-gear))
                      (driver command)))
      nil payload
      (do (println "Warning: taking spooky path for unknown code" flow)
          (driver driver (Clockwork. flow payload))))))

(defn halt
  "Immediately exit the flow, returning `result`"
  [result]
  (->Clockwork nil result))

(defn ->clockwork
  ([embedded]
   (->Clockwork :simple embedded))
  ([any chain]
   (->Clockwork :complected (prime any chain))))

;; convenience constructors, explicitly named
(defn simple
  "Embed a value directly"
  [any]
  (->clockwork any))

(defn engage
  "Embed a driver command (can be anything, really) in a Clockwork flow"
  ([op]
   (engage op nil))
  ([op steps]
   (if-not (instance? Clockwork op)
    (->clockwork op steps)
    (let [{:keys [flow payload]} op]
      (case flow
        nil op
        :simple (if-let [[->next & more-steps] (seq steps)]
                  (recur (->next payload) more-steps)
                  op)
        :complected (let [{:keys [command queue]} payload]
                      (->clockwork command (into queue steps)))
        (->clockwork op steps))))))

(defn mesh
  "Put any object through some ->gear functions"
  [any & steps]
  (engage any steps))

;; a gear driver
(def cog
  "The name `cog` implies letting change how you transfer motion to the gears, allowing
  you to try plugging different behavior to see how it flows"
  (foundry/create simple #(mesh %2 %1)))
