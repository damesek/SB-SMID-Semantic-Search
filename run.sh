#!/bin/bash
# Run the SB-SIMD vector search

echo "🎯 Starting SB-SIMD Vector Search..."
clojure -cp "simsimd.jar:$(clojure -Spath)" -M:run "$@"
