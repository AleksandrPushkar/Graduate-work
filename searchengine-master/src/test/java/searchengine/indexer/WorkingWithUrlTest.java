package searchengine.indexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class WorkingWithUrlTest {
    private final String url = "https://go.skillbox.ru/education/course/";

    @InjectMocks
    private WorkingWithUrl workingWithUrl;

    @Test
    public void testUrlCorrection(){
        String notCorrectUrl = "https:/go.skillbox.ru/education/course/";
        String urlHasWwwAndS = "https://www.skillbox.ru/education/course/";
        String urlHasWww = "http://www.skillbox.ru/education/course/";
        String urlHasNotSlashInEnd = "https://go.skillbox.ru";
        assertNull(workingWithUrl.urlCorrection(notCorrectUrl));
        String resultingURL = workingWithUrl.urlCorrection(urlHasWwwAndS);
        assertEquals("https://skillbox.ru/education/course/", resultingURL);
        resultingURL = workingWithUrl.urlCorrection(urlHasWww);
        assertEquals("http://skillbox.ru/education/course/", resultingURL);
        resultingURL = workingWithUrl.urlCorrection(urlHasNotSlashInEnd);
        assertEquals("https://go.skillbox.ru/", resultingURL);
        resultingURL = workingWithUrl.urlCorrection(url);
        assertEquals(url, resultingURL);
    }

    @Test
    public void testCheckForDisqualification() {
        String urlIsRef = "https://go.skillbox.ru/education/course#asd";
        String urlHasNotPath = "https://go.skillbox.ru";
        String urlIsInvalid3Char = "https://go.skillbox.ru/education/course.nc";
        String urlIsInvalid4Char = "https://go.skillbox.ru/education/course.zip";
        String urlIsInvalid5Char = "https://go.skillbox.ru/education/course.json";
        assertTrue(workingWithUrl.checkForDisqualification(urlIsRef));
        assertTrue(workingWithUrl.checkForDisqualification(urlHasNotPath));
        assertTrue(workingWithUrl.checkForDisqualification(urlIsInvalid3Char));
        assertTrue(workingWithUrl.checkForDisqualification(urlIsInvalid4Char));
        assertTrue(workingWithUrl.checkForDisqualification(urlIsInvalid5Char));
        assertFalse(workingWithUrl.checkForDisqualification(url));
    }

    @Test
    public void testCutUrlToPath() {
        String cutUrl = workingWithUrl.cutUrlToPath(url);
        assertEquals("/education/course/", cutUrl);
    }

    @Test
    public void testGetUrlIsNotNull() {
        URL urlResult = workingWithUrl.getURL(url);
        assertNotNull(urlResult);
        String strResult = urlResult.getProtocol() + "://" + urlResult.getHost() + urlResult.getPath();
        assertEquals(url, strResult);
    }

    @Test
    public void testGetUrlIsNull() {
        URL urlResult = workingWithUrl.getURL(null);
        assertNull(urlResult);
    }
}
