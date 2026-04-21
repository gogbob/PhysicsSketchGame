package io.github.physics_game;

public class ChartHelpText {

    public static String get(String key) {
        switch (key) {
            case "position":
                return "Position\nExplanation coming later.";
            case "velocity":
                return "Velocity\nExplanation coming later.";
            case "speed":
                return "Speed\nExplanation coming later.";
            case "acceleration":
                return "Acceleration\nExplanation coming later.";
            case "energy":
                return "Energy\nExplanation coming later.";
            default:
                return "";
        }
    }
}
