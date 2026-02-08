package ru.domdom.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для измерения времени выполнения методов.
 * Применяется к методам, которые необходимо мониторить.
 * Методы, помеченные этой аннотацией, будут автоматически
 * измеряться и их метрики будут отправляться в Prometheus.
 *
 * <p>Пример использования:</p>
 * <pre>
 * {@code
 * @TimedMethod(value = "user.service.getUser",
 *              description = "Получение пользователя по ID",
 *              extraTags = {"env=prod", "service=user-service"})
 * public User getUser(Long id) {
 *     return userRepository.findById(id);
 * }
 * }
 * </pre>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimedMethod {

    /**
     * Название метода для метрик.
     * Если не указано, будет использовано имя метода.
     */
    String value() default "";

    /**
     * Описание метода (необязательно).
     */
    String description() default "";

    /**
     * Дополнительные теги для метрик в формате "key=value".
     */
    String[] extraTags() default {};
}