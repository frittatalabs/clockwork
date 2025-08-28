(ns clockwork.gears
  (:require [clockwork.foundry :as foundry]))

(defn- fuse-or-append [q f]
  (cond (nil? f) q
        (empty? q) [f]
        :else (update q (dec (count q)) (partial comp f))))

;; convenience constructors
(declare complect simple)

(deftype Clockwork [tag value]
  clockwork.foundry/Cog
  (xform* [this f]
    (case tag
      :simple (simple (f value))
      :xform (let [[o q] value]
               (Clockwork. :xform [o (fuse-or-append q f)]))
      :complect (let [[o q] value] (Clockwork. :xform [o (conj q f)]))
      this))
  clockwork.foundry/Gear
  (mesh* [this f]
    (case tag
      :simple (f value)
      :xform (let [[o q] value]
               (complect o (fuse-or-append q f)))
      :complect (let [[o q] value]
                  (complect o (conj q f)))
      this))
  clojure.lang.IFn
  (applyTo [this args]
    (let [n (count args)]
        (if (= n 1)
          ;; exactly one arg: the mainspring
          (.invoke this (first args))
          ;; anything else is a clear arity error
          (throw (clojure.lang.ArityException.
                   n "Clockwork.applyTo")))))
  (invoke [this mainspring]
    (let [driver (foundry/->fn mainspring)]
      (loop [clock this
             next []]
        (let [flow (.tag clock)
              current (.value clock)]
          (case flow
            :simple (if-let [[step & more] (seq next)]
                      (recur (step current) (vec more))
                      (driver current))
            :xform (let [[gear q] current]
                     (assert (seq q) ":xform with empty queue should be unreachable")
                     (recur (complect gear (fuse-or-append q (partial driver))) next))
            :complect (let [[gear queue] current]
                        (if-let [[step & more] (seq queue)]
                          (recur (driver gear step) (into (vec more) next)) ;; make a queue instead of vec
                          (if-let [[step & more] (seq next)]
                            (recur (driver gear step) (vec more))
                            (driver gear (partial driver)))))
            current))))))

;; convenience constructors
(defn simple
  "Embed a value in a Clockwork"
  [embedded]
  (->Clockwork :simple embedded))

(defn complect
  "Construct a complected clockwork with a (possibly empty) coll of steps in a chain
  (complect it ()) is equivalent to (clockwork.foundry/mesh it)
  For chaining of multiple functions inline, just call (mesh gear f g ...)"
  [any chain]
  (->Clockwork :complect [any (vec chain)]))

(defn mesh
  "Put any object through some optional ->gear functions, no constraints (at this time!)
  We will figure it out (read: decomplect things) later"
  [any & gears]
  (if (instance? Clockwork any)
    (apply foundry/mesh any gears)
    (complect any gears)))

(defn halt
  "A clockwork that immediately stops the turning of gears"
  [exit] (->Clockwork :halt exit))
