package searchengine.indexer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {
    private String textError = "";
    private LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "СОЮЗ", "ПРЕДЛ", "ЧАСТ", "МС"};

    public LemmaFinder() {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException ex) {
            textError = ex.getMessage();
        }
    }

    public LuceneMorphology getLuceneMorphology() {
        return luceneMorphology;
    }

    public String getTextError() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ошибка произошла при доступе к данным. ");
        builder.append("Информация об ошибке: ");
        builder.append(textError);
        return builder.toString();
    }

    public Map<String, Integer> getLemmasPages(Document doc) {
        String textPage = clearTags(doc);
        return collectLemmas(textPage);
    }

    private String clearTags(Document doc) {
        return doc.text();
    }

    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     *
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    private Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }

        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
