(ns malli.destructure-test
  (:require [clojure.test :refer [deftest testing is]]
            [malli.destructure :as md]))

(def expectations
  [{:name "empty"
    :bind '[]
    :schema :cat}
   {:name "1 arg"
    :bind '[a]
    :schema [:cat :any]}
   {:name "2 args"
    :bind '[a b]
    :schema [:cat :any :any]}
   {:name "2 + varargs"
    :bind '[a b & cs]
    :schema [:cat :any :any [:* :any]]}
   {:name "sequence destructuring"
    :bind '[a [b1 [b2] & bs :as bss] & [c1 c2 & cs :as css]]
    :schema [:cat
             :any
             [:maybe
              [:cat
               [:? :any]
               [:maybe
                [:cat
                 [:? :any]
                 [:* :any]]]
               [:* :any]]]
             [:maybe
              [:cat
               [:? :any]
               [:? :any]
               [:* :any]]]]}
   {:name "map destructuring"
    :bind '[a {:keys [b c]
               :strs [d e]
               :syms [f g]
               :or {b 0, d 0, f 0} :as map}]
    :schema [:cat
             :any
             [:altn
              [:map [:map
                     [:b {:optional true} :any]
                     [:c {:optional true} :any]
                     ["d" {:optional true} :any]
                     ["e" {:optional true} :any]
                     ['f {:optional true} :any]
                     ['g {:optional true} :any]]]
              [:args [:schema
                      [:*
                       [:alt
                        [:cat [:= :b] :any]
                        [:cat [:= :c] :any]
                        [:cat [:= "d"] :any]
                        [:cat [:= "e"] :any]
                        [:cat [:= 'f] :any]
                        [:cat [:= 'g] :any]
                        [:cat :any :any]]]]]]]}
   {:name "Keyword argument functions now also accept maps"
    :bind '[& {:keys [a b], :strs [c d], :syms [e f] :as opts}]
    :schema [:cat
             [:altn
              [:map [:map
                     [:a {:optional true} :any]
                     [:b {:optional true} :any]
                     ["c" {:optional true} :any]
                     ["d" {:optional true} :any]
                     ['e {:optional true} :any]
                     ['f {:optional true} :any]]]
              [:args [:*
                      [:alt
                       [:cat [:= :a] :any]
                       [:cat [:= :b] :any]
                       [:cat [:= "c"] :any]
                       [:cat [:= "d"] :any]
                       [:cat [:= 'e] :any]
                       [:cat [:= 'f] :any]
                       [:cat :any :any]]]]]]}
   {:name "Nested Keyword argument"
    :bind '[[& {:keys [a b] :as opts}]
            & {:keys [a b] :as opts}]
    :schema [:cat
             [:maybe
              [:altn
               [:map [:map
                      [:a {:optional true} :any]
                      [:b {:optional true} :any]]]
               [:args [:* [:alt
                           [:cat [:= :a] :any]
                           [:cat [:= :b] :any]
                           [:cat :any :any]]]]]]
             [:altn
              [:map [:map
                     [:a {:optional true} :any]
                     [:b {:optional true} :any]]]
              [:args [:* [:alt
                          [:cat [:= :a] :any]
                          [:cat [:= :b] :any]
                          [:cat :any :any]]]]]]}])

(def schematized-expectations
  [{:name "empty"
    :bind '[]
    :schema :cat}
   {:name "1 arg"
    :bind '[a :- :int]
    :schema [:cat :int]}
   {:name "2 args"
    :bind '[a :- :int, b :- :boolean]
    :schema [:cat :int :boolean]}
   {:name "2 + varargs"
    :bind '[a, b :- :int & cs :- [:* :boolean]]
    :schema [:cat :any :int [:* :boolean]]}
   {:name "Sequence destructuring - 1"
    :bind '[a :- :int [b1 :- :int [b2 :- :int] & bs :as bss]]
    :schema [:cat
             :int
             [:maybe
              [:cat
               [:? :int]
               [:maybe
                [:cat
                 [:? :int]
                 [:* :any]]]
               [:* :any]]]]}
   {:name "Sequence destructuring - 2 (rest)"
    :bind '[a :- :int [b1 :- :int [b2 :- :int] & bs :- [:* :int] :as bss]]
    :schema [:cat
             :int
             [:maybe
              [:cat
               [:? :int]
               [:maybe
                [:cat
                 [:? :int]
                 [:* :any]]]
               [:* :int]]]]}
   {:name "Sequence destructuring - 3 (as)"
    :bind '[a :- :int [b1 :- :int [b2 :- :int] & bs :as bss :- [:* :int]]]
    :schema [:cat
             :int
             [:schema [:* :int]]]}
   {:name "Sequence destructuring - 4 (bind rest)"
    :bind '[a :- :int & [b1 :- :int [b2 :- :int] & bs :- [:* :int] :as bss]]
    :schema [:cat
             :int
             [:maybe
              [:cat
               [:? :int]
               [:maybe
                [:cat
                 [:? :int]
                 [:* :any]]]
               [:* :int]]]]}
   {:name "map destructuring"
    :bind '[a :- :int, {:keys [b c]
                        :strs [d e]
                        :syms [f g]
                        :or {b 0, d 0, f 0} :as map}
            :- [:map
                [:b :int]
                [:c :int]
                ["d" :string]
                ["e" :string]
                [f :symbol]
                [g :symbol]]]
    :schema [:cat
             :int
             [:map
              [:b :int]
              [:c :int]
              ["d" :string]
              ["e" :string]
              ['f :symbol]
              ['g :symbol]]]}
   {:name "Keyword argument functions now also accept maps"
    :bind '[& {:keys [a b], :strs [c d], :syms [e f] :as opts}
            :- [:map
                [:b :int]
                [:c :int]
                ["d" :string]
                ["e" :string]
                [f :symbol]
                [g :symbol]]]
    :schema [:cat
             [:map
              [:b :int]
              [:c :int]
              ["d" :string]
              ["e" :string]
              ['f :symbol]
              ['g :symbol]]]}
   {:name "Nested Keyword argument"
    :bind '[[& {:keys [a b] :as opts} :- [:map [:a :int] [:b :int]]]
            & {:keys [a b] :as opts} :- [:map [:a :int] [:b :int]]]
    :schema [:cat
             [:maybe
              [:map
               [:a :int]
               [:b :int]]]
             [:map
              [:a :int]
              [:b :int]]]}])

(deftest parse-test

  (testing "vanilla clojure"
    (doseq [{:keys [name bind schema]} expectations]
      (testing name
        (is (= schema (:schema (md/parse bind)))))))

  (testing "schematized clojure"
    (doseq [{:keys [name bind schema]} schematized-expectations]
      (testing name
        (is (= schema (:schema (md/parse bind))))))))
