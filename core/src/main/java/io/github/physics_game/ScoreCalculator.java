package io.github.physics_game;

public class ScoreCalculator {
    private static final int   BASE_SCORE    = 100;
    private static final int   SHAPE_PENALTY = 15;   // per object beyond free limit
    private static final float TIME_PENALTY  = 2.0f; // per second beyond 30 s

    /** Full control overload. */
    public static int calculateScore(int shapesUsed, float timeUsed,
                                     int freeObjects, int shapePenalty, float timePenalty) {
        int score = BASE_SCORE;
        int billable = Math.max(0, shapesUsed - freeObjects);
        score -= billable * shapePenalty;
        if (timeUsed > 30f) score -= (int)((timeUsed - 30f) * timePenalty);
        return Math.max(0, Math.min(100, score));
    }

    /** Standard call — respects the level's free-object allowance. */
    public static int calculateScore(int shapesUsed, float timeUsed, int freeObjects) {
        return calculateScore(shapesUsed, timeUsed, freeObjects, SHAPE_PENALTY, TIME_PENALTY);
    }

    /** Legacy call — treats every object as billable. */
    public static int calculateScore(int shapesUsed, float timeUsed) {
        return calculateScore(shapesUsed, timeUsed, 0, SHAPE_PENALTY, TIME_PENALTY);
    }

    /** Legacy 4-param overload (custom penalties, no free objects). */
    public static int calculateScore(int shapesUsed, float timeUsed,
                                     int shapePenalty, float timePenalty) {
        return calculateScore(shapesUsed, timeUsed, 0, shapePenalty, timePenalty);
    }

    public static int calculateStars(int score) {
        if (score >= 95) return 3;
        if (score >= 60) return 2;
        return 1;
    }
}
