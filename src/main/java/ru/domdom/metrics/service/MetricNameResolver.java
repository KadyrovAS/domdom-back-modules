package ru.domdom.metrics.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.stereotype.Component;
import ru.domdom.metrics.annotation.TimedMethod;

import java.lang.reflect.Method;

/**
 * Резолвер, формирующий ключ метрики для аннотированного метода.
 *
 * <p>Если в аннотации {@link TimedMethod} указано значение {@code value},
 * оно используется как ключ. Иначе ключ строится как {@code ИмяКласса.имяМетода}.
 * Учитываются прокси-объекты Spring AOP.
 *
 * @author Кадыров Андрей
 * @since 1.0.0
 * @see TimedMethod
 */
@Component
public class MetricNameResolver {

    /**
     * Определяет ключ метрики для точки соединения.
     *
     * @param joinPoint  точка соединения AspectJ
     * @param annotation аннотация {@link TimedMethod} (может быть {@code null})
     * @return ключ метрики
     */
    public String resolve(ProceedingJoinPoint joinPoint, TimedMethod annotation) {
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();

        String className = (target != null)
                ? AopProxyUtils.ultimateTargetClass(target).getSimpleName()
                : method.getDeclaringClass().getSimpleName();

        return className + "." + method.getName();
    }
}