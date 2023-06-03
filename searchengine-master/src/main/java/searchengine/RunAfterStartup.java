package searchengine;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Component
public class RunAfterStartup {

    /*Загрузки словаря в память из бинарного формата.В среднем
    занимает около четырех секунд*/
    @EventListener(ApplicationReadyEvent.class)
    public void dictionaryLoading() {
        lookupForMeanings("коробка");
        lookupForMeanings("навыков");
    }
}
