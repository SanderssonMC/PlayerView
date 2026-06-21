package dev.pv.stats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cosmetics + extra counters returned by api.sukie.net.
 *
 * NOTE: the exact JSON field names are not confirmed yet, so this holder keeps a
 * generic ordered map of label -> value that the GUI renders directly. Once we have
 * a real sample response, SukieApi can map specific fields into typed getters here.
 */
public class Cosmetics {

    public boolean available = false;
    public String error = null;

    /** Optional headline counters if sukie exposes them. -1 = unknown. */
    public long finalKills = -1;
    public long bedsDestroyed = -1;

    /** Everything else, shown as label: value rows. Insertion order preserved. */
    public final Map<String, String> entries = new LinkedHashMap<String, String>();

    public void put(String label, String value) {
        if (label == null || value == null) return;
        entries.put(label, value);
    }
}
