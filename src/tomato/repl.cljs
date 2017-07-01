(ns tomato.repl
  (:require [tomato.figures :as f]))

(defn handle-upper-form [v]
  (if (satisfies? f/ToSVG v)
    (f/to-svg v)
    v))