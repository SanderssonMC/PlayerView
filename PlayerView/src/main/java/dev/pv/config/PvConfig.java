package dev.pv.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Tiny flat-file config. Stored at config/playerview.cfg.
 * Keep your API keys here - this file is NOT meant to be shared.
 *
 * The sukie* "resolved" fields are filled in automatically the first time the
 * cosmetics endpoint is contacted successfully (auto-detected auth header + query
 * style), so later requests skip the probing step.
 */
public class PvConfig {

    private final File file;
    private final Properties props = new Properties();

    public PvConfig(File configDir) {
        this.file = new File(configDir, "playerview.cfg");
    }

    public void load() {
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (Exception ignored) {}
        } else {
            props.setProperty("hypixelApiKey", "");
            props.setProperty("sukieApiKey", "");
            seedSukieDefaults();
            save();
        }
        // make sure newer keys exist even on configs written by older versions
        seedSukieDefaults();
    }

    private void seedSukieDefaults() {
        props.putIfAbsent("sukieAuthHeader", "");
        props.putIfAbsent("sukieAuthPrefix", "");
        props.putIfAbsent("sukieQuery", "");
        props.putIfAbsent("sukieResolved", "false");
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "PlayerView config - keep your API keys private!");
            }
        } catch (Exception ignored) {}
    }

    public String getHypixelKey() { return props.getProperty("hypixelApiKey", "").trim(); }
    public String getSukieKey()   { return props.getProperty("sukieApiKey", "").trim(); }

    public void setHypixelKey(String k) { props.setProperty("hypixelApiKey", k); save(); }
    public void setSukieKey(String k)   {
        props.setProperty("sukieApiKey", k);
        // a new key may need re-detection
        clearSukieResolution();
        save();
    }

    // ---- sukie auto-resolution ----
    public boolean isSukieResolved() {
        return Boolean.parseBoolean(props.getProperty("sukieResolved", "false"));
    }
    public String getSukieHeader() { return props.getProperty("sukieAuthHeader", "").trim(); }
    public String getSukiePrefix() { return props.getProperty("sukieAuthPrefix", ""); }
    public String getSukieQuery()  { return props.getProperty("sukieQuery", "").trim(); }

    public void markSukieResolved(String header, String prefix, String query) {
        props.setProperty("sukieAuthHeader", header == null ? "" : header);
        props.setProperty("sukieAuthPrefix", prefix == null ? "" : prefix);
        props.setProperty("sukieQuery", query == null ? "" : query);
        props.setProperty("sukieResolved", "true");
        save();
    }

    public void clearSukieResolution() {
        props.setProperty("sukieResolved", "false");
        save();
    }
}
