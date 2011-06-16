(ns SWdeCasteljau.math)

(defn between [t v1 v2]
  (map #(+ (* (- 1 t) %1) (* t %2)) v1 v2))

(defn betweens [t vs]
  (map #(between t %1 %2) vs (rest vs)))

(defn de-casteljau [t vs]
  (take (count vs) (iterate (partial betweens t) vs)))

(defn approximate [vs density]
  (let [step (/ 1 density)]
    (for [i (range (inc density))]
      (first (last (de-casteljau (* step i) vs))))))

(def *t* (atom 0.5))

(def *points* (atom (de-casteljau @*t* [[10 10] [10 100] [100 10]])))

(def *density* (atom 10))
(def *curve* (atom nil))

(defn recalc-points []
  (reset! *points* (de-casteljau @*t* (first @*points*))))

(defn recalc-curve []
  (reset! *curve* (approximate (first @*points*) @*density*)))