# 接口在哪里 & 怎么 Debug

## 一、接口都写在哪里

### 1. 后端：接口实现（真正处理请求的代码）

所有 HTTP 接口都在 **Controller** 里定义，路径如下：

| 模块 | 文件路径 | 提供的接口 |
|------|----------|------------|
| 认证 | `backend/src/main/java/com/testplatform/controller/AuthController.java` | 注册、登录 |
| 当前用户 | `backend/src/main/java/com/testplatform/controller/MeController.java` | 获取当前用户 `/me` |
| 健康检查 | `backend/src/main/java/com/testplatform/controller/HealthController.java` | `/api/health` |
| UI 测试用例 | `backend/src/main/java/com/testplatform/controller/uitest/UiTestCaseController.java` | 用例的增删改查 |
| UI 测试执行 | `backend/src/main/java/com/testplatform/controller/uitest/UiTestExecutionController.java` | 发起执行、查详情、停止 |
| 执行实例 | `backend/src/main/java/com/testplatform/controller/uitest/ExecutionInstanceController.java` | 实例列表、创建实例 |

- 每个类头上的 `@RequestMapping("...")` 是**模块前缀**，方法上的 `@GetMapping` / `@PostMapping` 等是**具体路径**。
- 例如：`AuthController` 上是 `@RequestMapping("/api/auth")`，方法上是 `@PostMapping("/login")`，合起来就是 **POST /api/auth/login**。

### 2. 前端：谁在调这些接口

前端用 **axios** 发请求，封装在 `src/api/` 下：

| 文件 | 作用 |
|------|------|
| `frontend/src/api/request.js` | 封装 axios 实例：baseURL=`/api`、带 token、超时等 |
| `frontend/src/api/auth.js` | 登录、注册等接口调用 |
| `frontend/src/api/uiTest.js` | UI 测试相关：创建/更新用例、执行、实例列表等 |
| `frontend/src/api/health.js` | 健康检查等 |

- 页面里 `import { createCase, updateCase, ... } from '@/api/uiTest'`，然后调用 `createCase(payload)`，最终会发 **POST /api/ui-test/test-cases**（因为 request 的 baseURL 是 `/api`，所以路径是相对 `/api` 的）。

### 3. 前端请求如何到后端（代理）

开发时前端跑在 **5173**，后端跑在 **8080**。  
前端的 `vite.config.js` 里配置了代理：

```js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
},
```

所以浏览器里请求的是：`http://localhost:5173/api/xxx`，Vite 会把以 `/api` 开头的请求**转发到** `http://localhost:8080/api/xxx`，由后端 Controller 处理。

### 4. 接口文档写在哪里

文档是给人看的“接口说明”，不参与运行：

- **总览**：`backend/docs/README.md`
- **按模块**：`backend/docs/api/` 下：
  - `auth-api.md`：认证相关
  - `system-api.md`：健康检查等系统接口
  - `ui-test-api.md`：UI 测试用例、执行、实例

看某个接口的路径、参数、返回格式，就打开对应的 `*-api.md`。

---

## 二、怎么 Debug

### 方式 1：浏览器里看请求（前端 + 网络）

1. 打开页面，按 **F12** 打开开发者工具，切到 **Network（网络）**。
2. 勾选 **Preserve log**（保留日志），避免跳转后请求被清掉。
3. 在页面上操作（例如点「保存用例」），在列表里找到对应请求（如 `test-cases`）。
4. 点进去看：
   - **Headers**：请求 URL、Method、Status（200/400/500）。
   - **Payload / Request payload**：发出的请求体是否和预期一致（有没有 body、JSON 对不对）。
   - **Response**：后端返回的 JSON，尤其是错误时的 `message` 字段。

这样可以确认：请求有没有发出去、body 有没有带、状态码和错误信息是什么。

### 方式 2：看后端控制台（异常栈）

后端用 Spring Boot，未捕获的异常会在控制台打完整栈信息。

1. 在 **IDEA / 其他 IDE** 里运行后端（例如运行 `TestPlatformApplication`）。
2. 复现问题（例如再次点击保存用例或用 curl 发请求）。
3. 看控制台里的 **红色报错**：
   - 第一行通常是异常类型，例如 `HttpMessageNotReadableException`、`NullPointerException`、`DataAccessException`。
   - 下面几行是堆栈，会标出是哪个类、哪一行出的错。

根据异常类型就能判断是：请求体解析问题、空指针、数据库错误等。

### 方式 3：在后端打断点（推荐）

1. 在 IDE 里打开对应的 **Controller**（例如 `UiTestCaseController.java`）。
2. 在方法左侧行号处点击，打一个**断点**（红点），例如在 `create` 方法第一行。
3. 用 **Debug 模式** 启动后端（不要用 Run），确保断点生效。
4. 在浏览器里再操作一次（或用 curl 发请求），请求到达时程序会停在该行。
5. 在 Debug 窗口里看：
   - **Variables**：`req` 里的 `name`、`description`、`steps` 是否和前端发的一致。
   - **Step Over (F8)** 单步执行，看在哪一步报错或数据不对。

这样能精确看到“后端实际收到了什么、执行到哪一步出错”。

### 方式 4：用 curl 直接打后端（排除前端和代理）

有时要确认是前端/代理问题还是后端问题，可以直接请求后端端口：

```bash
# 直接请求后端 8080，不经过前端和代理
curl -X POST "http://localhost:8080/api/ui-test/test-cases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-token" \
  -d "{\"name\":\"测试\",\"description\":\"\",\"steps\":[]}"
```

- 若这样仍然 500：问题在后端或数据库，结合**方式 2、3** 看控制台/断点。
- 若这样 200 但浏览器里 500：多半是前端没带 body、或代理/跨域导致的问题，用**方式 1** 对比请求体。

### 方式 5：看接口返回的 message（500 时）

项目里做了全局异常处理（`GlobalExceptionHandler`），接口报错时会返回 JSON，例如：

```json
{ "message": "请求体不是有效的 JSON" }
```

或：

```json
{ "message": "测试用例不存在: 123" }
```

- 在浏览器 Network 里点开该请求，看 **Response** 的 `message`。
- 或用 curl 时直接看响应体。

这样不用看后端日志也能先知道“业务上报了什么错”。

---

## 三、快速对照：接口在哪实现、在哪调用、在哪文档

| 接口示例 | 后端实现位置 | 前端调用位置 | 文档位置 |
|----------|--------------|--------------|----------|
| POST /api/auth/login | AuthController.java | api/auth.js | docs/api/auth-api.md |
| GET /api/auth/me | MeController.java | （若有用到） | docs/api/auth-api.md |
| GET /api/health | HealthController.java | api/health.js | docs/api/system-api.md |
| POST /api/ui-test/test-cases | UiTestCaseController.java | api/uiTest.js `createCase()` | docs/api/ui-test-api.md |
| PUT /api/ui-test/test-cases/:id | UiTestCaseController.java | api/uiTest.js `updateCase()` | docs/api/ui-test-api.md |
| POST /api/ui-test/executions | UiTestExecutionController.java | api/uiTest.js `startExecution()` | docs/api/ui-test-api.md |
| GET /api/ui-test/instances | ExecutionInstanceController.java | api/uiTest.js `getInstances()` | docs/api/ui-test-api.md |

总结：

- **改接口逻辑 / 加新接口**：动后端 `controller` 包下对应类。
- **改前端怎么调**：动 `frontend/src/api/` 下对应 js。
- **查接口路径、参数、返回**：看 `backend/docs/api/` 下对应 md。
- **Debug**：Network 看请求响应 + 后端断点/控制台看异常和变量。
