package io.github.physics_game;

public class ScoreLevel {
    private float bestTime = Float.MAX_VALUE;
    private float bestShapeProportion = 0f;
    private int bestScore = -1;

    public ScoreLevel() {}

    public float getBestTime() {
        return bestTime;
    }
    public float getBestShapeProportion() {
        return bestShapeProportion;
    }
    public int getBestScore() {
        return bestScore;
    }
    public int getNumStars() {
        return ScoreCalculator.calculateStars(bestScore);
    }

    public void setNewBestScore(float newTime, float newShapeProportion, float freeProp) {
        boolean isFirst = bestScore < 0;
        int newScore = ScoreCalculator.calculateScore(newShapeProportion, newTime, freeProp);
        if (isFirst || newScore > bestScore) {
            bestScore = newScore;
            bestTime = newTime;
        } else if (newScore == bestScore && newTime < bestTime) {
            bestTime = newTime;
        }
        if (isFirst || newShapeProportion < bestShapeProportion) {
            bestShapeProportion = newShapeProportion;
        }
    }
}
