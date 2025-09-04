(ns clockwork.workshop
  (:require
   [clockwork.gears :as gears]
   [clockwork.gears.guard :as guard]
   [clockwork.gears.seqs :as seqs]
   [clockwork.workshop.log :as log]
   [clockwork.workshop.mem :as mem]
   [clockwork.workshop.reader :as reader]
   clojure.string))

;; let? returns nil if any variables (the bindings on the left) are nil
(guard/let?
    [x nil
     y (inc x)]
  (+ x y))
;; => nil

;; you can't be sneaky! It doesn't simply look for literal nil: but the result of any evaluation, if nil, triggers the guard
(guard/let?
    [x 2
     y ((fn [_ignore-x] nil) x)]
  (+ x y))
;; => nil

;; works like normal if no variables are nil
(guard/let?
    [x 6
     y (inc x)]
  (+ x y))
;; => 13

;; Zipping seqs and streams
;; It works *kind of* like `clojure.core/for` but zips down by column instead of "for each". We will use a helper
(defn sample [stream] (take 5 stream))

;; if we don't `sample`, this will be an infinite stream due to n being infinite
(sample
 (seqs/let-zip
     [n     (iterate inc 1)   ; 1, 2, 3, …
      badge ["🔥" "✨" "🎯" "🚀" "💎"]
      label    [:fire :sparkle :bullseye :rocket :diamond]]
   (->> label name clojure.string/capitalize (str "Column " n ": " badge " "))))
;; => ("Column 1: 🔥 Fire"
;;     "Column 2: ✨ Sparkle"
;;     "Column 3: 🎯 Bullseye"
;;     "Column 4: 🚀 Rocket"
;;     "Column 5: 💎 Diamond")

;; use it to take the max! In this case, I will just use finite lists so we don't need to `sample`
(seqs/let-zip
    [x (range 5)
     y [6 -7 8 2 4]
     z [42 0 2 -1 6]]
  (max x y z))
;; => (42 1 8 3 6)

;; quick comparison to highlight `for` vs `zip` with the same seqs
(for
    [x (range 3)
     y [:a :b :c]
     z (take 3 (range (* x 2) 22))]
  [x y z])
;; => ([0 :a 0] [0 :a 1] [0 :a 2] [0 :b 0] [0 :b 1] [0 :b 2] [0 :c 0] [0 :c 1] [0 :c 2] [1 :a 2] [1 :a 3] [1 :a 4] [1 :b 2] [1 :b 3] [1 :b 4] [1 :c 2] [1 :c 3] [1 :c 4] [2 :a 4] [2 :a 5] [2 :a 6] [2 :b 4] [2 :b 5] [2 :b 6] [2 :c 4] [2 :c 5] [2 :c 6])

(seqs/let-zip
    [x (range 3)
     y [:a :b :c]
     z (take 3 (range (* x 2) 22))]
  [x y z])
;; => ([0 :a 0] [1 :b 3] [2 :c 6])

;; Output Gear
;; logging some notes for debugging
(log/let-notes
    [x (log/with-note 3 "Assigning 3, (to x) but the next row will have no note.")
     y (- 99 x)
     z (let [xy (* x y)]
         (log/with-note xy (str "Multiplying x and y. The result is bound to z (It's " xy ")")))]
  (+ x y z)) ;; should result in the value of 387 when evaluated as an expression, but will print the notes to stdout before it does
;; => 387

;; Mem-Cell
(mem/let-cell
 "Initial State"
 ;; we will replace the initial state (above) with a new one, below. The clobbered state gets returned when you mem/put, because why not
 [initial (mem/put {:x {:y {:z "get-in to here"}}
                    :reset {:x "Hello" :y "World!"}})
  z (mem/get :x :y :z)
  ;; the familiar `for`-style synax, seen below, is supported
;; :let [description (str "They dared me! Anyway, z is " z)]
  ;;; Actually clockwork supports more than just :let :when and :while; but it's easier to just afford plain vals inline
  description (str "They dared me! Anyway, z is " z)
  replacement (mem/get :reset)
  _ (mem/put replacement)]
  ;; didn't need the local binding, but this is a tutorial
 description)

;; Programmable Reader
(reader/read " ,  Word! "
             reader/chew-whitespace
             reader/mark
             reader/end-of-word
             reader/emit)
;; => "Word!"

;; ll2
;; with vs let-, gears
;; gear-cog - stream vs each
;; gear-cog vs stream

;; ll3
;; mainsprings and combining gears

;; ll4
;; roll your own gear and mainspring defs

;; get those forms laid down in tests: equivalence between gears/flow and direct

;; arbitrary keyword dispatch hints a la :while :when :let

;; abstract machine vm
