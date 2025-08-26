(ns clockwork.gears)

(defprotocol Mainspring
  (embed [this v] "Embed a plain value")
  (decomplect [this gear ->next] "Mesh a gear through the next step"))

(defprotocol Gear
  ;; maybe extract to its own protocol: Cog?
  (xform* [this f] "Transform with a regular fn")
  (mesh* [this f] "Compose a gear step"))

(defn xform
  "Transform a gear with a series of functions, composed left-to-right"
  [clock & chain]
  (reduce xform* clock chain))

(defn mesh
  "Mesh a gear with one or more clockwork functions; composed left-to-right"
  ;; where a clockwork function is (->Construct ...) for some Construct that satisfies Gear
  [clock & chain]
  (reduce mesh* clock chain))

(declare complect escape simple)

(defn- fuse-or-append [q f]
  (cond (nil? f) q
        (empty? q) [f]
        :else (update q (dec (count q)) (partial comp f))))

;; probably should move this to the foundry (when that exists)
(deftype Clockwork [tag value]
  Gear
  (xform* [this f]
    (case tag
      :simple (simple (f value))
      :xform (let [[o q] value]
               (Clockwork. :xform [o (fuse-or-append q f)]))
      :complect (let [[o q] value] (Clockwork. :xform [o (conj q f)]))
      this))
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
    (loop [clock this
           next []]
      (let [flow (.tag clock)
            current (.value clock)]
        (case flow
          :simple (if-let [[step & more] (seq next)]
                    (recur (step current) (vec more))
                    (embed mainspring current))
          :xform (let [[gear q] current]
                   (assert (seq q) ":xform with empty queue should be unreachable")
                   (recur (complect gear (fuse-or-append q (partial embed mainspring))) next))
          :complect (let [[gear queue] current]
                      (if-let [[step & more] (seq queue)]
                        (recur (decomplect mainspring gear step) (into (vec more) next))
                        (if-let [[step & more] (seq next)]
                          (recur (decomplect mainspring gear step) (vec more))
                          (decomplect mainspring gear (partial embed mainspring)))))
          current)))))

;; convenience constructors
(defn simple [embedded] (Clockwork. :simple embedded))

(defn engage ;; just call it mesh? is that weird?
  "Engage a gear with no subsequent step (yet)"
  [any] (Clockwork. :complect [any []]))

(defn complect
  "Construct a complected clockwork with a (possibly empty) coll of steps in a chain
  (complect it ()) is equivalent to (engage it)
  For chaining of multiple functions inline, just call (mesh gear f g ...)"
  [any chain]
  (Clockwork. :complect [any (vec chain)]))

(defn halt
  "A clockwork that immediately stops the turning of gears"
  [exit] (Clockwork. :halt exit))
