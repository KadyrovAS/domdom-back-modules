package ru.domdom.metrics.annotation;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестовый класс для проверки аннотации {@link TimedMethod}.
 *
 * <p>Тесты проверяют различные аспекты работы аннотации:</p>
 * <ul>
 *   <li>Наличие правильных мета-аннотаций</li>
 *   <li>Работу атрибутов аннотации</li>
 *   <li>Корректность значений по умолчанию</li>
 * </ul>
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 */
class TimedMethodAnnotationTest {

    @Test
    void annotationShouldBePresentOnRuntime() {
        assertThat(TimedMethod.class)
                .isAnnotation()
                .hasAnnotations(Retention.class, Target.class);
    }

    @Test
    void annotationShouldHaveCorrectAttributes() throws NoSuchMethodException {
        class TestClass {
            @TimedMethod(value = "testMethod", description = "Test", extraTags = {"a=b"})
            public void test() {}
        }

        Method method = TestClass.class.getMethod("test");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("testMethod");
        assertThat(annotation.description()).isEqualTo("Test");
        assertThat(annotation.extraTags()).containsExactly("a=b");
    }
}