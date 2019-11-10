(ns ont-app.validation.doo
  (:require [doo.runner :refer-macros [doo-tests]]
            [ont-app.validation.core-test]
            ))

(doo-tests
 'ont-app.validation.core-test
 )
