package io.github.physics_game;

public class ScoreLevel {
    private float bestTime = 0f;
    private float bestShapeProportion = 0f;
    private int bestScore = Integer.MAX_VALUE;

    public ScoreLevel() {}

    public ScoreLevel(float bestTime, float bestShapeProportion) {
        this.bestTime = bestTime;
        this.bestShapeProportion = bestShapeProportion;
        this.bestScore = ScoreCalculator.calculateScore(bestTime, bestShapeProportion);
    }

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

    public void setNewBestScore(float newTime, float newShapeProportion) {
        this.bestScore = Math.min(ScoreCalculator.calculateScore(newTime, newShapeProportion), this.bestScore);
        this.bestTime = Math.min(newTime, this.bestTime);
        this.bestShapeProportion = Math.min(newShapeProportion, this.bestShapeProportion);
    }
}
