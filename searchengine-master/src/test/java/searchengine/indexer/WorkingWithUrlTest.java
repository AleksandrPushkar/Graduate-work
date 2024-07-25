package searchengine.indexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class WorkingWithUrlTest {
    @InjectMocks
    private WorkingWithUrl workingWithUrl;

    @Test
    public void testCutUrlToPath() {
        String url = "https://go.skillbox.ru/education/course";
        String cutUrl = workingWithUrl.cutUrlToPath(url);
        assertEquals("/education/course", cutUrl);
    }
}
