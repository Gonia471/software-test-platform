# UI 测试接口文档（UI Test）

- **模块说明**：UI 自动化测试相关接口，包括测试用例、执行、执行实例管理。
- **统一前缀**：`/api/ui-test`
- **认证**：通常需要携带 `Authorization: Bearer <token>`，具体可按项目安全配置调整。

## 模块与路径

- **测试用例管理**：`/api/ui-test/test-cases`
- **执行管理**：`/api/ui-test/executions`
- **执行实例管理**：`/api/ui-test/instances`

## 接口总览

### 测试用例（Test Cases）

1. [创建测试用例](#1-创建测试用例-post-apiui-testtest-cases)
2. [更新测试用例](#2-更新测试用例-put-apiui-testtest-casesid)
3. [获取测试用例详情](#3-获取测试用例详情-get-apiui-testtest-casesid)

### 执行（Executions）

4. [发起一次执行](#4-发起一次执行-post-apiui-testexecutions)
5. [查询执行详情](#5-查询执行详情-get-apiui-testexecutionsid)
6. [请求停止执行](#6-请求停止执行-post-apiui-testexecutionsidstop)

### 执行实例（Instances）

7. [查询所有执行实例](#7-查询所有执行实例-get-apiui-testinstances)
8. [创建执行实例](#8-创建执行实例-post-apiui-testinstances)

---

## 1. 创建测试用例 `POST /api/ui-test/test-cases`

- **功能**：创建一条新的 UI 测试用例。
- **是否鉴权**：建议是

### 请求

- **Method**：`POST`
- **URL**：`/api/ui-test/test-cases`
- **Content-Type**：`application/json`

#### Body 参数（`CreateOrUpdateCaseRequest`）

| 字段名      | 类型                 | 必填 | 说明               |
| ----------- | -------------------- | ---- | ------------------ |
| name        | `string`             | 是*  | 用例名称           |
| description | `string`             | 否   | 用例描述           |
| steps       | `List<Map<String,object>>` | 是*  | 步骤定义（结构由执行引擎约定） |

> 具体步骤结构可根据你的 UI 执行引擎约定，例如：
>
> ```json
> {
>   "action": "click",
>   "locator": "#login-button",
>   "timeout": 5000
> }
> ```

### 响应

- **状态码**
  - `200 OK`：创建成功

#### Body（`UiTestCaseDto`）

```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "steps": [
    {
      "...": "..."
    }
  ]
}
```

---

## 2. 更新测试用例 `PUT /api/ui-test/test-cases/{id}`

- **功能**：根据 ID 更新已存在的测试用例。
- **是否鉴权**：建议是

### 请求

- **Method**：`PUT`
- **URL**：`/api/ui-test/test-cases/{id}`

#### Path 参数

| 字段名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| id     | `long` | 是   | 用例 ID  |

#### Body 参数

- 与「创建测试用例」相同（`CreateOrUpdateCaseRequest`）。

### 响应

- **状态码**
  - `200 OK`：更新成功
  - `404 Not Found`：ID 不存在

#### Body（`UiTestCaseDto`）

结构同上。

---

## 3. 获取测试用例详情 `GET /api/ui-test/test-cases/{id}`

- **功能**：根据 ID 查询测试用例详情。

### 请求

- **Method**：`GET`
- **URL**：`/api/ui-test/test-cases/{id}`

#### Path 参数

| 字段名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| id     | `long` | 是   | 用例 ID  |

### 响应

- **状态码**
  - `200 OK`：查询成功
  - `404 Not Found`：未找到

#### Body（`UiTestCaseDto`）

同创建返回。

---

## 4. 发起一次执行 `POST /api/ui-test/executions`

- **功能**：根据「用例 + 执行实例」发起一次 UI 测试执行。

### 请求

- **Method**：`POST`
- **URL**：`/api/ui-test/executions`
- **Content-Type**：`application/json`

#### Body 参数（`StartExecutionRequest`）

| 字段名           | 类型      | 必填 | 说明                                       | 默认值  |
| ---------------- | --------- | ---- | ------------------------------------------ | ------- |
| testCaseId       | `long`    | 是   | 要执行的测试用例 ID                        | -       |
| instanceId       | `long`    | 是   | 使用的执行实例 ID（如浏览器环境定义）      | -       |
| headless         | `boolean` | 否   | 是否无头模式运行浏览器                    | `true`  |
| stopOnFailure    | `boolean` | 否   | 失败时是否立即停止后续步骤                | `false` |
| screenshotOnFailure | `boolean` | 否 | 失败时是否截图                             | `true`  |

### 响应

- **状态码**
  - `200 OK`：创建执行成功

#### Body（`StartExecutionResponse`）

```json
{
  "executionId": 1,
  "status": "PENDING"
}
```

> 具体 `status` 字段值由服务端实现决定，例如：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED` 等。

---

## 5. 查询执行详情 `GET /api/ui-test/executions/{id}`

- **功能**：查询某次执行的详细信息和步骤结果。

### 请求

- **Method**：`GET`
- **URL**：`/api/ui-test/executions/{id}`

#### Path 参数

| 字段名 | 类型   | 必填 | 说明       |
| ------ | ------ | ---- | ---------- |
| id     | `long` | 是   | 执行记录 ID |

### 响应

- **状态码**
  - `200 OK`：查询成功
  - `404 Not Found`：未找到

#### Body（`ExecutionDetailDto`）

```json
{
  "id": 1,
  "testCaseId": 1,
  "instanceId": 1,
  "status": "RUNNING",
  "options": {
    "headless": true,
    "stopOnFailure": false,
    "screenshotOnFailure": true
  },
  "startTime": "2024-05-01T10:00:00Z",
  "endTime": "2024-05-01T10:01:00Z",
  "errorMessage": null,
  "steps": [
    {
      "...": "..." // ExecutionStepDto 中的具体字段
    }
  ]
}
```

> `steps` 中每个元素为 `ExecutionStepDto`，包含每一步的执行结果，具体字段可根据需要在后续文档中再细化。

---

## 6. 请求停止执行 `POST /api/ui-test/executions/{id}/stop`

- **功能**：请求停止某个正在执行中的任务。

### 请求

- **Method**：`POST`
- **URL**：`/api/ui-test/executions/{id}/stop`

#### Path 参数

| 字段名 | 类型   | 必填 | 说明       |
| ------ | ------ | ---- | ---------- |
| id     | `long` | 是   | 执行记录 ID |

### 响应

- **状态码**
  - `200 OK`：已成功发出停止请求
  - `404 Not Found`：执行记录不存在

> 实际是否能立即停止，取决于执行引擎对「停止请求」的处理逻辑。

---

## 7. 查询所有执行实例 `GET /api/ui-test/instances`

- **功能**：查询所有可用的执行实例（如不同浏览器环境、远程 WebDriver 等）。

### 请求

- **Method**：`GET`
- **URL**：`/api/ui-test/instances`

### 响应

- **状态码**
  - `200 OK`：查询成功

#### Body（`List<ExecutionInstanceDto>`）

```json
[
  {
    "id": 1,
    "name": "Chrome Headless",
    "type": "local",
    "enabled": true,
    "config": {
      "browser": "chrome",
      "headless": true
    }
  }
]
```

---

## 8. 创建执行实例 `POST /api/ui-test/instances`

- **功能**：创建一个新的执行实例，用于描述某种执行环境（浏览器、远程 driver 等）。

### 请求

- **Method**：`POST`
- **URL**：`/api/ui-test/instances`
- **Content-Type**：`application/json`

#### Body 参数（`CreateInstanceRequest`）

| 字段名   | 类型                 | 必填 | 说明                             |
| -------- | -------------------- | ---- | -------------------------------- |
| name     | `string`             | 是   | 实例名称，如 `Chrome Headless`  |
| type     | `string`             | 是   | 实例类型，如 `local` / `remote` |
| remoteUrl| `string`             | 否   | 远程 WebDriver 地址（remote 时使用） |
| config   | `Map<String,object>` | 否   | 扩展配置，如浏览器版本、能力等  |

### 响应

- **状态码**
  - `200 OK`：创建成功

#### Body（`ExecutionInstanceDto`）

```json
{
  "id": 1,
  "name": "Chrome Headless",
  "type": "local",
  "enabled": true,
  "config": {
    "browser": "chrome",
    "headless": true
  }
}
```

