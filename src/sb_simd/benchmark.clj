(ns sb-simd.benchmark
  "Benchmark tests for SB-SIMD with various dataset sizes"
  (:require [sb-simd.core :as simd]
            [clojure.java.io :as io])
  (:import [java.util Random]))

(set! *warn-on-reflection* true)

;; Test configurations
(def test-configs
  [{:name "Small" :size 1000 :dim 128}
   {:name "SIFT-10K" :size 10000 :dim 128}
   {:name "SIFT-100K" :size 100000 :dim 128}
   {:name "BERT-10K" :size 10000 :dim 768}
   {:name "BERT-100K" :size 100000 :dim 768}
   {:name "OpenAI-10K" :size 10000 :dim 1536}
   {:name "OpenAI-100K" :size 100000 :dim 1536}])

(defn generate-random-vector
  "Generate normalized random vector"
  [^long dim ^Random rng]
  (let [vec (float-array dim)]
    (dotimes [i dim]
      (aset vec i (float (.nextGaussian rng))))
    ;; Normalize
    (let [norm (Math/sqrt (areduce vec i sum 0.0 
                                   (+ sum (* (aget vec i) (aget vec i)))))]
      (when (> norm 0.0)
        (dotimes [i dim]
          (aset vec i (float (/ (aget vec i) norm))))))
    vec))

(defn generate-dataset
  "Generate synthetic dataset"
  [size dim]
  (let [rng (Random. 42)]
    (vec (repeatedly size #(generate-random-vector dim rng)))))

(defn run-benchmark
  "Run benchmark for a specific configuration"
  [{:keys [name size dim]}]
  (println "\n" (apply str (repeat 60 "=")))
  (println (format "📊 %s: %,d vectors × %d dimensions" name size dim))
  (println (apply str (repeat 60 "=")))
  
  ;; Generate data
  (print "Generating dataset... ")
  (flush)
  (let [start (System/nanoTime)
        data (generate-dataset size dim)
        gen-time (/ (- (System/nanoTime) start) 1000000.0)]
    (println (format "✓ (%.1f ms)" gen-time))
    
    ;; Generate queries
    (let [queries (generate-dataset 100 dim)]
      
      ;; Warmup
      (print "Warming up... ")
      (flush)
      (dotimes [_ 10]
        (simd/search {:embeddings data
                     :query (first queries)
                     :k 10
                     :dimension dim}))
      (println "✓")
      
      ;; Measure performance
      (let [num-queries (if (> size 50000) 10 100)
            _ (println (format "Running %d queries..." num-queries))
            times (vec (for [i (range num-queries)]
                        (let [query (nth queries (mod i 100))]
                          (:time-ms (simd/search {:embeddings data
                                                  :query query
                                                  :k 10
                                                  :dimension dim})))))]
        
        ;; Calculate statistics
        (let [sorted-times (sort times)
              avg (/ (reduce + times) num-queries)
              p50 (nth sorted-times (quot num-queries 2))
              p90 (nth sorted-times (int (* 0.9 num-queries)))
              p99 (if (>= num-queries 100)
                   (nth sorted-times (int (* 0.99 num-queries)))
                   (last sorted-times))
              qps (/ 1000 avg)]
          
          (println "\n📈 RESULTS:")
          (println (format "  Search P50:  %.2f ms" p50))
          (println (format "  Search P90:  %.2f ms" p90))
          (println (format "  Search P99:  %.2f ms" p99))
          (println (format "  Average:     %.2f ms" avg))
          (println (format "  QPS:         %.0f" qps))
          (println (format "  Recall:      100%% (exact search)"))
          
          ;; Memory usage
          (let [runtime (Runtime/getRuntime)
                _ (.gc runtime)
                used-mb (/ (- (.totalMemory runtime) (.freeMemory runtime)) 
                          1048576.0)]
            (println (format "  Memory:      %.1f MB" used-mb))))))))

(defn -main [& args]
  (println "\n🎯 SB-SIMD BENCHMARK SUITE")
  
  ;; Check SimSIMD
  (if simd/simd-available?
    (println "✅ SimSIMD native library available")
    (println "⚠️ SimSIMD native library not found - using fallback Java implementation"))
  
  ;; Run selected tests
  (let [test-name (first args)
        configs (if test-name
                 (filter #(= (:name %) test-name) test-configs)
                 (take 3 test-configs))] ; Default: first 3 tests
    
    (if (empty? configs)
      (do
        (println "\nAvailable tests:")
        (doseq [config test-configs]
          (println (format "  %s" (:name config))))
        (println "\nUsage: clojure -M:benchmark [test-name]"))
      
      (do
        (println (format "\nRunning %d tests..." (count configs)))
        (doseq [config configs]
          (run-benchmark config)))))
  
  (.shutdown simd/executor)
  (System/exit 0))
