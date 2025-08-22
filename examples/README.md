# SB-SIMD Examples

This folder contains benchmark examples for testing SB-SIMD performance with various dataset sizes and dimensions.

## Available Tests

| Test Name | Vectors | Dimensions | Memory Required |
|-----------|---------|------------|-----------------|
| Small | 1,000 | 128 | ~10 MB |
| SIFT-10K | 10,000 | 128 | ~50 MB |
| SIFT-100K | 100,000 | 128 | ~500 MB |
| BERT-10K | 10,000 | 768 | ~300 MB |
| BERT-100K | 100,000 | 768 | ~3 GB |
| OpenAI-10K | 10,000 | 1,536 | ~600 MB |
| OpenAI-100K | 100,000 | 1,536 | ~6 GB |

## Running Tests

### Quick Test
```bash
# From examples directory
chmod +x *.sh

# Run small test
./run-sift.sh
```

### Full Benchmark Suite
```bash
# Run all tests up to 100K vectors
./run-all.sh
```

### Individual Test Categories
```bash
# SIFT dimensions (128D)
./run-sift.sh

# BERT dimensions (768D)
./run-bert.sh

# OpenAI dimensions (1536D)
./run-openai.sh
```

### Custom Test
```bash
# Run specific test by name
cd ..
clojure -cp "simsimd.jar:$(clojure -Spath)" \
        -m sb-simd.examples.benchmark BERT-100K
```

## Expected Results

Based on Apple M-series hardware (10 threads):

| Test | Expected Search Time | QPS |
|------|---------------------|-----|
| SIFT-10K | ~0.2ms | 5000 |
| SIFT-100K | ~3ms | 295 |
| BERT-10K | ~0.8ms | 811 |
| BERT-100K | ~7ms | 140 |
| OpenAI-10K | ~1.2ms | 663 |
| OpenAI-100K | ~12ms | 75 |

## Memory Guidelines

- **< 2GB RAM**: Run only Small and 10K tests
- **4GB RAM**: Can run all except OpenAI-100K
- **8GB+ RAM**: Can run all tests comfortably

## Troubleshooting

If you encounter OutOfMemoryError:
1. Increase JVM heap size: `-Xmx4G`
2. Reduce dataset size in benchmark.clj
3. Close other applications

## Adding Custom Tests

Edit `benchmark.clj` and add your configuration to `test-configs`:

```clojure
{:name "MyTest" :size 50000 :dim 512}
```

Then run:
```bash
clojure -m sb-simd.examples.benchmark MyTest
```
