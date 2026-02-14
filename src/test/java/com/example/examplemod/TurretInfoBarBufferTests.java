package com.example.examplemod;

import java.util.List;

public class TurretInfoBarBufferTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Turret Info Bar Buffer Tests");
        System.out.println("================================================");

        try {
            testSinglePrompt();
            testBatchPromptOrdering();
            testBatchPromptSameSkeleton();
            testNullSkeletonId();
            testNetworkLatencyOutOfOrderPackets();
            System.out.println("\nALL TURRET INFO BAR BUFFER TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testSinglePrompt() {
        TurretInfoBarBuffer buffer = new TurretInfoBarBuffer();
        buffer.upsertPrompt(7, "Ability Ready", 1L);

        List<TurretInfoBarBuffer.PromptSlot> slots = buffer.orderedSlots();
        assertEquals(1, slots.size(), "single prompt count");
        assertEquals(7, slots.get(0).skeletonId(), "single prompt skeleton id");
        assertEquals("Ability Ready", slots.get(0).prompt(), "single prompt text");
        System.out.println("[OK] single prompt");
    }

    static void testBatchPromptOrdering() {
        TurretInfoBarBuffer buffer = new TurretInfoBarBuffer();
        buffer.upsertPrompt(9, "Prompt 9", 1L);
        buffer.upsertPrompt(3, "Prompt 3", 1L);
        buffer.upsertPrompt(5, "Prompt 5", 1L);

        List<TurretInfoBarBuffer.PromptSlot> slots = buffer.orderedSlots();
        assertEquals(3, slots.size(), "batch prompt count");
        assertEquals(3, slots.get(0).skeletonId(), "batch order first");
        assertEquals(5, slots.get(1).skeletonId(), "batch order second");
        assertEquals(9, slots.get(2).skeletonId(), "batch order third");
        System.out.println("[OK] batch prompts ordered by skeleton id");
    }

    static void testNullSkeletonId() {
        TurretInfoBarBuffer buffer = new TurretInfoBarBuffer();
        buffer.upsertPrompt(null, "Prompt without id", 1L);

        List<TurretInfoBarBuffer.PromptSlot> slots = buffer.orderedSlots();
        assertEquals(1, slots.size(), "null id prompt count");
        assertEquals(-1, slots.get(0).skeletonId(), "null id fallback value");
        assertEquals("Prompt without id", slots.get(0).prompt(), "null id prompt text");
        System.out.println("[OK] null skeleton id");
    }

    static void testBatchPromptSameSkeleton() {
        TurretInfoBarBuffer buffer = new TurretInfoBarBuffer();
        buffer.upsertPromptBatch(4, List.of("line1", "line2", "line3"), 1L);

        List<TurretInfoBarBuffer.PromptSlot> slots = buffer.orderedSlots();
        assertEquals(3, slots.size(), "same skeleton batch line count");
        assertEquals(4, slots.get(0).skeletonId(), "same skeleton id line1");
        assertEquals(4, slots.get(1).skeletonId(), "same skeleton id line2");
        assertEquals("line2", slots.get(1).prompt(), "same skeleton line2 content");
        System.out.println("[OK] batch prompts under same skeleton id");
    }

    static void testNetworkLatencyOutOfOrderPackets() {
        TurretInfoBarBuffer buffer = new TurretInfoBarBuffer();
        buffer.upsertPrompt(12, "newer packet", 10L);
        buffer.upsertPrompt(12, "older packet", 5L);

        List<TurretInfoBarBuffer.PromptSlot> slots = buffer.orderedSlots();
        assertEquals(1, slots.size(), "latency prompt count");
        assertEquals("newer packet", slots.get(0).prompt(), "stale packet should be ignored");
        System.out.println("[OK] network latency stale packet rejection");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new RuntimeException(message + " failed. expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new RuntimeException(message + " failed. expected=" + expected + ", actual=" + actual);
        }
    }
}
