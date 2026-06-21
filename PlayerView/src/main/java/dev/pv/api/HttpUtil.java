package dev.pv.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Minimal blocking HTTP client. ALWAYS call from a worker thread, never the render thread. */
public final class HttpUtil {

    private HttpUtil() {}

    public static final class Response {
        public final int code;
        public final String body;
        public Response(int code, String body) { this.code = code; this.body = body; }
        public boolean ok() { return code >= 200 && code < 300; }
    }

    public static Response get(String urlStr) throws Exception {
        return get(urlStr, new HashMap<String, String>());
    }

    public static Response get(String urlStr, Map<String, String> headers) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "PlayerView/1.0 (Forge 1.8.9)");
        conn.setRequestProperty("Accept", "application/json");
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        int code = conn.getResponseCode();
        InputStream in = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String body = in == null ? "" : readAll(in);
        conn.disconnect();
        return new Response(code, body);
    }

    /** Download raw bytes (used for skin PNGs). */
    public static byte[] getBytes(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "PlayerView/1.0 (Forge 1.8.9)");
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
