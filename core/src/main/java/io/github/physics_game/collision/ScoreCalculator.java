package io.github.physics_game.collision;

public class ScoreCalculator {
    private static final int BaseScore= 100;
    private static final float TimePenalty = 2.0f;
    private static final int ShapePenalty = 15;

    public static int calculateScore(int shapesUsed, float timeUsed, int shapeLimit) {
        int score = BaseScore;

        // shape penalty
        score -= shapesUsed * ShapePenalty;

        // time penalty
        if (timeUsed > 30f) {
            score -= (int)((timeUsed - 30f) * TimePenalty);
        }

        // score has to be 0-100
        return Math.max(0, Math.min(100, score));
    }

    // calculate star based on score
    public static int calculateStars(int score) {
        if (score >= 80) return 3; // 3 stars
        if (score >= 50) return 2; // 2 stars
        return 1; // 1 stars
    }

}
