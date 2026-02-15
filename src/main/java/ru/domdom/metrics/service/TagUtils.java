package ru.domdom.metrics.service;

import ru.domdom.metrics.exception.InvalidExtraTagsException;

import java.util.HashMap;
import java.util.Map;

public final class TagUtils {

    private TagUtils() {}

    public static Map<String, String> fromExtraTags(String[] extraTags) {
        if (extraTags == null || extraTags.length == 0) {
            return Map.of();
        }
        if (extraTags.length % 2 != 0) {
            throw new InvalidExtraTagsException(
                    "extraTags must have even number of elements (key-value pairs), but got: " + extraTags.length
            );
        }
        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < extraTags.length; i += 2) {
            tags.put(extraTags[i], extraTags[i + 1]);
        }
        return tags;
    }
}