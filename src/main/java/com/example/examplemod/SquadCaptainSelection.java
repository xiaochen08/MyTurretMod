package com.example.examplemod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SquadCaptainSelection {
    private SquadCaptainSelection() {}

    public record Candidate(
            String id,
            double squadScore,
            int tier,
            int kills,
            long joinTick
    ) {}

    public record Decision(
            String oldCaptainId,
            String newCaptainId,
            String reason,
            List<String> rankedIds
    ) {}

    private static final Comparator<Candidate> RANKING = (a, b) -> {
        int scoreCmp = Double.compare(b.squadScore(), a.squadScore());
        if (scoreCmp != 0) return scoreCmp;
        int tierCmp = Integer.compare(b.tier(), a.tier());
        if (tierCmp != 0) return tierCmp;
        int killCmp = Integer.compare(b.kills(), a.kills());
        if (killCmp != 0) return killCmp;
        int joinCmp = Long.compare(a.joinTick(), b.joinTick());
        if (joinCmp != 0) return joinCmp;
        return a.id().compareTo(b.id());
    };

    public static Decision evaluate(List<Candidate> candidates, String currentCaptainId) {
        if (candidates == null || candidates.isEmpty()) {
            return new Decision(currentCaptainId, null, "NO_CANDIDATES", List.of());
        }

        List<Candidate> ranked = new ArrayList<>(candidates);
        ranked.sort(RANKING);
        Map<String, Candidate> byId = ranked.stream()
                .collect(Collectors.toMap(Candidate::id, Function.identity(), (a, b) -> a));

        Candidate best = ranked.get(0);
        Candidate current = currentCaptainId == null ? null : byId.get(currentCaptainId);
        String reason;
        Candidate selected;

        if (current == null) {
            selected = best;
            reason = "NO_CURRENT_CAPTAIN";
        } else if (RANKING.compare(current, best) == 0) {
            selected = current;
            reason = "KEEP_CURRENT_CAPTAIN";
        } else {
            selected = best;
            reason = "CURRENT_CAPTAIN_OUTRANKED";
        }

        List<String> rankedIds = ranked.stream().map(Candidate::id).filter(Objects::nonNull).toList();
        return new Decision(currentCaptainId, selected.id(), reason, rankedIds);
    }
}

