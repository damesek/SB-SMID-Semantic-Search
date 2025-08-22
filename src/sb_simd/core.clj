(ns sb-simd.core
  "SB-SIMD: High-performance brute-force vector search with SIMD acceleration"
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.util.concurrent Executors ExecutorService]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; Configuration
(def ^:const NUM-THREADS 10)
(def ^:const K 10)
(def ^ExecutorService executor (Executors/newFixedThreadPool NUM-THREADS))

;; ============================================================================
;; SimSIMD Integration with Fallback
;; ============================================================================

(def simd-available? 
  (try
    (Class/forName "lightning_index.SimSIMDNative")
    true
    (catch Exception _ false)))

(defn cosine-similarity
  "Calculate cosine similarity with fallback to pure Java"
  [^floats a ^floats b ^long dim]
  (if simd-available?
    (try
      (lightning_index.SimSIMDNative/cosineF32 a b dim)
      (catch UnsatisfiedLinkError _
        ;; Fallback to pure Java
        (let [dot (areduce a i sum 0.0
                          (+ sum (* (aget a i) (aget b i))))]
          (float dot))))
    ;; Pure Java implementation
    (let [dot (areduce a i sum 0.0
                      (+ sum (* (aget a i) (aget b i))))]
      (float dot))))

;; ============================================================================
;; Core Search Implementation
;; ============================================================================

(defn search-chunk
  "Search a chunk of the dataset in parallel"
  [{:keys [embeddings query-array start-idx end-idx k dim]}]
  (let [scores (float-array k)
        indices (int-array k)
        heap-size (atom 0)]
    
    ;; Initialize scores
    (dotimes [i k]
      (aset scores i Float/NEGATIVE_INFINITY))
    
    ;; Search through chunk
    (doseq [i (range start-idx end-idx)]
      (let [embedding (nth embeddings i)
            score (cosine-similarity query-array embedding dim)]
        
        (cond
          ;; Heap not full
          (< (long @heap-size) (long k))
          (do
            (aset scores (int @heap-size) (float score))
            (aset indices (int @heap-size) (int i))
            (swap! heap-size inc))
          
          ;; Better than worst in heap
          (> score (double (aget scores 0)))
          (let [;; Find minimum
                min-idx (loop [j 1 min-j 0 min-val (aget scores 0)]
                          (if (< j (long k))
                            (let [val (aget scores j)]
                              (if (< val min-val)
                                (recur (inc j) j val)
                                (recur (inc j) min-j min-val)))
                            min-j))]
            (aset scores (int min-idx) (float score))
            (aset indices (int min-idx) (int i))))))
    
    {:scores scores :indices indices :size @heap-size}))

(defn parallel-search
  "Parallel brute-force search across all embeddings"
  [embeddings query-array k dim]
  (let [total (count embeddings)
        chunk-size (quot total NUM-THREADS)
        
        ;; Create parallel tasks
        tasks (mapv (fn [t]
                     (let [start (long (* t chunk-size))
                           end (if (= t (dec NUM-THREADS))
                                total
                                (long (* (inc t) chunk-size)))]
                       (reify java.util.concurrent.Callable
                         (call [_]
                           (search-chunk
                            {:embeddings embeddings
                             :query-array query-array
                             :start-idx start
                             :end-idx end
                             :k k
                             :dim dim})))))
                   (range NUM-THREADS))
        
        ;; Execute in parallel
        futures (.invokeAll executor tasks)
        chunk-results (mapv #(.get ^java.util.concurrent.Future %) futures)]
    
    ;; Merge results from all chunks
    (let [all-pairs (for [result chunk-results
                         i (range (:size result))]
                     [(aget ^floats (:scores result) i)
                      (aget ^ints (:indices result) i)])
          sorted (reverse (sort-by first all-pairs))]
      (vec (take k sorted)))))

;; ============================================================================
;; Data Loading
;; ============================================================================

(defn load-embeddings
  "Load embeddings from JSON file"
  [path]
  (println (format "📖 Loading embeddings from %s..." path))
  (with-open [reader (io/reader path)]
    (let [data (json/read reader)
          verses (get data "verses" data)  ; Handle both formats
          embeddings (mapv (fn [item]
                            (float-array (get item "embedding")))
                          verses)]
      (println (format "✅ Loaded %d vectors" (count embeddings)))
      {:embeddings embeddings
       :metadata (mapv #(dissoc % "embedding") verses)
       :dimension (count (first embeddings))})))

;; ============================================================================
;; Public API
;; ============================================================================

(defn search
  "Search for k most similar vectors to query"
  [{:keys [embeddings query k dimension]
    :or {k K}}]
  (let [start (System/nanoTime)
        results (parallel-search embeddings query k dimension)
        elapsed (/ (- (System/nanoTime) start) 1000000.0)]
    {:results results
     :time-ms elapsed
     :qps (/ 1000 elapsed)}))

(defn create-index
  "Create searchable index from embeddings file"
  [embeddings-path]
  (load-embeddings embeddings-path))

;; ============================================================================
;; Main Demo
;; ============================================================================

(defn -main [& args]
  (println "\n🎯 SB-SIMD VECTOR SEARCH")
  
  ;; Check SimSIMD
  (if simd-available?
    (println "✅ SimSIMD available")
    (println "⚠️ SimSIMD not available - using fallback Java implementation"))
  
  ;; Load data
  (let [data-path (or (first args) "data/bible_embeddings_complete.json")
        {:keys [embeddings metadata dimension]} (create-index data-path)]
    
    ;; Demo search
    (println "\n🔍 Demo search with first vector...")
    (let [query (first embeddings)
          {:keys [results time-ms qps]} (search {:embeddings embeddings
                                                 :query query
                                                 :k 10
                                                 :dimension dimension})]
      
      (println (format "\n⚡ Search completed in %.2f ms" time-ms))
      (println (format "🚀 Throughput: %.0f searches/sec" qps))
      
      (println "\n🎯 TOP 10 RESULTS:")
      (doseq [[idx [score vec-idx]] (map-indexed vector results)]
        (let [verse (nth metadata vec-idx)]
          (println (format "%2d. [%.4f] %s: %s"
                          (long (inc idx))
                          score
                          (get verse "id")
                          (subs (get verse "text") 0 (min 70 (count (get verse "text")))))))))))
  
  (.shutdown executor)
  (System/exit 0))
