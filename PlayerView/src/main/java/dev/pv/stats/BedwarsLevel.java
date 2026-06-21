package dev.pv.stats;

/**
 * Converts BedWars Experience into a star level and progress toward the next level,
 * using Hypixel's prestige curve (500/1000/2000/3500 for the first 4 levels of each
 * prestige, then 5000 per level; 487000 XP per 100-level prestige).
 */
public final class BedwarsLevel {

    private static final int XP_PER_PRESTIGE = 487000;
    private static final int LEVELS_PER_PRESTIGE = 100;

    public final int level;       // completed stars
    public final long progress;   // XP into the current level
    public final long needed;     // XP required for the next level

    private BedwarsLevel(int level, long progress, long needed) {
        this.level = level;
        this.progress = progress;
        this.needed = needed;
    }

    private static long easyXp(int respectedLevel) {
        switch (respectedLevel) {
            case 1: return 500;
            case 2: return 1000;
            case 3: return 2000;
            case 4: return 3500;
            default: return 5000;
        }
    }

    public static BedwarsLevel of(long exp) {
        if (exp < 0) exp = 0;
        int prestiges = (int) (exp / XP_PER_PRESTIGE);
        int level = prestiges * LEVELS_PER_PRESTIGE;
        long rem = exp - (long) prestiges * XP_PER_PRESTIGE;

        // first four (cheap) levels of this prestige
        for (int i = 1; i <= 4; i++) {
            long need = easyXp(i);
            if (rem < need) return new BedwarsLevel(level, rem, need);
            level++;
            rem -= need;
        }
        // remaining levels cost 5000 each
        level += (int) (rem / 5000);
        long into = rem % 5000;
        return new BedwarsLevel(level, into, 5000);
    }
}
