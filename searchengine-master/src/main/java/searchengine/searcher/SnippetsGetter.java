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
import java.util.stream.Collectors;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Component
@RequiredArgsConstructor
public class SnippetsGetter {
    private static final int REQUIRED_SNIPPET_LENGTH = 245;
    private final LemmaFinder lemmaFinder;
    private final TextMatcher textMatcher;
    private final TextHighlighter textHighlighter;

    public boolean getSnippets(SnippetGetterData snippetGetterData) {
        String query = snippetGetterData.getQuery();
        Map<String, Set<String>> lemmasAndWordForms = getWordFormsLemmas(snippetGetterData.getLemmas());
        Set<String> queryWords = getQueryWordsThatParticipatedInSearch(query, lemmasAndWordForms);
        Set<String> wordFormsAllLemmas = getWordFormsInOneSet(lemmasAndWordForms.values());
        for (SearchPage page : snippetGetterData.getPages()) {
            String pageText = getPageText(page);
            String snippet = getSnippet(pageText, queryWords, lemmasAndWordForms.values());
            if (snippet == null) {
                return false;
            }
            snippet = textHighlighter.highlightWordsInTextInBold(snippet, wordFormsAllLemmas);
            if (snippet == null) {
                return false;
            }
            page.setSnippet(snippet);
        }
        return true;
    }

    private Map<String, Set<String>> getWordFormsLemmas(Set<String> lemmas) {
        Map<String, Set<String>> lemmasAndWordForms = new HashMap<>();
        for (String lemma : lemmas) {
            lemmasAndWordForms.put(lemma, getWordFormsLemma(lemma));
        }
        return  lemmasAndWordForms;
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

    private Set<String> getQueryWordsThatParticipatedInSearch(String query,
                                                              Map<String, Set<String>> lemmasAndWordForms){
        Set<String> queryWords = lemmaFinder.excludeParticles(query);
        excludeWordsThatWereNotTakenWhenSearchPages(queryWords, lemmasAndWordForms);
        return queryWords;
    }

    private void excludeWordsThatWereNotTakenWhenSearchPages(Set<String> queryWords,
                                                             Map<String, Set<String>> lemmasAndWordForms) {
        Iterator<String> queryWordIterator = queryWords.iterator();
        while (queryWordIterator.hasNext()) {
            boolean excludeWord = checkWordLemmasThatTheyWereNotIncludedInSearch(queryWordIterator.next(),
                    lemmasAndWordForms);
            if (excludeWord) {
                queryWordIterator.remove();
            }
        }
    }

    private boolean checkWordLemmasThatTheyWereNotIncludedInSearch(String queryWord,
                                                                   Map<String, Set<String>> lemmasAndWordForms) {
        List<String> queryWordLemmas = lemmaFinder.getWordLemmas(queryWord);
        for (String queryWordLemma : queryWordLemmas) {
            if (lemmasAndWordForms.containsKey(queryWordLemma)) {
                lemmasAndWordForms.get(queryWordLemma).add(queryWord);
                return false;
            }
        }
        return true;
    }

    private Set<String> getWordFormsInOneSet(Collection<Set<String>> wordFormsLemmas) {
        return wordFormsLemmas.stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    private String getPageText(SearchPage searchPage) {
        EntityPage entityPage = searchPage.getEntityPage();
        Document doc = Jsoup.parse(entityPage.getContent());
        searchPage.setTitle(doc.title());
        return doc.text();
    }

    private String getSnippet(String pageText, Set<String> queryWords, Collection<Set<String>> wordFormsLemmas) {
        if (pageText.length() < REQUIRED_SNIPPET_LENGTH) {
            return pageText;
        }
        String snippet = getSnippetByQueryWords(pageText, queryWords);
        if (snippet == null) {
            snippet = getSnippetByWordForms(pageText, wordFormsLemmas);
        }
        return snippet;
    }

    private String getSnippetByQueryWords(String pageText, Set<String> queryWords) {
        TreeMap<Integer, String> matchesIndexesAndQueryWords = new TreeMap<>();
        for (String queryWord : queryWords) {
            boolean matchFound = textMatcher.findMatchesWordInText(queryWord, pageText, true,
                    matchesIndexesAndQueryWords);
            if (!matchFound) {
                return null;
            }
        }
        return buildSnippetFromMatches(pageText, matchesIndexesAndQueryWords);
    }

    private String buildSnippetFromMatches(String pageText, TreeMap<Integer, String> indexesAndMatches) {
        int beginIndex = indexesAndMatches.firstKey();
        int endIndex = indexesAndMatches.lastKey() + indexesAndMatches.lastEntry().getValue().length();
        int snippetLength = endIndex - beginIndex;
        if (snippetLength < REQUIRED_SNIPPET_LENGTH) {
            String snippet = pageText.substring(beginIndex, endIndex);
            return increaseSnippetLength(snippet, pageText, beginIndex, endIndex);
        }
        int[] numberCharsBetweenMatches = getArrayWithNumberCharsBetweenMatches(pageText.length(), indexesAndMatches);
        TreeMap<Integer, int[]> indexesMatchesAndNumberChars = splitCharsBetweenMatches(numberCharsBetweenMatches,
                indexesAndMatches.keySet());
        int requiredNumberCharsInTwoArraysOneMatch = (REQUIRED_SNIPPET_LENGTH
                - getTotalLengthAllWordsInMatches(indexesAndMatches)) / indexesAndMatches.size();
        adjustNumberCharsMatches(requiredNumberCharsInTwoArraysOneMatch, indexesMatchesAndNumberChars);
        return executeBuildSnippetFromMatches(pageText, indexesMatchesAndNumberChars, indexesAndMatches);
    }

    private String increaseSnippetLength(String snippet, String text, int matchBeginIndex, int matchEndIndex) {
        String[] wordsBeforeMatches = getArrayWordsNotIncludedInSnippet(0, matchBeginIndex, text);
        String[] wordsAfterMatches = getArrayWordsNotIncludedInSnippet(matchEndIndex, null, text);
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

    private int[] getArrayWithNumberCharsBetweenMatches(int pageTextLength,
                                                        TreeMap<Integer, String> indexesAndMatches) {
        int[] numberChars = new int[indexesAndMatches.size() + 1];
        int endPreviousMatch = 0;
        int indexInArray = 0;
        for (Entry<Integer, String> entry : indexesAndMatches.entrySet()) {
            numberChars[indexInArray] = entry.getKey() - endPreviousMatch;
            endPreviousMatch = entry.getKey() + entry.getValue().length();
            indexInArray++;
            if (entry.equals(indexesAndMatches.lastEntry())) {
                numberChars[indexInArray] = pageTextLength - endPreviousMatch;
            }
        }
        return numberChars;
    }

    private TreeMap<Integer, int[]> splitCharsBetweenMatches(int[] numberCharsBetweenMatches,
                                                             Set<Integer> matchesIndexes) {
        TreeMap<Integer, int[]> matchesIndexesAndNumberChars = new TreeMap<>();
        int i = 0;
        for (Integer matchIndex : matchesIndexes) {
            int[] numberChars = new int[2];
            if (i == 0) {
                numberChars[0] = numberCharsBetweenMatches[i];
            } else {
                numberChars[0] = numberCharsBetweenMatches[i] / 2;
            }
            i++;
            if (i == matchesIndexes.size()) {
                numberChars[1] = numberCharsBetweenMatches[i];
            } else {
                numberChars[1] = numberCharsBetweenMatches[i] / 2;
            }
            matchesIndexesAndNumberChars.put(matchIndex, numberChars);
        }
        return matchesIndexesAndNumberChars;
    }

    private int getTotalLengthAllWordsInMatches(TreeMap<Integer, String> indexesAndMatches) {
        int totalLength = 0;
        for (String word : indexesAndMatches.values()) {
            totalLength += word.length();
        }
        return totalLength + (indexesAndMatches.size() - 1);
    }

    private void adjustNumberCharsMatches(int requiredNumberCharsInTwoArraysOneMatch,
                                          TreeMap<Integer, int[]> matchesIndexesAndNumberChars) {
        Map<Integer, Integer> matchesIndexesAndNumberMissingChars = findMatchesWithSmallNumberChars(
                requiredNumberCharsInTwoArraysOneMatch, matchesIndexesAndNumberChars);
        int totalNumberMissingChars = calculateTotalNumberMissingChars(matchesIndexesAndNumberMissingChars.values());
        int requiredNumberCharsInOneArray = getLengthForOneArrayTheMatch(requiredNumberCharsInTwoArraysOneMatch);
        for (Entry<Integer, int[]> entry : matchesIndexesAndNumberChars.entrySet()) {
            if (matchesIndexesAndNumberMissingChars.containsKey(entry.getKey())) {
                continue;
            }
            totalNumberMissingChars = AdjustNumberCharsMatch(requiredNumberCharsInOneArray, totalNumberMissingChars,
                    entry.getValue());
        }
    }

    private Map<Integer, Integer> findMatchesWithSmallNumberChars(int requiredNumberChars,
                                                                  TreeMap<Integer, int[]>
                                                                          matchesIndexesAndNumberChars) {
        Map<Integer, Integer> matchesIndexesAndNumberMissingChars = new HashMap<>();
        for (Entry<Integer, int[]> entry : matchesIndexesAndNumberChars.entrySet()) {
            int[] numberChars = entry.getValue();
            int totalNumberChars = numberChars[0] + numberChars[1];
            int numberMissingChars = totalNumberChars - requiredNumberChars;
            if (numberMissingChars < 0) {
                numberMissingChars = Math.abs(numberMissingChars);
                matchesIndexesAndNumberMissingChars.put(entry.getKey(), numberMissingChars);
            }
        }
        return matchesIndexesAndNumberMissingChars;
    }

    private int calculateTotalNumberMissingChars(Collection<Integer> numberMissingChars) {
        int totalNumberMissingChars = 0;
        for (Integer missingChars : numberMissingChars) {
            totalNumberMissingChars += missingChars;
        }
        return totalNumberMissingChars;
    }

    private int getLengthForOneArrayTheMatch(int requiredNumberCharsInTwoArraysOneMatch) {
        if (requiredNumberCharsInTwoArraysOneMatch <= 0) {
            return 0;
        }
        if (requiredNumberCharsInTwoArraysOneMatch == 1) {
            return 1;
        }
        return requiredNumberCharsInTwoArraysOneMatch / 2;
    }

    private Integer AdjustNumberCharsMatch(int requiredNumberCharsInOneArray,
                                           int totalNumberMissingChars, int[] numberCharsArray) {
        int firstElementIndex = 0;
        int secondElementIndex = 1;
        int lessChars = numberCharsArray[firstElementIndex];
        int moreChars = numberCharsArray[secondElementIndex];
        if (lessChars > moreChars) {
            int temp = moreChars;
            moreChars = lessChars;
            lessChars = temp;
            firstElementIndex = 1;
            secondElementIndex = 0;
        }
        Integer result = adjustOneNumberChars(firstElementIndex, lessChars, requiredNumberCharsInOneArray,
                totalNumberMissingChars, numberCharsArray);
        if (result == null) {
            requiredNumberCharsInOneArray += (requiredNumberCharsInOneArray - lessChars);
        } else {
            totalNumberMissingChars = result;
        }
        return adjustOneNumberChars(secondElementIndex, moreChars, requiredNumberCharsInOneArray,
                totalNumberMissingChars, numberCharsArray);
    }

    private Integer adjustOneNumberChars(int arrayIndex, int numberCharsInArray, int requiredNumberCharsInOneArray,
                                         int totalNumberMissingChars, int[] numberCharsArray) {
        if (numberCharsInArray < requiredNumberCharsInOneArray) {
            return null;
        }
        int numberFreeChars = numberCharsInArray - requiredNumberCharsInOneArray;
        if (numberFreeChars <= totalNumberMissingChars) {
            return totalNumberMissingChars - numberFreeChars;
        }
        numberFreeChars -= totalNumberMissingChars;
        numberCharsArray[arrayIndex] = numberCharsInArray - numberFreeChars;
        return 0;
    }

    private String executeBuildSnippetFromMatches(String pageText, TreeMap<Integer, int[]> matchesIndexesAndNumberChars,
                                                  TreeMap<Integer, String> indexesAndMatches) {
        StringBuilder builder = new StringBuilder();
        for (Entry<Integer, int[]> entry : matchesIndexesAndNumberChars.entrySet()) {
            String word = indexesAndMatches.get(entry.getKey());
            int[] numberChars = entry.getValue();
            int numberCharsFromLeft = numberChars[0];
            int numberCharsFromRight = numberChars[1];
            addSubstringToStringBuilder(true, numberCharsFromLeft, entry.getKey(),
                    null, pageText, builder);
            builder.append(word);
            addSubstringToStringBuilder(false, numberCharsFromRight, null,
                    entry.getKey() + word.length(), pageText, builder);
            if (!entry.equals(matchesIndexesAndNumberChars.lastEntry())) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    private void addSubstringToStringBuilder(boolean fromLeft, int numberCharsInSubstring, Integer matchBeginIndex,
                                             Integer matchEndIndex, String pageText, StringBuilder builder) {
        if (numberCharsInSubstring == 0) {
            return;
        }
        if (fromLeft) {
            String leftSubstring = pageText.substring(matchBeginIndex - numberCharsInSubstring, matchBeginIndex);
            leftSubstring = adjustOneSideSubstring(true, matchBeginIndex, null,
                    leftSubstring, pageText);
            builder.append(leftSubstring);
            return;
        }
        String rightSubstring = pageText.substring(matchEndIndex, matchEndIndex + numberCharsInSubstring);
        rightSubstring = adjustOneSideSubstring(false, null, matchEndIndex,
                rightSubstring, pageText);
        builder.append(rightSubstring);
    }

    private String adjustOneSideSubstring(boolean fromLeft, Integer matchBeginIndex, Integer matchEndIndex,
                                          String substring, String pageText) {
        if (fromLeft) {
            if (substring.charAt(0) == ' ') {
                return removeSpaceOnOneSide(true, substring);
            }
            return deletePartWordOnOneSideIfWasBreak(true, matchBeginIndex, null,
                    substring, pageText);
        }
        if (substring.charAt(substring.length() - 1) == ' ') {
            return removeSpaceOnOneSide(false, substring);
        }
        return deletePartWordOnOneSideIfWasBreak(false, null, matchEndIndex, substring, pageText);
    }

    private String removeSpaceOnOneSide(boolean fromLeft, String substring) {
        int numberSpaces = getNumberSpaces(fromLeft, substring);
        if (numberSpaces == 0) {
            return substring;
        }
        if (fromLeft) {
            return substring.substring(numberSpaces);
        }
        return substring.substring(0, substring.length() - numberSpaces);
    }

    private int getNumberSpaces(boolean fromLeft, String substring) {
        int count = 0;
        while (true) {
            if (count == substring.length()) {
                break;
            }
            char symbol;
            if (fromLeft) {
                symbol = substring.charAt(count);
            } else {
                symbol = substring.charAt(substring.length() - 1 - count);
            }
            if (symbol != ' ') {
                break;
            }
            count++;
        }
        return count;
    }

    private String deletePartWordOnOneSideIfWasBreak(boolean fromLeft, Integer matchBeginIndex, Integer matchEndIndex,
                                                     String substring, String pageText) {
        int substringLength = substring.length();
        int charIndex;
        if (fromLeft) {
            charIndex = matchBeginIndex - substringLength - 1;
        } else {
            charIndex = matchEndIndex + substringLength;
        }
        if (charIndex < 0 || charIndex == pageText.length()) {
            return substring;
        }
        if (textMatcher.checkChar(true, charIndex, pageText)) {
            return substring;
        }
        if (fromLeft) {
            return removeBreak(true, substring);
        }
        return removeBreak(false, substring);
    }

    private String removeBreak(boolean fromLeft, String substring) {
        int spaceIndex;
        if (fromLeft) {
            spaceIndex = substring.indexOf(' ');
        } else {
            spaceIndex = substring.lastIndexOf(' ');
        }
        if (spaceIndex == -1) {
            return "";
        }
        if (fromLeft) {
            substring = substring.substring(spaceIndex + 1);
            return removeSpaceOnOneSide(true, substring);
        }
        substring = substring.substring(0, spaceIndex);
        return removeSpaceOnOneSide(false, substring);
    }

    private String getSnippetByWordForms(String pageText, Collection<Set<String>> wordFormsLemmas) {
        pageText = pageText.toLowerCase(Locale.ROOT);
        TreeMap<Integer, String> matchesIndexesAndWordForms = new TreeMap<>();
        for (Set<String> wordFormsLemma : wordFormsLemmas) {
            if (!findWordForm(pageText, wordFormsLemma, matchesIndexesAndWordForms)) {
                return null;
            }
        }
        return buildSnippetFromMatches(pageText, matchesIndexesAndWordForms);
    }

    private boolean findWordForm(String pageText, Set<String> wordFormsLemma,
                                 TreeMap<Integer, String> matchesIndexesAndWordForms) {
        for (String wordFormLemma : wordFormsLemma) {
            boolean matchFound = textMatcher.findMatchesWordInText(wordFormLemma, pageText,
                    true, matchesIndexesAndWordForms);
            if (matchFound) {
                return true;
            }
        }
        return false;
    }
}