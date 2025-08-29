(ns clockwork.foundry)

(defprotocol Gear
  "A protocol to enable continuing composition"
  (mesh* [this ->gear] "Engage a gear through a step"))

(defprotocol Mainspring
  "The logic that drives a clockwork gear - by convention you can use a multi-arity fn instead
  of extending this protocol: it was formalized in a protocol to allow IFn implementations to
  extend a different behavior to driving gears. A call to the 1-arity maps to `embed` and a call to
  the 2-arity maps to drive"
  (embed [_ value] "Embed a plain value")
  (drive [_ gear ->mesh] "Mesh a gear through the next step (i.e. decomplect things)"))

(defn meshed
  "Engage a gear with one or more step functions; composed left-to-right"
  ;; where a ->next clockwork function produces some Gear for infinite continuing composition
  [gear chain]
  (reduce mesh* gear chain))

(defn mesh
  [gear & chain]
  (meshed gear chain))

#_
(defn chain
  "A left-to-right composition of step fns: `compose` with reversed order; similar to `->` in ordering, but not a macro. First class and composeable"
  [gears]
  #(apply mesh % gears))

#_
(defn compose - composed
  "A right-to-left composition of step fns: the heavy-duty version of `comp` - (and not a macro, so first class, and think \"composeable\")"
  [gears]
  (chain (reverse gears)))

(defn create
  "Builds a 2-arity function that can be used to drive gears or to create a Mainspring implementation"
  [->embed ->drive]
  (fn
    ([it] (->embed it))
    ([->to-mesh ->next] (->drive ->to-mesh ->next))))

(defn ->fn
  "Makes a mainspring act like a 2-arity fn; if it's not a mainspring, then whatever it is just gets returned.
  This sugar exists to allow a gear implementation to just call its driver rather than the protocol methods"
  [mainspring]
  (if (satisfies? Mainspring mainspring)
    (create (partial embed mainspring) (partial drive mainspring))
    mainspring))

#_
(defn ->mainspring
  "Constructs a Mainspring from a couple of functions, as an alternative to `create`.
  Probably only really useful if you have an IFn and want it to decomplect (mesh / drive) differently
  than it would do by direct invocation"
  ([construct decomplect]
   (reify Mainspring
     (embed [_ any] (construct any))
     (drive [_ gear ->mesh]
       (decomplect gear ->mesh)))))

(defn pass [x f]
  (println f x)
  (f x))

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
  "Just a basic mainspring to kick things off with - or is it end them with?"
  (create identity pass))

(defn engage ;; congruent with (chain pass core mainsprings), if we were to parameterize `chain` - aka pass = mesh*
  "mainspring-generating functions can be chained here, and brought to life"
  [& mainsprings]
  (reduce pass core (reverse mainsprings)))

(defmacro let-with
  "A helper for gears to use in building their own let-like form
  The idea is is wraps your constructors args, calls engage, and puts it in a `with`"
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
