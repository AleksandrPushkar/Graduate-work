package searchengine.indexer;

import java.net.MalformedURLException;
import java.net.URL;

public class WorkUrl {
    private static String regexUrl = "https?://[\\w&&[^_]]+-?[\\w&&[^_]]*\\..+";
    private static String regexUrlHasWww = "https?://www\\..+";

    public static String urlCorrection(String url) {
        if (!url.matches(regexUrl)) {
            return null;
        }

        if (url.matches(regexUrlHasWww)) {
            if (url.charAt(4) == 's') {
                url = url.substring(0, 8) + url.substring(12);
            } else {
                url = url.substring(0, 7) + url.substring(11);
            }
        }

        if (url.indexOf('/', 8) == -1) {
            if (checkUrlEnding(url))
                url += '/';
        }

        return url;
    }

    public static boolean checkForDisqualification(String url) {
        URL urlObj = getURL(url);
        return urlObj == null
                || urlObj.getRef() != null
                || urlObj.getPath().equals("")
                || !checkUrlEnding(url);
    }

    private static boolean checkUrlEnding(String url) {
        String ending1 = url.substring(url.length() - 3);
        String ending2 = url.substring(url.length() - 4);
        String ending3 = url.substring(url.length() - 5);

        return !ending1.equals(".nc") && !ending2.equals(".zip") &&
                !ending2.equals(".pdf") && !ending2.equals(".png") &&
                !ending2.equals(".jpg") && !ending2.equals(".ico") &&
                !ending2.equals(".doc") && !ending3.equals(".json") &&
                !ending3.equals(".docx");
    }

    public static String cutUrlToPath(String url) {
        int index = url.indexOf('/', 8);
        return url.substring(index);
    }

    public static URL getURL(String url) {
        try {
            if (url != null)
                return new URL(url);
        } catch (MalformedURLException ex) {
        }
        return null;
    }
}
