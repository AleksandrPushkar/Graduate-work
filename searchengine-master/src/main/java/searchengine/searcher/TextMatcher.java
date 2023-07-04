package searchengine.searcher;

import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Component
public class TextMatcher {
    public boolean findMatchesWordInText(String word, String text, boolean oneMatch,
                                         TreeMap<Integer, String> indexesAndWords) {
        int additionsCount = 0;
        int pageTextLength = text.length();
        int index = 0;
        while (true) {
            index = text.indexOf(word, index);
            if (index == -1) {
                return false;
            }
            int charIndexAfter = index + word.length();
            boolean isSuccessful = checkCharsOnSidesMatch(index, charIndexAfter, text);
            if (isSuccessful) {
                indexesAndWords.put(index, word);
                additionsCount += 1;
            }
            if (isSuccessful && oneMatch) {
                return true;
            }
            index = charIndexAfter + 1;
            if (index >= pageTextLength && additionsCount > 0) {
                return true;
            } else if (index >= pageTextLength) {
                return false;
            }
        }
    }

    private boolean checkCharsOnSidesMatch(int matchStartIndex, int charIndexAfter, String text) {
        if (charIndexAfter < text.length()) {
            return checkChars(matchStartIndex, charIndexAfter, text);
        }
        return checkChars(matchStartIndex, null, text);
    }

    private boolean checkChars(int matchStartIndex, Integer charIndexAfter, String text) {
        boolean charBeforeNotRus = true;
        if (matchStartIndex != 0) {
            charBeforeNotRus = checkChar(false, matchStartIndex - 1, text);
        }
        if (charIndexAfter != null) {
            boolean charAfterNotRus = checkChar(false, charIndexAfter, text);
            return charBeforeNotRus && charAfterNotRus;
        }
        return charBeforeNotRus;
    }

    public boolean checkChar(boolean makeCheckNotHyphen, int charIndex, String text) {
        int charCode = text.charAt(charIndex);
        boolean charNotRus = checkCharNonRus(charCode);
        if (makeCheckNotHyphen) {
            return charNotRus && charCode != '-';
        }
        return charNotRus;
    }

    private boolean checkCharNonRus(int charCode) {
        return charCode != 1025 && charCode < 1040
                || charCode > 1103 && charCode != 1105;
    }
}
