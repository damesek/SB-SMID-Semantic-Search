# SB-SIMD Semantic ENN Search Engine 
### *A stateless semantic Exact NN engine for dynamic subsets.*

Perfect for workloads where every query might apply heavy filters, producing unique subsets, or even stream-like data that changes every second. ANN indexes struggle here, but SB-SIMD stateless ENN can simply rebuild on the fly. Eg. ideal for streams, RAG environments, complex website filters.


----

I have a pet project where I’ve been struggling a lot with one particular challenge: *semantic dynamic search*.
The essence of similarity search is that it doesn’t look for exact matches, but for the closest results.

For example, if a user types “I love white sneakers”, the engine doesn’t just look for the exact keyword “white sneaker”, but understands semantic similarity. It can also return products described as “beloved white sneaker” by others, or where the description only says “casual sneaker white”.

Now, think about how many options this opens up in an e-commerce webshop. *In my case, perhaps even more — since the data can also come dynamically through graphs.*

Result is  0.5 - 5 ms overhead per query vs no index time.

👉 What you see here is the solution I’ve built for this pet project. 

----

<br>
Perhaps you’ve already encountered the **HNSW filtering problem**.  
HNSW is extremely fast for large static datasets, but as soon as you apply **dynamic filters** (e.g. `WHERE user_id=...` or `category=...`), performance degrades dramatically:

- The index is built globally, not per-filter.  
- When you restrict to a subset, the graph connectivity is no longer optimal.  
- The engine often ends up scanning far more nodes than expected, so query latency can jump from **<1 ms → 20–100 ms**.  
- Workarounds (pre-building per-filter indexes, or post-filtering results) are either memory-intensive or increase latency further.  

Why this matters

This is why **HNSW works great for global, static similarity search**,  
but it’s not ideal for **highly dynamic, filter-heavy workloads**.

By contrast, a **stateless ENN (Exact Nearest Neighbor) engine**, implemented here in **Clojure**, can rebuild on the fly for each filtered subset. Overload max 0,5ms - 5ms in normal cases. 

That makes it practical for use cases with:
- rapidly changing datasets,  
- stream-like data,  
- or queries that always narrow the search space dynamically.  

## ✅ Best fit use cases

- **Small / medium subsets**  (< 100k, max. 700ms ~ 1Mx768D)

  When user queries always filter down the space to 1k–10k vectors, the rebuild + search fits easily within low latency budgets.  
<br>

- **Extreme filtering / rapidly changing custom subsets** 

  Perfect for workloads where every query might apply heavy filters, producing unique subsets, or even stream-like data that changes every second.  
  (ANN indexes struggle here, but a stateless ENN can simply rebuild on the fly.)

<br>

- **Ephemeral / ad-hoc searches**

  Ideal for real-time logs, sensor data, or session-based datasets where the relevant subset changes constantly and indexing on-the-fly is more efficient.  
<br>

- **Edge / mobile / microservices**

  Lightweight in-memory search when Postgres, Milvus, or Qdrant are too heavy.  
<br>

- **Rapid prototyping / experimentation**

  When you need quick iteration without maintaining a persistent index.

<br>


---

## ⚠️ Less suitable

- **Very large, stable datasets**  

  Millions of vectors (> 1M), where persistent ANN engines dominate with <10 ms query times.  
<br>

- **Database-first environments**  

  When transactional guarantees, SQL integration, or auditability are required.

<br>

- **Not a competitor to pgvector or Milvus, but rather a niche complement:**

  “A stateless ENN engine for dynamic subsets.”  
<br>  

## Performance

### Scaling Performance

![SB-SIMD Scaling Performance](research/sb_simd_scaling_loglog.png)

### Rebuild Cost Comparison

![ANN Rebuild Total Cost](research/ann_rebuild_total_cost_bert100k.png)

### Benchmark Results

With SimSIMD Native Library on Apple M-series (10 threads):

| Dataset | Vectors | Dimensions | Search Time | QPS |
|---------|---------|------------|-------------|-----|
| SIFT | 10K | 128 | 0.99ms | 829 |
| SIFT | 100K | 128 | 3.90ms | 210 |
| SIFT | 1M | 128 | 38.83ms | 23 |
| BERT | 10K | 768 | 0.95ms | 564 |
| BERT | 100K | 768 | 9.88ms | 98 |
| OpenAI | 10K | 1536 | 1.33ms | 729 |
| OpenAI | 100K | 1536 | 13.62ms | 72 |
| Bible | 31K | 768 | 2.83ms | 345 |

## Installation

### Prerequisites

1. Java 11+
2. Clojure CLI tools

### Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/sb-simd.git
cd sb-simd
```

## Usage

### Quick Start

```bash
# Run benchmarks
java -cp "classes:simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark

# Run specific test
java -cp "classes:simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark SIFT-100K
```

### As a Library

```clojure
(require '[sb-simd.core :as simd])

;; Load embeddings
(def index (simd/create-index "data/embeddings.json"))

;; Search
(def results (simd/search {:embeddings (:embeddings index)
                           :query query-vector
                           :k 10
                           :dimension 768}))

;; Results include similarity scores and indices
(:results results) ; => [[0.95 123] [0.94 456] ...]
(:time-ms results) ; => 3.14
(:qps results)     ; => 318
```

## Data Format

Embeddings should be in JSON format:

```json
[
  {
    "id": "item1",
    "text": "Example text",
    "embedding": [0.1, 0.2, ...]
  },
  ...
]
```

Or with metadata:

```json
{
  "metadata": {...},
  "verses": [
    {
      "id": "item1",
      "text": "Example text",
      "embedding": [0.1, 0.2, ...]
    }
  ]
}
```

## Architecture

SB-SIMD uses a brute-force approach with several optimizations:

1. **SIMD Operations** - Hardware-accelerated cosine similarity via SimSIMD
2. **Parallel Search** - Dataset split across threads for concurrent processing
3. **Heap-based Top-K** - Efficient tracking of best matches
4. **Zero-copy Arrays** - Direct memory operations on float arrays



## Comparison with Other Methods

| Method | Build Time | Search@100K | Recall | Update |
|--------|------------|-------------|--------|--------|
| SB-SIMD | 0ms | 3.9ms | 100% | Instant |
| FAISS-Flat | 100ms | 7.7ms | 100% | Rebuild |
| HNSW | 8.3s | 0.15ms | 99% | Incremental |
| Annoy | 611ms | 0.05ms | 95% | Rebuild |

## Examples

See the `examples/` directory for benchmark scripts:

```bash
cd examples
./run-all.sh    # Run all benchmarks
./run-sift.sh   # SIFT dimension tests
./run-bert.sh   # BERT dimension tests
```

## Native Libraries

The project includes pre-compiled native libraries for macOS ARM64:
- `libsimsimd.dylib` - SimSIMD core library
- `libsimsimdjni.dylib` - JNI wrapper

For other platforms, the system falls back to pure Java implementation (slower but functional).

## License

MIT

## Credits

- SimSIMD for SIMD operations
- Clojure community for the amazing language
