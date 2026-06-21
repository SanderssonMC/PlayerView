package dev.pv.stats;

/** Hypixel network level from total network experience. */
public final class NetworkLevel {

    private NetworkLevel() {}

    public static int fromExp(double exp) {
        if (exp <= 0) return 1;
        // level = floor(-2.5 + sqrt(12.25 + 0.0008 * exp))
        double level = -2.5 + Math.sqrt(12.25 + 0.0008 * exp);
        return (int) Math.floor(level) + 1;
    }
}
