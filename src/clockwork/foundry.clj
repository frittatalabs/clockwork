(ns clockwork.foundry)

;; replaces the protocol below, which may supplement the function-only design later if needed
(defn create
  "Builds a 2-arity function that can be used to drive gears or to create a Mainspring implementation"
  [embed drive]
  (fn
    ([it] (embed it))
    ([->next gear] (drive ->next gear))))

(def core
  "The most basic gear driver. Does almost nothing"
  (create identity #(%1 %2)))

;; this can stay as documentation but we are not going to use it
(defprotocol Mainspring
  "The logic that drives a clockwork gear - by convention you can use a multi-arity fn instead
  of extending this protocol: it was formalized in a protocol to allow IFn implementations to
  extend a different behavior to driving gears. A call to the 1-arity maps to `embed` and a call to
  the 2-arity maps to drive"
  (embed [_ value] "Embed a plain value")
  (drive [_ ->mesh gear] "Mesh a gear through the next step (i.e. decomplect things)"))

(defmacro with
  "It's like `let` but pluggable. (It's a gear‑agnostic general comprehension writer. What?)"
  [gear bindings & body]
  (let [steps (partition 2 bindings)
        mainspring (gensym "mainspring")]
    `(let [gear# ~gear
           ~mainspring (if (satisfies? Mainspring gear#)
                         (create (partial embed gear#) (partial drive gear#))
                         gear#)]
       ~(reduce
          (fn [form [binding expr]]
            (list mainspring `(fn [~binding] ~form) expr))
          (list mainspring (cons 'do body))
          (reverse steps)))))

(defn- escapement
  "An incomprehensible gear driver that has very sharp teeth. Caution - internal use only"
  [->next ->step]
  #(->step (fn [result] ((->next result) %))))

(defprotocol Gear
  "A protocol for gear implementors to ease working with the shape. You might not need it"
  (mesh* [any ->next]))

