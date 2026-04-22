package io.github.physics_game;

import com.badlogic.gdx.math.Rectangle;

public class ChartHelpTarget {
    public final String key;
    public final Rectangle iconBounds;
    public final Rectangle groupBounds;

    public ChartHelpTarget(String key, Rectangle iconBounds, Rectangle groupBounds) {
        this.key = key;
        this.iconBounds = iconBounds;
        this.groupBounds = groupBounds;
    }
}
