(ns clockwork.core
  (:require [clockwork.foundry :as foundry]
            [clockwork.gears.guard :as guard]))

(defmacro ^:private export
  ([re-export] (list `export re-export nil))
  ([internal external]
   (let [exporting (or external (symbol (name internal)))]
     `(do
        (require '~(symbol (namespace internal)))
        (intern *ns* (with-meta '~exporting (meta #'~internal))
                #'~internal)))))

(export foundry/mesh)

(export guard/let?)


#_(defn list-gear
    ([v] (g/simple v))
    ([v f] (reduce (fn [acc x]
                     (let [res (f x)]
                       (if (sequential? res)
                         (into acc res)
                         (conj acc res))))
                   [] v)))

;; workshop tests - menagerie & mimics: for-mimic (and let-mimic)

;; other gears - sugar and flash: ring+, what coperina says
;; stream (zip)
;; input, output, middleware
;; sprocket

;; tease macro-trace and ns-hydration and tape-optimizer
;; possibly implement the generic vm
