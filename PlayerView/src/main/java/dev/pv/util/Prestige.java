package dev.pv.util;

/**
 * BedWars prestige coloring per 100 levels. Returns both an int RGB (for GUI text)
 * and a legacy section-code string (for chat). 1000+ uses a single colour as a
 * simplification (real Hypixel uses multi-colour rainbow stars at high prestiges).
 */
public final class Prestige {

    private Prestige() {}

    public static final char STAR = '\u272B'; // ✫

    private static int bracket(int level) {
        int b = level / 100;
        return Math.min(b, 10);
    }

    public static int rgb(int level) {
        switch (bracket(level)) {
            case 0:  return 0xAAAAAA; // gray
            case 1:  return 0xFFFFFF; // white
            case 2:  return 0xFFAA00; // gold
            case 3:  return 0x55FFFF; // aqua
            case 4:  return 0x00AA00; // dark green
            case 5:  return 0x00AAAA; // dark aqua
            case 6:  return 0xAA0000; // dark red
            case 7:  return 0xFF55FF; // light purple
            case 8:  return 0x5555FF; // blue
            case 9:  return 0xAA00AA; // dark purple
            default: return 0xFF5555; // 1000+ red (simplified)
        }
    }

    public static String code(int level) {
        switch (bracket(level)) {
            case 0:  return "\u00a77";
            case 1:  return "\u00a7f";
            case 2:  return "\u00a76";
            case 3:  return "\u00a7b";
            case 4:  return "\u00a72";
            case 5:  return "\u00a73";
            case 6:  return "\u00a74";
            case 7:  return "\u00a7d";
            case 8:  return "\u00a79";
            case 9:  return "\u00a75";
            default: return "\u00a7c";
        }
    }

    /** "[104✫]" with colour, for chat. */
    public static String tag(int level) {
        String c = code(level);
        return "\u00a77[" + c + level + c + STAR + "\u00a77]";
    }
}
