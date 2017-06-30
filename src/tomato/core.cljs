(ns tomato.core
  (:require [rum.core :as rum]
            [cljs.js]

            [tomato.eval :as ev]))

(enable-console-print!)


(defonce app-state
         (atom
           {:elements ['(f/square)]}))

(defonce element-values (ev/async-map-atom ::element-values
                                           (comp ev/eval-elements :elements)
                                           app-state))

(rum/defc drawing-area < rum/reactive []
  (println (rum/react element-values))
          [:svg ;{:width "600px" :height "200px"}
           (for [e (:elements (rum/react element-values))]

                  [:text (str e)]

                  )])

(rum/defc app-area []
  [:div {:style {:display "flex"}}


   [:div {:style {:flex "0 0 65%"}} (drawing-area)]
   [:div {:style {:flex "1"}} "hi"]

   ])


(rum/mount (app-area) (js/document.getElementById "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  (println (:elements @app-state))
  )
