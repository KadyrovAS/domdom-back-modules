# Method Metrics Spring Boot Starter

Spring Boot стартер для автоматического сбора метрик времени выполнения методов с интеграцией в Prometheus.

## 🎯 Назначение

Стартер позволяет легко добавлять мониторинг времени выполнения методов в Spring Boot приложениях. Метрики автоматически экспортируются в Prometheus через Micrometer.

## 🚀 Быстрый старт

### 1. Добавление в проект

Добавьте зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>com.dom_dom</groupId>
    <artifactId>method-metrics-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Базовая конфигурация

В `application.yml`:

```yaml
method:
  metrics:
    enabled: true
    prefix: "method"
    histogram: true
    percentiles: [0.5, 0.95, 0.99]

management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
```

### 3. Использование

Помечайте методы аннотацией `@TimedMethod`:

```java
import com.dom_dom.metrics.annotation.TimedMethod;

@Service
public class UserService {
    
    @TimedMethod(value = "user.service.getUser", 
                description = "Получение пользователя по ID")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
}
```

### 4. Просмотр метрик

Запустите приложение и откройте:
```
http://localhost:8080/actuator/prometheus
```

## ⚙️ Конфигурация

### Основные параметры

```yaml
method:
  metrics:
    # Включение/отключение сбора метрик
    enabled: true
    
    # Префикс для метрик
    prefix: "method"
    
    # Включение гистограмм
    histogram: true
    
    # Процентили для гистограмм
    percentiles: [0.5, 0.95, 0.99]
```

## 📊 Примеры использования

### Простой вариант

```java
@TimedMethod
public void simpleMethod() {
    // Автоматическое имя метрики: ClassName.methodName
}
```

### С кастомным именем и описанием

```java
@TimedMethod(
    value = "payment.process",
    description = "Обработка платежа"
)
public PaymentResult processPayment(PaymentRequest request) {
    return paymentGateway.process(request);
}
```

### С дополнительными тегами

```java
@TimedMethod(
    value = "order.create",
    description = "Создание заказа",
    extraTags = {"service=order", "env=prod"}
)
public Order createOrder(OrderRequest request) {
    return orderService.create(request);
}
```

## 📈 Метрики в Prometheus

Стартер создает метрики в формате:

```
# HELP method_user_service_getUser_seconds Получение пользователя по ID
# TYPE method_user_service_getUser_seconds summary
method_user_service_getUser_seconds_count{class="UserService",method="user.service.getUser"} 42
method_user_service_getUser_seconds_sum{class="UserService",method="user.service.getUser"} 1.234
method_user_service_getUser_seconds{quantile="0.5",class="UserService",method="user.service.getUser"} 0.025
method_user_service_getUser_seconds{quantile="0.95",class="UserService",method="user.service.getUser"} 0.045
```

## 🔧 Разработка

### Сборка проекта

```bash
mvn clean package
```

### Запуск тестов

```bash
mvn test
```

## 📁 Структура проекта

```
src/main/java/com/dom_dom/metrics/
├── annotation/           # Аннотация @TimedMethod
├── aspect/              # Spring AOP аспект
├── autoconfigure/       # Автоконфигурация Spring Boot
└── service/            # Сервис обработки метрик
```

## ✅ Поддерживаемые функции

- ✅ Автоматическое измерение времени выполнения методов
- ✅ Интеграция с Prometheus через Micrometer
- ✅ Гибкая конфигурация через application.yml
- ✅ Поддержка тегов для метрик
- ✅ Обработка исключений (метрики записываются даже при ошибках)
- ✅ Полное покрытие тестами
- ✅ Поддержка Spring Boot 3.x
- ✅ Работа с наследованием и интерфейсами


---

**Разработано для DomDom монолита**   
**Версия:** 1.0.0-SNAPSHOT