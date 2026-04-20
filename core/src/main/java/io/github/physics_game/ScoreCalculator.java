package io.github.physics_game;

public class ScoreCalculator {
    private static final int BaseScore= 100;

    private static final float TIME_PENALTY = 2.0f;
    private static final int SHAPE_PENALTY = 100;

    public static int calculateScore(float timeUsed, float shapeProportion, float freeProp, int shapePenalty, float timePenalty) {
        int score = BaseScore;

        // shape penalty
        if(shapeProportion >= 0.5f) score -= (int)((shapeProportion - 0.5f) * shapePenalty);

        // time penalty
        if (timeUsed > 30f) {
            score -= (int)((timeUsed - 30f) * timePenalty);
        }

        // score has to be 0-100
        return Math.max(0, Math.min(100, score));
    }

    /** Standard call — respects the level's free-object allowance. */
    public static int calculateScore(float shapePropUsed, float timeUsed, float freeProp) {
        return calculateScore(shapePropUsed, timeUsed, freeProp, SHAPE_PENALTY, TIME_PENALTY);
    }

    /** Legacy call — treats every object as billable. */
    public static int calculateScore(float shapePropUsed, float timeUsed) {
        return calculateScore(shapePropUsed, timeUsed, 0, SHAPE_PENALTY, TIME_PENALTY);
    }

    /** Legacy 4-param overload (custom penalties, no free objects). */
    public static int calculateScore(float shapePropUsed, float timeUsed,
                                     int shapePenalty, float timePenalty) {
        return calculateScore(shapePropUsed, timeUsed, 0, shapePenalty, timePenalty);
    }

    // calculate star based on score
    public static int calculateStars(int score) {
        if (score >= 95) return 3;
        if (score >= 60) return 2;
        return 1;
    }



}
