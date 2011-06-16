(ns SWdeCasteljau.core
  (:gen-class)
  (:use (guiftw swing events)
	(SWdeCasteljau math PaintPanel)
	clojure.pprint
	(clojure.contrib (string :only (replace-str))))
  (:import (javax.swing JLabel JFrame JSlider JToggleButton
			SwingUtilities)
	   (java.awt.event MouseListener MouseMotionListener MouseEvent
			   ActionListener)
	   (javax.swing.event ChangeListener)
	   SWdeCasteljau.PaintPanel
	   net.miginfocom.swing.MigLayout))


(def *moving-points* (atom nil))
(def *info-widget* (atom nil))

(defn refresh-info []
  (.setText @*info-widget*
	    (str "<html><b>Punkty:</b><br>"
		 (apply print-str
			 (interleave
			  (for [row @*points*]
			    (apply print-str
				   (interleave (map (fn [[x y]] (format "%3.0f, %3.0f" (float x) (float y))) row)
						(repeat ";"))))
			  (repeat "<br><hr>")))
		 "</html>")))

(def info (swing [JLabel []]))

(defn mouse-dragged [event]
  (if (-> @*moving-points* second first)
    (let [p (list (max 0 (.getX event)) (max 0 (.getY event)))]
      (reset! *points* (de-casteljau @*t* (concat (first @*moving-points*)
						 [p]
						 (rest (second @*moving-points*)))))
      (recalc-curve)))
  (.repaint (.getComponent event))
  (refresh-info))

(defn mouse-down [event]
  (let [ex (.getX event)
	ey (.getY event)]
    (let [;; punkt dotknięty znajdzie będzie pierwszą pozycją drugiej
	  ;; ze zwróconych list.
	  threshold 5
	  found-points (split-with (fn [[x y]] (not (and (< x (+ ex 5))
							 (> x (- ex 5))
							 (< y (+ ey 5))
							 (> y (- ey 5)))))
				   (first @*points*))
	  create-or-delete (= (.getButton event) MouseEvent/BUTTON3)]
      (reset! *moving-points*
	      (cond (and create-or-delete (first (second found-points))) ;; delete
		    (do (reset! *points* (de-casteljau @*t* (concat (first found-points)
								   (rest (second found-points)))))
			(recalc-curve)
			nil)
		    
		    create-or-delete ;; create
		    [(first found-points) [[ex ey]]]
		    
		    true ;; move
		    found-points))
      (mouse-dragged event))))


(defn mouse-up [event]
  )

(def *paint-panel* (atom nil))
(def *density-label* (atom nil))
(defn update-density
  ([event]
     (let [value (.. event getSource getValue)]
       (reset! *density* value))
     (recalc-curve)
     (.repaint @*paint-panel*)
     (update-density))
  ([] (.setText @*density-label* (str "<html><b>Dokładność:</b> "
				       @*density*
				       "</html>"))))

(def window (swing [JFrame [:title "SW: de Casteljau"
			    :default-close-operation JFrame/EXIT_ON_CLOSE
			    :layout (MigLayout. "wrap 2"
						"0[grow] [300!,grow,fill]"
						"0[top] [top] [top] [top] [top] [top] [top,grow]0")
			    :size ^unroll (800 700)
			    :visible true]]))
(def paint-panel (swing
		    [PaintPanel [:mouse++pressed mouse-down
				 :mouse++released mouse-up
				 :mouse-motion+mouse-dragged mouse-dragged
				 :*lay "span 1 7, grow"]]))

(def help-label (swing
		 [JLabel [:text "<html><b>Obsługa:</b><br>Lewy klawisz: przesuwaj punkty<br>Prawy klawisz: dodawaj/usuwaj punkty</html>"]]))

(def density-label (swing [JLabel []]))

(def density-slider (swing
		     [JSlider [:minimum 1
			       :value 10
			       :maximum 100
			       :change+state-changed update-density]]))

(def *t-label* (atom nil))
(defn update-t
  ([] (.setText @*t-label* (str "<html><b>t = </b>"
				(format "%1.3f" @*t*)
				"</html>")))
  ([event]
     (reset! *t* (double (/ (.. event getSource getValue) 1000)))
     (update-t)
     (recalc-points)
     (refresh-info)
     (.repaint @*paint-panel*)))
(def t-label (swing [JLabel []]))
(def *t-slider* (atom nil))
(def t-slider (swing [JSlider [:minimum 0
			       :value 500
			       :maximum 1000
			       :change+state-changed update-t]]))
(def *t-animator* (agent 0.01))
(def *t-animate* (atom nil))

(def *T* 4000)
(def *start*  (System/currentTimeMillis))

(defn t-step [_]
  (when @*t-animate*
    (reset! *t* (/ (+ 1 (Math/sin (* 2 Math/PI (- 0.5 (double (/ (mod (- (System/currentTimeMillis) *start*) *T*) *T*)))))) 2))
    (recalc-points)
    (SwingUtilities/invokeLater
     #(do (update-t)
	  (.repaint @*paint-panel*)))
    (Thread/sleep 16 666)
    (send *t-animator* t-step)))
(defn toggle-animation [event]
  (let [animate  (.. event getSource isSelected)]
    (reset! *t-animate* animate)
    (when animate
      (.setEnabled @*t-slider* false)
      (send *t-animator* t-step))
    (when (not animate)
      (.setEnabled @*t-slider* true))))
(def t-animate (swing [JToggleButton [:text "Animuj"
				      :change+state-changed toggle-animation]]))

(defn -main [& args]
  (set-laf "Nimbus")
  (let [window (window nil)
	paint-panel (paint-panel window)
	help-label (help-label window)
	density-label (density-label window)
	density-slider (density-slider window)
	t-label (t-label window)
	t-slider (t-slider window)
	t-animate (t-animate window)
	info (info window)]
    (reset! *paint-panel* paint-panel)
    (reset! *density-label* density-label)
    (reset! *t-label* t-label)
    (reset! *t-slider* t-slider)
    (reset! *info-widget* info))
  (update-density)
  (update-t)
  (recalc-curve)
  (.repaint @*paint-panel*)
  (refresh-info))