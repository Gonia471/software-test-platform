# API 测试执行记录接口文档

## 概述

API 测试执行记录模块用于记录和查询 API 测试的执行历史，为测试报告页面提供数据支持。

## 接口列表

### 1. 获取执行记录列表

```
GET /api/api-test/executions
```

**参数：**
- `limit` (可选, 默认50): 返回记录数量

**响应示例：**
```json
[
  {
    "id": 1,
    "collectionId": 5,
    "collectionName": "获取用户列表",
    "status": "SUCCESS",
    "duration": 156,
    "httpStatus": 200,
    "statusText": "OK",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "collectionId": 6,
    "collectionName": "创建用户",
    "status": "FAILED",
    "duration": 234,
    "httpStatus": 400,
    "statusText": "Bad Request",
    "createdAt": "2024-01-15T10:25:00Z"
  }
]
```

---

### 2. 获取执行详情

```
GET /api/api-test/executions/{id}
```

**路径参数：**
- `id`: 执行记录ID

**响应示例：**
```json
{
  "id": 1,
  "collectionId": 5,
  "collectionName": "获取用户列表",
  "status": "SUCCESS",
  "duration": 156,
  "httpStatus": 200,
  "statusText": "OK",
  "errorMessage": null,
  "request": {
    "method": "GET",
    "url": "https://api.example.com/users",
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer token123"
    },
    "body": null
  },
  "response": {
    "status": 200,
    "statusText": "OK",
    "headers": {
      "content-type": "application/json",
      "date": "Mon, 15 Jan 2024 10:30:00 GMT"
    },
    "body": "{\"data\":[{\"id\":1,\"name\":\"张三\"}],\"total\":1}",
    "size": 256,
    "duration": 156
  },
  "assertions": [
    {
      "type": "status",
      "description": "验证响应状态码",
      "passed": true,
      "expected": "200",
      "actual": "200"
    },
    {
      "type": "contains",
      "description": "验证响应包含用户数据",
      "passed": true,
      "expected": "张三",
      "actual": "张三"
    }
  ],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

### 3. 保存执行记录

```
POST /api/api-test/executions
```

**请求体：**
```json
{
  "collectionId": 5,
  "collectionName": "获取用户列表",
  "status": "SUCCESS",
  "request": {
    "method": "GET",
    "url": "https://api.example.com/users",
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer token123"
    },
    "body": null
  },
  "response": {
    "status": 200,
    "statusText": "OK",
    "headers": {
      "content-type": "application/json"
    },
    "body": "{\"data\":[{\"id\":1,\"name\":\"张三\"}],\"total\":1}",
    "size": 256,
    "duration": 156
  },
  "assertions": [
    {
      "type": "status",
      "description": "验证响应状态码",
      "passed": true,
      "expected": "200",
      "actual": "200"
    }
  ]
}
```

**响应示例：**
```json
{
  "id": 1,
  "collectionId": 5,
  "collectionName": "获取用户列表",
  "status": "SUCCESS",
  "duration": 156,
  "httpStatus": 200,
  "statusText": "OK",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### 4. 获取执行统计

```
GET /api/api-test/executions/statistics
```

**响应示例：**
```json
{
  "total": 150,
  "success": 120,
  "failed": 25,
  "error": 5,
  "successRate": 80.0
}
```

---

## 数据模型说明

### ApiTestExecution 执行记录实体

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| collectionId | Long | 关联的接口集合ID |
| collectionName | String | 接口名称 |
| status | Enum | SUCCESS / FAILED / ERROR |
| requestJson | String | 请求配置（JSON） |
| responseJson | String | 响应数据（JSON） |
| duration | Integer | 执行时间（毫秒） |
| httpStatus | Integer | HTTP状态码 |
| statusText | String | HTTP状态文本 |
| assertionsJson | String | 断言结果（JSON） |
| errorMessage | String | 错误信息 |
| requestHeaders | String | 请求头（JSON） |
| requestBody | String | 请求体 |
| responseHeaders | String | 响应头（JSON） |
| responseBody | String | 响应体 |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |

---

## 执行状态说明

- **SUCCESS**: 执行成功，HTTP状态码在200-299范围内
- **FAILED**: 执行失败，通常是断言失败或业务错误
- **ERROR**: 执行错误，通常是网络错误或系统异常

---

## 前端集成

```javascript
import request from './request'

export function listApiExecutions(limit = 50) {
  return request.get('/api/api-test/executions', { params: { limit } })
}

export function getApiExecutionDetail(id) {
  return request.get(`/api/api-test/executions/${id}`)
}

export function saveApiExecution(data) {
  return request.post('/api/api-test/executions', data)
}

export function getApiExecutionStatistics() {
  return request.get('/api/api-test/executions/statistics')
}
```

---

## 使用场景

1. **测试报告页面**: 显示所有API测试执行历史
2. **执行详情**: 查看某次执行的请求、响应和断言结果
3. **统计分析**: 统计成功率、失败率等指标
4. **调试**: 复现和分析失败的测试
