package io.github.physics_game;

public class ChartHelpText {

    public static String get(String key) {
        switch (key) {
            case "position":
                return "[GOLD]Position X and Y[]\n" +
                    "\n" +
                    "[GRAY]Where is the object?[]\n" +
                    "[WHITE]Position tells us where an object is located on the coordinate plane. " +
                    "The x-position shows how far left or right the object is, and the y-position " +
                    "shows how high or low it is. As time passes, these values change when the " +
                    "object moves.[]\n" +
                    "\n" +
                    "[CYAN]Formulas:[]\n" +
                    "[WHITE]x = x0 + vx * t[]\n" +
                    "[WHITE]y = y0 + vy * t + (1/2) * ay * t^2[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]x0, y0 = starting position[]\n" +
                    "[LIGHT_GRAY]vx, vy = velocity[]\n" +
                    "[LIGHT_GRAY]ay = vertical acceleration[]\n" +
                    "[LIGHT_GRAY]t = time[]";
            case "velocity":
                return "[GOLD]Velocity X and Y[]\n" +
                    "\n" +
                    "[GRAY]What direction is an object traveling and how fast?[]\n" +
                    "[WHITE]Velocity describes how the position of an object changes over time. " +
                    "The x-velocity tells us how fast the object moves left or right, while the " +
                    "y-velocity tells us how fast it moves upward or downward. Velocity includes " +
                    "direction, which is why it can be positive or negative.[]\n" +
                    "\n" +
                    "[CYAN]Formulas:[]\n" +
                    "[WHITE]vx = dx / dt[]\n" +
                    "[WHITE]vy = dy / dt[]\n" +
                    "[WHITE]vy = vy0 + ay * t[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]dx, dy = change in position[]\n" +
                    "[LIGHT_GRAY]dt = change in time[]\n" +
                    "[LIGHT_GRAY]vy0 = initial vertical velocity[]\n" +
                    "[LIGHT_GRAY]ay = vertical acceleration[]";
            case "speed":
                return "[GOLD]Speed[]\n" +
                    "\n" +
                    "[GRAY]How fast is an object traveling?[]\n" +
                    "[WHITE]Speed tells us how fast an object is moving, regardless of direction. " +
                    "Unlike velocity, speed is always a positive value because it only measures " +
                    "the magnitude of motion, not the direction. It is obtained using the Pythagorean theorem with" +
                    "the x and y velocity.[]\n" +
                    "\n" +
                    "[CYAN]Formula:[]\n" +
                    "[WHITE]speed = sqrt(vx^2 + vy^2)[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]vx, vy = velocity components[]\n" +
                    "[LIGHT_GRAY]sqrt = square root[]";
            case "acceleration":
                return "[GOLD]Acceleration (Y)[]\n" +
                    "\n" +
                    "[GRAY]How fast is an object changing its velocity?[]\n" +
                    "[WHITE]Acceleration describes how velocity changes over time. " +
                    "In this game, the main acceleration is gravity, which constantly pulls " +
                    "objects downward. This means the vertical velocity changes every moment, " +
                    "even if no other forces are applied, increasing constantly.[]\n" +
                    "\n" +
                    "[CYAN]Formulas:[]\n" +
                    "[WHITE]a = dv / dt[]\n" +
                    "[WHITE]vy = vy0 + ay * t[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]dv = change in velocity[]\n" +
                    "[LIGHT_GRAY]dt = change in time[]\n" +
                    "[LIGHT_GRAY]ay = vertical acceleration (gravity)[]\n" +
                    "[LIGHT_GRAY]vy0 = initial vertical velocity[]";
            case "energy":
                return "[GOLD]Energy (Kinetic & Potential)[]\n" +
                    "\n" +
                    "[GRAY]How much energy does an object have?[]\n" +
                    "[WHITE]Energy describes the ability of an object to do work. In this game, " +
                    "we mainly observe kinetic energy (movement) and potential energy (height). " +
                    "As an object falls, its potential energy decreases while its kinetic energy " +
                    "increases. Because of non conservative forces like friction, the total energy will be lost.[]\n" +
                    "\n" +
                    "[CYAN]Formulas:[]\n" +
                    "[WHITE]KE = (1/2) * m * v^2[]\n" +
                    "[WHITE]PE = m * g * h[]\n" +
                    "[WHITE]E_total = KE + PE[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]m = mass[]\n" +
                    "[LIGHT_GRAY]v = speed[]\n" +
                    "[LIGHT_GRAY]g = gravity[]\n" +
                    "[LIGHT_GRAY]h = height[]\n" +
                    "\n" +
                    "[LIGHT_GRAY]Total energy stays nearly constant if no energy is lost[]";
            default:
                return "";
        }
    }
}
