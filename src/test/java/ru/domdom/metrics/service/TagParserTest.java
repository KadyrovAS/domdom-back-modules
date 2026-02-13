package ru.domdom.metrics.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Модульные тесты для {@link TagParser}.
 * <p>
 * Проверяют парсинг строк с тегами, обработку некорректных форматов,
 * пустых и null-входных данных, а также обрезку пробелов.
 * </p>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
class TagParserTest {

    @Test
    void shouldParseValidTags() {
        String[] tags = {"env=prod", "service=user-service", "version=1.0"};
        Map<String, String> result = TagParser.parse(tags);
        assertThat(result)
                .hasSize(3)
                .containsEntry("env", "prod")
                .containsEntry("service", "user-service")
                .containsEntry("version", "1.0");
    }

    @Test
    void shouldSkipInvalidTagFormat() {
        String[] tags = {"env=prod", "invalid", "key=value=extra"};
        Map<String, String> result = TagParser.parse(tags);
        assertThat(result)
                .hasSize(1)
                .containsEntry("env", "prod");
    }

    @Test
    void shouldReturnEmptyMapForNullInput() {
        assertThat(TagParser.parse(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapForEmptyArray() {
        assertThat(TagParser.parse(new String[0])).isEmpty();
    }

    @Test
    void shouldTrimWhitespace() {
        String[] tags = {" env = prod ", "service = user-service"};
        Map<String, String> result = TagParser.parse(tags);
        assertThat(result)
                .hasSize(2)
                .containsEntry("env", "prod")
                .containsEntry("service", "user-service");
    }
}