#!/bin/bash
# Run all benchmarks - simplified version

cd ..  # Go to sb-simd root directory

echo "SB-SIMD COMPLETE BENCHMARK SUITE"
echo ""

# Run tests using java directly to avoid clojure command issues
echo "Running Small test (1,000 vectors)..."
java -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark Small

echo ""
echo "Running SIFT-10K test (10,000 × 128D)..."
java -Xmx1G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark SIFT-10K

echo ""
echo "Running SIFT-100K test (100,000 × 128D)..."
java -Xmx2G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark SIFT-100K

echo ""
echo "Running BERT-10K test (10,000 × 768D)..."
java -Xmx2G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark BERT-10K

echo ""
echo "Running BERT-100K test (100,000 × 768D)..."
java -Xmx4G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark BERT-100K

echo ""
echo "Running OpenAI-10K test (10,000 × 1536D)..."
java -Xmx3G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark OpenAI-10K

echo ""
echo "Running OpenAI-100K test (100,000 × 1536D)..."
java -Xmx6G -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark OpenAI-100K

echo ""
echo "All benchmarks complete!"
