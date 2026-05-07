# 系统基础接口文档（System）

- **模块说明**：系统运行状态相关基础接口
- **基础路径**：`/api`

## 接口列表

1. [健康检查](#1-健康检查-get-apihealth)

---

## 1. 健康检查 `GET /api/health`

- **功能**：检查后端服务是否正常启动，可用于负载均衡健康检查或前端心跳检测。
- **是否鉴权**：否

### 请求

- **Method**：`GET`
- **URL**：`/api/health`
- **Query 参数**：无

### 响应

- **状态码**
  - `200 OK`：服务正常

#### Body 示例

```json
{
  "status": "ok",
  "service": "software-test-platform"
}
```

