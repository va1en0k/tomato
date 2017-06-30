(ns tomato.eval
  (:require [cljs.reader]
            [promesa.core :as p :include-macros true]
            [replumb.core :as rpl]
            [tomato.eval.io]))

(defn eval-str [s]
  (p/promise
    (fn [resolve reject]
      (println "EVAL STR:" s)
      (rpl/read-eval-call
        (rpl/options :browser
                     ["/src/cljs" "/js/compiled/out"]
                     tomato.eval.io/fetch-file!)
        resolve
        ;#(if (rpl/success? %)
        ;   (resolve %)
        ;   (reject %))
        s))))

(defn sequential-async-map [f coll]
  (if (empty? coll)
    (p/promise [])
    (p/alet [fs (p/await (f (first coll)))
             rs (p/await (sequential-async-map f (rest coll)))]
            (cons fs rs))))


(defn code-to-forms [code]
  (cljs.reader/read-string (str "[" code "\n]")))

(defn eval-forms [forms]
  (println "damin?" forms)
  (sequential-async-map
    eval-str
    (map str forms)))

(defn eval-code [code]
  (eval-forms (code-to-forms code)))





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
