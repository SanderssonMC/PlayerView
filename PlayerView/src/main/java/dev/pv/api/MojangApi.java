package dev.pv.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.util.Base64;
import java.util.UUID;

/** Resolves usernames to UUIDs and builds GameProfiles (with skin textures) from Mojang. */
public final class MojangApi {

    private MojangApi() {}

    private static final JsonParser PARSER = new JsonParser();

    /** Bundle of everything we resolve for a player. */
    public static final class Resolved {
        public final GameProfile profile; // has the "textures" property attached when available
        public final String skinUrl;      // direct PNG url for the 2D fallback (may be null)
        public Resolved(GameProfile profile, String skinUrl) {
            this.profile = profile;
            this.skinUrl = skinUrl;
        }
    }

    /** Returns the undashed UUID for a name, or null if the player does not exist. */
    public static String uuidForName(String name) throws Exception {
        HttpUtil.Response r = HttpUtil.get("https://api.mojang.com/users/profiles/minecraft/" + name);
        if (r.code == 204 || r.code == 404) return null;          // no such player
        if (!r.ok() || r.body.isEmpty()) return null;
        JsonObject o = PARSER.parse(r.body).getAsJsonObject();
        return o.has("id") ? o.get("id").getAsString() : null;
    }

    /** Adds dashes to a 32-char UUID. */
    public static String dash(String undashed) {
        if (undashed == null || undashed.length() != 32) return undashed;
        return undashed.substring(0, 8) + "-" + undashed.substring(8, 12) + "-"
                + undashed.substring(12, 16) + "-" + undashed.substring(16, 20) + "-"
                + undashed.substring(20);
    }

    /**
     * Fetches the player's profile from the session server (signed, so the client will accept
     * the textures), builds a GameProfile with the "textures" property attached for 3D rendering,
     * and also pulls out the raw skin PNG url for the 2D fallback.
     */
    public static Resolved resolve(String undashedUuid, String name) {
        GameProfile profile = new GameProfile(parseUuid(undashedUuid), name);
        String skinUrl = null;
        try {
            HttpUtil.Response r = HttpUtil.get(
                    "https://sessionserver.mojang.com/session/minecraft/profile/"
                            + undashedUuid + "?unsigned=false");
            if (r.ok() && !r.body.isEmpty()) {
                JsonObject o = PARSER.parse(r.body).getAsJsonObject();
                JsonArray props = o.getAsJsonArray("properties");
                if (props != null) {
                    for (int i = 0; i < props.size(); i++) {
                        JsonObject p = props.get(i).getAsJsonObject();
                        if (!"textures".equals(p.get("name").getAsString())) continue;
                        String value = p.get("value").getAsString();
                        String signature = p.has("signature") ? p.get("signature").getAsString() : null;
                        // attach for the entity renderer
                        if (signature != null) {
                            profile.getProperties().put("textures", new Property("textures", value, signature));
                        } else {
                            profile.getProperties().put("textures", new Property("textures", value));
                        }
                        // decode for the 2D fallback
                        try {
                            String json = new String(Base64.getDecoder().decode(value));
                            JsonObject tex = PARSER.parse(json).getAsJsonObject().getAsJsonObject("textures");
                            if (tex != null && tex.has("SKIN")) {
                                skinUrl = tex.getAsJsonObject("SKIN").get("url").getAsString();
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return new Resolved(profile, skinUrl);
    }

    private static UUID parseUuid(String undashed) {
        try { return UUID.fromString(dash(undashed)); }
        catch (Exception e) { return UUID.randomUUID(); }
    }
}
