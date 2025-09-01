(ns clockwork.gears.stream-test
  (:require [clockwork.gears.stream :as stream]
            [clojure.test :refer [deftest is testing]]
            [clockwork.foundry :as foundry]))

(testing "Stream comprehensions - like for, but for streams: (you zip them)"
  (deftest test-embed
    (is (= 5 (first (stream/zipping 5)))))
  (deftest test-drive
    (is (= [{:x 5 :y 0 :z -2}
            {:x 3 :y 1 :z 2}
            {:x 1 :y 2 :z 4}
            {:x -1 :y 3 :z 9}
            {:x -3 :y 4 :z 0}]
           (take 5 (foundry/with stream/zipping
                       [x (range 5 -10 -2)
                        y (range)
                        z (list -2 2 4 9 0)]
                     {:x x :y y :z z}))))))
