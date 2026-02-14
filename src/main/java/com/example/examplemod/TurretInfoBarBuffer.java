package com.example.examplemod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side prompt buffer for the turret info bar.
 * Stores prompts per skeleton id and rejects stale updates by sequence.
 */
public class TurretInfoBarBuffer {
    private final Map<Integer, PromptBucket> slots = new LinkedHashMap<>();

    public void upsertPrompt(Integer skeletonId, String prompt, long sequence) {
        upsertPromptBatch(skeletonId, List.of(prompt), sequence);
    }

    public void upsertPromptBatch(Integer skeletonId, List<String> prompts, long sequence) {
        int id = skeletonId == null ? -1 : skeletonId;
        PromptBucket existing = slots.get(id);
        if (existing != null && sequence < existing.sequence) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (prompts != null) {
            for (String prompt : prompts) {
                lines.add(prompt == null ? "" : prompt);
            }
        }
        slots.put(id, new PromptBucket(id, sequence, lines));
    }

    public List<PromptSlot> orderedSlots() {
        List<PromptBucket> ordered = new ArrayList<>(slots.values());
        ordered.sort(Comparator.comparingInt(PromptBucket::skeletonId));
        List<PromptSlot> flattened = new ArrayList<>();
        for (PromptBucket bucket : ordered) {
            for (String line : bucket.prompts) {
                flattened.add(new PromptSlot(bucket.skeletonId, line, bucket.sequence));
            }
        }
        return flattened;
    }

    public void clear() {
        slots.clear();
    }

    private static final class PromptBucket {
        private final int skeletonId;
        private final long sequence;
        private final List<String> prompts;

        private PromptBucket(int skeletonId, long sequence, List<String> prompts) {
            this.skeletonId = skeletonId;
            this.sequence = sequence;
            this.prompts = prompts;
        }

        public int skeletonId() {
            return skeletonId;
        }
    }

    public static final class PromptSlot {
        private final int skeletonId;
        private final String prompt;
        private final long sequence;

        private PromptSlot(int skeletonId, String prompt, long sequence) {
            this.skeletonId = skeletonId;
            this.prompt = prompt;
            this.sequence = sequence;
        }

        public int skeletonId() {
            return skeletonId;
        }

        public String prompt() {
            return prompt;
        }

        public long sequence() {
            return sequence;
        }
    }
}
