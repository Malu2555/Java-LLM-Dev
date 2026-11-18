package com.llm.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * A performace and memory benchmark simulating high-dimensional vector embeddings.
 * This class contrasts an object-oriented wrapper layout (List of ArrayList of floats)
 * against a flat,contigous 1D primitive array segment layout (float[]).
 * This is our starting point on understanding evaluation metrics for a java LLM developer role.
 * What we are targeting here is 1.-Cache Locality-How closely data is packed in physical memory.
 * Contigious memory  has a lesser memory overhead,2.Memory Footprint-Total bytes consumed by the data
 * in the JVM heap
 */

public class VectorProcessingBenchmark {
    //simulating 1000 embedding vectors(e.g pulling a batch from a vector store response)
    private static final int VECTOR_COUNT=1000;
    //1536 dimensions corresponding directly to modern models like OpenAI's text-embedding-3-small
    private static final int DIMENSIONS=1536;

    public static void main(String[] args){
        System.out.println(" JAVA VECTOR EMBEDDING MEMORY & LATENCY BENCHMARK  ");
        System.out.println("=====================================================\n");
        System.out.printf("simulating storage and processing of %d vectors(%d dimensions each)...%n%n",
            VECTOR_COUNT,DIMENSIONS);
        //Warm up  phase to trigger the HotSpot  JIT Compiler for balanced evaluations
        for (int i=0;i<3;i++){
            runObjectVectorWarmup();
            runPrimitiveVectorWarmup();

        }
        //BENCHMARK 1: REFERENCE-TYPE OBJECT WRAPPER VECTORS (List<List<Float>>)
        System.gc();
        try{Thread.sleep(200);} catch(InterruptedException ignored){}
        //Memory benchmark-for refernce type vs primitive types
        long memBeforeObject=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long startObject=System.nanoTime();// latency benchmark
        List<List<Float>> objectVectors=new ArrayList<>(VECTOR_COUNT);
        double objectDotProductSum = 0;

        //Allocation phase
        for (int i=0;i<VECTOR_COUNT;i++){
            List<Float> vector= new ArrayList<>(DIMENSIONS);
            for(int d=0;d<DIMENSIONS;d++){
                vector.add((float)(Math.random() * 2.0 - 1.0));
            }
            objectVectors.add(vector);
        }
        //Traversal & Processing Phase:Execute  Standard linear similarity computations
        for(int i = 0; i < VECTOR_COUNT-1;i++){
            objectDotProductSum += computeObjectDotProduct(objectVectors.get(i),objectVectors.get(i + 1));

        }
        long endObject = System.nanoTime();
        long memAfterObject =Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        double objectTimeMs=(endObject-startObject) /1_000_000.0;
        long objectMemkb=(memAfterObject-memBeforeObject)/1024;
        System.out.println("---REFERENCE-TYPE WRAPPER LAYOUT (List<List>float>>) ---");
        System.out.printf ("Execution latency : %.3f ms%n",objectTimeMs);
        System.out.printf("Heap Memory Used : %d KB%n",Math.max(0,objectMemkb));
        System.out.println("Mechanics     :Suffers from a 16-byte object header for every floating");
        System.out.println("               point entry,pointer chasing,and heavy unboxing costs.");

        //BENCHMARK 2:FLAT CONTIGOUS PRIMITIVE ARRAY LAYOUT (1D float[])
        // This should be implemented during ingestion,indexing and embedding of data in real production
        System.gc();
        try {Thread.sleep(200);} catch (InterruptedException ignored) {}
        long memBeforePrimitive = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long startPrimitive = System.nanoTime();

        //Allocate a single contigous primitive array holding all entries linearly
        float[]primitiveVectors = new float[VECTOR_COUNT*DIMENSIONS];
        double primitiveDotProductSum = 0;

        //Allocation phase
        for(int i = 0;i < primitiveVectors.length;i++){
            primitiveVectors[i]=(float)(Math.random() * 2.0 - 1.0);

        }
        //Traversal & Processing phase:Parallel scan leveraging linear math offsets
        for(int i = 0;i < VECTOR_COUNT-1; i++){
            primitiveDotProductSum +=computePrimitiveDotProduct(primitiveVectors, i, i + 1);
        }
        long endPrimitive=System.nanoTime();
        long memAfterPrimitive=Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        double primitiveTimeMs = (endPrimitive - startPrimitive) / 1_000_000.0;
        long primitiveMemKb=(memAfterPrimitive-memBeforePrimitive) / 1024;

        System.out.println("---FLAT CONTIGOUS PRIMITIVE LAYOUT(1D float[])---");
        System.out.printf("Execution Latency : %.3f ms%n", primitiveTimeMs);
        System.out.printf("Heap Memory Used : %d KB%n",Math.max(0,primitiveMemKb));
        System.out.println("Mechanics          :Allocates zero object wrappers.Maximizes CPU cache line");
        System.out.println("                     locality by reading adjacent memory locations sequentially.");
        System.out.println("--------------------------------------------------------------------\n");

        //Architectural Analysis Summary
        System.out.println("       ANALYSIS SUMMARY");
        System.out.printf("Throughput Speedup Factor : %.1fx faster%n",(objectTimeMs/primitiveTimeMs));
        System.out.printf("Memory Reduction Factor   : %.1fx less RAM footprint",((double) objectMemkb / Math.max(1,primitiveMemKb)));
        // Simple confirmation statement  ensuring JIT compiler  optimizations do not  prune math paths
        if (objectDotProductSum == primitiveDotProductSum && objectDotProductSum !=0.0) {
            System.nanoTime();
        }
    }
    /**
     * Calculates the vector dot  product via Standard List  objects.
     */
    private static double computeObjectDotProduct(List<Float> v1,List<Float> v2){
        double dotProduct = 0;
        for (int i = 0; i< DIMENSIONS; i++) {
            //Implicit compiler cost: get(i).floatvalue()-unboxing creates massive CPU overhead
            // unboxing/boxing are cpu cycles wasted converting primitives(like int) to objects(like Integer)
            // So using Primitives types saves you from the unboxing and boxing CPU overheads which consume-
            // Memory and time.NB-Learn why we are looping through dimensions here
            dotProduct +=v1.get(i)*v2.get(i);
        }
        return dotProduct;
    }
    /**
     * Calculates the vector dot product using explicit math offsets across a 1D primitive array.
     * Zero unboxing overheads here-notice that.
     */
    private static double computePrimitiveDotProduct(float[] array,int vec1Idx,int vec2Idx) {
        double dotProduct = 0;
        int offset1= vec1Idx * DIMENSIONS;
        int offset2= vec2Idx * DIMENSIONS;
        for (int i = 0; i < DIMENSIONS; i++) {
            // Sequentially loads raw values  withouth objects or reference  dereferencing
            // You are not converting any Objects into primitives thus saving Memory and time-
            // due to cpu cycles during unboxing.
        dotProduct += array[offset1 + i] * array[offset2 + i];
        }
        return dotProduct;
        /**Points to note
         * The main difference between a function and a method is association.
         * All methods are functions.
         * So,A function is a standalone block of code that performs a specific task,
         * While a method is a function that belongs to a specific object or class.
         * All of the above are thus methods-Methods are core to OOP,while functions rule
         * procedural or functional programming.
         */
    }
    private static void runObjectVectorWarmup(){
        List<List<Float>> warm = new ArrayList<>(2);// some warmup unboxing vector calculations
        for(int i= 0; i < 2; i++){
            List<Float> v = new ArrayList <>(DIMENSIONS);
            for(int d = 0; d<DIMENSIONS;d++) v.add(0.0f);
            warm.add(v);
        }
        computeObjectDotProduct(warm.get(0),warm.get(1));
    }
    private static void runPrimitiveVectorWarmup(){ //some warm up vector calculations-zero unboxing
        float[] warm = new float[2 * DIMENSIONS];//easy to create,easy to run
        computePrimitiveDotProduct(warm, 0, 1);
    }
}
/**Some Java terminology reminders here
 * private,public and restricted stand for-
 * static Means-
 * void means-
 * remember to run the two commands to run this file-javac and java
 */
