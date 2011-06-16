(defproject SWdeCasteljau "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.0"]
		 [guiftw "0.2.0-SNAPSHOT"]
		 [com.miglayout/miglayout "[3.7,3.8)" :classifier "swing"]]
  :aot [#"SWdeCasteljau\..*"]
  :main SWdeCasteljau.core2
  :dev-dependencies [[swank-clojure "1.3.0"]])
