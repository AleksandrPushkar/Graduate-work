package searchengine.searcher;

import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Component
public class TextMatcher {
    public void findMatchesWordInText(String word, String text, TreeMap<Integer, String> indexesAndWords) {
        int pageTextLength = text.length();
        int index = 0;
        while (true) {
            index = text.indexOf(word, index);
            if (index == -1) {
                return;
            }
            int charIndexAfter = index + word.length();
            boolean isSuccessful = checkCharsOnSidesMatch(index, charIndexAfter, text);
            if(isSuccessful) {
                indexesAndWords.put(index, word);
            }
            index = charIndexAfter + 1;
            if (index >= pageTextLength) {
                return;
            }
        }
    }

    public boolean checkCharsOnSidesMatch(int matchStartIndex, int charIndexAfter, String text) {
        if (charIndexAfter < text.length()) {
            return checkChars(matchStartIndex, charIndexAfter, text);
        }
        return checkChars(matchStartIndex, null, text);
    }

    private boolean checkChars(int matchStartIndex, Integer charIndexAfter, String text) {
        boolean charBeforeNotRus = true;
        if (matchStartIndex != 0) {
            charBeforeNotRus = checkCharNonRus(matchStartIndex - 1, text);
        }
        if (charIndexAfter != null) {
            boolean charAfterNotRus = checkCharNonRus(charIndexAfter, text);
            return charBeforeNotRus && charAfterNotRus;
        }
        return charBeforeNotRus;
    }

    private boolean checkCharNonRus(int charIndex, String text) {
        int charCode = text.charAt(charIndex);
        return charCode != 1025 && charCode < 1040
                || charCode > 1103 && charCode != 1105;
    }
}
