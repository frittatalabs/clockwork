(ns clockwork.core
  (:require [clockwork.foundry :as foundry]
            [clockwork.gears.guard :as guard]
            [clockwork.gears.seqs :as seqs]))

(defmacro ^:private export
  ([re-export] (list `export re-export nil))
  ([internal external]
   (let [exporting (or external (symbol (name internal)))]
     `(intern *ns*
              (with-meta '~exporting (meta #'~internal))
              #'~internal))))

(export guard/let?)
(export seqs/let-zip)

