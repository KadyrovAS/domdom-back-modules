package ru.domdom.metrics.service;

import org.junit.jupiter.api.Test;
import ru.domdom.metrics.exception.InvalidExtraTagsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagUtilsTest {

    @Test
    void shouldConvertEmptyArrayToEmptyMap() {
        assertThat(TagUtils.fromExtraTags(new String[]{})).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapForNull() {
        assertThat(TagUtils.fromExtraTags(null)).isEmpty();
    }

    @Test
    void shouldConvertValidArrayToMap() {
        String[] input = {"key1", "value1", "key2", "value2"};
        Map<String, String> result = TagUtils.fromExtraTags(input);
        assertThat(result)
                .hasSize(2)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }

    @Test
    void shouldThrowExceptionForOddNumberOfElements() {
        String[] input = {"key1", "value1", "key2"};
        assertThatThrownBy(() -> TagUtils.fromExtraTags(input))
                .isInstanceOf(InvalidExtraTagsException.class)
                .hasMessageContaining("even number");
    }
}