(ns clockwork.gears-prop-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clockwork.gears :as g]
            [clockwork.gears-test :as test]))

;; --- Property-based tests ---

(def pure-fns
  [inc
   #(* 2 %)
   #(+ 10 %)
   dec
   identity])

(def gear-fns
  [test/step-inc
   test/step-times2
   test/step-add10
   (fn [x] (g/simple (- x)))])

(def gen-pure-fn (gen/elements pure-fns))
(def gen-gear-fn (gen/elements gear-fns))

(def gen-pure-fn-seq (gen/vector gen-pure-fn 0 5))
(def gen-gear-fn-seq (gen/vector gen-gear-fn 0 5))

;; --- Properties ---

(defspec xform-vs-reduce-xform*
  50
  (prop/for-all [init gen/int
                 fs gen-pure-fn-seq]
    (let [cw1 (apply g/xform (g/simple init) fs)
          cw2 (reduce (fn [gear f] (g/xform* gear f))
                      (g/simple init)
                      fs)]
      (= (cw1 test/driver) (cw2 test/driver)))))

(defspec mesh-vs-reduce-mesh*
  50
  (prop/for-all [init gen/int
                 fs gen-gear-fn-seq]
    (let [cw1 (apply g/mesh (g/simple init) fs)
          cw2 (reduce (fn [gear f] (g/mesh* gear f))
                      (g/simple init)
                      fs)]
      (= (cw1 test/driver) (cw2 test/driver)))))

(defspec complect-empty-equals-engage
  50
  (prop/for-all [v gen/int]
    (= ((g/complect v []) test/driver)
       ((g/engage v) test/driver))))

(defspec xform-then-mesh-equivalence
  50
  (prop/for-all [init gen/int
                 pfs  gen-pure-fn-seq
                 gfs  gen-gear-fn-seq]
    (let [cw1 (as-> (g/engage init) clock
                (apply g/xform clock pfs)
                (apply g/mesh  clock gfs))
          cw2 (as-> (g/engage init) clock
                  (reduce g/xform* clock pfs)
                  (reduce g/mesh* clock gfs))]
      (or (empty? gfs)
          (= (cw1 test/driver) (cw2 test/driver))))))

(defspec engage-embeds-value
  50
  (prop/for-all [v gen/int]
    (= [:embedded v] ((g/engage v) test/driver))))
