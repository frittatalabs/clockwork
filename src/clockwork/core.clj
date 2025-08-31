(ns clockwork.core
  (:require [clockwork.foundry :as foundry]
            [clockwork.gears.guard :as guard]
            [clockwork.gears.stream :as stream]))

(defmacro ^:private export
  ([re-export] (list `export re-export nil))
  ([internal external]
   (let [exporting (or external (symbol (name internal)))]
     `(do
        (require '~(symbol (namespace internal)))
        (intern *ns* (with-meta '~exporting (meta #'~internal))
                #'~internal)))))

;; (export foundry/mesh)

(export clockwork.gears.guard/let?)

;; (export stream/let-zip)

;; gear-generating gears and utilities
;; drive that work by building a sprocket
;; build the sprocket to show off and also use.
;; workshop tests - menagerie
;; build some sprocket demos in the workshop
;; zip demos - how it works and zip vs for; do a compelling like a "max" demo. Or even start with a "sorted"
;; guard demos in workshop
;; combine guard demos, with a stream or list gear, for extra sparks
;; reader demos in workshop
;; flesh out more (composed) reader methods
;; "memory" state gear
;; logging gear
;; env/input

;; other gears - sugar and flash: ring+
;; continuations gear
;; validation gear

;; github, and redis, unfortunately :P
;; tease macro-trace and ns-hydration and tape-optimizer
;; possibly implement the generic vm

;; Later
;;
;; ns->gear generator
;; boot a driver basic->composing
