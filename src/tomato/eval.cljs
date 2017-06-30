(ns tomato.eval
  (:require [cljs.reader]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :as rt]
            [cljs.tools.reader.impl.utils :refer [whitespace?]]
            [clojure.string :as string]
            [promesa.core :as p :include-macros true]
            [replumb.core :as rpl]
            [tomato.eval.io]))

(defn eval-str [s]
  (p/promise
    (fn [resolve reject]
      ;(println "EVAL STR:" s)
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
  (sequential-async-map
    eval-str
    (map str forms)))


(defn eval-sources [sources]
  (sequential-async-map
    eval-str
    (map str sources)))

(defn eval-code [code]
  (eval-forms (code-to-forms code)))

(defn split-to-forms [code]
  (loop [rr (rt/source-logging-push-back-reader code)
         forms []]
    (if-let [form (r/read rr false nil)]
      (recur rr (conj forms {:code (meta form)
                             :form form}))
      forms)))



(defn async-map-atom [key f a]
  ;; f : value -> promise
  (let [na (atom)
        upd (fn [v]
              (promesa.core/then (f v) #(reset! na %)))]
    (upd @a)
    (add-watch a key #(upd %4))
    na))


(defn maybe [fn]
  (try
    (fn)
    (catch js/Error e
      nil)))


(defn forms-around-pos [source pos]
  (loop [i 0
         vs []]
    (if (> i pos)
      vs
      (let [s (subs source i)
            rr (rt/source-logging-push-back-reader s)
            frm (maybe #(r/read rr false nil))]
        (if (nil? frm)
          (recur (inc i) vs)
          (let [src (rt/peek-source-log @(.-frames rr))
                good? (and (not (whitespace? (first src)))
                           (>= (+ i (count src)) pos)
                           (or (empty? vs)
                               (-> vs first :source (string/ends-with? src) not)))
                vs' (if good?
                      (cons {:source src
                             :form frm
                             :pos i} vs)
                      vs)]
            (recur (inc i) vs')))))))


