package searchengine.indexer;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class WorkingWithUrl {
    private static final String REGEX_URL = "https?://[\\w&&[^_]]+-?[\\w&&[^_]]*\\..+";
    private static final String REGEX_URL_HAS_WWW = "https?://www\\..+";
    private static final String[] invalidEndings4Chars = new String[]{
            ".zip", ".pdf", ".png", ".jpg", ".ico", ".doc"};
    private static final String[] invalidEndings5Chars = new String[]{".json", ".docx", ".jpeg"};

    public String urlCorrection(String url) {
        if (!url.matches(REGEX_URL)) {
            return null;
        }
        if (url.matches(REGEX_URL_HAS_WWW)) {
            if (url.charAt(4) == 's') {
                url = url.substring(0, 8) + url.substring(12);
            } else {
                url = url.substring(0, 7) + url.substring(11);
            }
        }
        if (url.indexOf('/', 8) == -1) {
            if (checkUrlEnding(url)) {
                url += '/';
            }
        }
        return url;
    }

    public boolean checkForDisqualification(String url) {
        URL urlObj = getURL(url);
        return urlObj == null || urlObj.getRef() != null
                || urlObj.getPath().equals("") || !checkUrlEnding(url);
    }

    private boolean checkUrlEnding(String url) {
        String ending = url.substring(url.length() - 3);
        if (ending.equals(".nc")) {
            return false;
        }
        ending = url.substring(url.length() - 4);
        if (checkInvalidUrl(ending)) {
            return false;
        }
        ending = url.substring(url.length() - 5);
        return !checkInvalidUrl(ending);
    }

    private boolean checkInvalidUrl(String ending) {
        String[] invalidEndings;
        if (ending.length() == 4) {
            invalidEndings = invalidEndings4Chars;
        } else {
            invalidEndings = invalidEndings5Chars;
        }
        for (String invalidEnding : invalidEndings) {
            if (ending.equals(invalidEnding)) {
                return true;
            }
        }
        return false;
    }

    public String cutUrlToPath(String url) {
        int index = url.indexOf('/', 8);
        return url.substring(index);
    }

    public URL getURL(String url) {
        try {
            if (url != null) {
                return new URL(url);
            }
        } catch (MalformedURLException ex) {
        }
        return null;
    }
}
