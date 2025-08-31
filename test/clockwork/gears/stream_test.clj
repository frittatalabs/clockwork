(ns clockwork.gears.stream-test
  (:require [clockwork.gears.stream :as stream]
            [clojure.test :refer [deftest is testing]]
            [clockwork.foundry :as foundry]))

(testing "Stream comprehensions - like for, but for streams: (you zip them)"
  (let [streaming (stream/create foundry/core)]
    (deftest test-embed
      (is (= 5 (first (streaming 5)))))
    (deftest test-drive
      (is (= [1 2 3] (take 7 (foundry/with streaming
                                 [x (range 5 -10 -2)
                                  y (range)
                                  z (list -2 2 4 9 0)]
                               {:x x :y y :z z})))))))
