(ns clockwork.gears-test
  (:require [clojure.test :refer [deftest is]]
            [clockwork.foundry :as foundry]
            [clockwork.gears :as gears :refer [->clockwork]]))

;; --- scaffolding ---

(def driver
  (foundry/create
    #(vector :embedded %)
    (fn [f x]
      (f (if (vector? x) (second x) x)))))

(def step-inc (comp gears/simple inc))
(defn times-two [x] (* 2 x))
(defn step-times2 [x] (gears/simple (times-two x)))

(deftest simple-invocation
  (is (= [:embedded 42] ((gears/simple 42) driver))))

(deftest complect-with-vector-queue
  ;; complect takes a prebuilt queue (collection) of Gear-returning steps
  (let [cw (->clockwork 1 [step-inc step-times2])]
    (is (= [:embedded 4] (cw driver)))))

(deftest mesh-multiple-steps-equals-vector-construction
  (let [cw1 (->clockwork 1 [step-inc step-times2 step-inc])
        cw2 (gears/mesh 1 step-inc step-times2 step-inc)
        cw3 (gears/mesh (gears/simple 1) step-inc step-times2 step-inc)]
    (is (= [:embedded 5] (cw1 driver) (cw2 driver) (cw3 driver)))))

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

(deftest mesh-with-no-functions-equals-empty-vector
  (is (= ((gears/->clockwork 43 []) driver)
         ((gears/mesh 43) driver))))

