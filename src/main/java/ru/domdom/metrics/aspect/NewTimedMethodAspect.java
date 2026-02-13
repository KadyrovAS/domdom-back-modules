//package ru.domdom.metrics.aspect;
//
//import lombok.RequiredArgsConstructor;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.springframework.stereotype.Component;
//import ru.domdom.metrics.annotation.TimedMethod;
//import ru.domdom.metrics.service.TimedMethodProcessor;
//
//import java.lang.reflect.Method;
//
//@Aspect
//@RequiredArgsConstructor
//public class NewTimedMethodAspect {
//
//
//    private final TimedMethodProcessor processor;
//
//    @Around("@annotation(annotation)")
//    public Object measure(
//            ProceedingJoinPoint pjp,
//            TimedMethod annotation
//    ) throws Throwable {
//
//        MethodSignature signature = (MethodSignature) pjp.getSignature();
//        Method method = signature.getMethod();
//
//        String metricKey = processor.resolveMetricKey(pjp, method, annotation);
//
//        long start = System.nanoTime();
//        try {
//            return pjp.proceed();
//        } finally {
//            processor.record(metricKey, annotation, method, System.nanoTime() - start);
//        }
//    }
//}
//
//}
