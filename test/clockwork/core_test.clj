(ns clockwork.core-test
  (:require [clockwork.core :refer :all]
            [clojure.test :refer [deftest is testing]]))

#_
(testing "let-like convenience forms"
  (deftest nil-guard "a drop-in nil-safe replacement for `let`"
    (is (= 3 (let [x 1 y (inc x)] (+ x y))))
    (is (nil? (let? [x nil y (inc x)] (+ x y))))
    (is (nil? (let? [x 1 y nil] (+ x y))))))
