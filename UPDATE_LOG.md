
### 3.8.0

* 调整 `local-solon-cloud-plugin` 的 config 和 i18n 服务，如果没有 group 配置，则文件不带 group 前缀（之前默认给了 DEFAULT_GROUP 组名，显得复杂）
* 调整 `rocketmq-solon-clouud-plugin` 的适配，事件属性移除 '!' （并兼容旧格式）
* 调整 `aliyun-ons-solon-clouud-plugin` 的适配，事件属性移除 '!' （并兼容旧格式）
* 调整 `rocketmq5-solon-clouud-plugin` 的适配，事件属性移除 '!' （并兼容旧格式）。添加 sql92 过滤支持

### 3.7.3

* 新增 solon-cloud-telemetry 插件
* 新增 opentelemetry-solon-cloud-plugin 插件
* 新增 xxljob3-solon-cloud-plugin 插件（在 solon-jakarta 仓库）


### 3.7.2

* 新增 resilience4j-solon-cloud-plugin 插件
* 新增 solon-cloud-gateway websocket 代理支持
* 修复 solon-cloud-gateway Completable:doOnError 会中断传递的问题
* sentinel 升为 1.8.9

### 3.7.0

* 移除 solon.xxx 和 nami.xxx 风格的发布包

### 3.5.2

* 完善 solon-cloud 组件自动注册
* 添加 solon-cloud CloudBreaker 注解的降级处理配置支持

### v3.4.3

* 修正 file-s3-solon-cloud-plugin 文件分隔符
* 修正 local-solon-cloud-plugin 文件分隔符

### v3.4.1

* 添加 aliyun-oss-solon-cloud-plugin 阿里云oss获取临时文件url逻辑
* 优化 RunUtil.parallel（已弃用） 改用 RunUtil.async
* 优化 local-solon-cloud-plugin 在启动时，预热 RunUtil

### v3.4.0

* 优化 solon-cloud DiscoveryUtils:tryLoadAgent 兼容性
* 优化 solon-cloud Config pull 方法，确保不输出 null

### v3.3.3

* 新增 nacos3-solon-cloud-plugin 插件
* 添加 solon-cloud-gateway ExContext:route() 方法，并优化 ExContextImpl 的 route 获取
* 优化 solon-cloud-gateway Route:getPredicates,getFilters 增加只读近控制