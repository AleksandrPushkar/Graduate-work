package searchengine.indexer;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "СОЮЗ", "ПРЕДЛ", " ЧАСТ"};

    public Map<String, Integer> getLemmas(Document doc) {
        String text = clearTags(doc);
        return collectLemmas(text);
    }

    private String clearTags(Document doc) {
        return doc.text();
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        Map<String, Integer> lemmas = new HashMap<>();
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
            addNormalWordsToMap(normalForms, lemmas);
        }
        return lemmas;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        wordBase = wordBase.toUpperCase();
        for (String property : particlesNames) {
            if (wordBase.contains(property)) {
                return true;
            }
        }
        return false;
    }

    private void addNormalWordsToMap(List<String> normalForms, Map<String, Integer> lemmas) {
        for (String normalWord : normalForms) {
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
    }

    public Set<String> excludeParticles(String query) {
        String[] words = arrayContainsRussianWords(query);
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        Iterator<String> wordIterator = wordSet.iterator();
        while (wordIterator.hasNext()) {
            String word = wordIterator.next();
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                wordIterator.remove();
            }
        }
        return wordSet;
    }
}
