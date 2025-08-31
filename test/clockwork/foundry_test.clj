(ns clockwork.foundry-test
  (:require [clockwork.foundry :as foundry]
            [clojure.test :refer [deftest is testing]]))

(testing "Test `escapement`, which no one should use anyway, but let's just make sure"
  (deftest escapement-test
    (is (= (list "6" "66")
           ((#'foundry/escapement #(list (% 3) (% 33)) #(#'foundry/escapement (* 2 %))) str)))))
