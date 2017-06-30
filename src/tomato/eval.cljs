(ns tomato.eval
  (:require [promesa.core :as p :include-macros true]
            [tomato.figures]))



(defn eval-form [form state]
  (p/promise
    (fn [resolve reject]
      (binding [cljs.js/*eval-fn* #(do (println %) (cljs.js/js-eval %))
                cljs.js/*load-fn* #()]
        (println form)
        (cljs.js/eval state form {:ns      'tomato.user
                                  :context :statement}
                      #(do
                         (println %1 %2)
                         (println @state)
                         (resolve %1)))))))


(defn eval-forms
  ([elements] (eval-forms elements (atom)))
  ([elements state]
   (if (empty? elements)
     (p/promise [])
     (p/alet [fs (p/await (eval-form (first elements) state))
              rs (p/await (eval-forms (rest elements) state))]
             (cons fs rs)))))

(defn eval-elements [elements]
  (eval-forms (cons '(ns tomato.user
                       (:require [tomato.figures :as f]))
                    elements)))

(defn async-map-atom [key f a]
  ;; f : value -> promise
  (let [na (atom)
        upd (fn [v]
              (promesa.core/then (f v) #(reset! na %)))]
    (upd @a)
    (add-watch a key #(upd %4))
    na))


(def example-1 ['(ns tomato.user)
                ;'(+ 1 2)
                ;'(+ 3 4)
                '(def a 5)
                '(inc a)])
