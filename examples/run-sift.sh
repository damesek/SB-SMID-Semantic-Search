#!/bin/bash
# Run benchmarks for SB-SIMD

cd ..  # Go to sb-simd root directory

echo "🎯 SB-SIMD Benchmark Suite"
echo ""

# Small test (1K vectors)
echo "1️⃣ Running Small test (1,000 × 128D)..."
clojure -cp "simsimd.jar:$(clojure -Spath)" \
        -M -m sb-simd.benchmark Small

# SIFT tests
echo ""
echo "2️⃣ Running SIFT-10K test (10,000 × 128D)..."
clojure -cp "simsimd.jar:$(clojure -Spath)" \
        -M -m sb-simd.benchmark SIFT-10K

echo ""
echo "3️⃣ Running SIFT-100K test (100,000 × 128D)..."
java -Xmx2G -cp "simsimd.jar:$(clojure -Spath)" \
     clojure.main -m sb-simd.benchmark SIFT-100K
