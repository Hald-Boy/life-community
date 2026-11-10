# Life Community — 基于 Spring Cloud Alibaba 的内容社区微服务

一个类似微博 / X 的内容社区平台，从单体架构演进为 Spring Cloud 微服务架构，按业务域拆分为用户、内容、举报三个微服务，实践了服务治理、统一网关、配置中心与服务容错。

## 技术栈

| 层面 | 技术 |
|---|---|
| 基础框架 | Spring Boot 3.2.4 / Java 17 |
| 微服务 | Spring Cloud 2023.0.1 / Spring Cloud Alibaba 2023.0.1.0 |
| 注册中心 / 配置中心 | Nacos 2.3.2 |
| 服务间调用 | OpenFeign |
| 网关 | Spring Cloud Gateway |
| 服务容错 | Sentinel 1.8.7（限流 / 熔断 / 降级） |
| 数据存储 | MySQL 8.0 / Redis |
| ORM | MyBatis |
| 认证 | JWT |
| 前端 | Vue 3 + Vite（独立仓库） |

## 架构

```
                    ┌─────────────┐
                    │   浏览器     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Gateway 8080│  ← JWT 统一验签、路由转发
                    └──┬───┬───┬──┘
                       │   │   │
              ┌────────┘   │   └────────┐
              │            │            │
     ┌────────▼───┐ ┌────▼─────┐ ┌───▼────────┐
     │ user-service │ │ content-  │ │ report-    │
     │   :8081      │ │ service   │ │ service    │
     │              │ │  :8082    │ │  :8083     │
     │ 用户/关注/   │ │ 帖子/评论 │ │ 举报/通知   │
     │ 收藏/通知    │ │ 点赞/热评 │ │            │
     └──────┬───────┘ └────┬─────┘ └─────┬──────┘
            │              │             │
            └──────────────┼─────────────┘
                           │
                    ┌──────▼──────┐
                    │ Nacos 8848  │  ← 注册发现 + 配置中心
                    └─────────────┘
```

## 核心模块

| 服务 | 端口 | 职责 |
|---|---|---|
| user-service | 8081 | 注册登录、用户资料、关注 / 粉丝、收藏、通知 |
| content-service | 8082 | 帖子信息流、图文发帖、评论、点赞、热评排序 |
| report-service | 8083 | 内容举报、管理员审核、通知 |
| gateway-service | 8080 | 统一入口、JWT 验签、路由转发 |

## 关键设计

### 1. Redis 点赞缓存架构

点赞操作实时写入 Redis（Set 记录点赞用户、String 维护计数），通过定时任务每 60 秒将 Redis 与 MySQL 做 diff 同步，将高并发点赞的写库压力从实时请求路径剥离。查询点赞状态以 Redis 为准、数据库作为兜底，解决异步同步窗口期内按钮状态与计数不一致的问题。

### 2. ZSet 热评排序

设计加权评分公式（点赞 ×3 + 回复 ×1 − 踩 ×2，作者发帖额外加权）配合时间衰减因子，存入 Redis ZSet 实现评论热度榜倒序；应用启动时从 MySQL 预热重建热点数据。

### 3. Nacos 配置中心

将 datasource、redis、mybatis 等公共配置从本地 yml 迁移到 Nacos，本地仅保留服务名、配置中心地址与端口。支持配置动态刷新（@RefreshScope），改配置不重启服务。本地 yml 作为兜底，Nacos 配置优先级更高。

### 4. Gateway 统一认证

所有请求先经过 Gateway 全局过滤器验签 JWT，验签通过后将用户身份信息注入请求头转发给下游服务。下游服务拦截器只信任网关传递的身份信息，不暴露对外端口，实现"一次认证、全网信任"的级联信任模型。

### 5. Sentinel 服务容错

- **限流**：对核心接口配置 QPS 阈值，超出直接拒绝
- **熔断**：针对 Feign 下游调用配置慢调用比例熔断，下游响应超时自动断开
- **降级**：Feign fallback 类返回兜底数据，避免线程阻塞引发雪崩

### 6. OpenFeign 服务间调用

content-service 查询帖子列表时，通过 Feign 批量调用 user-service 获取作者信息（distinct 去重），解决 N+1 查询问题。Feign 基于 JDK 动态代理，开发者只需声明接口，无需手写 HTTP 调用。

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0（库名 `news_course_design`）
- Redis 6+
- Nacos 2.3.2（单机模式，端口 8848）

## 启动步骤

1. 启动 MySQL、Redis、Nacos
2. 在 Nacos 控制台创建配置 `content-service.yaml`（DEFAULT_GROUP），写入 datasource / redis / mybatis / logging 配置
3. 按顺序启动：gateway-service → user-service → content-service → report-service
4. 前端独立仓库启动 Vite 开发服务器，代理 `/user` `/content` `/report` 到 `localhost:8080`

## 项目结构

```
news-community-cloud/
├── pom.xml                  # 父 POM，统一版本管理
├── user-service/            # 用户域
├── content-service/         # 内容域
├── report-service/          # 举报域
├── gateway-service/         # 网关
├── seed.sql                 # 初始化 SQL
├── start-all.bat            # 一键启动脚本
└── stop-all.bat             # 一键停止脚本
```
