(ns SWdeCasteljau.core2
  (:gen-class)
  (:use (guiftw swing events)
	(SWdeCasteljau math)
	(clojure pprint)
	(clojure.contrib (string :only (replace-str))))
  (:import (guiftw.swing Canvas CanvasListener)
           (javax.swing JLabel JFrame JSlider JToggleButton
                        SwingUtilities)
	   (java.awt.event MouseListener MouseMotionListener MouseEvent
			   ActionListener)
           (java.awt Color RenderingHints)
	   (javax.swing.event ChangeListener)
           net.miginfocom.swing.MigLayout))

(def *initial-gui-state* {:animator (agent nil)})

(defn refresh-info [gui]
  (.setText (-> @gui :ids :info)
	    (str "<html><b>Punkty:</b><br>"
		 (apply print-str
			 (interleave
			  (for [row @*points*]
			    (apply print-str
				   (interleave (map (fn [[x y]] (format "%3.0f, %3.0f" (float x) (float y))) row)
						(repeat ";"))))
			  (repeat "<br><hr>")))
		 "</html>")))

(defn mouse-dragged [gui event]
  (let [moving-points  (:moving-points @gui)]
    (if (-> moving-points second first)
      (let [p (list (max 0 (.getX event)) (max 0 (.getY event)))]
	(reset! *points* (de-casteljau @*t* (concat (first moving-points)
						    [p]
						    (rest (second moving-points)))))
	(recalc-curve)))
    (.repaint (.getComponent event))
    (refresh-info gui)))

(defn mouse-down [gui event]
  (let [moving-points (:moving-points @gui)
	ex (.getX event)
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
      (swap! gui
	     #(assoc % :moving-points
		     (cond (and create-or-delete (first (second found-points))) ;; delete
			   (do (reset! *points* (de-casteljau @*t* (concat (first found-points)
									   (rest (second found-points)))))
			       (recalc-curve)
			       nil)

			   create-or-delete ;; create
			   [(first found-points) [[ex ey]]]

			   true ;; move
			   found-points)))
      (mouse-dragged gui event))))

(defn update-density
  ([gui] (.setText (-> @gui :ids :density-label)
		   (str "<html><b>Dokładność:</b> "
			@*density*
			"</html>")))
  ([gui event]
     (let [value (.. event getSource getValue)]
       (reset! *density* value))
     (recalc-curve)
     (.repaint (-> @gui :ids :paint-panel))
     (update-density gui)))

(defn update-t
  ([gui] (.setText (-> @gui :ids :t-label)
		   (str "<html><b>t = </b>"
			(format "%1.3f" @*t*)
			"</html>")))
  ([gui event]
     (reset! *t* (double (/ (.. event getSource getValue) 1000)))
     (update-t gui)
     (recalc-points)
     (refresh-info gui)
     (.repaint (-> @gui :ids :paint-panel))))

(def *T* 4000)
(def *start*  (System/currentTimeMillis))

(defn t-step [_ gui]
  (when (-> @gui :animate)
    (reset! *t* (/ (+ 1 (Math/sin (* 2 Math/PI (- 0.5 (double (/ (mod (- (System/currentTimeMillis) *start*) *T*) *T*)))))) 2))
    (recalc-points)
    (SwingUtilities/invokeLater
     #(do (update-t gui)
	  (.repaint (-> @gui :ids :paint-panel))))
    (Thread/sleep 16 666)
    (send (-> @gui :animator) t-step gui)))


(defn paint-canvas [_ event]
  (let [g (.getGraphics event)
        widget (.getComponent event)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_ANTIALIASING
                         RenderingHints/VALUE_ANTIALIAS_ON)
      (.setColor Color/WHITE)
      (.fillRect 0 0 (.getWidth widget) (.getHeight widget))
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
    (dorun (map (fn [[x y]] (.drawOval g (- x 5) (- y 5) 10 10)) (first @*points*)))))

(defn toggle-animation [gui event]
  (let [animate  (.. event getSource isSelected)]
    (swap! gui #(assoc % :animate animate))
    (.setEnabled (-> @gui :ids :t-slider) (not animate))
    (when animate
      (send (-> @gui :animator) t-step gui))))

(def window (swing [JFrame [:title "SW: de Casteljau"
                            :default-close-operation JFrame/EXIT_ON_CLOSE
			    :layout (MigLayout. "wrap 2"
						"0[grow] [300!,grow,fill]"
						"0[top] [top] [top] [top] [top] [top] [top,grow]0")
			    :size ^unroll (800 700)
			    :visible true]
		    [Canvas [:*id :paint-panel
                             :*lay "span 1 7, grow"
                             :canvas+paint paint-canvas
                             :mouse++pressed mouse-down
                             :mouse-motion+mouse-dragged mouse-dragged]]
		    [JLabel [:text (str "<html><b>Obsługa:</b><br>Lewy klawisz: przesuwaj punkty<br>"
					"Prawy klawisz: dodawaj/usuwaj punkty</html>")]]
		    [JLabel [:*id :density-label :text "density"]]
		    [JSlider [:*id :density-slider
			      :change+state-changed update-density
			      :minimum 1, :value 10, :maximum 100]]
		    [JLabel [:*id :t-label :text "t"]]
		    [JSlider [:*id :t-slider
			      :change+state-changed update-t
			      :minimum 0, :value 500, :maximum 1000]]
		    [JToggleButton [:*id :toggle-animation
				    :change+state-changed toggle-animation
				    :text "Animuj"]]
		    [JLabel [:*id :info :text "Info"]]]))

(defn -main [& args]
  (set-laf "Nimbus")
  (recalc-points)
  (recalc-curve)
  (doto (window (atom *initial-gui-state*))
    (-> deref :root .validate)
    refresh-info
    update-t
    update-density))
