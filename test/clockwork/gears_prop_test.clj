(ns clockwork.gears-prop-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clockwork.foundry :as foundry]
            [clockwork.gears :as gears]
            [clockwork.gears-test :as test]))

;; --- Property-based tests ---

(defn add-ten [x]
  (+ 10 x))

(def pure-fns
  [inc
   test/times-two
   add-ten
   dec
   identity])

(def gear-fns
  [test/step-inc
   test/step-times2
   (comp gears/simple add-ten)
   (fn [x] (gears/simple (- x)))])

(def gen-pure-fn (gen/elements pure-fns))
(def gen-gear-fn (gen/elements gear-fns))

(def gen-pure-fn-seq (gen/vector gen-pure-fn 0 5))
(def gen-gear-fn-seq (gen/vector gen-gear-fn 0 5))

;; --- Properties ---

#_
(defspec xform-vs-reduce-xform*
  50
  (prop/for-all [init gen/int
                 fs gen-pure-fn-seq]
    (let [cw1 (apply foundry/xform (gears/simple init) fs)
          cw2 (reduce (fn [gear f] (foundry/xform* gear f))
                      (gears/simple init)
                      fs)]
      (= (cw1 test/driver) (cw2 test/driver)))))

(defspec mesh-vs-reduce-mesh*
  50
  (prop/for-all [init gen/int
                 fs gen-gear-fn-seq]
    (let [cw1 (apply foundry/mesh (gears/simple init) fs)
          cw2 (reduce (fn [gear f] (foundry/mesh* gear f))
                      (gears/simple init)
                      fs)]
      (= (cw1 test/driver) (cw2 test/driver)))))

(defspec complect-empty-equals-engage
  50
  (prop/for-all [v gen/int]
    (= ((gears/->clockwork v []) test/driver)
       ((gears/mesh v) test/driver))))

#_
(defspec xform-then-mesh-equivalence
  50
  (prop/for-all [init gen/int
                 pfs  gen-pure-fn-seq
                 gfs  gen-gear-fn-seq]
    (let [cw1 (as-> (gears/mesh init) clock
                (apply foundry/xform clock pfs)
                (apply foundry/mesh  clock gfs))
          cw2 (as-> (gears/mesh init) clock
                  (reduce foundry/xform* clock pfs)
                  (reduce foundry/mesh* clock gfs))]
      (or (empty? gfs)
          (= (cw1 test/driver) (cw2 test/driver))))))

(defspec engage-embeds-value
  50
  (prop/for-all [v gen/int]
    (= [:embedded v] ((gears/mesh v) test/driver))))
