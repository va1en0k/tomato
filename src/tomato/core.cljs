(ns tomato.core
  (:require [rum.core :as rum]
            [cljs.js]
            [promesa.core :as p :include-macros true]
            [replumb.common]
            [cljsjs.parinfer]

            [tomato.eval :as ev]
            [linked.core :as linked]
            ))

(enable-console-print!)

(defn indexed-by [key forms]
  (into (linked.core/map) (for [f forms]
                            [(key f) f])))

(defonce code-state
         (atom {:snippets (indexed-by :sym
                                      (map #(assoc % :sym (gensym)
                                                     :source (:source (:code %)))
                                           (ev/split-to-forms
                                             "
                    (defn circle [r [cx cy]]
                     [:circle {:r  r
                               :cx cx
                               :cy cy}])

                    (circle 10 [10 20] 1)
                    (ciarcle 10 [16 25])

                    ")))}))

(defonce selected-forms
         (atom nil))


(def element-values
  (ev/async-map-atom ::element-values
                     (fn [state]
                       (p/map
                         #(into (linked.core/map)
                                (map vector (keys (:snippets state)) %))
                         (ev/eval-sources (map (comp :source second) (:snippets state)))))
                     code-state))

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


(rum/defc movable-circle [[x y] cb]
  [:circle {:cx     x
            :cy     y
            :r      5
            :fill   "red"
            :stroke "red"}])


(rum/defc selected-elements < rum/reactive []
  (let [els (rum/react selected-forms)]
    [:g
     (for [{:keys [form] :as e} els]
       (cond
         (and (vector? form) (= (count form) 2)) (movable-circle form println)
         :default nil))]))



(rum/defc drawing-area < rum/reactive []
  [:svg
   {:style {:border "1px solid" :width "99%" :height "100%"}}

   (for [[key e] (rum/react element-values)]
     (try
       (maybe-figure key (:value e))
       (catch js/Error e
         nil)))

   (selected-elements)])


(defn show-success [v]
  [:pre {:style {:background-color "#b0f0a6"
                 :max-height       "5em"}}
   (str "=> " (:value v))])

(defn show-fail [v]
  [:pre {:style {:background-color "#fe7c66"
                 :max-height       "5em"}}
   (println v)
   (replumb.common/extract-message true (:error v))])

(defn show-result [v]
  (cond
    (nil? v) nil
    (:success? v) (show-success v)
    :default (show-fail v)))

(defn line-count [code]
  (inc (count (filter #(= % "\n") code))))


(defn get-selection [el]
  (if el
    [(.-selectionStart el)
     (.-selectionEnd el)]
    nil))


(defn set-selection! [el sel]
  (when (and el sel)
    (set! (.-selectionStart el) (first sel))
    (set! (.-selectionEnd el) (second sel))))


(defn handle-select [key source [start end]]
  (reset! selected-forms (map #(assoc % :snippet key)
                              (ev/forms-around-pos source start))))


(rum/defc form-editor < rum/reactive
  [key form-atom value-atom]
  (println @form-atom)
  (let [change-source! (fn [v]
                         (swap! form-atom assoc :source v))
        form (rum/react form-atom)
        source (:source form)
        result (rum/react value-atom)
        error? (not (or (nil? result) (:success? result)))]
    [:div {:style {:margin-bottom "10px"}}
     [:textarea
      {:style     {:width      "100%"
                   :height     (-> source line-count inc (* 1.1) (min 10) (str "em"))
                   :display    "block"
                   :box-sizing "border-box"}
       :on-change #(change-source! (.. % -target -value))
       :on-select #(handle-select key source (get-selection (.-target %)))
       :value     source}]
     [:div
      (if (:warning result)
        [:pre
         {:style {:background-color "#f4ff65" :padding "2px"}}
         (:warning result)]
        nil)
      (show-result result)]]))

(rum/defc code-editor < rum/reactive []
  [:div
   (for [[key form] (:snippets (rum/react code-state))]
     (rum/with-key
       (form-editor
         key
         (rum/cursor-in code-state [:snippets key])
         (rum/cursor-in element-values [key]))
       key))])


(rum/defc dbg-atom < rum/reactive [a]
  [:div (str (rum/react a))])

(rum/defc app-area []
  [:div {:style {:display "flex" :flex-flow "row wrap" :height "300px"}}
   [:div {:style {:flex "0 0 45%"}} (drawing-area)]
   [:div {:style {:flex "1 1 55%" :width "200px"}} (code-editor)]
   [:div {:style {:order "2"}} (dbg-atom selected-forms)]])


(rum/mount
  (app-area)
  (js/document.getElementById "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  (println (:elements @code-state))
  )
