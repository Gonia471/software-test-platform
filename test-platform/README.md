# 软件测试系统（自研版）

基于 B/S 架构的软件测试系统，**完全自研**，统一技术栈：前端 Vue3，后端 Java Spring Boot。

## 目录结构

```
test-platform/
├── frontend/          # 前端：门户 + 接口测试 + UI 测试（Vue3）
├── backend/           # 后端：认证、接口测试、UI 测试管理（Spring Boot）
├── docs/              # 项目内文档（开发流程等）
└── README.md          # 本文件
```

## 技术栈

| 部分     | 技术 |
|----------|------|
| 前端     | Vue 3 + Vue Router + Pinia + Axios |
| 后端     | Java 17+ / Spring Boot 3.x + Spring Security + MyBatis / JPA |
| 数据库   | MySQL 8.x 或 PostgreSQL |
| UI 测试执行 | Karate（由后端调用） |

## 开发流程

前后端开发阶段与顺序见：**[docs/开发流程.md](./docs/开发流程.md)**。按该文档逐步完成即可。

**B/S 架构**（客户端 vs 服务端、本地如何模拟）见：**[docs/BS架构说明.md](./docs/BS架构说明.md)**，可直接用于论文中的架构描述。

## 运行说明（阶段 0）

1. **环境**：JDK 17+、Maven、Node 18+、MySQL 8（详见 [docs/环境说明.md](./docs/环境说明.md)）。
2. **数据库**：创建库 `test_platform`，并修改 `backend/src/main/resources/application.yml` 中的数据库账号密码。
3. **后端**：`cd backend && mvn spring-boot:run`，默认端口 8080。
4. **前端**：`cd frontend && npm install && npm run dev`，默认 http://localhost:5173 。
5. 验证：访问 http://localhost:8080/api/health 与 http://localhost:5173 ，能打开登录占位页并进入侧边栏即可。
