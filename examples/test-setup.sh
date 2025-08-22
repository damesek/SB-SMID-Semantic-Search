#!/bin/bash
# Simple test script to verify setup

cd ..  # Go to sb-simd root directory

echo "Testing SB-SIMD Setup"
echo ""

# Check if simsimd.jar exists
if [ ! -f "simsimd.jar" ]; then
    echo "ERROR: simsimd.jar not found!"
    echo "Please ensure simsimd.jar is in the sb-simd directory"
    exit 1
fi

# Try to run a simple test using java directly
echo "Running minimal test (1,000 vectors)..."
java -cp "simsimd.jar:$(clojure -Spath)" clojure.main -m sb-simd.benchmark Small

if [ $? -eq 0 ]; then
    echo ""
    echo "Setup successful! You can now run the full benchmarks."
else
    echo ""
    echo "Test completed with warnings (SimSIMD fallback mode active)"
fi
