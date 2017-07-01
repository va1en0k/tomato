(ns tomato.figures
  (:require clojure.string))

(defprotocol ToSVG
  (to-svg [this]))

;(extend-protocol ToSVG
;  PersistentVector
;  (to-svg [s] s))


(defprotocol ToPath
  (to-path [this]))

(defprotocol ToBezier
  (to-bezier [this]))

(defn svg-d [& cmds]
  (clojure.string/join " " (map #(if (keyword? %) (name %)
                                                  (str %))
                                (flatten cmds))))

(defrecord Path [d]
  ToSVG
  (to-svg [this]
    [:path {:d (svg-d d)}]))

(defrecord OneBezier [x1 p1 p2 x2]
  ToPath
  (to-path [this]
    (->Path [:M x1 :C p1 p2 x2]))
  ToSVG
  (to-svg [this]
    (to-svg (to-path this))))

(defn bezier [x1 p1 p2 x2]
  (->OneBezier x1 p1 p2 x2))



;(defrecord Circle [r [x y]]
;  )
