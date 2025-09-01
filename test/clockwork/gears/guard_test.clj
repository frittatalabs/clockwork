(ns clockwork.gears.guard-test
  (:require [clojure.test :refer [deftest is testing]]
            [clockwork.gears.guard :as guard]
            [clockwork.foundry :as foundry]))

(testing "Make sure the mainspring works as advertised"
  (deftest test-defaults
    (let [expected 79]
      ;; test some "gear turns"
      (is (= expected (guard/let? [x (dec expected) y (inc x)] y)))
      ;; think of `with` as "composable let"
      (is (= expected (foundry/with guard/nilsafe [x expected] x)))
      (testing "short circuits"
        (is (nil? (foundry/with guard/nilsafe [x nil y (inc x)] y)))
        (is (nil? (guard/let? [x nil y (inc x)] y))))))
  (testing "Custom built guards - and composed"
    (let [even-7 (guard/create even? 7) ;; would return 7 if it sees an even value
          target 67]
      (deftest test-custom
        (is (= target (foundry/with even-7
                        [x target]
                        x)))
        (is (= 7 (foundry/with even-7
                   [x (inc target)] ;; even triggers the guard
                   x))))
      (deftest test-composed
        ;; test composed - I think this is a little extra for now, but it's written.
        ;; we want to pipe through nil-safe and then even-7, treated as one (composed) "gear"
        (let [->nilsafe (#'guard/guarded-link) ;; this will create a fn that accepts a driver and returns a guarded version of that driver
              safe->even7 (->nilsafe even-7)]
            (is (= (* target 2) (foundry/with safe->even7
                                  [x target] ;; target is odd, so we make it past the guard
                                  (* x 2))))  ;; we can double now: in the body we are past the guards
            (is (= 7 (foundry/with safe->even7
                       [x target
                        y (* x 2)] ;; even - we are going to get the "escape" value of 7
                       y)))
            (is (nil? (foundry/with safe->even7
                        [x nil ;; will trip the first guard - the nil? (returning nil) guard
                         y (* x 2)]
                        y))))))))
