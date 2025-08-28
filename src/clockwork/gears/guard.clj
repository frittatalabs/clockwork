(ns clockwork.gears.guard
  "Protects the gear driver from guarded values getting passed in"
  (:require [clockwork.foundry :as foundry]))

(defn create
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
       (fn [gear ->mesh]
         (if (guard? gear)
           (mainspring escape)
           (mainspring gear ->mesh)))))))

(defmacro let?
  "A drop-in replacement for let, which short-circuits to safely return `nil` when it sees one, no exceptions"
  [bindings & body]
  `(foundry/with (foundry/engage (create)) ~bindings ~@body))

(defmacro let-with
  "This lets you build a one-off customized guarded let
  the guard? gate and escape are optional, the form will be parsed till we see a vector form (the bindings)

  You can specify a guard (defaults to `nil?`, as in `create`) if not present,
  with an optional \"escape\" value (`nil`, also as in `create`) following the guard.
  In other words, `let?` is like a sugar to this with no args specified"
  [& guarded-let]
  `(foundry/let-with create ~guarded-let))
