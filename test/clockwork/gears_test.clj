(ns clockwork.gears-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clockwork.gears :as g]))

;; --- scaffolding ---

(defrecord DummyMainspring []
  g/Mainspring
  (embed [_ value] [:embedded value])
  (decomplect [_ gear f]
    (f gear)))

(def driver (->DummyMainspring))

(defn step-inc    [x] (g/simple (inc x)))
(defn step-times2 [x] (g/simple (* 2 x)))
(def step-add10 (comp g/simple #(+ 10 %)))

(deftest simple-invocation
  (is (= [:embedded 42] ((g/simple 42) driver))))

(deftest escape-bypasses
  (is (= :escape ((g/halt :escape) driver))))

(deftest mesh-multiple-args-vs-apply
  ;; mesh consumes Gear-returning steps
  (let [cw1 (apply g/mesh (g/simple 1) [step-inc step-times2 step-inc])
        cw2 (g/mesh (g/simple 1) step-inc step-times2 step-inc)]
    (is (= [:embedded 5] (cw1 driver)))
    (is (= [:embedded 5] (cw2 driver)))))

(deftest xform-multiple-args-vs-apply
  ;; xform consumes pure functions
  (let [cw1 (apply g/xform (g/simple 1) [inc #(* 3 %)])
        cw2 (g/xform (g/simple 1) inc #(* 3 %))]
    (is (= [:embedded 6] (cw1 driver)))
    (is (= [:embedded 6] (cw2 driver)))))

(deftest complect-with-vector-queue
  ;; complect takes a prebuilt queue (collection) of Gear-returning steps
  (let [cw (g/complect 1 [step-inc step-times2])]
    (is (= [:embedded 4] (cw driver)))))

(deftest xform-on-simple-single-fn
  ;; Protocol methods (*) only accept a single fn
  (is (= [:embedded 4] ((g/xform* (g/simple 3) inc) driver))))

(deftest mesh-on-simple-single-fn
  ;; mesh* must receive a Gear-returning step
  (is (= [:embedded 4] ((g/mesh* (g/simple 3) step-inc) driver))))

(deftest xform-then-mesh
  ;; Pure transforms, then compose a gear step
  (let [cw (-> (g/engage 2)
               (g/xform inc)
               (g/xform #(+ % 10))
               (g/mesh step-times2))]
    (is (= [:embedded 26] (cw driver)))))

(deftest xform-empty-chain-is-noop
  ;; No extra args => no steps
  (let [cw (g/xform (g/simple 10))]
    (is (= [:embedded 10] (cw driver)))))

(deftest complect-empty-vector-equals-engage
  (is (= ((g/complect 7 []) driver)
         ((g/engage 7) driver))))

;; --- Engage-specific tests ---

(deftest engage-equivalence
  (doseq [v [0 1 -3 42]]
    (is (= ((g/engage v) driver)
           ((g/complect v []) driver)))))

(deftest engage-has-empty-queue
  (doseq [v [0 1 -3 42]]
    (let [[tag [_ q]] [(.-tag (g/engage v))
                       (.-value (g/engage v))]]
      (is (= :complect tag))
      (is (vector? q))
      (is (empty? q)))))

;; --- Property-based tests ---

(def pure-fns
  [inc
   #(* 2 %)
   #(+ 10 %)
   dec
   identity])

(def gear-fns
  [step-inc
   step-times2
   step-add10
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
      (= (cw1 driver) (cw2 driver)))))

(defspec mesh-vs-reduce-mesh*
  50
  (prop/for-all [init gen/int
                 fs gen-gear-fn-seq]
    (let [cw1 (apply g/mesh (g/simple init) fs)
          cw2 (reduce (fn [gear f] (g/mesh* gear f))
                      (g/simple init)
                      fs)]
      (= (cw1 driver) (cw2 driver)))))

(defspec complect-empty-equals-engage
  50
  (prop/for-all [v gen/int]
    (= ((g/complect v []) driver)
       ((g/engage v) driver))))

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
          (= (cw1 driver) (cw2 driver))))))

(defspec engage-embeds-value
  50
  (prop/for-all [v gen/int]
    (= [:embedded v] ((g/engage v) driver))))
