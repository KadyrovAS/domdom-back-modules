package ru.domdom.metrics.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилитный класс для парсинга массива строк с тегами в формате "key=value".
 *
 * <p>Поддерживаются только простые пары ключ=значение. Строки, содержащие несколько
 * символов '=', игнорируются. Пробелы вокруг ключа и значения обрезаются.
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagParser {

    /**
     * Преобразует массив строк в карту тегов.
     *
     * @param tagStrings массив строк вида "key=value" (может быть {@code null})
     * @return карта тегов, где ключ и значение уже обрезаны
     */
    public static Map<String, String> parse(String[] tagStrings) {
        Map<String, String> tags = new HashMap<>();
        if (tagStrings == null) {
            return tags;
        }
        for (String tagString : tagStrings) {
            if (tagString != null) {
                int firstEq = tagString.indexOf('=');
                int lastEq = tagString.lastIndexOf('=');
                if (firstEq != -1 && firstEq == lastEq) {
                    String[] parts = tagString.split("=", 2);
                    if (parts.length == 2) {
                        tags.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        return tags;
    }
}