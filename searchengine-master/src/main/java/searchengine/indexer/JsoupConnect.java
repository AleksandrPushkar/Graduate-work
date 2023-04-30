package searchengine.indexer;

import lombok.RequiredArgsConstructor;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.config.JsoupConfig;
import searchengine.model.EntityPage;
import searchengine.model.EntitySite;

@Component
@RequiredArgsConstructor
public class JsoupConnect {
    private final JsoupConfig config;

    public Document getPageCode(String url, EntityPage page, EntitySite site) {
        Document doc = null;
        int statusCode;
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .referrer(config.getReferrer())
                    .execute();
            statusCode = response.statusCode();

            Thread.sleep(145);
            doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .referrer(config.getReferrer())
                    .get();
        } catch (HttpStatusException ex) {
            statusCode = ex.getStatusCode();
            if (site != null)
                site.setLastError(buildTextHttpError(statusCode));
        } catch (Exception ex) {
            statusCode = 0;
            if (site != null)
                site.setLastError(buildTextAnotherError(ex));
        }

        page.setCode(statusCode);
        page.setContent(doc == null ? "" : doc.toString());
        return doc;
    }

    public String buildTextHttpError(int statusCode) {
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
        StringBuilder builder = new StringBuilder();
        builder.append("Произошла ошибка при индексации сайта. ");
        builder.append("Эта ошибка из серии ");
        builder.append(httpStatus.series());
        builder.append(". ");
        builder.append("Описание: ");
        builder.append(httpStatus);
        builder.append(".");
        return builder.toString();
    }

    private String buildTextAnotherError(Exception ex) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ошибка возникла при индексации. Причина по ");
        builder.append("которой возникла ошибка: URL не соответствует ");
        builder.append("шаблону \"URL для индексации\", а так же может ");
        builder.append("быть другая причина. Описание ошибки: ");
        builder.append(ex.getMessage());
        return builder.toString();
    }
}
