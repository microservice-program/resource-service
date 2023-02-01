package app.music.resourceservice.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class RequestUtils {
    private RequestUtils() {
    }

    public static List<Long> parseStringToListLong(String input) {
        List<Long> ids = new ArrayList<>();
        if (!StringUtils.hasText(input)) return ids;
        String[] numbers = input.split(",");
        for (String number : numbers) {
            ids.add(Long.valueOf(number.trim()));

        }
        return ids;
    }
}
