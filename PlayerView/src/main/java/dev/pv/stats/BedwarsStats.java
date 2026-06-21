package dev.pv.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.image.BufferedImage;

/**
 * All the data PlayerView needs about one player.
 * Holds the raw Bedwars object so numbers can be recomputed per display mode.
 */
public class BedwarsStats {

    public enum Mode {
        OVERALL("Overall"),
        CORES("Core Modes");

        public final String label;
        Mode(String label) { this.label = label; }
    }

    // identity
    public String name;
    public String uuid;          // undashed

    // bedwars star
    public long experience;
    public BedwarsLevel level;

    // network-level info (from player root, not Bedwars)
    public int networkLevel;
    public long karma;
    public long achievementPoints;
    public long gifted;
    public long coins;

    // cosmetics from sukie (filled in later, may be unavailable)
    public Cosmetics cosmetics = new Cosmetics();

    // skin image (downloaded off-thread, uploaded to GL lazily in the GUI)
    public BufferedImage skin;

    // raw bedwars stats object for per-mode lookups
    private JsonObject bw;

    private static final String[] CORE_PREFIXES = {
            "eight_one_", "eight_two_", "four_three_", "four_four_"
    };

    public static BedwarsStats parse(JsonObject player, String name, String uuid) {
        BedwarsStats s = new BedwarsStats();
        s.name = name;
        s.uuid = uuid;

        s.networkLevel     = NetworkLevel.fromExp(getDouble(player, "networkExp"));
        s.karma            = getLong(player, "karma");
        s.achievementPoints = getLong(player, "achievementPoints");

        JsonObject gifting = obj(player, "giftingMeta");
        if (gifting != null) {
            long ranks = getLong(gifting, "ranksGiven");
            long bundles = getLong(gifting, "bundlesGiven");
            s.gifted = Math.max(ranks, bundles);
        }

        JsonObject stats = obj(player, "stats");
        s.bw = stats != null ? obj(stats, "Bedwars") : null;
        if (s.bw == null) s.bw = new JsonObject();

        s.experience = (long) getDouble(s.bw, "Experience");
        s.level = BedwarsLevel.of(s.experience);
        s.coins = getLong(s.bw, "coins");
        return s;
    }

    /** Read a per-mode counter, summing the four core modes when mode == CORES. */
    public long get(String suffix, Mode mode) {
        if (mode == Mode.OVERALL) {
            return getLong(bw, suffix);
        }
        long total = 0;
        for (String p : CORE_PREFIXES) total += getLong(bw, p + suffix);
        return total;
    }

    public long wins(Mode m)        { return get("wins_bedwars", m); }
    public long losses(Mode m)      { return get("losses_bedwars", m); }
    public long kills(Mode m)       { return get("kills_bedwars", m); }
    public long deaths(Mode m)      { return get("deaths_bedwars", m); }
    public long finalKills(Mode m)  { return get("final_kills_bedwars", m); }
    public long finalDeaths(Mode m) { return get("final_deaths_bedwars", m); }
    public long bedsBroken(Mode m)  { return get("beds_broken_bedwars", m); }
    public long bedsLost(Mode m)    { return get("beds_lost_bedwars", m); }
    public long gamesPlayed(Mode m) { return get("games_played_bedwars", m); }
    public long winstreak(Mode m)   { return get("winstreak", m); }

    public double ratio(long a, long b) { return b == 0 ? a : (double) a / (double) b; }

    // ---- json helpers ----
    private static JsonObject obj(JsonObject o, String key) {
        if (o == null) return null;
        JsonElement e = o.get(key);
        return (e != null && e.isJsonObject()) ? e.getAsJsonObject() : null;
    }

    private static long getLong(JsonObject o, String key) {
        if (o == null) return 0;
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return 0;
        try { return e.getAsLong(); } catch (Exception ex) {
            try { return (long) e.getAsDouble(); } catch (Exception ex2) { return 0; }
        }
    }

    private static double getDouble(JsonObject o, String key) {
        if (o == null) return 0;
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return 0;
        try { return e.getAsDouble(); } catch (Exception ex) { return 0; }
    }
}
