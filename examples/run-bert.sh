#!/bin/bash
# Run BERT dimension benchmarks (768D)

cd ..  # Go to sb-simd root directory

echo "🎯 BERT Dimension Benchmarks (768D)"
echo ""

# BERT-10K
echo "1️⃣ Running BERT-10K test (10,000 × 768D)..."
java -Xmx1G -cp "simsimd.jar:$(clojure -Spath)" \
     clojure.main -m sb-simd.benchmark BERT-10K

echo ""
# BERT-100K
echo "2️⃣ Running BERT-100K test (100,000 × 768D)..."
java -Xmx4G -cp "simsimd.jar:$(clojure -Spath)" \
     clojure.main -m sb-simd.benchmark BERT-100K

echo ""
echo "✅ BERT benchmarks complete"
