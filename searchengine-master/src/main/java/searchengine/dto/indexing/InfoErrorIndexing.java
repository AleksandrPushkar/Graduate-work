package searchengine.dto.indexing;

public class InfoErrorIndexing {
    public static String getInfoErrorIndexingAlreadyRunning() {
        return "Индексация уже запущена";
    }

    public static String getInfoErrorIndexingNotRunning() {
        return "Индексация не запущена";
    }

    public static String getInfoErrorInvalidUrl() {
        return  "Недопустимый URL-адрес";
    }

    public static String getInfoErrorSiteNotFound() {
        return "Данная страница находится за пределами " +
                "сайтов, указанных в конфигурационном файле";
    }

    public static String getInfoErrorPageCodeNotReceived(int code) {
        if (code == 0) {
            return "Ошибка при получини кода странциы. Возможная " +
                    "причина: некорректный URL";
        }

        return "Ошибка при получини кода странциы. Код ответа: " + code;
    }

    public static String getInfoErrorLemmaFinderNotReadyWork() {
        return "Внутренняя ошибка сервера. Индексация страницы не произведена.";
    }
}