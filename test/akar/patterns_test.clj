(ns akar.patterns-test
  (:require [akar.patterns :refer :all]
            [akar.primitives :refer :all]
            [akar.combinators :refer :all]
            [clojure.test :refer :all])
  (:import [clojure.lang Keyword]))

(deftest patterns-test

  (testing "basic patterns"

    (testing "!any"
      (is (= :success
             (match* :random-value (clauses*
                                     !any (fn [] :success))))))

    (testing "!pfail"
      (is (= clause-not-applied
             (try-match* :some-value (clauses*
                                       !fail (fn [] :success))))))

    (testing "!var"
      (is (= :some-value
             (match* :some-value (clauses*
                                   !var (fn [x] x))))))

    (testing "!pred"
      (let [!even (!pred even?)
            !odd (!pred odd?)
            block (clauses*
                    !even (fn [] :even)
                    !odd (fn [] :odd))]
        (is (= :odd
               (match* 9 block)))
        (is (= :even
               (match* 8 block)))))

    (testing "!cst"
      (let [block (clauses*
                    (!cst 4) (fn [] :fier)
                    (!cst 5) (fn [] :fünf))]
        (is (= :fier
               (match* 4 block)))
        (is (= :fünf
               (match* 5 block)))))

    (testing "!view"
      (let [block (clauses*
                    (!view inc) (fn [x] x))]
        (is (= 10
               (match* 9 block)))))

    (testing "!some and !nil"
      (let [block (clauses*
                    !some (fn [x] x)
                    !nil (fn [] :default))]
        (is (= 21
               (match* 21 block)))
        (is (= :default
               (match* nil block))))))

  (testing "collection patterns"
    (let [block (clauses*
                  !empty (fn [] :empty)
                  !cons (fn [hd tl] {:hd hd :tl tl})
                  !any (fn [] :not-sequential))]

      (testing "!empty"
        (is (= :empty
               (match* [] block))))

      (testing "!cons"
        (is (= {:hd 3 :tl [4 5]}
               (match* [3 4 5] block))))

      (testing "non-sequential data fallthrough for both !empty and !cons"
        (is (= :not-sequential
               (match* :some-random-data block)))))

    (let [block (clauses*
                  (!further-many !seq [!var !any !var]) (fn [a b] [a b]))]
      (testing "!seq"
        (is (= [2 4]
               (match* [2 3 4] block)))))

    (let [block (clauses*
                  (!and (!key :k) (!optional-key :l) (!optional-key :m)) (fn [a b c] [a b c])
                  !any (fn [] :stuff))]

      (testing "!key and !optional-key"
        (is (= [:x :y nil]
               (match* {:k :x :l :y} block)))

        (is (= :stuff
               (match* [] block)))))

    (let [block (clauses*
                  (!further (!variant :add) [(!cst 0) !var]) (fn [y] [:num y])
                  (!further (!variant :sub) [!var (!cst 0)]) (fn [x] [:num x])
                  (!at (!variant :num)) (fn [node _] node))]

      (testing "!variant"
        (is (= [:num 3]
               (match* [:add 0 3] block)))
        (is (= [:num 5]
               (match* [:sub 5 0] block)))
        (is (= [:num 11]
               (match* [:num 11] block))))))

  (testing "string patterns"

    (testing "!regex"
      (let [block (clauses*
                    (!regex #"^F (.*) (.*)$") (fn [[handle name]]
                                                {:event  :followed
                                                 :handle handle
                                                 :name   name})
                    !any (fn [] :bad-event))]

        (testing "captures values from a string that matches regex"
          (is (= {:event  :followed
                  :handle "@doofus"
                  :name   "Doofus"}
                 (try-match* "F @doofus Doofus" block))))

        (testing "doesn't match invalid strings"
          (is (= :bad-event
                 (try-match* "F X" block))))

        (testing "doesn't match non-strings"
          (is (= :bad-event
                 (try-match* :not-even-a-string block)))))

      (let [block (clauses*
                    (!regex #"^F [0-9]{1}$") (fn [] :match)
                    !any (fn [] :no-match))]

        (testing "matches a string against a regex that captures nothing"
          (is (= :match
                 (try-match* "F 7" block))))

        (testing "doesn't match invalid srings"
          (is (= :no-match
                 (try-match* "F 11" block)))))))

  (testing "type introspection patterns"

    (testing "!class"
      (let [block (clauses*
                    (!class String) (fn [] :string)
                    (!class Keyword) (fn [] :keyword))]
        (is (= :string
               (try-match* "SomeString" block)))
        (is (= :keyword
               (try-match* :some-keyword block)))
        (is (= clause-not-applied
               (try-match* 4 block)))))

    (testing "!tag"
      (let [block (clauses*
                    (!tag :some-tag) (fn [] :yes))]
        (is (= :yes
               (try-match* {:tag :some-tag} block)))))

    (testing "!type"
      (let [block (clauses*
                    (!and (!type :card) !var) (fn [card] (:details card)))]
        (is (= "Details"
               (try-match* (with-meta {:details "Details"} {:type :card}) block)))))))
