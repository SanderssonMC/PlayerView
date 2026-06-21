package dev.pv.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pv.config.PvConfig;
import dev.pv.stats.Cosmetics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for api.sukie.net/v1/bedwars/cosmetics.
 *
 * The endpoint is private and undocumented, so this client is SELF-RESOLVING:
 * the first successful call auto-detects which auth header and which player-query
 * style the API expects, then caches that combo in the config so later calls make
 * a single request. If the cached combo ever stops authenticating (e.g. key change),
 * it transparently re-probes.
 *
 * The response schema is also unknown, so parsing is generic: primitive fields are
 * flattened into Cosmetics.entries (label -> value) and rendered as-is, while we also
 * try to pick out a "final kills" and "beds destroyed" counter. Failures are never
 * fatal - the rest of the mod still works.
 */
public final class SukieApi {

    private SukieApi() {}

    private static final JsonParser PARSER = new JsonParser();
    private static final String BASE = "https://api.sukie.net/v1/bedwars/cosmetics";

    /** Candidate auth headers as {headerName, valuePrefix}. */
    private static final String[][] HEADER_CANDIDATES = {
            {"Authorization", "Bearer "},
            {"Authorization", ""},
            {"x-api-key", ""},
            {"X-API-Key", ""},
            {"api-key", ""},
            {"apikey", ""},
            {"key", ""},
    };

    /** Candidate ways to address a player. Tokens are resolved by {@link #buildUrl}. */
    private static final String[] QUERY_CANDIDATES = {
            "player", "uuid", "name", "uuidDashed", "pathUuid", "pathName"
    };

    public static Cosmetics fetch(PvConfig config, String name, String undashedUuid) {
        Cosmetics c = new Cosmetics();
        String key = config.getSukieKey();
        if (key == null || key.isEmpty()) {
            c.error = "no sukie key";
            return c;
        }

        // 1) try the cached combo first
        if (config.isSukieResolved()) {
            HttpUtil.Response r = tryOnce(config.getSukieHeader(), config.getSukiePrefix(),
                    config.getSukieQuery(), key, name, undashedUuid);
            if (r != null && r.ok()) {
                parseInto(r.body, c);
                return c;
            }
            if (r != null && (r.code == 401 || r.code == 403)) {
                config.clearSukieResolution(); // stale; fall through to re-probe
            } else if (r != null) {
                c.error = "sukie HTTP " + r.code;
                return c;
            }
        }

        // 2) probe: find an auth header that gets past 401/403 (using the first query style)
        String okHeader = null, okPrefix = null;
        for (String[] h : HEADER_CANDIDATES) {
            HttpUtil.Response r = tryOnce(h[0], h[1], QUERY_CANDIDATES[0], key, name, undashedUuid);
            if (r == null) continue;
            if (r.code == 401 || r.code == 403) continue; // wrong auth, keep looking
            okHeader = h[0];
            okPrefix = h[1];
            if (r.ok()) { // first query style already worked
                config.markSukieResolved(okHeader, okPrefix, QUERY_CANDIDATES[0]);
                parseInto(r.body, c);
                return c;
            }
            break; // auth accepted but wrong query style; resolve that next
        }

        if (okHeader == null) {
            c.error = "sukie auth failed - no known header accepted the key";
            return c;
        }

        // 3) with a working header, find the query style that returns 2xx
        for (String q : QUERY_CANDIDATES) {
            HttpUtil.Response r = tryOnce(okHeader, okPrefix, q, key, name, undashedUuid);
            if (r != null && r.ok()) {
                config.markSukieResolved(okHeader, okPrefix, q);
                parseInto(r.body, c);
                return c;
            }
        }

        c.error = "sukie auth ok but no query style returned data";
        return c;
    }

    private static HttpUtil.Response tryOnce(String header, String prefix, String query,
                                             String key, String name, String undashed) {
        if (header == null || header.isEmpty()) return null;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(header, (prefix == null ? "" : prefix) + key);
        try {
            return HttpUtil.get(buildUrl(query, name, undashed), headers);
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildUrl(String token, String name, String undashed) {
        String dashed = MojangApi.dash(undashed);
        if ("player".equals(token))     return BASE + "?player=" + name;
        if ("name".equals(token))       return BASE + "?name=" + name;
        if ("uuid".equals(token))       return BASE + "?uuid=" + undashed;
        if ("uuidDashed".equals(token)) return BASE + "?uuid=" + dashed;
        if ("pathUuid".equals(token))   return BASE + "/" + undashed;
        if ("pathName".equals(token))   return BASE + "/" + name;
        return BASE + "?player=" + name;
    }

    // ---- parsing (schema-agnostic) ----

    private static void parseInto(String body, Cosmetics c) {
        try {
            if (body == null || body.isEmpty()) { c.error = "empty sukie body"; return; }
            JsonElement el = PARSER.parse(body);
            if (!el.isJsonObject()) { c.error = "unexpected sukie response"; return; }
            map(el.getAsJsonObject(), c);
            c.available = true;
        } catch (Exception e) {
            c.error = e.getMessage();
        }
    }

    private static void map(JsonObject o, Cosmetics c) {
        // unwrap a common { "data": {...} } / { "player": {...} } envelope
        if (o.has("data") && o.get("data").isJsonObject()) o = o.getAsJsonObject("data");
        else if (o.has("player") && o.get("player").isJsonObject()) o = o.getAsJsonObject("player");

        for (Map.Entry<String, String> kv : flatten(o, "").entrySet()) {
            String key = kv.getKey();
            String val = kv.getValue();
            String lower = key.toLowerCase();

            if (c.finalKills < 0 && lower.contains("final") && lower.contains("kill")) {
                c.finalKills = parseLong(val);
            } else if (c.bedsDestroyed < 0 && lower.contains("bed")
                    && (lower.contains("destroy") || lower.contains("broken"))) {
                c.bedsDestroyed = parseLong(val);
            } else {
                c.put(prettify(key), val);
            }
        }
    }

    private static Map<String, String> flatten(JsonObject o, String prefix) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonObject()) {
                out.putAll(flatten(v.getAsJsonObject(), key));
            } else if (v.isJsonPrimitive()) {
                out.put(key, v.getAsString());
            } else if (v.isJsonArray()) {
                out.put(key, "[" + v.getAsJsonArray().size() + "]");
            }
        }
        return out;
    }

    private static long parseLong(String s) {
        try { return (long) Double.parseDouble(s.trim()); } catch (Exception e) { return -1; }
    }

    private static String prettify(String key) {
        String last = key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : key;
        last = last.replace('_', ' ').replace('-', ' ').trim();
        if (last.isEmpty()) return key;
        return Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }
}
