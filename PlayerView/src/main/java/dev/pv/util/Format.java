package dev.pv.util;

import java.util.Locale;

public final class Format {

    private Format() {}

    /** 1428 -> "1,428" */
    public static String grp(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    /** 57.288 -> "57.29" */
    public static String ratio(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0.00";
        return String.format(Locale.US, "%.2f", v);
    }

    /** 0.948 -> "94.8%" (input is a fraction 0..1) */
    public static String pct(double frac) {
        if (Double.isNaN(frac) || Double.isInfinite(frac)) return "0%";
        return String.format(Locale.US, "%.1f%%", frac * 100.0);
    }
}
