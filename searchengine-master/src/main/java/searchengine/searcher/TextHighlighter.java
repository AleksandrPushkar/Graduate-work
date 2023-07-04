package searchengine.searcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;

@Component
@RequiredArgsConstructor
public class TextHighlighter {
    private final TextMatcher textMatcher;

    public String highlightWordsInTextInBold(String text, Set<String> words) {
        TreeMap<Integer, String> indexesMatchesAndWords = findWordsInText(text, words);
        if(indexesMatchesAndWords.isEmpty()) {
            return null;
        }
        return highlightWordsInText(text, indexesMatchesAndWords);
    }

    private TreeMap<Integer, String> findWordsInText(String text, Set<String> words) {
        TreeMap<Integer, String> indexesMatchesAndWords = new TreeMap<>();
        text = text.toLowerCase(Locale.ROOT);
        for (String word : words) {
            textMatcher.findMatchesWordInText(word, text, false, indexesMatchesAndWords);
        }
        return indexesMatchesAndWords;
    }

    private String highlightWordsInText(String text, TreeMap<Integer, String> indexesMatchesAndWords) {
        StringBuilder builder = new StringBuilder(text);
        int countCharsAdded = 0;
        for (Entry<Integer, String> entry : indexesMatchesAndWords.entrySet()) {
            int wordStartIndex = entry.getKey() + countCharsAdded;
            int wordEndIndex = entry.getKey() + entry.getValue().length() + countCharsAdded;
            highlightWord(wordStartIndex, wordEndIndex, builder);
            countCharsAdded += 7;
        }
        return builder.toString();
    }

    private void highlightWord(int wordStartIndex, int wordEndIndex, StringBuilder builder) {
        builder.insert(wordStartIndex, "<b>");
        builder.insert(wordEndIndex + 3, "</b>");
    }
}
