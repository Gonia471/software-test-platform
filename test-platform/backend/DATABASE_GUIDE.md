# 数据库管理指南

## 问题背景

之前每次修改代码都可能破坏数据库，导致之前做好的功能（如 UI 测试、脚本库等）无法正常使用。

## 解决方案

现在使用**非破坏性数据库模式**，平时开发不会破坏已有数据。

## 文件说明

| 文件 | 作用 | 运行时机 |
|------|------|----------|
| `schema.sql` | 创建表结构 | 每次启动 |
| `data.sql` | 插入初始数据 | 每次启动 |
| `reset-database.ps1` | 完全重置数据库 | 仅需要清空数据时 |

## 日常开发流程

### 正常开发（不删除数据）
```bash
# 1. 启动后端
mvn spring-boot:run

# 2. 启动前端
cd ../frontend
npm run dev
```

### 需要清空所有数据时
```powershell
# 运行重置脚本
.\reset-database.ps1

# 然后重新启动后端
mvn spring-boot:run
```

## 常见问题

### Q: 为什么 UI 测试/脚本库报错？
A: 通常是因为数据库表结构不完整。执行 `reset-database.ps1` 清空数据库，然后重启后端。

### Q: 如何备份当前数据？
A: 备份 `backend/data/test_platform.mv.db` 文件即可。

### Q: 修改了 Entity 类后需要做什么？
A: 如果是新增字段，需要在 `schema.sql` 中添加对应的 `ALTER TABLE` 语句（使用条件判断）。如果是新增表，直接在 `schema.sql` 中添加 `CREATE TABLE IF NOT EXISTS`。

## 开发模式

使用 `dev-token` 作为 Authorization header 即可跳过登录：
```
Authorization: Bearer dev-token
```

这会自动使用 `username = 'test'` 的用户。