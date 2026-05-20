# Skada

排行榜 SaaS 平台，支持多租户、多指标、排行榜计划与实例滚动。

## 项目结构

```
skada/
├── common/          # 公共模块（拦截器、统一响应、分布式锁、过滤器）
├── api-service/     # C端 API 服务（分数上报、排行榜查询、手动滚动）端口 8801
├── mng-service/     # B端 管理服务（租户管理、指标管理、排行榜配置）端口 8811
├── mng-web/         # 管理台前端（Vue3 + Ant Design Vue）端口 3000
└── doc/             # 需求文档、技术规范、Postman 集合
```

## 核心概念

| 概念 | 英文 | 说明 |
|------|------|------|
| 租户 | Tenant | SAAS 接入方，拥有独立的ID和密钥 |
| 指标 | Metric | 租户定义的上报维度，如"击杀数"、"得分" |
| 排行榜计划 | Leaderboard Plan | 关联多个指标，配置滚动策略的排行榜定义 |
| 排行榜实例 | Leaderboard Instance | 排行榜计划的具体执行实例，每次滚动产生新实例 |
| 分数记录 | Score Record | 用户在某个实例中某个指标的值 |

## 快速开始

**环境要求**：JDK 21、MySQL 8.x、Redis、Node.js v24

```bash
# 1. 初始化数据库
mysql -u root -p < mng-service/src/main/resources/sql/schema.sql

# 2. 构建后端
mvn clean package -DskipTests

# 3. 启动服务
java -jar api-service/target/skada-api-service-1.0.0-SNAPSHOT.jar &
java -jar mng-service/target/skada-mng-service-1.0.0-SNAPSHOT.jar &

# 4. 启动前端
cd mng-web && npm install && npm run dev
```

**管理台登录**：`admin` / `admin123`

## 技术栈

- **后端**：Spring Boot 4.0.6、MyBatis 4.0.1、MySQL、Redis、Log4j2
- **前端**：Vue 3、Ant Design Vue 4、TypeScript、Vite
- **规范**：JSON（Gson）、参数化查询、BCrypt密码加密、统一异常处理
