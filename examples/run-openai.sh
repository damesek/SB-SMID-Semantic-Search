#!/bin/bash
# Run OpenAI dimension benchmarks (1536D)

cd ..  # Go to sb-simd root directory

echo "🎯 OpenAI Dimension Benchmarks (1536D)"
echo ""

# OpenAI-10K
echo "1️⃣ Running OpenAI-10K test (10,000 × 1536D)..."
java -Xmx2G -cp "simsimd.jar:$(clojure -Spath)" \
     clojure.main -m sb-simd.benchmark OpenAI-10K

echo ""
# OpenAI-100K (requires significant memory)
echo "2️⃣ Running OpenAI-100K test (100,000 × 1536D)..."
echo "⚠️  This requires ~600MB RAM"
java -Xmx6G -cp "simsimd.jar:$(clojure -Spath)" \
     clojure.main -m sb-simd.benchmark OpenAI-100K

echo ""
echo "✅ OpenAI benchmarks complete"
