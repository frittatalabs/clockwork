(ns clockwork.gears-test
  (:require [clojure.test :refer [deftest is]]
            [clockwork.foundry :as foundry]
            [clockwork.gears :as gears]))

;; --- scaffolding ---

(def driver
  (foundry/create
    #(vector :embedded %)
    foundry/pass))

(def step-inc (comp gears/simple inc))
(defn times-two [x] (* 2 x))
(defn step-times2 [x] (gears/simple (times-two x)))

(deftest simple-invocation
  (is (= [:embedded 42] ((gears/simple 42) driver))))

(deftest mesh-multiple-args-vs-apply
  ;; mesh consumes Gear-returning steps
  (let [cw1 (apply foundry/mesh (gears/simple 1) [step-inc step-times2 step-inc])
        cw2 (foundry/mesh (gears/simple 1) step-inc step-times2 step-inc)]
    (is (= [:embedded 5] (cw1 driver)))
    (is (= [:embedded 5] (cw2 driver)))))

#_
(deftest xform-multiple-args-vs-apply
  ;; xform consumes pure functions
  (let [cw1 (apply foundry/xform (gears/simple 1) [inc #(* 3 %)])
        cw2 (foundry/xform (gears/simple 1) inc #(* 3 %))]
    (is (= [:embedded 6] (cw1 driver)))
    (is (= [:embedded 6] (cw2 driver)))))

(deftest complect-with-vector-queue
  ;; complect takes a prebuilt queue (collection) of Gear-returning steps
  (let [cw (gears/->clockwork 1 [step-inc step-times2])]
    (is (= [:embedded 4] (cw driver)))))

#_
(deftest xform-on-simple-single-fn
  ;; Protocol methods (*) only accept a single fn
  (is (= [:embedded 4] ((foundry/xform* (gears/simple 3) inc) driver))))

(deftest mesh-on-simple-single-fn
  ;; mesh* must receive a Gear-returning step
  (is (= [:embedded 4] ((foundry/mesh* (gears/simple 3) step-inc) driver))))

#_
(deftest xform-then-mesh
  ;; Pure transforms, then compose a gear step
  (let [cw (-> (gears/mesh 2)
               (foundry/xform inc)
               (foundry/xform #(+ % 10))
               (foundry/mesh step-times2))]
    (is (= [:embedded 26] (cw driver)))))

#_
(deftest xform-empty-chain-is-noop
  ;; No extra args => no steps
  (let [cw (foundry/xform (gears/simple 10))]
    (is (= [:embedded 10] (cw driver)))))

(deftest ->clockwork-empty-vector-equals-engage
  (is (= ((gears/->clockwork 7 []) driver)
         ((gears/mesh 7) driver))))

;; --- Engage-specific tests ---

(deftest engage-equivalence
  (doseq [v [0 1 -3 42]]
    (is (= ((gears/mesh v) driver)
           ((gears/->clockwork v []) driver)))))
