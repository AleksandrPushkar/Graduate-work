package searchengine.searcher;

import com.github.demidko.aot.WordformMeaning;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.model.EntityPage;
import searchengine.indexer.LemmaFinder;

import java.util.*;
import java.util.Map.Entry;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Component
@RequiredArgsConstructor
public class SnippetsGetter {
    private static final int REQUIRED_SNIPPET_LENGTH = 245;
    private final LemmaFinder lemmaFinder;
    private final TextMatcher textMatcher;
    private final TextHighlighter textHighlighter;

    public boolean getSnippets(SnippetGetterData snippetGetterData) {
        for (SearchPage page : snippetGetterData.getPages()) {
            String pageText = getPageText(page);
            TreeMap<Integer, String> indexesAndWordForms = findWordFormsInPageText(
                    pageText, snippetGetterData.getLemmas());
            if (indexesAndWordForms.isEmpty()) {
                return false;
            }
            String snippet = getSnippet(snippetGetterData.getQuery(), pageText, indexesAndWordForms);
            snippet = textHighlighter.highlightWordsInTextInBold(snippet, indexesAndWordForms.values());
            page.setSnippet(snippet);
        }
        return true;
    }

    private String getPageText(SearchPage searchPage) {
        EntityPage entityPage = searchPage.getEntityPage();
        Document doc = Jsoup.parse(entityPage.getContent());
        searchPage.setTitle(doc.title());
        return doc.text();
    }

    private TreeMap<Integer, String> findWordFormsInPageText(String pageText, List<String> lemmas) {
        TreeMap<Integer, String> indexesAndWordForms = new TreeMap<>();
        pageText = pageText.toLowerCase(Locale.ROOT);
        for (String lemma : lemmas) {
            for (String wordForm : getWordFormsLemma(lemma)) {
                textMatcher.findMatchesWordInText(wordForm, pageText, indexesAndWordForms);
            }
        }
        return indexesAndWordForms;
    }

    private Set<String> getWordFormsLemma(String lemma) {
        Set<String> uniqueWordFormsLemma = new HashSet<>();
        List<WordformMeaning> wordFormsDiffMeanings = lookupForMeanings(lemma);
        if (wordFormsDiffMeanings.isEmpty()) {
            uniqueWordFormsLemma.add(lemma);
        }
        for (WordformMeaning wordFormMeaning : wordFormsDiffMeanings) {
            for (WordformMeaning wordForm : wordFormMeaning.getTransformations()) {
                uniqueWordFormsLemma.add(wordForm.toString());
            }
        }
        return uniqueWordFormsLemma;
    }

    private String getSnippet(String query, String pageText, TreeMap<Integer, String> indexesAndWordForms) {
        if (pageText.length() < REQUIRED_SNIPPET_LENGTH) {
            return pageText;
        }
        String snippet = getSnippetFromFirstToLatestMatch(pageText, indexesAndWordForms);
        if (snippet.length() == REQUIRED_SNIPPET_LENGTH) {
            return snippet;
        }
        if (snippet.length() > REQUIRED_SNIPPET_LENGTH) {
            return reduceSnippetLength(query, snippet, pageText, indexesAndWordForms);
        }
        int matchStartIndex = indexesAndWordForms.firstKey();
        Entry<Integer, String> entry = indexesAndWordForms.lastEntry();
        int matchEndIndex = entry.getKey() + entry.getValue().length();
        return increaseSnippetLength(snippet, pageText, matchStartIndex, matchEndIndex);
    }

    private String getSnippetFromFirstToLatestMatch(String pageText, TreeMap<Integer, String> indexesAndWordForms) {
        if (indexesAndWordForms.size() == 1) {
            return indexesAndWordForms.firstEntry().getValue();
        }
        int startIndex = indexesAndWordForms.firstKey();
        Entry<Integer, String> entry = indexesAndWordForms.lastEntry();
        int finishIndex = entry.getKey() + entry.getValue().length();
        return pageText.substring(startIndex, finishIndex);
    }

    private String reduceSnippetLength(String query, String snippetFromLemmas, String pageText,
                                       TreeMap<Integer, String> indexesAndWordForms) {
        String snippetFromQueryWords = getSnippetByQueryWords(query, pageText, indexesAndWordForms);
        if (snippetFromQueryWords != null) {
            return snippetFromQueryWords;
        }
        StringBuilder snippetBuilder = getStringBuilderWithThreeWords(pageText, indexesAndWordForms.firstKey());
        String[] words = snippetFromLemmas.split("\\s+");
        for (String word : words) {
            int futureSnippetLength = snippetBuilder.length() + 1 + word.length();
            if (futureSnippetLength > REQUIRED_SNIPPET_LENGTH) {
                break;
            }
            snippetBuilder.append(" ");
            snippetBuilder.append(word);
        }
        return snippetBuilder.toString();
    }

    private String getSnippetByQueryWords(String query, String pageText, TreeMap<Integer, String> indexesAndWordForms) {
        TreeMap<Integer, String> indexesAndQueryWords = findQueryWordsInMatchesWordForms(query, indexesAndWordForms);
        if (indexesAndQueryWords.size() == 0) {
            return null;
        }
        int startIndex = indexesAndQueryWords.firstKey();
        int endIndex = indexesAndQueryWords.lastKey() + indexesAndQueryWords.lastEntry().getValue().length();
        int snippetLength = endIndex - startIndex;
        if (snippetLength == REQUIRED_SNIPPET_LENGTH) {
            return pageText.substring(startIndex, endIndex);
        }
        if (snippetLength > REQUIRED_SNIPPET_LENGTH) {
            return getSnippetFromQueryWordsEvenShorterLength(pageText, indexesAndQueryWords);
        }
        String snippet = pageText.substring(startIndex, endIndex);
        return increaseSnippetLength(snippet, pageText, startIndex, endIndex);
    }

    private TreeMap<Integer, String> findQueryWordsInMatchesWordForms(String query,
                                                                      TreeMap<Integer, String> indexesAndWordForms) {
        Set<String> queryWords = lemmaFinder.excludeParticles(query);
        TreeMap<Integer, String> indexesAndQueryWords = new TreeMap<>();
        for (String queryWord : queryWords) {
            if (indexesAndWordForms.containsValue(queryWord)) {
                addQueryWordInMap(queryWord, indexesAndQueryWords, indexesAndWordForms);
            }
        }
        return indexesAndQueryWords;
    }

    private void addQueryWordInMap(String queryWord, TreeMap<Integer, String> indexesAndQueryWords,
                                                     TreeMap<Integer, String> indexesAndWordForms) {
        for (Entry<Integer, String> entry : indexesAndWordForms.entrySet()) {
            if (entry.getValue().equals(queryWord)) {
                indexesAndQueryWords.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private String getSnippetFromQueryWordsEvenShorterLength(String pageText,
                                                             TreeMap<Integer, String> indexesAndQueryWords) {
        int startIndex = indexesAndQueryWords.firstKey();
        int finishIndex = 0;
        for (Entry<Integer, String> entry : indexesAndQueryWords.entrySet()) {
            int matchEndIndex = entry.getKey() + entry.getValue().length();
            int estimatedSnippetLength = matchEndIndex - startIndex;
            if (estimatedSnippetLength > REQUIRED_SNIPPET_LENGTH) {
                break;
            }
            finishIndex = matchEndIndex;
        }
        if (finishIndex == 0) {
            return null;
        }
        String snippet = pageText.substring(startIndex, finishIndex);
        return increaseSnippetLength(snippet, pageText, startIndex, finishIndex);
    }

    private StringBuilder getStringBuilderWithThreeWords(String pageText, int matchStartIndex) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] wordsBeforeMatches = getArrayWordsNotIncludedInSnippet(0, matchStartIndex, pageText);
        for (int i = wordsBeforeMatches.length; i > 0; i--) {
            String word = wordsBeforeMatches[i - 1];
            stringBuilder.insert(0, word);
            if ((i == wordsBeforeMatches.length - 2) || i == 1) {
                break;
            }
            stringBuilder.insert(0, " ");
        }
        return stringBuilder;
    }

    private String increaseSnippetLength(String snippet, String text, int startIndex, int endIndex) {
        String[] wordsBeforeMatches = getArrayWordsNotIncludedInSnippet(0, startIndex, text);
        String[] wordsAfterMatches = getArrayWordsNotIncludedInSnippet(endIndex, null, text);
        int i = 1;
        StringBuilder snippetBuilder = new StringBuilder(snippet);
        while (true) {
            String word;
            boolean isInserted = true;
            if (i <= wordsBeforeMatches.length) {
                word = wordsBeforeMatches[wordsBeforeMatches.length - i];
                isInserted = insertWordInSnippet(word, true, snippetBuilder);
            }
            if (!isInserted) {
                break;
            }
            if (i <= wordsAfterMatches.length) {
                word = wordsAfterMatches[i - 1];
                isInserted = insertWordInSnippet(word, false, snippetBuilder);
            }
            if (!isInserted) {
                break;
            }
            i++;
        }
        return snippetBuilder.toString();
    }

    private String[] getArrayWordsNotIncludedInSnippet(int start, Integer stop, String text) {
        if ((stop != null && stop == 0) || start == text.length()) {
            return new String[0];
        }
        String substring;
        if (stop == null) {
            substring = text.substring(start);
        } else {
            substring = text.substring(start, stop);
        }
        substring = substring.trim();
        return substring.split("\\s+");
    }

    private boolean insertWordInSnippet(String word, boolean inBegin, StringBuilder snippetBuilder) {
        int snippetLengthAfterIncrease = snippetBuilder.length() + 1 + word.length();
        if (snippetLengthAfterIncrease > REQUIRED_SNIPPET_LENGTH) {
            return false;
        }
        if (inBegin) {
            snippetBuilder.insert(0, " ");
            snippetBuilder.insert(0, word);
        } else {
            snippetBuilder.append(" ");
            snippetBuilder.append(word);
        }
        return true;
    }
}
