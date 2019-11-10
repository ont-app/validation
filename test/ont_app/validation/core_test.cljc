(ns ont-app.validation.core-test
  (:require
   #?(:cljs [cljs.test :refer-macros [async deftest is testing]]
      :clj [clojure.test :refer :all])
   [ont-app.validation.core :as validation :refer [report-test]]
   [ont-app.igraph.core :as igraph :refer [add]]
   [ont-app.igraph.graph :as graph :refer [make-graph]]
   ))

(def test-model (add (make-graph)
                     [[:test/X :rdf/type :test/Y]
                      ]))

(def validator (add validation/ontology
                    [[:test/CheckIsa
                      :rdf/type :validation/Test
                      :validation/query
                      [[:test/X :rdf/type :?type]]
                      :validation/resultTest :validation/ExpectNonEmpty
                      :validation/bindingTest :test/ObjectIsY
                      :rdfs/comment "Asserts that the only triple in the graph has test/Y as the object"
                      ]
                     [:test/ObjectIsY
                      :proto/elaborates :validation/BindingTest
                      :igraph/compiledAs (fn [g bmap]
                                           (println "bmap:" bmap)
                                           (println "eq?" (= (:?type bmap) :test/Y))
                                           (if (= (:?type bmap)
                                                  :test/Y)
                                             (assoc bmap :validation/status
                                                    :validation/Success)
                                             (assoc bmap :validation/status
                                                    :failure)))
                                             
                      :validation/commentTemplate
                      "{{?type}} Should be :test/Y."
   
                      ]]))

(deftest test-validator
  (testing "Just a simple test against a simple model"
    (let [result (validation/check-model validator test-model)
          ]
      (is (= (igraph/normal-form (igraph/difference result test-model))
             {})))))
             
           
