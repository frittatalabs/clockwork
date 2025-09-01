(ns clockwork.gears.guard
  "Protects the gear driver from guarded values getting passed in"
  (:require [clockwork.foundry :as foundry]))

(defn- guarded-link
  "Create a nil-safe (or otherwise guarded) version of your gear by passing your mainspring to the resulting fn.
  This generator can accept zero, one or two options:
  a guard? predicate: if no options, defaults to `nil?`
  if a second arg follows the guard, it will be used as the 'escape' value
  the default escape is `nil` (this is what returns when the guard? predicate is triggered)"
  [& [guard-or-nil? escape]]
  (fn [mainspring]
    (let [guard? (or guard-or-nil? nil?)]
      (foundry/create
       mainspring
       (fn [->mesh gear]
         (if (guard? gear)
           (mainspring escape)
           (mainspring ->mesh gear)))))))

(defn create
  "Drive a flow with custom guards around the flow values, exiting early with an escape value if triggered;
  uses `nil` as the default escape value if not provided"
  ([guard-or-nil?] (create guard-or-nil? nil))
  ([guard? escape]
   (foundry/create
    identity
    (fn [->next gear]
      (if (guard? gear)
        escape
        (->next gear))))))

(def nilsafe (create nil?))

(defmacro let?
  "A drop-in replacement for let, which short-circuits to safely return `nil` when it sees one, no exceptions"
  [bindings & body]
  `(foundry/with nilsafe ~bindings ~@body))

