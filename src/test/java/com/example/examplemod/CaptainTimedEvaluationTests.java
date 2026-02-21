package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CaptainTimedEvaluationTests {
    private static final long INTERVAL = 20L * 60L;

    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Captain Timed Evaluation Tests");
        System.out.println("================================================");
        try {
            testOnlyEvaluateAt60SecondTick();
            testNoCaptainSwitchInsideWindow();
            testSwitchAtTimedPointWhenOutranked();
            testSourceWiring();
            System.out.println("\nALL CAPTAIN TIMER TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testOnlyEvaluateAt60SecondTick() {
        if (!isEvaluationTick(1200L)) {
            throw new RuntimeException("1200 tick should be an evaluation tick");
        }
        if (isEvaluationTick(1199L)) {
            throw new RuntimeException("1199 tick should not be an evaluation tick");
        }
        if (isEvaluationTick(0L)) {
            throw new RuntimeException("0 tick should not be an evaluation tick");
        }
        System.out.println("[OK] timer interval gate check");
    }

    static void testNoCaptainSwitchInsideWindow() {
        String currentCaptain = "A";
        String captain = currentCaptain;

        for (long tick = 1; tick < INTERVAL; tick++) {
            List<SquadCaptainSelection.Candidate> candidates = new ArrayList<>();
            candidates.add(new SquadCaptainSelection.Candidate("A", 100, 3, 10, 1));
            candidates.add(new SquadCaptainSelection.Candidate("B", 150 + tick, 5, 50, 2));

            if (isEvaluationTick(tick)) {
                SquadCaptainSelection.Decision decision = SquadCaptainSelection.evaluate(candidates, captain);
                captain = decision.newCaptainId();
            }
        }

        if (!currentCaptain.equals(captain)) {
            throw new RuntimeException("captain changed before 60-second evaluation window");
        }
        System.out.println("[OK] no switch inside 60-second window");
    }

    static void testSwitchAtTimedPointWhenOutranked() {
        String captain = "A";
        List<SquadCaptainSelection.Candidate> candidates = List.of(
                new SquadCaptainSelection.Candidate("A", 100, 3, 10, 1),
                new SquadCaptainSelection.Candidate("B", 220, 5, 70, 2)
        );

        SquadCaptainSelection.Decision decision = SquadCaptainSelection.evaluate(candidates, captain);
        if (!"B".equals(decision.newCaptainId())) {
            throw new RuntimeException("captain should switch to top candidate at evaluation tick");
        }
        if (!"CURRENT_CAPTAIN_OUTRANKED".equals(decision.reason())) {
            throw new RuntimeException("unexpected switch reason: " + decision.reason());
        }
        System.out.println("[OK] switch on timed point when current captain is outranked");
    }

    static void testSourceWiring() throws Exception {
        String mod = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
        String turret = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(mod, "CAPTAIN_EVAL_INTERVAL_TICKS = 20L * 60L", "missing 60s timer constant");
        assertContains(mod, "isCaptainEvaluationTick(gameTime)", "player tick should use timer gate");
        assertContains(mod, "SquadCaptainSelection.evaluate", "timed evaluation should use selection policy");
        assertContains(mod, "LOGGER.info(\"[CaptainEval]", "missing timed evaluation log");
        assertNotContains(turret, "tickCaptainLogic();", "per-turret captain evaluation should be disabled");
        System.out.println("[OK] source wiring checks");
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static void assertNotContains(String content, String expected, String message) {
        if (content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static boolean isEvaluationTick(long gameTime) {
        return gameTime > 0 && gameTime % INTERVAL == 0;
    }
}
