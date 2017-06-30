(ns tomato.core
  (:require [rum.core :as rum]
            [cljs.js]
            [promesa.core :as p :include-macros true]

            [tomato.eval :as ev]
            [linked.core :as linked]))

(enable-console-print!)


(defonce app-state
         (atom
           {:forms (linked/map
                     'circle {:form '(defn circle [r [cx cy]]
                                       [:circle {:r  r
                                                 :cx cx
                                                 :cy cy}])}
                     (gensym) {:form '(circle 10 [10 20])}
                     (gensym) {:form '(circle 10 [16 25])})}))

(defonce element-values
         (ev/async-map-atom ::element-values
                            (fn [state]
                              (p/map
                                #(into (linked.core/map)
                                       (map vector (keys (:forms state)) %))
                                (ev/eval-forms (map (comp :form second) (:forms state)))))
                            app-state))

(defn maybe-read [str]
  (try
    (cljs.reader/read-string str)
    (catch js/Error e
      nil)))


(defn maybe-figure [key str]
  (if-let [v (maybe-read str)]
    (do (println str)
        [:g {:key key} v])
    nil))


(rum/defc drawing-area < rum/reactive []
  [:svg
   {:style {:border "1px solid" :width "99%" :height "100%"}}

   (for [[key e] (rum/react element-values)]
     (try
       (maybe-figure key (:value e))
       (catch js/Error e
         nil)))])

(rum/defc form-editor < rum/reactive {:key-fn #(identity %)}
  [key form-atom]
  (let [change-code (fn [v]
                      (swap! form-atom assoc :form v))]
    [:textarea
     {:style     {:width "100%" :height "100%"}
      :on-change #(change-code (.. % -target -value))
      :value     (str (:form (rum/react form-atom)))}]))

(rum/defc code-editor < rum/reactive []
  [:div
   (for [form (:forms (rum/react app-state))]
     (form-editor (first form) (rum/cursor-in app-state [:forms (first form)])))])


(rum/defc app-area []
  [:div {:style {:display "flex" :height "300px"}}
   [:div {:style {:flex "0 0 65%"}} (drawing-area)]
   [:div {:style {:flex "1"}} (code-editor)]])


(rum/mount
  (app-area)
  (js/document.getElementById "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  (println (:elements @app-state))
  )
