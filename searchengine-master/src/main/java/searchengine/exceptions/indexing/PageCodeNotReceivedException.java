package searchengine.exceptions.indexing;

public class PageCodeNotReceivedException extends RuntimeException {
    public PageCodeNotReceivedException(int statusCode) {
        super(getErrorText(statusCode));
    }

    private static String getErrorText(int statusCode) {
        if (statusCode == 0)
            return "Ошибка при получении доступа к странице. Возможная причина: некорректный URL";
        else
            return "Ошибка при получении доступа к странице. Код ответа: " + statusCode;
    }
}
