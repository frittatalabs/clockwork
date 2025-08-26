(ns clockwork.gears-test
  (:require [clojure.test :refer [deftest is]]
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
