package dev.pv.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pv.stats.BedwarsStats;

import java.util.HashMap;
import java.util.Map;

/** Talks to api.hypixel.net using the new API-Key header scheme. */
public final class HypixelApi {

    private HypixelApi() {}

    private static final JsonParser PARSER = new JsonParser();

    public static class HypixelException extends Exception {
        public HypixelException(String msg) { super(msg); }
    }

    /** Fetches and parses a player's BedWars stats. Throws with a friendly message on failure. */
    public static BedwarsStats fetch(String apiKey, String undashedUuid, String name) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new HypixelException("No Hypixel API key set. Use /pv key <key>.");
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("API-Key", apiKey);

        HttpUtil.Response r = HttpUtil.get(
                "https://api.hypixel.net/player?uuid=" + undashedUuid, headers);

        if (r.code == 429) throw new HypixelException("Hypixel rate limit hit - slow down.");
        if (r.code == 403) throw new HypixelException("Hypixel rejected the key (403). Check /pv key.");
        if (r.body == null || r.body.isEmpty()) {
            throw new HypixelException("Empty response from Hypixel (HTTP " + r.code + ").");
        }

        JsonObject root = PARSER.parse(r.body).getAsJsonObject();
        boolean success = root.has("success") && root.get("success").getAsBoolean();
        if (!success) {
            String cause = root.has("cause") ? root.get("cause").getAsString() : "unknown error";
            throw new HypixelException("Hypixel API error: " + cause);
        }
        if (!root.has("player") || root.get("player").isJsonNull()) {
            throw new HypixelException("That player has never logged into Hypixel.");
        }

        JsonObject player = root.getAsJsonObject("player");
        String displayName = player.has("displayname") && !player.get("displayname").isJsonNull()
                ? player.get("displayname").getAsString() : name;
        return BedwarsStats.parse(player, displayName, undashedUuid);
    }
}
