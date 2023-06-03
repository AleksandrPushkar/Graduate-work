package searchengine.workersservices.searcher;

import com.github.demidko.aot.WordformMeaning;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.exceptions.search.LemmasFinderNotReadyWorkException;
import searchengine.model.EntityPage;
import searchengine.workersservices.indexer.LemmaFinder;

import java.util.*;
import java.util.Map.Entry;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@RequiredArgsConstructor
public class MinerSnippet {
    private static final int REQUIRED_SNIPPET_LENGTH = 245;
    private final String query;
    private final List<SearchPage> pages;
    private final HashSet<String> lemmas;
    private String pageText;
    private TreeMap<Integer, String> indexesAndWordForms;

    /*Метод осуществляет обход списка страниц. В процессе обхода
    получает текст страницы, ищет словоформы в тексте страницы.
    Если словоформы в тексте не найдены, то метод прирывается.
    Получает сниппет, выделяет совпадения и добавляет сниппет
    в объект SearchPage*/
    public void mine() {
        for (SearchPage page : pages) {
            String pageTextInLowercase = getPageText(page);
            findWordFormsInPageText(pageTextInLowercase);
            if (indexesAndWordForms.size() == 0) {
                pages.clear();
                return;
            }

            String snippet = getSnippet();
            snippet = highlightMatchesInBold(snippet);
            page.setSnippet(snippet);
        }
    }

    /*Данный метод получает сущность страницы из SearchPage.
    С помощью библиотеки Jsoup парсит контент страницы(html-код),
    получает заголовок страницы и добавляет его в в объект SearchPage,
    получает текст страницы. Далее переводит текст страницы в нижний
    регистр(нужно для поиска словоформ, так как словоформы
    идут в нижнем регистре) и возвращает его*/
    private String getPageText(SearchPage page) {
        EntityPage entityPage = page.getEntityPage();
        Document doc = Jsoup.parse(entityPage.getContent());
        page.setTitle(doc.title());
        pageText = doc.text();
        return pageText.toLowerCase(Locale.ROOT);
    }

    /*Данный метод обходит список лемм. В процессе обхода метод получает
    для каждой леммы набор словоформ и производит поиск каждой слоформы
    в тексте страницы*/
    private void findWordFormsInPageText(String pageTextInLowercase) {
        indexesAndWordForms = new TreeMap<>();
        for (String lemma : lemmas) {
            HashSet<String> wordFormsLemma = getWordFormsLemma(lemma);
            for (String wordForm : wordFormsLemma) {
                findMatchesWordInText(wordForm, true,
                        pageTextInLowercase, indexesAndWordForms);
            }
        }
    }

    /*Метод lookupForMeanings(String word) - возвращает список словоформ имеющих
    разное значений, например лемма замок(устройство для запирания дверей) это
    одновременно производная леммы "замокнуть" (под дождем например).
    Метод getTransformations() вызывается у словоформы с определенным значением
    и возвращает все словоформы по общей лемме.
    Во втором цикле for осуществляется обход всех словоформ леммы определенного
    значения, и добавления словоформ в набор. Набор содержит только уникальные
    словоформы, это делается для того, чтобы исключить повторения словоформ с
    одинаковым написанием.*/
    private HashSet<String> getWordFormsLemma(String lemma) {
        HashSet<String> uniqueWordFormsLemma = new HashSet<>();
        List<WordformMeaning> wordFormsDiffMeanings
                = lookupForMeanings(lemma);

        for (WordformMeaning wordFormMeaning : wordFormsDiffMeanings) {
            for (WordformMeaning wordForm : wordFormMeaning.getTransformations()) {
                uniqueWordFormsLemma.add(wordForm.toString());
            }
        }

        return uniqueWordFormsLemma;
    }

    /*Если параметр moreOne = true, тогда в тексте ищутся все совпадения, если false,
    тогда при первом нахождении совпадения метод завершается.

    Логика работы цикла:
    if (1) Если совпадения не найдено, метод завершается.

    if (2) Проверяет, есть ли еще символы после совпадения.Если есть, проверяет символы
    перед совпадением и после совпадения, чтобы это не были символ русского алфавита(таким
    образом, исключает совпадения в словофоме). Если это не символ русского алфавита, тогда
    добавляет совпадения в TreeMap. Дальше проверяет, есть ли последующий символ после
    символа, который идет после совпадения. Если есть, тогда цикл продолжается. Если
    символа нет, тогда метод завершается.

    Если второй оператор if не сработал(после совпадения нет символов), тогда проверяет
    символ перед совпадением, если это символ не русского алфавита, добавляет совпадения
    в TreeMap. Метод завершается.*/

    private void findMatchesWordInText(String word, boolean moreOne, String text,
                                       TreeMap<Integer, String> indexesAndWords) {
        int pageTextLength = text.length();
        int index = 0;
        while (true) {
            index = text.indexOf(word, index);
            if (index == -1)
                return;

            int charIndexAfter = index + word.length();
            if (charIndexAfter < pageTextLength) {
                boolean checkCharsBeforeAndAfter = checkChars(
                        index, charIndexAfter, text);

                if (checkCharsBeforeAndAfter) {
                    indexesAndWords.put(index, word);
                    if (!moreOne)
                        return;
                }

                index = charIndexAfter + 1;
                if (index < pageTextLength)
                    continue;

                return;
            }

            boolean checkCharBefore = checkChars(
                    index, null, text);

            if (checkCharBefore)
                indexesAndWords.put(index, word);

            return;
        }
    }

    /*Параметры метода: matchStartIndex - индекс начала совпадения, charIndexAfter
    - индекс символа после совпадения, text - текст в котором найдено совпадение.
    Данный метод проверяет символ перед совпадениям и символы после совпадения.
    Если символы являются не символами русского алфавита, то метод вернет true,
    иначе false*/
    private boolean checkChars(int matchStartIndex, Integer charIndexAfter, String text) {
        boolean charBeforeNotRus = true;
        if (matchStartIndex != 0) {
            int charIndexBefore = matchStartIndex - 1;
            charBeforeNotRus = checkCharNonRus(charIndexBefore, text);
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

    /*Логика метода
    Если текст страницы меньше требуемой длины сниппета, то возвращаем
    текст страницы.
    Если длина сниппета больше требуемой длины сниппета, уменьшает длину
    сниппета и возвращает сниппет.
    Если длина сниппета меньше требуемой длины сниппета, увеличивает длину
    сниппета и возвращает сниппет.
    Возвращает сниппет у которого длина равна требуемой длине сниппета*/
    private String getSnippet() {
        if (pageText.length() <= REQUIRED_SNIPPET_LENGTH)
            return pageText;

        String snippet = getSnippetFromFirstMatchToLatest();
        if (snippet.length() > REQUIRED_SNIPPET_LENGTH) {
            return reduceSnippet(snippet);
        }

        if (snippet.length() < REQUIRED_SNIPPET_LENGTH) {
            int matchStartIndex = indexesAndWordForms.firstKey();
            Entry<Integer, String> pair = indexesAndWordForms.lastEntry();
            int matchEndIndex = pair.getKey() + pair.getValue().length();
            return increaseSnippet(snippet, pageText, matchStartIndex, matchEndIndex);
        }

        return snippet;
    }

    //Данный метод формирует сниппет от начала совпадений до конца
    private String getSnippetFromFirstMatchToLatest() {
        if (indexesAndWordForms.size() == 1)
            return indexesAndWordForms.firstEntry().getValue();

        int startIndex = indexesAndWordForms.firstKey();
        Entry<Integer, String> pair = indexesAndWordForms.lastEntry();
        int finishIndex = pair.getKey() + pair.getValue().length();
        return pageText.substring(startIndex, finishIndex);
    }

    /*Данный метод уменьшает длину сниппета. В начале метода уменьшение
    производится по запросу поиска, т.е. по поиску слов из запроса в тексте
    страницы, если слова из запроса не найдены в тексте страницы, то уменьшение
    производится по всем найдены словоформам лемм путем дробления и формированием
    по одному слову до нужной длины*/
    private String reduceSnippet(String snippet) {
        String newSnippet = collectSnippetByQueryWords(snippet);
        if (newSnippet != null) {
            return newSnippet;
        }

        StringBuilder builder = getStringBuilderWithThreeWords();
        String[] words = snippet.split("\\s");
        for (String word : words) {
            int futureSnippetLength = builder.length() + 1 + word.length();
            if (futureSnippetLength > REQUIRED_SNIPPET_LENGTH)
                break;

            builder.append(" ");
            builder.append(word);
        }

        return builder.toString();
    }

    /*Данный метод собирает сниппет меньшей длины по словам запроса.
    Логика работы метода
    Если слова запроса не найдены в сниппете, то медод возвращает null.
    Если новый сниппет(от начала и до конца совпадения) равен требуемой длине
    сниппета, возвращает новый сниппет.
    Если новый сниппет больше требуемой длины сниппета, то запускает цикл по
    обходу совпадений для того чтобы найти подходящую длину сниппета по совподениям.
    Логика работы цикла:
    1) если стартовый индекс совпадения больше последнего индекса совпадения, это значит
    что индекс совпадений уже перебраны до середины(от самого меньшего индекса и самого
    большего индекса, двигается к середине), тогда метод завершается и возращает значение
    null
    2) если длина нового сниппета равна требуемой длине сниппета, тогда возвращает новый
    сниппет
    3) если длина нового сниппета меньше требуемой длины сниппета, тогда цикл прирывает
    4) в процессе итерации не сработали операторы if, тогда цикл переходит к следующей
    итерации(к следующим индексам).
    По заврешению цикла, происходи проверка, если длина нового сниппета больше требуемой
    длины сниппета(в процессе работы цикла небыла найдена подходящая длина сниппета),
    метод завершается и возвращает null.
    Если завершился цикл и не сработала проверка, это значит, что найдена длина нового
    сниппета которая меньше требуемой длины сниппета и длина новго сниппета увеличиваеся
    с помощью метода increaseSnippet*/
    private String collectSnippetByQueryWords(String snippet) {
        TreeMap<Integer, String> indexesAndWord = findQueryWordsInSnippet(snippet);
        if (indexesAndWord.size() == 0)
            return null;

        int startIndex = indexesAndWord.firstKey();
        int endIndex = indexesAndWord.lastKey()
                + indexesAndWord.lastEntry().getValue().length();

        int newSnippetLength = endIndex - startIndex;
        if (newSnippetLength == REQUIRED_SNIPPET_LENGTH)
            return snippet.substring(startIndex, endIndex);

        if (newSnippetLength > REQUIRED_SNIPPET_LENGTH) {
            ArrayList<Integer> indexes = new ArrayList<>(indexesAndWord.keySet());
            newSnippetLength = 0;
            for (int i = 1; i < indexes.size(); i++) {
                startIndex = indexes.get(i);
                endIndex = indexes.get(indexes.size() - i - 1);
                if (startIndex > endIndex)
                    return null;

                endIndex = endIndex + indexesAndWord.get(endIndex).length();
                newSnippetLength = endIndex - startIndex;
                if (newSnippetLength == REQUIRED_SNIPPET_LENGTH)
                    return snippet.substring(startIndex, endIndex);
                else if (newSnippetLength < REQUIRED_SNIPPET_LENGTH) {
                    break;
                }
            }

            if (newSnippetLength > REQUIRED_SNIPPET_LENGTH)
                return null;
        }

        String newSnippet = snippet.substring(startIndex, endIndex);
        return increaseSnippet(newSnippet, snippet, startIndex, endIndex);
    }

    /*Данный метод ищет слова запрос в сниппете. При поиски находятся одно совпадения
    каждого слова. В начале метода из запроса убираются все частины с помощью метода
    excludeParticles. Далее сниппет переводится в нижний регистр. В цикле foreach
    осуществляется поиск всех слов запроса в тексте сниппета. В результате метода
    возвращается TreeMap с индексами и словами запроса которые были найдены в тексте
    сниппета. TreeMap соритаруется по индексам, т.е. по порядку совподения слов запроса
    в тексте сниппета*/
    private TreeMap<Integer, String> findQueryWordsInSnippet(String snippet) {
        LemmaFinder lemmaFinder = new LemmaFinder();
        if (lemmaFinder.getLuceneMorphology() == null) {
            throw new LemmasFinderNotReadyWorkException();
        }

        ArrayList<String> queryWords = lemmaFinder.excludeParticles(query);
        snippet = snippet.toLowerCase(Locale.ROOT);
        TreeMap<Integer, String> indexesAndWords = new TreeMap<>();
        for (String word : queryWords) {
            findMatchesWordInText(word, false, snippet, indexesAndWords);
        }

        return indexesAndWords;
    }

    /* Данный метод возвращает StringBuilder с тремя словами не вошедними в
       начало snippet */
    private StringBuilder getStringBuilderWithThreeWords() {
        StringBuilder builder = new StringBuilder();
        String[] wordsBeforeMatches = getArrayWordsNotIncludedInSnippet(
                0, indexesAndWordForms.firstKey(), pageText);

        for (int i = wordsBeforeMatches.length; i > 0; i--) {
            String word = wordsBeforeMatches[i - 1];
            builder.insert(0, word);
            if ((i == wordsBeforeMatches.length - 2) || i == 1)
                break;

            builder.insert(0, " ");
        }

        return builder;
    }

    /*В начале метода получает два массива со словами до совпадения и
    после совпадения. Далее в цикле while слова из массивов добавляются
    в StringBuilder по очереди с обеих сторон до тех пор, пока сниппет
    не станет требуемой длины, после чего сниппет возвращается.*/
    private String increaseSnippet(String snippet, String text,
                                   int startIndex, int endIndex) {

        String[] wordsBeforeMatches = getArrayWordsNotIncludedInSnippet(
                0, startIndex, text);

        String[] wordsAfterMatches = getArrayWordsNotIncludedInSnippet(
                endIndex, null, text);

        int i = 1;
        StringBuilder snippetBuilder = new StringBuilder(snippet);
        while (true) {
            if (i <= wordsBeforeMatches.length) {
                String word = wordsBeforeMatches[wordsBeforeMatches.length - i];
                if (notInsertedWordInSnippet(word, 0, snippetBuilder))
                    break;
            }

            if (i <= wordsAfterMatches.length) {
                String word = wordsAfterMatches[i - 1];
                if (notInsertedWordInSnippet(word, null, snippetBuilder))
                    break;
            }

            i++;
        }

        return snippetBuilder.toString();
    }

    private boolean notInsertedWordInSnippet(String word, Integer offset, StringBuilder snippetBuilder) {
        int snippetLengthAfterIncrease
                = snippetBuilder.length() + 1 + word.length();

        if (snippetLengthAfterIncrease > REQUIRED_SNIPPET_LENGTH)
            return true;

        if (offset == null) {
            snippetBuilder.append(" ");
            snippetBuilder.append(word);
        } else {
            snippetBuilder.insert(offset, " ");
            snippetBuilder.insert(offset, word);
        }

        return false;
    }

    private String[] getArrayWordsNotIncludedInSnippet(Integer start, Integer stop, String text) {
        if ((stop != null && stop == 0) || start == text.length())
            return new String[0];

        String substring;
        if (stop != null)
            substring = text.substring(start, stop);
        else
            substring = text.substring(start);

        substring = substring.trim();
        return substring.split("\\s+");
    }

    /*Данный метод выделяет совпадения в тексте сниппета жирным шрифтом.
    В методе используется два String Builder. Один для получения сниппета
    с выдилеными совпадени жирным шрифтом и при этом сохраняется регистр
    всех символов в сниппете. Второй для поиска совпадений в тексте сниппета.
    В методе формируется набор wordForms из значений TreeMap, это делается
    для того, чтобы убрать повторяющиеся значения(словоформы).Логика цикла
    while который ищет совпадения в тексте очень похожа на логику цикла while
    в метода findMatchesWordInText. Основное отличие в том, что если найдено
    совпадения, то он выделяется жирным шрифтом*/
    private String highlightMatchesInBold(String snippet) {
        StringBuilder snippetBuilder = new StringBuilder(snippet);
        StringBuilder lowercaseSnippetBuilder
                = new StringBuilder(snippet.toLowerCase(Locale.ROOT));

        HashSet<String> wordForms = new HashSet<>(indexesAndWordForms.values());
        for (String word : wordForms) {
            int index = 0;
            while (true) {
                index = lowercaseSnippetBuilder.indexOf(word, index);
                if (index == -1)
                    break;

                int charIndexAfter = index + word.length();
                boolean checkChars = checkCharsOnSidesMatch(index, charIndexAfter,
                        lowercaseSnippetBuilder.toString());

                if (checkChars) {
                    highlightMatch(index, charIndexAfter,
                            snippetBuilder, lowercaseSnippetBuilder);
                    charIndexAfter += 7;
                }

                if (charIndexAfter == lowercaseSnippetBuilder.length())
                    break;

                index = charIndexAfter;
            }
        }

        return snippetBuilder.toString();
    }

    private boolean checkCharsOnSidesMatch(int startIndexMatch, int charIndexAfter, String text) {
        if (charIndexAfter < text.length())
            return checkChars(startIndexMatch, charIndexAfter, text);

        return checkChars(startIndexMatch, null, text);
    }

    private void highlightMatch(int startIndex, int endIndex, StringBuilder snippetBuilder,
                                StringBuilder lowercaseSnippetBuilder) {

        snippetBuilder.insert(startIndex, "<b>");
        snippetBuilder.insert(endIndex + 3, "</b>");
        lowercaseSnippetBuilder.insert(startIndex, "<b>");
        lowercaseSnippetBuilder.insert(endIndex + 3, "</b>");
    }
}
