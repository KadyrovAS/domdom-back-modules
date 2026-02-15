package ru.domdom.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimedMethod {
    /**
     * Имя метрики (обязательное поле).
     */
    String value();

    /**
     * Дополнительные теги в виде пар "ключ, значение".
     * Например: @TimedMethod(value = "my.metric", extraTags = {"env", "prod", "region", "ru"})
     */
    String[] extraTags() default {};
}