    package serialization.test.benchmark;

    import serialization.test.model.Product;
    import serialization.test.generator.DataGenerator;
    import serialization.test.serializer.Serializer;
    import serialization.test.serializer.JsonSerializer;
    import serialization.test.serializer.ProtobufSerializer;
    import java.util.*;
    import java.text.DecimalFormat;

    public class SerializationBenchmark {
        
        private static final int TRIALS = 10;
        
        // Iteration constants per payload size
        private static final int ITERATIONS_SMALL = 1_000_000;   // 5 items
        private static final int ITERATIONS_MEDIUM = 10_000;   // 100 items
        private static final int ITERATIONS_LARGE = 1_000;     // 10_000 items
        
        static class TrialResult {
            double serializationTime;
            double deserializationTime;
            double endToEndTime;
            int messageSize;
            
            TrialResult(double ser, double deser, double e2e, int size) {
                this.serializationTime = ser;
                this.deserializationTime = deser;
                this.endToEndTime = e2e;
                this.messageSize = size;
            }
        }
        
        static class BenchmarkResult {
            String format;
            String payloadSize;
            double avgSerialization;
            double avgDeserialization;
            double avgEndToEnd;
            double avgSize;
            double stdSerialization;
            double stdDeserialization;
            double stdEndToEnd;
            double stdSize;
            
            BenchmarkResult(String format, String payloadSize, List<TrialResult> trials) {
                this.format = format;
                this.payloadSize = payloadSize;
                
                double[] serTimes = trials.stream().mapToDouble(t -> t.serializationTime).toArray();
                double[] deserTimes = trials.stream().mapToDouble(t -> t.deserializationTime).toArray();
                double[] e2eTimes = trials.stream().mapToDouble(t -> t.endToEndTime).toArray();
                double[] sizes = trials.stream().mapToDouble(t -> t.messageSize).toArray();
                
                this.avgSerialization = calculateMean(serTimes);
                this.avgDeserialization = calculateMean(deserTimes);
                this.avgEndToEnd = calculateMean(e2eTimes);
                this.avgSize = calculateMean(sizes);
                
                this.stdSerialization = calculateStd(serTimes);
                this.stdDeserialization = calculateStd(deserTimes);
                this.stdEndToEnd = calculateStd(e2eTimes);
                this.stdSize = calculateStd(sizes);
            }

            private double calculateCV(double std, double mean) {
                if (mean == 0) return 0;
                return (std / mean) * 100;
            }
            
            private double calculateMean(double[] values) {
                return Arrays.stream(values).average().orElse(0.0);
            }

            private String calculateDifference(double jsonValue, double protoValue) {
                double diff = ((jsonValue - protoValue) / jsonValue) * 100;
                if (diff > 0) {
                    return String.format("Proto %.1f%% faster", diff);
                } else {
                    return String.format("JSON %.1f%% faster", -diff);
                }
            }
            
            private double calculateStd(double[] values) {
                double mean = calculateMean(values);
                double variance = Arrays.stream(values)
                    .map(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0);
                return Math.sqrt(variance);
            }

            private String formatSize(double bytes) {
                if (bytes < 1024) {
                    return String.format("%.0f B", bytes);
                } else if (bytes < 1024 * 1024) {
                    return String.format("%.2f KB", bytes / 1024);
                } else {
                    return String.format("%.2f MB", bytes / (1024 * 1024));
                }
            }
            
            @Override
            public String toString() {
                DecimalFormat df = new DecimalFormat("#,##0.0");
                
                // Calculate coefficients of variation (%)
                double cvSer = calculateCV(stdSerialization, avgSerialization);
                double cvDeser = calculateCV(stdDeserialization, avgDeserialization);
                double cvE2E = calculateCV(stdEndToEnd, avgEndToEnd);
                
                return String.format(
                    "%-22s  %-18s\n" +
                    "  Serialization:   %12s ns  (σ = ±%5.1f%%)\n" +
                    "  Deserialization: %12s ns  (σ = ±%5.1f%%)\n" +
                    "  End-to-End:      %12s ns  (σ = ±%5.1f%%)\n" +
                    "  Size:            %10s\n",
                    format,
                    payloadSize,
                    df.format(avgSerialization), cvSer,
                    df.format(avgDeserialization), cvDeser,
                    df.format(avgEndToEnd), cvE2E,
                    formatSize(avgSize));
            }
        }
        
        public static void main(String[] args) {
            try {
                System.out.println("Serialization Benchmark\n");
                
                // Prepare data payloads
                Map<String, List<Product>> payloads = new LinkedHashMap<>();
                payloads.put("Small (5 items)", DataGenerator.generateSmallPayload());
                payloads.put("Medium (100 items)", DataGenerator.generateMediumPayload());
                payloads.put("Large (10,000 items)", DataGenerator.generateLargePayload());
                
                // Serializers to test
                List<Serializer> serializers = Arrays.asList(
                    new JsonSerializer(),
                    new ProtobufSerializer()
                );
                
                List<BenchmarkResult> results = new ArrayList<>();
                
                // Run benchmarks
                for (Map.Entry<String, List<Product>> entry : payloads.entrySet()) {
                    String payloadName = entry.getKey();
                    List<Product> data = entry.getValue();
                    
                    System.out.println("\n▶ Testing payload: " + payloadName);
                    
                    // Determine iterations based on payload size
                    int iterations = getIterationsForPayload(data.size());
                    System.out.println("  Iterations per trial: " + iterations);
                    
                    for (Serializer serializer : serializers) {
                        System.out.println("  " + serializer.getFormatName() + "...");
                        
                        // Warmup JIT (Uses the number of iterations determined for the payload size)
                        warmup(serializer, data, iterations);
                        
                        // Trials
                        List<TrialResult> trials = new ArrayList<>();
                        for (int trial = 0; trial < TRIALS; trial++) {
                            trials.add(runTrial(serializer, data, iterations));
                            System.out.print("    Trial " + (trial + 1) + "/" + TRIALS + " completed\r");
                        }
                        System.out.println();
                        
                        BenchmarkResult result = new BenchmarkResult(
                            serializer.getFormatName(), payloadName, trials);
                        results.add(result);
                    }
                }
                
                // Results
                System.out.println("\n\nFINAL RESULTS:\n");
                for (BenchmarkResult r : results) {
                    System.out.println(r.toString());
                }
                
                // Mostrar configurações usadas
                System.out.println("\nBenchmark Configuration:");
                System.out.println("Small payload (5 itens): " + ITERATIONS_SMALL + " iterations per trial");
                System.out.println("Medium payload (100 itens): " + ITERATIONS_MEDIUM + " iterations per trial");
                System.out.println("Large payload (10.000 itens): " + ITERATIONS_LARGE + " iterations per trial");
                System.out.println("Trials per configuration: " + TRIALS);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static int getIterationsForPayload(int size) {
            if (size <= 5) {
                return ITERATIONS_SMALL;
            } else if (size <= 100) {
                return ITERATIONS_MEDIUM;
            } else {
                return ITERATIONS_LARGE;
            }
        }
        
        private static void warmup(Serializer serializer, List<Product> data, int iterations) throws Exception {
            int hash = 0; // prevent JIT from eliminating code
            
            for (int i = 0; i < iterations; i++) {
                byte[] serializedData = serializer.serialize(data);
                List<Product> deserializedData = serializer.deserialize(serializedData);
                
                // Consume results to keep JIT from optimizing
                hash += Arrays.hashCode(serializedData) + deserializedData.size();
            }
            
            // Prevent JIT from optimizing away the entire loop
            if (hash == 0) {
                System.out.println("  (hash: " + hash + ")"); // This will never happen, but keeps the code "alive"
            }
        }
        
        private static TrialResult runTrial(Serializer serializer, List<Product> data, int iterations) throws Exception {
            int hash = 0; // prevent JIT from eliminating code
            
            // Serialization
            byte[] serSink = null;
            long startSer = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                serSink = serializer.serialize(data);
                hash += Arrays.hashCode(serSink); // consume result to prevent optimization
            }
            long endSer = System.nanoTime();
            double serTime = (endSer - startSer) / (double) iterations;
            
            // Measure message size
            int messageSize = serSink.length;
            
            // Deserialization
            List<Product> deserSink = null;
            long startDeser = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                deserSink = serializer.deserialize(serSink);
                hash += deserSink.size(); // consume result to prevent optimization
            }
            long endDeser = System.nanoTime();
            double deserTime = (endDeser - startDeser) / (double) iterations;
            
            // End-to-end (serialize + copy + deserialize)
            long startE2E = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                byte[] ser = serializer.serialize(data);
                byte[] copy = new byte[ser.length];
                System.arraycopy(ser, 0, copy, 0, ser.length);
                List<Product> e2eResult = serializer.deserialize(copy);
                hash += Arrays.hashCode(ser) + e2eResult.size(); // consume results to prevent optimizations
            }
            long endE2E = System.nanoTime();
            double e2eTime = (endE2E - startE2E) / (double) iterations;
            
            // Prevent JIT from optimizing away the entire trial
            if (hash == 0) {
                System.out.println("  (trial hash: " + hash + ")"); // Never happens, but keeps code alive
            }
            
            return new TrialResult(serTime, deserTime, e2eTime, messageSize);
        }
    }