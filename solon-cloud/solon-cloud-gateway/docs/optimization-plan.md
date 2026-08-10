# solon-cloud-gateway 安全审计与优化方案

> 版本: 4.0.4 (solon-parent) / Vert.x 4.5.27
> 日期: 2026-08-08

## 一、审计结论

### 1. RCE 排查（未发现直接 RCE）
- 请求体零反序列化（`ExContextImpl.newRequest()` 绑定原始 ReadStream，`HttpRouteHandler` 原样 Buffer/Stream 转发）
- 谓词/过滤器按 `前缀=` 查静态工厂表，无反射、无表达式求值、无动态类加载
- 存在间接高危风险：hop-by-hop 头透传（请求走私面）、注册中心可被利用为 SSRF 跳板、RemoteAddr 谓词可被伪造头绕过

### 2. 线程假死（判定：存在）
| 主路径 | 机理 |
|---|---|
| 连接池无限排队 | 共享 HttpClient maxPoolSize=250，Vert.x 4.5 `maxWaitQueueSize` 默认 -1（无上限），池耗尽后无限排队 |
| 超时链路失效 | `requestTimeout` 从未被读取；代码注册路由无任何超时；responseTimeout 默认 1800s |
| 事件循环 DNS 阻塞 | `RemoteAddrPredicate.test()` 对主机名触发同步 DNS，realIp 可被客户端头伪造 |
| 响应永不完成 | filter 链悬挂/上游永不回调时无兜底定时器 |

## 二、优化方案与执行结果

### 阶段 1：纯修复

#### P1-2 CloudGatewayFilterMix.filter() 判空反转
- **问题**: config 构建成功反而抛异常，失败却把 null 加入过滤器链
- **修复**: `filter == null` 时抛 `IllegalArgumentException`，合法时加入链
- **文件**: `CloudGatewayFilterMix.java`

#### P1-3 路由快照化
- **问题**: `routeFind()` 每请求 `new ArrayList + Collections.sort`（O(n log n)，事件循环上）
- **修复**: `volatile List<Route> sortedRoutes`，`route()/routeRemove()` 变更时 `refreshSorted()` 重建一次；`routeFind` 遍历快照
- **文件**: `CloudGatewayConfiguration.java`

### 阶段 2：假死根治

#### P0-1 超时链路修复
- **三超时语义**: connectTimeout（连接）→ requestTimeout（等待响应，**现在真正生效**）→ responseTimeout（整体完成兜底）
- **默认兜底**: `ctx.timeout() == null` 时注入全局默认 `TimeoutProperties`（requestTimeout=10s）
- **网关总超时定时器**: `CloudGatewayCompletion.scheduleTimeout(seconds)`，超时未完成则强制 504；`postComplete()` 幂等（`completed` 标记）+ `cancelTimer`
- **文件**: `HttpRouteHandler.java`、`CloudGatewayCompletion.java`、`CloudGatewayHandler.java`

#### P0-2 连接池快速失败 + 按 host 拆池
- `maxWaitQueueSize=1000`（池满+队列满 → 立即 503 快速失败，不再无限排队）
- `ConcurrentHashMap<String, HttpClient> poolMap` 按 `scheme://host:port` 拆池，单上游故障不拖垮全局
- **文件**: `HttpRouteHandler.java`、`HttpPoolProperties.java`（新增）

### 阶段 3：安全收敛

#### P0-3 RemoteAddr 谓词去 DNS
- `test()` 中先做 IP 字面量判定（IPv4 纯数字 / IPv6 含冒号），非字面量（主机名）直接不匹配，杜绝事件循环同步 DNS 阻塞
 - 匹配逻辑抽取为公共工具 `IpMatcher`（buildRule / isIpLiteral / matches），`RemoteAddr` 与 `XForwardedRemoteAddr` 共用
- **文件**: `RemoteAddrPredicateFactory.java`、`IpMatcher.java`（新增）

#### P0-4 信任模型选择：双谓词对齐 SCG 命名（替代 clientIp 配置方案）
- **决策**: 对标 Spring Cloud Gateway——SCG 没有 realIp 类配置组，信任模型靠**谓词选择**表达（`RemoteAddrPredicate` 用 socket、`XForwardedRemoteAddrPredicate` 信任 XFF 头）。原 clientIp 配置方案（socket/proxy/header 三模式）取消，理由：
  1. SCG 无此配置先例（真实 IP 处理分散在谓词与 x-forwarded 组）
  2. 默认 header 本就不防伪造，配置项价值低
  3. `realIp()` 回归升级前行为（无条件信任 X-Real-IP / X-Forwarded-For），升级零破坏
- **谓词更名对齐 SCG**：`RemoteAddrDirect` → `RemoteAddr`（socket 语义）、原 `RemoteAddr`（信任头语义）→ `XForwardedRemoteAddr`——配置键随语义对齐，与 SCG 命名一致
- **语义对照（与 SCG 同构）**:
  - `RemoteAddr`: 匹配 TCP 对端 socket 地址，不信任任何客户端头 ≈ SCG `RemoteAddrPredicate`（严格 ACL 出口）
  - `XForwardedRemoteAddr`: 匹配 `realIp()`（信任 X-Real-IP / X-Forwarded-For 头）≈ SCG `XForwardedRemoteAddrPredicate`（代理后场景）
- **文件**: `RemoteAddrPredicateFactory.java`（改 socket 语义）、`XForwardedRemoteAddrPredicateFactory.java`（新增，承接原信任头逻辑）、`IpMatcher.java`、`ExContextImpl.java`、`RouteFactoryManager.java`

#### P0-5 x-forwarded 出站头生成（对标 SCG XForwardedHeadersFilter）
- `solon.cloud.gateway.xForwarded.*` 配置组（9 个布尔开关，默认值与 SCG 一致：for 追加、host/port/proto 覆盖）：
  - `enabled`（总开关）、`forEnabled/forAppend`（X-Forwarded-For 逐跳追加对端 IP）、
  - `hostEnabled/hostAppend`（X-Forwarded-Host，默认覆盖式=原始 Host，兼容升级前行为）、
  - `portEnabled/portAppend`（X-Forwarded-Port，缺省按 http80/https443）、
  - `protoEnabled/protoAppend`（X-Forwarded-Proto）
- `XForwardedHeaders.apply()` 在 Http/WebSocket 路由出站头统一应用；`X-Real-IP` 仍以 `realIp()` 计算值无条件覆盖
- **文件**: `XForwardedProperties.java`（新增）、`XForwardedHeaders.java`（新增）、`HttpRouteHandler.java`、`WebSocketRouteHandler.java`、`GatewayProperties.java`

#### P1-1 hop-by-hop 头剥离
- 转发时剥离标准 hop-by-hop 集合（Connection/Keep-Alive/Proxy-*/TE/Trailer/Transfer-Encoding/Upgrade）+ `Connection` 头显式列出的 token
- **文件**: `HttpRouteHandler.java`、`ExConstants.java`（新增 `Connection` 常量）

#### P1-4 谓词正则 ReDoS 防护
- 正则长度上限 512（超长启动期 fail-fast）；输入长度预检 >1024 直接不匹配
- **文件**: `HeaderPredicateFactory.java`、`QueryPredicateFactory.java`、`CookiePredicateFactory.java`

### 阶段 4：配置化与防呆

#### P2-1 硬编码选项配置化（对齐 SCG httpclient 结构）
- `HttpClientProperties extends TimeoutProperties`：超时平铺 + `pool` 子组（`HttpPoolProperties`：maxConnections=250/maxWaitQueueSize=1000/maxIdleTime=60/keepAliveTimeout=60）+ `websocket` 子组（`WebSocketProperties`：maxConnections=200/idleTimeout=60/closingTimeout=10/pingInterval=30）
- 结构与 Spring Cloud Gateway `httpclient` 组一致（connect-timeout/response-timeout 平铺 + pool/websocket 子组）；QPS 适配公式见 `HttpPoolProperties` 类注释
- `HttpRouteHandler` 全部选项由 `httpClientProps.getPool()` 注入；`WebSocketRouteHandler` 由 `getWebsocket()` 注入
- **文件**: `HttpClientProperties.java`、`HttpPoolProperties.java`、`WebSocketProperties.java`、`HttpRouteHandler.java`、`WebSocketRouteHandler.java`、`RouteFactoryManager.java`

#### P2-2 responseTimeout 默认收敛
- 1800s → 60s
- **文件**: `TimeoutProperties.java`

#### P2-3 WebSocket 心跳保活
- `pingInterval > 0` 时双向 `writePing`（30s），连接关闭时取消定时器；超时语义与 HTTP 对齐（requestTimeout 为握手等待超时）
- **文件**: `WebSocketRouteHandler.java`

#### P2-4 httpclient 出网能力补齐（proxy / ssl / compression，对齐 SCG httpclient 组）
- `httpClient.compression`：出站 GZip 压缩（对齐 SCG `httpclient.compression` 默认 false）
- `httpClient.proxy.*`（`HttpProxyProperties`）：企业出网代理（HTTP/SOCKS4/SOCKS5），`host/port/type/username/password/nonProxyHostsPattern/enabled`；`nonProxyHostsPattern` 命中的上游 host 走直连（独立连接池，key 加 `proxy|`/`direct|` 前缀）；正则上限 512 启动期 fail-fast
- `httpClient.ssl.*`（`HttpSslProperties`）：上游 mTLS 客户端证书（JKS/PKCS12）与自定义信任库；`keyPassword` 缺省回退 `keyStorePassword`
- `ClientOptionsUtil` 统一装配（HTTP 与 WebSocket 客户端共用；WS 客户端为全局单例，nonProxyHostsPattern 不生效）
- **文件**: `HttpProxyProperties.java`（新增）、`HttpSslProperties.java`（新增）、`ClientOptionsUtil.java`（新增）、`HttpClientProperties.java`、`HttpRouteHandler.java`、`WebSocketRouteHandler.java`

#### P2-5 谓词/过滤器工厂 enabled 开关（对齐 SCG predicate.*.enabled 配置习惯）
- `solon.cloud.gateway.predicate.{Name}=false` 关闭对应谓词工厂、`solon.cloud.gateway.filter.{Name}=false` 关闭对应过滤器工厂（键为小写连字符，如 `remote-addr`；缺省 true）
- 关闭后 `getPredicate/getFilter` 返回 null（视为未注册，路由跳过，不报错）
- 开关检查在 `RouteFactoryManager` 工厂表查询之后（注册表级别统一开关，不侵入单个工厂）
- **文件**: `GatewayProperties.java`、`RouteFactoryManager.java`

## 三、配置示例

```yaml
solon.cloud.gateway:
  httpClient:                      # 超时 + 连接池 + WebSocket + 出网能力（对齐 SCG httpclient 结构）
    connectTimeout: 10            # 连接建立超时（秒）
    requestTimeout: 10            # 等待响应头超时（秒），现在真正生效
    responseTimeout: 60           # 整体完成兜底（秒），默认 1800 → 60
    compression: false             # 出站 GZip 压缩（对齐 SCG httpclient.compression）
    proxy:                         # 企业出网代理（对齐 SCG httpclient.proxy）
      enabled: false               # 总开关（不配置即直连）
      host: 10.0.0.1               # 代理地址
      port: 8080                   # 代理端口（默认 8080）
      type: HTTP                   # HTTP / SOCKS4 / SOCKS5
      username: user               # 代理认证用户名
      password: pass               # 代理认证密码
      nonProxyHostsPattern: 'localhost|127.*'  # 不走代理的主机正则（命中直连）
    ssl:                           # 上游 mTLS / 自定义信任库（对齐 SCG httpclient.ssl）
      enabled: false               # 总开关
      keyStore: /etc/gateway/client.p12    # 客户端证书库（JKS/PKCS12）
      keyStorePassword: secret
      keyStoreType: PKCS12       # JKS / PKCS12
      keySassword: keypass        # 密钥口令（缺省回退 key-store-password）
      trustStore: /etc/gateway/trust.jks   # 自定义信任库
      trustStorePassword: trustsecret
      trustStoreType: JKS
    pool:                          # 连接池子组（对齐 SCG httpclient.pool）
      maxConnections: 250         # 每上游 host 最大连接数 ≈ QPS×平均响应秒数
      maxWaitQueueSize: 1000    # 池等待队列上限，超限快速失败(503)
      maxIdleTime: 60            # 空闲超时（秒）
      keepAliveTimeout: 60       # keep-alive 保持时长（秒）
    websocket:                     # WebSocket 子组（对齐 SCG httpclient.websocket）
      maxConnections: 200         # WebSocket 最大连接数
      idleTimeout: 60             # 连接空闲判定（秒）
      closingTimeout: 10          # 关闭超时（秒）
      pingInterval: 30            # 双向心跳（秒），<=0 关闭
  predicate:                       # 谓词工厂启用开关（对齐 SCG predicate.*.enabled，缺省 true）
    remoteAddr: false             # 关闭 RemoteAddr 谓词（键为小写连字符）
  filter:                          # 过滤器工厂启用开关（对齐 SCG filter.*.enabled，缺省 true）
    stripPrefix: false            # 关闭 StripPrefix 过滤器
  xForwarded:                      # X-Forwarded-* 出站头生成（对齐 SCG x-forwarded 组）
    enabled: true                  # 总开关
    forEnabled: true              # X-Forwarded-For 生成
    forAppend: true               # 逐跳追加对端 socket IP（对齐 SCG 默认）
    hostEnabled: true             # X-Forwarded-Host 生成（覆盖式=原始 Host，兼容升级前）
    portEnabled: true             # X-Forwarded-Port（缺省按 http80/https443）
    protoEnabled: true            # X-Forwarded-Proto（http/https）
```

**信任模型（谓词选择，与 SCG 同构）**:

```yaml
solon.cloud.gateway:
  routes:
    - id: admin
      predicates:
        - XForwardedRemoteAddr(10.0.0.0/8) # 信任 X-Real-IP/XFF 头：部署在代理后，匹配真实客户端（默认）
        # - RemoteAddr(10.0.0.0/8)          # 纯 socket 对端：直连暴露/严格 ACL，匹配 TCP 对端（不可伪造）
```

## 四、验证

### 单测（GatewayOptimizationTest，纯逻辑、不启动服务器）
- XForwardedRemoteAddr 谓词拒绝主机名（去 DNS）：IPv4 匹配 / 主机名不匹配 / IPv6 不抛异常 / 空 IP 不匹配
- RemoteAddr 谓词（socket 版）：匹配纯 TCP 对端、不信任伪造头、对端 null 防御
- x-forwarded 出站头默认值：总开关开启、XFF 追加、Host/Port/Proto 覆盖（对齐 SCG）
- Header/Query 谓词正则长度防护（>512 启动期报错）
- 输入长度预检（>1024 直接不匹配）
- httpclient proxy/ssl/compression 默认值（默认关闭、端口 8080、类型 HTTP，对齐 SCG）
- 谓词/过滤器工厂 enabled 开关（关闭后构建返回 null，缺省全开）

结果: `Tests run: 8, Failures: 0, Errors: 0` ✓

### 编译
`mvn clean compile` 通过 ✓

### 行为变更声明（需在发布说明中标注）
1. `realIp()` 回归升级前行为：无条件信任 `X-Real-IP` / `X-Forwarded-For` 头（原 clientIp 配置方案取消，`clientIp.mode` / `clientIp.trustedProxies` 配置键不再生效）；需要严格 ACL 请用 `RemoteAddr` 谓词匹配纯 socket 对端
2. 谓词更名对齐 SCG：`RemoteAddrDirect` → `RemoteAddr`（socket 语义），原 `RemoteAddr`（信任头语义）→ `XForwardedRemoteAddr`——**使用原 `RemoteAddr` 做代理后 ACL 的配置需改为 `XForwardedRemoteAddr`**
3. 新增 `x-forwarded` 出站头生成（`xForwarded.*` 配置）：默认开启 `X-Forwarded-For`（逐跳追加对端 IP）、`X-Forwarded-Host`（覆盖式，兼容升级前行为）、`X-Forwarded-Port`、`X-Forwarded-Proto`——新增头需在上游/日志侧确认无冲突；`xForwarded.enabled=false` 可整体关闭
4. `httpClient` 子组化：连接池参数收进 `httpClient.pool.*`（max-connections/max-wait-queue-size/max-idle-time/keep-alive-timeout），WebSocket 收进 `httpClient.websocket.*`——均为本次新增配置（原平铺结构从未发布），无历史配置负担；超时子项 `connect-timeout`/`request-timeout`/`response-timeout` 仍在顶层，与原配置键完全一致
5. `requestTimeout` 生效：无显式超时配置的路由默认 10s 响应超时（此前无限等待）
6. `responseTimeout` 默认 60s（此前 1800s）
7. hop-by-hop 头不再透传上游
8. `httpClient` 新增出网能力配置：`compression`（出站 GZip，默认关）、`proxy.*`（代理，默认关）、`ssl.*`（mTLS/信任库，默认关）——全部默认关闭，不改变现有行为；`nonProxyHostsPattern` 命中 host 走直连（独立连接池，key 加 `proxy|`/`direct|` 前缀）
9. 新增谓词/过滤器工厂开关：`predicate.{Name}=false` / `filter.{Name}=false`（默认全开）——关闭后对应谓词/过滤器配置不再生效（路由跳过，不报错）

## 五、遗留建议（未纳入本次改动）
- P3-1: 注册中心/`LbRouteHandler` 目标地址网段白名单（依赖部署网络拓扑，可选加固）
- 网关级并发闸（全局 Semaphore）防极端洪峰
- 新增 `CloudGatewayMetrics`（池等待数、超时数、503 计数器）
