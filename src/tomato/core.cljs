(ns tomato.core
  (:require [rum.core :as rum]
            [cljs.js]

            [tomato.eval :as ev]))

(enable-console-print!)


(defonce app-state
         (atom
           {:code "
           (ns tomato.user
            (:require [tomato.figures :as f]))

           (+ 1 2)

           (def sq (f/square))

           "}))

(defonce element-values (ev/async-map-atom ::element-values
                                           (comp ev/eval-code :code)
                                           app-state))

(rum/defc drawing-area < rum/reactive []
  [:svg
   {:style {:border "1px solid" :width "99%" :height "100%"}}

   (for [e (rum/react element-values)]
     (try
       (cljs.reader/read-string (:value e))
       (catch js/Error e
         nil)))])

;[:textarea ]

(rum/defc code-editor < rum/reactive []
  (let [change-code (fn [v]
                      (swap! app-state assoc :code v))]
    [:textarea
     {:style     {:width "100%" :height "100%"}
      :on-change #(change-code (.. % -target -value))}
     (:code (rum/react app-state))]))

(rum/defc app-area []
  [:div {:style {:display "flex" :height "300px"}}

   [:div {:style {:flex "0 0 65%"}} (drawing-area)]
   [:div {:style {:flex "1"}} (code-editor)]

   ])


(rum/mount (app-area) (js/document.getElementById "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  (println (:elements @app-state))
  )
