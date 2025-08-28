(ns clockwork.foundry)

(defprotocol Mainspring
  "The logic that drives a clockwork gear"
  (embed [_ value] "Embed a plain value")
  (drive [_ gear ->mesh] "Mesh a gear through the next step"))

(def decomplect drive)

(defprotocol Cog
  (xform* [this f] "Transform with a regular fn"))

(defn xform
  "Transform with a series of functions, composed left-to-right"
  [clock & chain]
  (reduce xform* clock chain))

(defprotocol Gear
  "The implementation of mesh* should return a Gear, to enable continuing composition"
  (mesh* [this ->gear] "Engage a gear through a step"))

(defn chain
  "A left-to-right composition of step fns"
  [gears]
  (fn [clock]
    (reduce mesh* clock gears)))

(defn compose
  "A right-to-left composition of step fns"
  [gears]
  (chain (reverse gears)))

(defn mesh
  "Engage a gear with one or more step functions; composed left-to-right"
  ;; where a clockwork function produces some Gear for infinite continuing composition
  [clock & ->gears]
  ((chain ->gears) clock))

(defn create
  "Builds a mainspring: for embedding a value into a thing, and deconstructing it.
  Returns a 2-arity function that can be used to drive gears or to create a Mainspring implementation"
  [construct decomplect]
  (fn
    ([it] (construct it))
    ([gear ->mesh] (decomplect gear ->mesh))))

(defn ->fn
  "Makes a mainspring act like a 2-arity fn; if it's not a mainspring, then whatever it is just gets returned"
  [mainspring]
  (if (satisfies? Mainspring mainspring)
    (create (partial embed mainspring) (partial drive mainspring))
    mainspring))

(defn ->mainspring
  "Converts a 2-arity clockwork handler function to a mainspring
  Probably only really useful if you have an IFn and want it to decomplect (mesh / drive) differently
  than it would do by direct invocation"
  ([driver] (->mainspring driver driver))
  ([construct decomplect]
   (reify Mainspring
     (embed [_ any] (construct any))
     (drive [_ gear ->mesh]
       (decomplect gear ->mesh)))))

(defn pass [x f] (f x))

(defmacro with
  "Gear‑agnostic general comprehension writer. What in the world?"
  [gear bindings & body]
  (let [steps (partition 2 bindings)
        mainspring (gensym "mainspring")]
    `(let [gear# ~gear
           ~mainspring (if (satisfies? Mainspring gear#)
                         (->fn gear#)
                         gear#)]
       ~(reduce
          (fn [form [binding expr]]
            (list mainspring expr `(fn [~binding] ~form)))
          (list mainspring (cons 'do body))
          (reverse steps)))))

(def core
  "Just a basic gear to kick things off with - or is it end them with?"
  (create identity pass))

(defn engage
  "mainspring-generating functions can be chained here"
  [& mainsprings]
  (reduce pass core (reverse mainsprings)))

(defmacro let-with
  "A 'lower-level' helper for gears to use in building their own let-like form"
  [mainspring form]
  (let [[parameters let-like]
        (split-with (complement vector?) form)]
    `(with (pass core ~(cons mainspring parameters))
         ~@let-like)))

(def ^:private escapement
  "A mainspring that you can do most anything with, but it has very sharp teeth. Caution - internal use only"
  (create (fn [?] #(% ?))
          (fn [gear ->mesh]
            #(gear (fn [?] ((->mesh ?) %))))))
