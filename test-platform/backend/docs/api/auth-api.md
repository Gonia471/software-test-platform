# 认证 & 用户接口文档（Auth）

- **模块说明**：用户注册、登录、获取当前登录用户信息
- **基础路径**：`/api/auth`

## 公共说明

- 响应统一为 JSON。
- 除「登录」「注册」外，其余接口一般需要在 Header 中携带：

  - `Authorization: Bearer <JWT_TOKEN>`

## 接口列表

1. [用户注册](#1-用户注册-post-apiauthregister)
2. [用户登录](#2-用户登录-post-apiauthlogin)
3. [获取当前用户信息](#3-获取当前用户信息-get-apiauthme)

---

## 1. 用户注册 `POST /api/auth/register`

- **功能**：创建一个新用户账号，并返回登录凭证。
- **是否鉴权**：否

### 请求

- **Method**：`POST`
- **URL**：`/api/auth/register`
- **Content-Type**：`application/json`

#### Body 参数（`RegisterRequest`）

| 字段名     | 类型     | 必填 | 说明           | 约束                |
| ---------- | -------- | ---- | -------------- | ------------------- |
| username   | `string` | 是   | 用户名         | 长度 2~32，非空     |
| password   | `string` | 是   | 密码           | 长度 4~64，非空     |

### 响应

- **状态码**
  - `200 OK`：注册成功
  - `400 Bad Request`：参数不合法（如长度不满足约束）

#### Body（`AuthResponse`）

```json
{
  "token": "string",   // JWT 访问令牌
  "username": "string" // 用户名
}
```

---

## 2. 用户登录 `POST /api/auth/login`

- **功能**：使用用户名密码登录，获取 JWT 令牌。
- **是否鉴权**：否

### 请求

- **Method**：`POST`
- **URL**：`/api/auth/login`
- **Content-Type**：`application/json`

#### Body 参数（`LoginRequest`）

| 字段名   | 类型     | 必填 | 说明   | 约束        |
| -------- | -------- | ---- | ------ | ----------- |
| username | `string` | 是   | 用户名 | 非空        |
| password | `string` | 是   | 密码   | 非空        |

### 响应

- **状态码**
  - `200 OK`：登录成功
  - `401 Unauthorized`：用户名或密码错误

#### Body（`AuthResponse`）

```json
{
  "token": "string",
  "username": "string"
}
```

---

## 3. 获取当前用户信息 `GET /api/auth/me`

- **功能**：获取当前登录用户的基本信息。
- **是否鉴权**：是（必须携带有效 JWT）

### 请求

- **Method**：`GET`
- **URL**：`/api/auth/me`
- **Headers**

| Header 名称    | 必填 | 说明                           |
| -------------- | ---- | ------------------------------ |
| Authorization  | 是   | `Bearer <token>` 格式的 JWT    |

### 响应

- **状态码**
  - `200 OK`：获取成功
  - `401 Unauthorized`：未携带令牌或令牌无效

#### Body 示例

```json
{
  "username": "alice"
}
```

