(ns ont-app.validation.ont
  (:require
   [clojure.string :as str]
   ;;
   [taoensso.timbre :as timbre]
   ;;
   [ont-app.igraph.core :as igraph :refer [add]]
   [ont-app.igraph.graph :as g :refer [make-graph]]
   [ont-app.igraph-vocabulary.core :as igv]
   [ont-app.prototypes.core :as proto]
   [ont-app.vocabulary.core :as voc]
   )
  )
(voc/cljc-put-ns-meta!
 'ont-app.validation.ont
 {
  :vann/preferredNamespacePrefix "validation"
  :vann/preferredNamespaceUri "http://rdf.naturallexicon.org/validation/ont#"
  })


(def the igraph/unique)
(def ontology-ref (atom (igraph/union igv/ontology
                                      proto/ontology)))

(defn update-ontology [to-add]
  (swap! ontology-ref add to-add))

(update-ontology
  [[:validation/Property
   :rdfs/subClassOf :rdf/Property
   :rdfs/comment "A property used in testing"
   ]
  [:validation/Test
   :rdfs/comment "A test for well-formedness within a given model"
   :rdf/type :rdfs/Class
   :rdf/type :proto/Prototype
   :proto/hasParameter :validation/resultsTest
   :proto/hasParameter :validation/bindingTest
   ]
  [:validation/resultsTest
   :rdf/type :validation/TestProperty
   :proto/aggregation :proto/Inclusive
   :rdfs/domain :validation/Test
   :rdfs/range :validation/ResultsTest
   :rdfs/comment "
<test> resultsTest <ResultsTest>
<BindingTest> igraph/compiledAs <fn>
Asserts that <fn> :=  (fn [test-model test-name]...) -> <test-outcome-map> for a binding to some query. 
Where
<test-outcome-map> {:validation/status..., ...}
:validation/status = :validation/Success if conformant.
"
   ]  
  [:validation/bindingTest
   :rdf/type :validation/TestProperty
   :proto/aggregation :proto/Inclusive
   :rdfs/domain :validation/Test
   :rdfs/range :validation/BindingTest
   :rdfs/comment "
<test> bindingTest <BindingTest>
<BindingTest> igraph/compiledAs <fn>
Asserts that <fn> :=  (fn [model bmap]...) -> <bmap'> for a binding to some 
query. :validation/status = :validation/Success if conformant.
Exceptions being if <BindingTest> :- #{ExpectEmpty ExpectNonEmpty}
"
   ]  
  
  [:validation/Issue
   :rdf/type :rdfs/Class
   :rdfs/comment "A case where some test failed."
   ]
  [:validation/TestProperty
   :rdf/type :proto/Prototype
   :rdf/type :validation/Property
   :rdfs/domain :validation/Test
   ]
  [:validation/query
   :rdfs/subClassOf :validation/TestProperty
   :rdfs/domain :validation/Test
   :rdfs/range :validation/TestFunction
   :rdfs/comment
   "Asserts a query against some model whose bindings will be tested"
   ]
  [:validation/TestFunction :proto/elaborates :proto/Function
   :proto/elaborates :proto/Function
   :proto/hasParameter :validation/commentTemplate
   :rdfs/comment
   "A function applied to the output of the query of some test whose output is a map. A successful test outcome has {:validation/status :validation/Success}. Other outcomes have keys specific to that test. Output of this function provides bindings for the comment template."
   ]
  [:validation/commentTemplate
   :proto/aggregation :proto/Occlusive
   :rdfs/domain :validation/Test
   :rdfs/comment "A selmer template keyed to the values of a binding in some binding test describing the resulting issue upon failure of said test."
   ]

  [:validation/ResultsTest
   :proto/elaborates :validation/TestFunction
   :proto/argumentList [:?test-model :?test-name :?results]
   :rdfs/comment
   "A test applied to the set of results to the <query> of some test, rather than any specific binding within the output of said query."
   ]
  [:validation/BindingTest
   :proto/elaborates :validation/TestFunction
   :proto/argumentList [:?model :?bmap]
   :rdfs/comment
   "A test linked to a function applied to the binding of some test query."
   ]
  [:validation/ExpectNonEmpty
   :proto/elaborates :validation/ResultsTest
   :igraph/compiledAs
   (fn [test-model test-name results]
     (if (empty? results)
       {:validation/status :validation/ExpectedBindings
        :query (the (test-model test-name))
        }
       {:validation/status :validation/Success}))
   :validation/commentTemplate "Expected non-empty result for '{{query}}'"
   :rdfs/comment "Record an issue if a binding test returns nothing"
   ]
  [:validation/ExpectEmpty
   :proto/elaborates :validation/BindingTest
   :igraph/compiledAs
   (fn [model bmap]
     (assoc bmap
            :validation/status :validation/NoBindingsExpected))
   :validation/commentTemplate "Expected no bindings."
   :rdfs/comment "Record an issue if a binding test applied to <model> returns any <bmap>"
   ]
  
  #_[:validation/testFunction
   :rdf/type :validation/TestProperty
   :proto/aggregation :proto/Exclusive
   :rdfs/comment "Asserts the function definition for some named binding test.
Should be (fn [model bmap]...) -> <annotated bmap>. 
:validation/status other than:validation/Success implies an issue."
   ]

   ])

(def ontology @ontology-ref)
