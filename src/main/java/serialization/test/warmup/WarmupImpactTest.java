package serialization.test.warmup;

import serialization.test.model.Product;
import serialization.test.generator.DataGenerator;
import serialization.test.serializer.Serializer;
import serialization.test.serializer.JsonSerializer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WarmupImpactTest {
    
    private static final int TOTAL_ITERATIONS = 1000;
    private static final String OUTPUT_DIR = "warmup_results";
    
    static class DataPoint {
        int iteration;
        double serTime;
        double deserTime;
        double e2eTime;
        int messageSize;
        
        DataPoint(int iteration, double serTime, double deserTime, double e2eTime, int messageSize) {
            this.iteration = iteration;
            this.serTime = serTime;
            this.deserTime = deserTime;
            this.e2eTime = e2eTime;
            this.messageSize = messageSize;
        }
        
        String toCsvRow() {
            return String.format("%d,%.2f,%.2f,%.2f,%d",
                iteration, serTime, deserTime, e2eTime, messageSize);
        }
        
        static String getCsvHeader() {
            return "iteration,ser_time_ns,deser_time_ns,e2e_time_ns,message_size_bytes";
        }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("\nWarmup Impact Test - JSON Serialization");
            
            // Create output directory
            Paths.get(OUTPUT_DIR).toFile().mkdirs();
            System.out.println("Results will be saved to: " + OUTPUT_DIR + "/\n");
            
            // Generate test data (small payload)
            List<Product> testData = DataGenerator.generateSmallPayload();
            System.out.println("Payload: " + testData.size() + " products (SMALL)");
            System.out.println("Total iterations per experiment: " + TOTAL_ITERATIONS + "\n");
            
            // Create JSON serializer
            Serializer serializer = new JsonSerializer();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Experiment 1: No Warmup - measure 1000 iterations directly
            System.out.println("Experiment 1: Without Warmup");
            System.out.println("----------------------------------------");
            List<DataPoint> noWarmupResults = runExperimentNoWarmup(serializer, testData);
            exportResults(noWarmupResults, timestamp + "_no_warmup_json.csv");
            System.out.println("  Results saved to: " + timestamp + "_no_warmup_json.csv\n");
            
            // Experiment 2: With Warmup - 1000 warmup + 1000 measured
            System.out.println("Experiment 2: With Warmup");
            System.out.println("----------------------------------------");
            List<DataPoint> withWarmupResults = runExperimentWithWarmup(serializer, testData);
            exportResults(withWarmupResults, timestamp + "_with_warmup_json.csv");
            System.out.println("  Results saved to: " + timestamp + "_with_warmup_json.csv\n");
            
            System.out.println("\nTest completed!");
            System.out.println("\nTwo CSV files have been created in " + OUTPUT_DIR + "/");
            System.out.println("  1. *_no_warmup_json.csv  - 1000 iterations without warmup");
            System.out.println("  2. *_with_warmup_json.csv - 1000 iterations after 1000 warmup");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static List<DataPoint> runExperimentNoWarmup(Serializer serializer, List<Product> data) throws Exception {
        List<DataPoint> results = new ArrayList<>();
        
        System.out.print("  Running " + TOTAL_ITERATIONS + " iterations without warmup");
        for (int iter = 1; iter <= TOTAL_ITERATIONS; iter++) {
            DataPoint dp = measureIteration(serializer, data, iter);
            results.add(dp);
            if (iter % 200 == 0) System.out.print(".");
        }
        System.out.println(" done");
        
        return results;
    }
    
    private static List<DataPoint> runExperimentWithWarmup(Serializer serializer, List<Product> data) throws Exception {
        List<DataPoint> results = new ArrayList<>();
        
        // Warmup phase - 1000 iterations without measuring
        System.out.print("  Warmup: 1000 iterations");
        for (int i = 0; i < 1000; i++) {
            runSilentIteration(serializer, data);
            if ((i + 1) % 200 == 0) System.out.print(".");
        }
        System.out.println(" done");
        
        // Force garbage collection to clean memory from warmup
        System.out.print("  Cleaning memory");
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(100);
            System.out.print(".");
        }
        System.out.println(" done");
        
        // Small pause to let system stabilize
        Thread.sleep(500);
        
        // Measurement phase - 1000 iterations measuring
        System.out.print("  Measuring: " + TOTAL_ITERATIONS + " iterations (after warmup)");
        for (int iter = 1; iter <= TOTAL_ITERATIONS; iter++) {
            DataPoint dp = measureIteration(serializer, data, iter);
            results.add(dp);
            if (iter % 200 == 0) System.out.print(".");
        }
        System.out.println(" done");
        
        return results;
    }
    
    private static DataPoint measureIteration(Serializer serializer, List<Product> data, int iteration) throws Exception {
        int hash = 0;
        
        // Force GC before first iteration to reduce initial picos
        if (iteration == 1) {
            System.gc();
            Thread.sleep(50);
        }
        
        // Measure serialization
        long startSer = System.nanoTime();
        byte[] serialized = serializer.serialize(data);
        long endSer = System.nanoTime();
        
        // Measure deserialization
        long startDeser = System.nanoTime();
        List<Product> deserialized = serializer.deserialize(serialized);
        long endDeser = System.nanoTime();
        
        // Measure end-to-end
        long startE2E = System.nanoTime();
        byte[] ser = serializer.serialize(data);
        byte[] copy = new byte[ser.length];
        System.arraycopy(ser, 0, copy, 0, ser.length);
        List<Product> e2eResult = serializer.deserialize(copy);
        long endE2E = System.nanoTime();
        
        // Consume results to prevent JIT optimization
        hash += Arrays.hashCode(serialized) + deserialized.size() + e2eResult.size();
        
        return new DataPoint(
            iteration,
            (endSer - startSer),
            (endDeser - startDeser),
            (endE2E - startE2E),
            serialized.length
        );
    }
    
    private static void runSilentIteration(Serializer serializer, List<Product> data) throws Exception {
        byte[] serialized = serializer.serialize(data);
        List<Product> deserialized = serializer.deserialize(serialized);
        // Prevent optimization
        if (deserialized.isEmpty()) {
            System.out.print("");
        }
    }
    
    private static void exportResults(List<DataPoint> results, String filename) throws Exception {
        String fullPath = OUTPUT_DIR + "/" + filename;
        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            writer.println(DataPoint.getCsvHeader());
            for (DataPoint dp : results) {
                writer.println(dp.toCsvRow());
            }
        }
    }
}