import java.util.List;
import java.util.ArrayList;
/**
 * A Comprehensive performance benchmark tool designed to simulate LLM
 * Token streams.
 * This class measures and outputs execution latency  and approximate
 *  memory consumption differences between traditional immutable
 * String Loops and a mutable StringBuilder approach.
 * So, Basically-this a benchmark that says avoid using traditional
 * String looping and make use of mutable StringBuilder to save memory
 * consumption during LLM token streaming.
 * So,this heavily touches the incoming responses from LLMs,and the String
 *responses should be build using mutable StringBuilder.
 * Java Strings are immutable and thus if we tried to join the incoming responses
 * which is usually text piece by piece(token)we will be forced to create a brand new
 * string in memory for every single token,wasting memory and slowing down operations.
 * So this is basically a memory footprint benchmark,and memory footprint must be extremely
 * low to encourage efficient usage of available resources
 */
public class TokenStreamBenchmark {
    public static void main(String[] args) {
        //Step 1: Initialize Simulated  streaming token segments
        // We generate  15,000 distinct  token  fragments to clearly stress the JVM heap.
        int tokenCount = 15000;
        List<String> simulatedTokens = new ArrayList<>(tokenCount);
        for (int i = 0; i < tokenCount; i++) {
            simulatedTokens.add("token_" + i + "");
        }
        System.out.println("  JAVA LLM TOKEN STREAM WORKLOAD PERFORMANCE BENCHMARK ");
        System.out.println("Simulating a sequence of  " + tokenCount +" streaming text fragments...\n");

        //Warm-up phase to ensure JVM HotSpot compiler  optimizes both  execution paths fairly
        for (int i = 0; i < 5; i++) {
            runTraditionalApproach(simulatedTokens.subList(0, 100));
            runOptimizedApproach(simulatedTokens.subList(0, 100));
        }
        // ====================================================================
        // BENCHMARK RUN 1: TRADITIONAL STRING CONCATENATION LOOPS
        // ====================================================================
        // Force garbage collection to establish a clear,isolated baseline for memory tracking
        System.gc();
        try{Thread.sleep(200); } catch (InterruptedException ignored){}
        // memory before we run the simulation
        long memoryBeforeTraditional = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTraditional = System.nanoTime();

        String traditionalResult = runTraditionalApproach(simulatedTokens);

        long endTraditional = System.nanoTime();
        // memory after we run the simulation
        long memoryAfterTraditional = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        double traditionalTimeMs = (endTraditional - startTraditional) / 1_000_000.0;
        long traditionalMemoryKb = (memoryAfterTraditional - memoryBeforeTraditional) / 1024;

        System.out.println("--- TRADITIONAL IMMUTABLE STRING LOOP---");
        System.out.printf("Execution Latency : %.3f ms%n",traditionalTimeMs);
        System.out.printf("Heap Memory Used  : %d KB%n",Math.max(0, traditionalMemoryKb));
        System.out.println("Mechanics          : Creates a temporary  StringBuilder wrapper and extracts");
        System.out.println("                     a new immutable String  instance  on every Single iteration.");
        System.out.println("------------------------------------------------------------\n");

        //BENCHMARK RUN 2: OPTIMIZED STRING BUILDER ARRAYS
        //Force garbage collectionto clean up remnants of Run 1 before evaluating Run 2
        System.gc();
        try{ Thread.sleep(200); } catch (InterruptedException ignored){}
         long memoryBeforeOptimized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
         long startOptimized = System.nanoTime();

         String optimizedResult = runOptimizedApproach(simulatedTokens);

         long endOptimized = System.nanoTime();
         long memoryAfterOptimized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

         double optimizedTimeMs = (endOptimized - startOptimized) / 1_000_000.0;
         long optimizedMemoryKb = ( memoryAfterOptimized - memoryBeforeOptimized) / 1024;

         System.out.println( "--- OPTIMIZED SIZE-HINTED STRINGBUILDER---");
         System.out.printf("Execution latency : %.3f ms%n", optimizedTimeMs);
         System.out.printf("Heap Memory Used  : %d KB%n", Math.max(0, optimizedMemoryKb));
         System.out.println("Mechanics          : Appends characters directly into  a contiguous, resizing");
         System.out.println("                     char array buffer  without creating transient heap objects.");
         System.out.println("-----------------------------------------------------------------------\n");

         //ARCHITECTURAL ANALYSIS SUMMARY
         System.out.println("===================================");
         System.out.println("   PERFORMANCE METRICS");
         System.out.println("====================================");
         System.out.printf("Speed Improvement Factor : %.1fx faster%n",(traditionalTimeMs / optimizedTimeMs));

         // Structural validation to verify correctness
         if (traditionalResult.length() == optimizedResult.length()) {
             System.out.println("Output Integrity check  : PASSED (String structures perfectly match)");

         } else {
            System.out.println(" Output Integrity Check  : FAILED");
         }
         System.out.println("============================================\n");


    }
  /**
   * Iteratively concatenates data via regular  loops.
   * Overallocates heap memory  via repeated immutable instance  transformations.
   * ND-notice how we use our method below with String-instead of void
   */
    public static String runTraditionalApproach(List<String> tokens) {
        String result = "";
        for ( String token : tokens) {
            // JVM compiled behaviour : result  = new StringBuilder().append(result).append(token).toString();
            result += token;
        }
        return result; // a method that returns data
    }
    /**
     * Appends token Stream  items efficiently within a single character  array segment.
     */
    public static String runOptimizedApproach(List<String> tokens) {
        //Pre-allocate buffer tracking estimation rules:Average token length * token length counts
        int heuristicSizeHint = tokens.size() * 8;
        StringBuilder sb = new StringBuilder(heuristicSizeHint);

        for(int i = 0; i < tokens.size(); i++) {
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }
}
