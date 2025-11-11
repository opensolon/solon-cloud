# Resilience4j Solon Cloud 插件

该插件为 Solon Cloud 框架提供了基于 Resilience4j 的断路器和流量控制功能。

## 功能特性

- 基于 Resilience4j 的限流器实现
- 支持 QPS 和并发数控制
- 与 Solon Cloud 断路器服务无缝集成
- 提供标准的断路器 API

## 依赖配置

在项目的 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>resilience4j-solon-cloud-plugin</artifactId>
</dependency>
```

## 配置说明

### 基本配置

在 `app.yml` 或 `app.properties` 中配置断路器参数：

```yaml
solon:
  cloud:
    local:
      breaker:
        root: 1000  # 默认断路器阈值（QPS）
        api_user: 500  # 特定断路器的阈值
        api_order: 200
```

### 配置参数说明

- `solon.cloud.local.breaker.root`: 默认断路器的 QPS 阈值
- `solon.cloud.local.breaker.{name}`: 特定断路器的 QPS 阈值

## 使用示例

### 1. 在控制器中使用

```java
@Controller
public class UserController {
    @CloudBreaker(name = "api_user")  // 使用名为 "api_user" 的断路器
    @Mapping("/api/user/info")
    public Result getUserInfo(@Param("userId") String userId) {
        // 业务逻辑
        return Result.succeed(userService.getUserInfo(userId));
    }
}
```

### 2. 在服务层中使用

```java
@Service
public class OrderService {
    @CloudBreaker(name = "api_order")
    public Order getOrderById(String orderId) {
        // 业务逻辑
        return orderMapper.selectById(orderId);
    }
    
    @CloudBreaker(name = "api_order")
    public void createOrder(Order order) {
        // 业务逻辑
        orderMapper.insert(order);
    }
}
```

### 3. 手动使用断路器

```java
@Controller
public class DemoController {
    @Inject
    private CloudBreakerService breakerService;
    
    @Mapping("/demo")
    public String demo() {
        try (AutoCloseable entry = breakerService.entry("demo_breaker")) {
            // 受保护的代码块
            return "success";
        } catch (BreakerException e) {
            // 断路器已打开，请求被拒绝
            return "too many requests";
        }
    }
}
```

## 断路器机制

### 限流策略

- 基于 Resilience4j 的 RateLimiter 实现
- 支持每秒请求数（QPS）限制
- 当请求超过阈值时，抛出 `BreakerException`

### 配置动态更新

断路器阈值支持动态更新，当配置变更时，断路器会自动重新加载规则：

```java
@Configuration
public class BreakerConfig {
    @Bean
    public void refreshBreaker(@Inject CloudBreakerService breakerService) {
        // 当配置变更时，断路器会自动更新
    }
}
```

## 异常处理

当断路器触发时，会抛出 `BreakerException` 异常，开发者可以捕获并处理：

```java
try (AutoCloseable entry = breakerService.entry("my_breaker")) {
    // 正常业务逻辑
} catch (BreakerException e) {
    // 处理断路器异常
    log.warn("请求被限流: {}", e.getMessage());
    return Result.failure("系统繁忙，请稍后重试");
}
```

## 性能监控

Resilience4j 提供了丰富的监控指标，可以集成到监控系统中：

- 请求成功率
- 限流次数
- 断路器状态
- 响应时间分布

## 注意事项

1. 断路器名称应该具有明确的业务含义
2. 合理设置阈值，避免过度限制或过度宽松
3. 在生产环境中建议结合监控系统使用
4. 断路器的配置变更会立即生效，无需重启应用

## 版本兼容性

- Solon Framework: 3.7.1+
- Resilience4j: 2.2.0+
- Java: 8+

## 许可证

Apache License 2.0