(ns clockwork.workshop.mem
  (:require [clockwork.foundry :as foundry]
            [clockwork.gears :as gears]
            [clockwork.workshop.mem :as mem]))

(defn get
  "Gets the value stored in the cell, with an optional seq of keys as per `get-in`.
  Returns the entire cell if no keys given"
  [& ks]
  (gears/engage [:recall ks]))

(defn put
  "Clobber the cell with the new contents. Who designed this thing? Terrible user story, be careful with this one"
  [new-state]
  (gears/engage [:store new-state]))

(defn recall
  ([f [tag param :as rogue]]
   #(case tag
      :recall ((f (get-in % param)) %)
      :store ((f %) param)
      ((f rogue) %)))
  ([value] #(vector value %)))

(defmacro let-cell
  [initial-state bindings & body]
  `(let [abstract-cell#
         (foundry/with gears/cog
             ~bindings
           ~@body)
         cell-program# (abstract-cell# recall)
         [result# final#] (cell-program# ~initial-state)]
     (println "Final state: " final#)
     result#))
