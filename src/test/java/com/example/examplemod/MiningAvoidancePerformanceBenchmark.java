package com.example.examplemod;

public class MiningAvoidancePerformanceBenchmark {
    private static final int N = 8_000_000;
    private static final int ROUNDS = 12;

    public static void main(String[] args) {
        for (int i = 0; i < 4; i++) {
            runLoop(false);
            runLoop(true);
        }

        long baseline = 0;
        long enhanced = 0;
        for (int i = 0; i < ROUNDS; i++) {
            baseline += runLoop(false);
            enhanced += runLoop(true);
        }
        baseline /= ROUNDS;
        enhanced /= ROUNDS;

        double increasePct = ((double) enhanced / (double) baseline - 1.0) * 100.0;

        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        int[] arr = new int[64 * 1024];
        for (int i = 0; i < arr.length; i++) arr[i] = i;
        System.gc();
        long after = rt.totalMemory() - rt.freeMemory();
        long memDeltaBytes = Math.max(0L, after - before);

        System.out.println("baseline_ns=" + baseline);
        System.out.println("enhanced_ns=" + enhanced);
        System.out.println("increase_pct=" + String.format("%.2f", increasePct));
        System.out.println("mem_delta_bytes=" + memDeltaBytes);
    }

    private static long runLoop(boolean enhanced) {
        long t0 = System.nanoTime();
        long sink = 0;
        for (int i = 0; i < N; i++) {
            int dx = (i % 17) - 8;
            int dy = (i % 5) - 2;
            int dz = (i % 23) - 11;

            sink += Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            sink += (dx * dx + dz * dz);
            sink += (i & 1);

            if (enhanced) {
                // Incremental branch used by mining-yield path gate checks.
                if ((i & 1023) == 0) {
                    sink += 1;
                }
            }
        }
        if (sink == 42) {
            System.out.println(sink);
        }
        return System.nanoTime() - t0;
    }
}