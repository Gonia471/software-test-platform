# API 测试接口文档

## 概述

API 测试模块提供接口集合和环境变量的管理功能，支持树形结构的接口组织。

## 接口列表

### 1. 接口集合管理

#### 获取集合列表
```
GET /api/api-test/collections
```

**请求头：**
- Authorization: Bearer {token} (可选，当前配置为permitAll)

**响应示例：**
```json
[
  {
    "id": 1,
    "name": "用户管理",
    "nodeType": "FOLDER",
    "children": [
      {
        "id": 2,
        "name": "登录接口",
        "nodeType": "CASE",
        "method": "POST",
        "url": "{{base_url}}/api/auth/login",
        "params": [],
        "headers": [],
        "bodyType": "raw",
        "bodyRaw": "{\"username\":\"test\",\"password\":\"123456\"}",
        "bodyRawType": "json",
        "bodyForm": [],
        "authType": "none",
        "authConfig": null,
        "assertions": [],
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z",
        "children": null
      }
    ],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

#### 创建集合/接口
```
POST /api/api-test/collections
```

**请求体：**
```json
{
  "name": "新接口",
  "description": "接口描述",
  "nodeType": "CASE",
  "parentId": null,
  "method": "GET",
  "url": "https://api.example.com/users",
  "params": [],
  "headers": [{"key": "Content-Type", "value": "application/json", "enabled": true}],
  "bodyType": "none",
  "bodyRaw": "",
  "bodyRawType": "json",
  "bodyForm": [],
  "authType": "none",
  "authConfig": {},
  "assertions": []
}
```

**响应示例：**
```json
{
  "id": 3,
  "name": "新接口",
  "nodeType": "CASE",
  "method": "GET",
  "url": "https://api.example.com/users",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

#### 更新集合/接口
```
PUT /api/api-test/collections/{id}
```

**路径参数：**
- `id`: 集合ID

**请求体：**
```json
{
  "name": "更新后的名称",
  "description": "更新后的描述",
  "method": "POST",
  "url": "https://api.example.com/users/create",
  "params": [],
  "headers": [],
  "bodyType": "raw",
  "bodyRaw": "{\"name\":\"张三\"}",
  "bodyRawType": "json",
  "bodyForm": [],
  "authType": "bearer",
  "authConfig": {"token": "{{token}}"},
  "assertions": [
    {"type": "status", "expected": "200"},
    {"type": "contains", "path": "$.message", "expected": "success"}
  ]
}
```

**响应示例：**
```json
{
  "id": 3,
  "name": "更新后的名称",
  "updatedAt": "2024-01-02T00:00:00Z"
}
```

#### 删除集合/接口
```
DELETE /api/api-test/collections/{id}
```

**路径参数：**
- `id`: 集合ID

**响应示例：**
```json
{
  "message": "删除成功"
}
```

---

### 2. 环境变量管理

#### 获取环境列表
```
GET /api/api-test/environments
```

**响应示例：**
```json
[
  {
    "id": 1,
    "name": "开发环境",
    "variables": [
      {"key": "base_url", "value": "http://localhost:8080", "enabled": true},
      {"key": "token", "value": "dev-token-123", "enabled": true}
    ],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

#### 创建环境
```
POST /api/api-test/environments
```

**请求体：**
```json
{
  "name": "测试环境",
  "variables": [
    {"key": "base_url", "value": "https://test.example.com", "enabled": true},
    {"key": "token", "value": "test-token-456", "enabled": true}
  ]
}
```

**响应示例：**
```json
{
  "id": 2,
  "name": "测试环境",
  "variables": [
    {"key": "base_url", "value": "https://test.example.com", "enabled": true}
  ],
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

#### 更新环境
```
PUT /api/api-test/environments/{id}
```

**路径参数：**
- `id`: 环境ID

**请求体：**
```json
{
  "name": "生产环境",
  "variables": [
    {"key": "base_url", "value": "https://api.example.com", "enabled": true},
    {"key": "api_key", "value": "prod-key-789", "enabled": true}
  ]
}
```

**响应示例：**
```json
{
  "id": 2,
  "name": "生产环境",
  "updatedAt": "2024-01-02T00:00:00Z"
}
```

#### 删除环境
```
DELETE /api/api-test/environments/{id}
```

**路径参数：**
- `id`: 环境ID

**响应示例：**
```json
{
  "message": "删除成功"
}
```

---

### 3. 发送 HTTP 请求（代理）

#### 发送请求
```
POST /api/api-test/send
```

**请求体：**
```json
{
  "method": "POST",
  "url": "http://localhost:8080/api/auth/login",
  "headers": {
    "Content-Type": "application/json"
  },
  "body": "{\"username\":\"test\",\"password\":\"123456\"}"
}
```

**响应示例：**
```json
{
  "status": 200,
  "statusText": "OK",
  "headers": {
    "content-type": "application/json",
    "date": "Fri, 01 Jan 2024 00:00:00 GMT"
  },
  "body": "{\"message\":\"登录成功\",\"token\":\"eyJhbGciOiJIUzI1NiJ9...\"}",
  "duration": 156,
  "size": 256
}
```

**错误响应示例：**
```json
{
  "status": 0,
  "statusText": "Error",
  "error": "Connection refused",
  "headers": {},
  "body": "",
  "duration": 0,
  "size": 0
}
```

---

## 数据模型说明

### ApiCollection 集合节点

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 名称 |
| description | String | 描述 |
| nodeType | Enum | FOLDER(文件夹) / CASE(接口用例) |
| parentId | Long | 父节点ID，null表示根节点 |
| method | String | HTTP方法：GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS |
| url | String | 请求URL |
| paramsJson | String | URL参数，JSON数组 |
| headersJson | String | 请求头，JSON数组 |
| bodyType | String | none / raw / x-www-form-urlencoded / form-data |
| bodyRaw | String | 原始请求体内容 |
| bodyRawType | String | json / xml / text |
| bodyForm | String | 表单数据，JSON数组 |
| authType | String | none / bearer / basic / apikey |
| authConfig | String | 认证配置，JSON对象 |
| assertions | String | 断言规则，JSON数组 |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |

### ApiEnvironment 环境

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String | 环境名称 |
| variablesJson | String | 变量列表，JSON数组 |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |

---

## 前端集成

前端 API 调用示例：

```javascript
// 获取集合列表
const collections = await request.get('/api/api-test/collections')

// 创建集合
const newCollection = await request.post('/api/api-test/collections', {
  name: '新文件夹',
  nodeType: 'FOLDER'
})

// 创建接口
const newCase = await request.post('/api/api-test/collections', {
  name: '获取用户列表',
  nodeType: 'CASE',
  parentId: 1,
  method: 'GET',
  url: '{{base_url}}/api/users'
})

// 获取环境列表
const environments = await request.get('/api/api-test/environments')

// 创建环境
const newEnv = await request.post('/api/api-test/environments', {
  name: '测试环境',
  variables: [
    { key: 'base_url', value: 'https://test.example.com', enabled: true }
  ]
})

// 发送请求
const response = await request.post('/api/api-test/send', {
  method: 'GET',
  url: 'http://example.com/api/data',
  headers: { 'Authorization': 'Bearer token123' }
})
```

---

## 注意事项

1. **用户隔离**：所有数据按用户隔离，每个用户只能访问自己的数据
2. **级联删除**：删除文件夹时会同时删除所有子节点
3. **变量替换**：前端负责解析 `{{variable}}` 格式的变量，后端只负责存储和转发
4. **CORS 支持**：通过后端代理转发请求，解决跨域问题
5. **错误处理**：所有错误返回统一的错误信息，详见 GlobalExceptionHandler
