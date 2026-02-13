### 待定

* solon-cloud-gateway 增加 websocket 的转发支持（协议头：ws）

### 3.6.8

* 修复 `quartz-solon-cloud-plugin` quartz.Scheduler 定制无效的问题
* 修复 `minio7-solon-cloud-plugin` bucketDef 无效的问题
* 修复 `minio-solon-cloud-plugin` bucketDef 无效的问题
* 添加 `nacos-solon-cloud-plugin` ak/sk 配置支持
* 添加 `nacos2-solon-cloud-plugin` ak/sk 配置支持
* 添加 `nacos3-solon-cloud-plugin` ak/sk 配置支持

### 3.6.5

* 修复 solon-cloud-gateway Completable:doOnError 会中断传递的问题

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