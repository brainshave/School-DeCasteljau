(ns SWdeCasteljau.PaintPanel
  (:gen-class
   :extends javax.swing.JPanel)
  (:use SWdeCasteljau.math)
  (:import (java.awt Color RenderingHints)))

(defn -paintComponent [this g]
  (doto g
    (.setRenderingHint RenderingHints/KEY_ANTIALIASING
		       RenderingHints/VALUE_ANTIALIAS_ON)
    (.setColor Color/WHITE)
    (.fillRect 0 0 (.getWidth this) (.getHeight this))
    (.setColor Color/GRAY))
  (dorun (map #(dorun (map (fn [[x1 y1] [x2 y2]] (.drawLine g x1 y1 x2 y2))
			   % (rest %)))
	      @*points*))
  (.setColor g Color/BLACK)
  (let [approxims @*curve*]
    (dorun (map (fn [[x1 y1] [x2 y2]]
		  (.drawLine g x1 y1 x2 y2))
		approxims (rest approxims))))
  (let [[x y] (last (last @*points*))]
    (.setColor g Color/BLACK)
    (.drawOval g (- x 8) (- y 8) 16 16))
  (.setColor g Color/RED)
  (dorun (map (fn [[x y]] (.drawOval g (- x 5) (- y 5) 10 10)) (first @*points*))))
    