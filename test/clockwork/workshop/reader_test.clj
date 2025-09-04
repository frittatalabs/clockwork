(ns clockwork.workshop.reader-test
  (:require [clockwork.gears :as gears]
            [clockwork.workshop.reader :as reader]
            [clojure.test :refer [deftest is testing]]))

(testing "Run a little reader through some paces"
  (let [state (reader/->StringState " a test string to read" 0 0)]
    (deftest string-state-test
      (let [skipped (reader/seek* state [:whitespace "a"] false)]
        (is (= \t (reader/current* skipped)) "seek* false + current*")
        (let [marked (reader/seek* (reader/mark* skipped) [:whitespace] true)]
          (is (= "test" (reader/emit* marked)) "emit* after mark*"))))
    (deftest reader-embed-test
      (is (= 6 (reader/decomplect (gears/simple 6) nil))))
    (deftest reader-drive-test
      (testing "Real composition begins: let's compose *reader methods*"
        (deftest a-word!
          (is (= "Word!"
                 ;; we print to stdout because it's a demo/tutorial. It's OK to be a little noisy when learning
                 (binding [*out* (java.io.StringWriter.)]
                   (reader/read " ,  Word! " reader/chew-whitespace
                                             reader/mark
                                             reader/end-of-word
                                             reader/emit)))))))))
