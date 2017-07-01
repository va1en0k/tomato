(ns tomato.core
  (:require [rum.core :as rum]
            [cljs.js]
            [promesa.core :as p :include-macros true]
            [replumb.common]
            [cljsjs.parinfer]
            [linked.core :as linked]

            [tomato.eval :as ev]
            [tomato.figures :as f]))

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
                    (ns tomato.user
                      (:require [tomato.figures :as f]))

                    (defn circle [r [cx cy]]
                     [:circle {:r  r
                               :cx cx
                               :cy cy}])

                    (circle 10 [10 20] 1)
                    (ciarcle 10 [16 25])

                    (f/->OneBezier [10 10] [39 73] [145 34] [206 16])

                    ")))}))

(defonce cursor (atom nil))


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
  (when-let [v (maybe-read str)]
    [:g {:key key} v]))


(def keyed-by-first-arg {:key-fn #(identity %)})


(defn get-selected-forms [code-state cursor]
  (when-let [[snp-key [start _]] cursor]
    (when-let [{:keys [source]} (snp-key (:snippets code-state))]
      (indexed-by #(map % [:snippet :pos])
                  (map #(assoc % :snippet snp-key)
                       (ev/forms-around-pos source start))))))


(defn handle-select [key [start end]]
  (reset! cursor [key [start end]]))


(rum/defc movable-circle < keyed-by-first-arg rum/reactive
  [key drag-n-drop-target [x y] cb]

  (let [color (if (= (first (rum/react drag-n-drop-target)) key)
                "red"
                "green")
        on-change cb]
    [:circle {:cx            x
              :cy            y
              :r             5
              :fill          color
              :stroke        color
              :on-mouse-down #(reset! drag-n-drop-target [key on-change])}]))

(defn replace-form! [key new-value]
  ;(println key new-value)
  (when-let [{:keys [snippet pos] old-form-source :source} ((get-selected-forms @code-state @cursor) key)]
    (when-let [{:keys [source]} (snippet (:snippets @code-state))]
      (let [new-source (str
                         (subs source 0 pos)
                         new-value
                         (subs source (+ pos (count old-form-source))))]
        (println source)
        (println new-source)
        (println old-form-source "->" (str new-value))
        (swap! code-state assoc-in
               [:snippets snippet :source]
               new-source)
        ;(handle-select snippet [pos pos])
        ))))

(rum/defc selected-elements < rum/reactive
  [drag-n-drop-target]

  (let [els (get-selected-forms (rum/react code-state) (rum/react cursor))]
    [:g
     (for [[key {:keys [form] :as e}] els]
       (cond
         (and (vector? form) (= (count form) 2) (every? number? form))
         (movable-circle key drag-n-drop-target form #(replace-form! key %))

         :default nil))]))

(defn get-mouse-position-in-svg [e]
  (let [svg (loop [el (.-target e)]
              (if (= "svg" (.-tagName el))
                el
                (recur (.-parentNode el))))
        bounds (.getBoundingClientRect svg)]
    [(- (.. e -clientX) (.-left bounds))
     (- (.. e -clientY) (.-top bounds))]))


(rum/defcs drawing-area < rum/reactive (rum/local nil ::drag-n-drop-target)
  [{::keys [drag-n-drop-target]}]
  [:svg
   {:style         {:border "1px solid" :width "99%" :height "100%"}
    :on-mouse-up   #(reset! drag-n-drop-target nil)
    :on-mouse-move #(when-let [[key cb] @drag-n-drop-target]
                      (cb (get-mouse-position-in-svg %)))}

   (for [[key e] (rum/react element-values)]
     (try
       (maybe-figure key (:value e))
       (catch js/Error e
         nil)))

   (selected-elements drag-n-drop-target)])


(defn show-success [v]
  [:pre {:style {:background-color "#b0f0a6"
                 :max-height       "5em"
                 :padding          "2px"}}
   (str "=> " (:value v))])

(defn show-fail [v]
  [:pre {:style {:background-color "#fe7c66"
                 :max-height       "5em"
                 :padding          "2px"}}
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




(rum/defc form-editor < rum/reactive
  [key form-atom value-atom]
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
       :on-select #(handle-select key (get-selection (.-target %)))
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
   ;[:div {:style {:order "2"}} (dbg-atom selected-forms)]
   ])


(rum/mount
  (app-area)
  (js/document.getElementById "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  )
