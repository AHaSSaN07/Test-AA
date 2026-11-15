package io.github.ahassan07.carvideo;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/** Settings shared between the phone screen and the car screen. */
final class Prefs {
    static final String DEFAULT_HOME = "https://m.youtube.com/";
    static final float SCALE_SMALL = 1.0f;
    static final float SCALE_MEDIUM = 1.3f;
    static final float SCALE_LARGE = 1.6f;

    private static final String FILE = "settings";
    private static final String KEY_HOME = "home";
    private static final String KEY_SITES = "sites";
    private static final String KEY_SCALE = "scale";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String home(Context context) {
        String url = toUrl(prefs(context).getString(KEY_HOME, DEFAULT_HOME));
        return url != null ? url : DEFAULT_HOME;
    }

    static String sitesText(Context context) {
        return prefs(context).getString(KEY_SITES, "");
    }

    /** The extra sites as full addresses, in the order the user typed them. */
    static List<String> sites(Context context) {
        List<String> result = new ArrayList<>();
        for (String line : sitesText(context).split("\n")) {
            String url = toUrl(line);
            if (url != null && !result.contains(url)) {
                result.add(url);
            }
        }
        return result;
    }

    static float scale(Context context) {
        float scale = prefs(context).getFloat(KEY_SCALE, SCALE_MEDIUM);
        return Math.max(0.8f, Math.min(2.0f, scale));
    }

    static void save(Context context, String home, String sites, float scale) {
        prefs(context).edit()
                .putString(KEY_HOME, home == null ? "" : home.trim())
                .putString(KEY_SITES, sites == null ? "" : sites.trim())
                .putFloat(KEY_SCALE, scale)
                .apply();
    }

    /** Turns "vimeo.com" into "https://vimeo.com"; returns null for blank or unusable text. */
    static String toUrl(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.isEmpty() || value.contains(" ")) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.contains("://") || !value.contains(".")) {
            return null;
        }
        return "https://" + value;
    }

    /** A web address is opened as is; anything else becomes a YouTube search. */
    static String fromInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String url = toUrl(text);
        if (url != null) {
            return url;
        }
        return "https://m.youtube.com/results?search_query=" + Uri.encode(text.trim());
    }

    /** Short label for a site row, e.g. "vimeo.com". */
    static String label(String url) {
        String host = Uri.parse(url).getHost();
        if (host == null || host.isEmpty()) {
            return url;
        }
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
