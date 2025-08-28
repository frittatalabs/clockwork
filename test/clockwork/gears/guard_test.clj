(ns clockwork.gears.guard-test
  (:require [clojure.test :refer [deftest is testing]]
            [clockwork.gears.guard :as guard]
            [clockwork.foundry :as foundry]))

(testing "Make sure the mainspring works as advertised"
  (let [safety (guard/create)]
    (deftest test-defaults
      (let [safety-gear (foundry/engage safety)
            expected 79]
        ;; test some "gear turns"
        (is (= expected (guard/let-with [x (dec expected) y (inc x)] y)))
        ;; think of `with` as "composable let"
        (is (= expected (foundry/with safety-gear [x expected] x)))
        (is (= nil (foundry/with safety-gear [x nil y (inc x)] y)))))
    (testing "Custom built guards - and composed"
      (let [even-7 (guard/create even? 7)
            target 67]
        (deftest test-custom
            ;; let-with is not much different than `with` for this gear, due to how it works. "auto-executing"
            ;; here it just buys us "inline mainspring syntax" - it calls create and then engage for us
            (is (= target (guard/let-with even? ;; would return nil if it sees an even value
                            [x target]
                            x)))
            (is (= 7 (foundry/with (foundry/engage even-7)
                       [x (inc target)] ;; even triggers the guard
                       x))))
        (deftest test-composed ;;; call it WITH or engage
          ;; test composed via the foundry engage + with, but also guard/let-with
          (let [safe->even7 (foundry/engage safety even-7)] ;; pipes through nil-safe and then even-7
              (is (= (* target 2) (foundry/with safe->even7
                                    [x target] ;; target is odd, so we make it past the guard
                                    (* x 2))))  ;; we can double now: in the body we are past the guards
              (is (= 7 (foundry/with safe->even7
                         [x target
                          y (* x 2)] ;; even - we are going to get the "escape" value of 7
                         y)))
              (is (= nil (foundry/with safe->even7
                           [x nil ;; will trip the first guard - the nil? (returning nil) guard
                            y (* x 2)]
                           y)))
              ;; a couple with the "inline" let-with variety, for fun (and coverage)
              (is (= 66 (guard/let-with even? 66 ;; using 66 as the escape value, instead of 7
                          [x 21
                           y (* x 2) ;; we are even now, so we get the "escape" value as the form value
                           ;; the above line causes a short-circuit. y is the escape value of 66
                           z (inc y)] ;; we don't actually inc y, because we don't reach here
                          ;; this is a distraction, there's no point paying attention, z is a mirage
                          z))) ;; 66 was returned as a short-circuit, above. Ignore z!
               ;; let isn't as composable (in clockwork as in life) but hey, that's what `with` is for
              (is (= target (guard/let-with even? ;; would return nil since not specified, if an even value
                             [x target
                              y (* 3 target)]
                             ;; that was silly, but we made it through the gauntlet!
                             (- y (* 2 target)))))))))))
