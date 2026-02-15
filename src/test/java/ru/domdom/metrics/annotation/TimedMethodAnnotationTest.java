package ru.domdom.metrics.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

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
            @TimedMethod(value = "test.method", extraTags = {"key1", "value1", "key2", "value2"})
            public void test() {}
        }

        Method method = TestClass.class.getMethod("test");
        TimedMethod annotation = method.getAnnotation(TimedMethod.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("test.method");
        assertThat(annotation.extraTags()).containsExactly("key1", "value1", "key2", "value2");
    }
}