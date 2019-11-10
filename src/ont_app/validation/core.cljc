(ns ont-app.validation.core
  (:require
   ;; third party libs
   [selmer.parser :as selmer]
   ;; local libs
   [ont-app.igraph.core :as igraph
    :refer [
            add
            normalize-flat-description
            query
            ]
    ]
   [ont-app.igraph-vocabulary.core :as igv
    :refer [
            mint-kwi
            ]]
   [ont-app.prototypes.core :as proto
    :refer [
            get-description
            ]
    ]
   [ont-app.validation.ont :as ont]
   [ont-app.vocabulary.core :as voc]
   
   ))

(voc/cljc-put-ns-meta!
 'ont-app.validation.core
 {
  :voc/mapsTo 'ont-app.validation.ont
  }
 )

(def ontology ont/ontology)

;; FUN WITH READER MACROS
#?(:cljs
   (enable-console-print!)
   )
#?(:cljs
   (defn on-js-reload [] )
   )
;; NO READER MACROS BEYOND THIS POINT

(def the igraph/unique)

;;;;;;;;;;;;;;;;;;;
;; MODEL CHECKING
;;;;;;;;;;;;;;;;;;

(defn add-issue [model test-id bmap]
  "Returns `model` with an issue recorded for `test-id` based on `bmap`
Where
<model> is the IGraph under scrutiny
<test-id> names the test which generated <bmap>
<bmap> is a binding from a test query, possibly annotated after the fact.
"
  (let [issue (mint-kwi ::Issue :bmap (str (hash bmap)))
        ]
    (add model {test-id {:validation/isssue #{issue}}
                issue (normalize-flat-description bmap)})))

(defn add-comment-to-issue [test-model binding-test issue]
  "Returns `issue`, annotated per `binding-test`'s commentTemplate in `test-model`"
  (if-let [comment-template (the (test-model binding-test
                                             :validation/commentTemplate))
           ]
    (assoc issue
           :rdfs/comment (selmer/render
                          comment-template
                          issue))
    ;; else no template
    issue))

^:reduce-fn
(defn collect-test-result [test-model test-id binding-test model bmap]
  "Returns `model`, annotated with an issue for `bmap` if it is not successful
Where
<model> is the IGraph under scrutiny
<bmap> is a binding from the <test query>, which <binding-test> will scrutinize
<test-query> is a query in the native format for <model>, specified in <test-model>
<binding-test> names a binding test for <test-id>, associated with a <test-fn>
<test-id> names the test associated with <test query>
<test-model> is an IGraph containing <test-query> <binding-test> for <test-id>
<test-fn> := (fn [model bmap] ...)-> <bmap'>
<bmap'> {:status ..., ...}, <bmap> annotated for status and issue-specific values
<status> is :validation/success if there are no issues, and otherwise names an issue.
"
  (let [_bm bmap
        test-fn (the (test-model binding-test :igraph/compiledAs))
        add-comment (partial add-comment-to-issue test-model binding-test)

        ]
    (assert test-fn)
    (if (not (= (:validation/status (test-fn model bmap))
                :validation/Success))
      (add-issue model test-id
                 (add-comment
                  (assoc bmap
                         :rdf/type :validation/Issue
                         :validation/testID test-id
                         :validation/bindingTest binding-test
                         )))
      ;; else success, and no issue...
      model)))

^:reduce-fn
(defn collect-results-test-issues [test-model test-id test-query-results
                                   model results-test]
  "Returns `model`' annotated for `results-test`"

  (let [f (the (test-model test-id :igraph/compiledAs))
        ]
    (collect-test-result test-model test-id results-test
                         model
                         (f test-model test-id test-query-results))))

  
^:reduce-fn
(defn collect-binding-test-issues [test-model test-id test-query-results
                                   model binding-test]
  "Returns `model`, annotated for `binding-test`
Where
<model> is the IGraph under scrutiny
<binding-test> names a binding test for <test-id>, associated with a <test-fn>
<test-id> names a test associated with some <test-query>
<test-fn> := (fn [model bmap] ...)-> <bmap'>
<bmap> is a binding returned 
<bmap'> {:status ..., ...} 
<status> is :success if there are no issues, and otherwise names an issue.
"
  
  (reduce (partial collect-test-result test-model test-id binding-test)
          model
          test-query-results))

  
^:reduce-fn  
(defn collect-issues [test-model model test-id]
  "Returns `model`, possibly annotated per `test-id`
Where
<model> is the IGraph under scrutiny
<test-id> names a test associated with some <test-query> and <binding test>
<test-query> is a query in the native format for <model>, specified in
  <test-model>
<binding-test> names a binding test for <test-id>, associated with a <test-fn>
<test-fn> := (fn [model bmap] ...)-> <bmap'>
<bmap> is a binding returned 
<bmap'> {:status ..., ...} 
<status> is :success if there are no issues, and otherwise names an issue.
"
  #dbg
  (let [desc (proto/get-description test-model test-id)
        results (query model (the (:validation/query desc)))
        ]

    (reduce (partial collect-binding-test-issues test-model test-id results)
            (reduce (partial collect-results-test-issues test-model
                             test-id results)
                    model
                    (desc :validation/resultsTest))
            (desc :validation/bindingTest))))

(defn report-test [test-model target-model test-id]
  (let [g (collect-issues test-model target-model test-id)
        ]
    (igraph/normal-form (igraph/difference g target-model))))

(defn check-model [test-model model]
  "returns `model`, annotated with issues per `task-model`
"
  (let [test-bindings (query test-model [[:?test :validation/query :?query]])
        ]
    (reduce (partial collect-issues test-model)
            model
            (map :?test test-bindings))))

    
