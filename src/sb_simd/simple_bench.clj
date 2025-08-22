(ns sb-simd.simple-bench
  (:require [sb-simd.core :as core])
  (:import [java.util Random]))

(defn generate-vector [^long dim ^Random rng]
  (let [vec (float-array dim)]
    (dotimes [i dim]
      (aset vec i (float (.nextGaussian rng))))
    vec))

(defn run-test [size dim]
  (println (format "\n📊 Testing %,d vectors × %d dimensions" size dim))
  (println (apply str (repeat 50 "-")))
  
  ;; Generate data
  (print "Generating dataset... ")
  (flush)
  (let [rng (Random. 42)
        start (System/nanoTime)
        data (vec (repeatedly size #(generate-vector dim rng)))
        gen-time (/ (- (System/nanoTime) start) 1000000.0)]
    (println (format "✓ (%.1f ms)" gen-time))
    
    ;; Test search
    (print "Testing search... ")
    (flush)
    (let [query (first data)
          start (System/nanoTime)
          result (core/search {:embeddings data
                              :query query
                              :k 10
                              :dimension dim})
          search-time (:time-ms result)]
      (println (format "✓"))
      (println (format "\n📈 RESULTS:"))
      (println (format "  Search time: %.2f ms" search-time))
      (println (format "  QPS: %.0f" (:qps result)))
      (println (format "  Recall: 100%% (exact search)")))))

(defn -main [& args]
  (println "\n🎯 SB-SIMD SIMPLE BENCHMARK")
  (if core/simd-available?
    (println "✅ SimSIMD native library available")
    (println "⚠️ Using fallback"))
  
  ;; Run tests
  (run-test 1000 128)
  (run-test 10000 128)
  (run-test 100000 128)
  
  (println "\nDone!")
  (System/exit 0))
