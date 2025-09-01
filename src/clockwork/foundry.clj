(ns clockwork.foundry)

;; this can stay as documentation but we are not going to use it
(defprotocol Mainspring
  "The logic that drives a clockwork gear - by convention you can use a multi-arity fn instead
  of extending this protocol: it was formalized in a protocol to allow IFn implementations to
  extend a different behavior to driving gears. A call to the 1-arity maps to `embed` and a call to
  the 2-arity maps to drive"
  (embed [_ value] "Embed a plain value")
  (drive [_ ->mesh gear] "Mesh a gear through the next step (i.e. decomplect things)"))

(defn create
  "Builds a 2-arity function that can be used to drive gears or to create a Mainspring implementation"
  [->embed ->drive]
  (fn
    ([it] (->embed it))
    ([->to-mesh ->next] (->drive ->to-mesh ->next))))

(defn mainspring->driver
  "Makes a mainspring act like a 2-arity fn; if it's not a mainspring, then whatever it is just gets returned.
  This sugar exists to allow a gear implementation to just call its driver rather than the protocol methods"
  [mainspring]
  (if (satisfies? Mainspring mainspring)
    (create (partial embed mainspring) (partial drive mainspring))
    mainspring))

(defmacro with
  "Gear‑agnostic general comprehension writer. What in the world?"
  [gear bindings & body]
  (let [steps (partition 2 bindings)
        mainspring (gensym "mainspring")]
    `(let [gear# ~gear
           ~mainspring (if (satisfies? Mainspring gear#)
                         (mainspring->driver gear#)
                         gear#)]
       ~(reduce
          (fn [form [binding expr]]
            (list mainspring `(fn [~binding] ~form) expr))
          (list mainspring (cons 'do body))
          (reverse steps)))))

(defn- escapement
  "A gear driver you can do most anything with, but it has very sharp teeth. Caution - internal use only"
  [->next ->step]
  #(->step (fn [result] ((->next result) %))))
