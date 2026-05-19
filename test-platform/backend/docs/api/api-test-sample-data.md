# API 测试前端录入样例

这份文档给你直接在项目界面里手动录入 API 测试用例使用，不需要再看后端造数接口。

当前环境：

- 环境名：`dev`
- 地址：`http://localhost:8080`
- URL 统一写法：`{{dev}}/api/...`

建议先在环境变量里确认这些值已经存在：

- `dev = http://localhost:8080`
- `demo_phone = 你自己的11位手机号`
- `demo_org_name = 演示企业空间`
- `dynamic_phone =`
- `dynamic_org_name =`

## 使用说明

### 断言填写规则

- `STATUS`：表达式填状态码，例如 `200`
- `JSONPATH`：表达式建议写 `/status`、`/phone`
- `CONTAINS`：表达式直接填要包含的文本，例如 `token`

### 当前实现注意点

- 不建议把 JSONPath 写成 `$.status`
- 更推荐写成 `/status`
- 前置 HTTP 的提取变量功能演示时不够稳，答辩演示建议重点用脚本库函数生成变量
- 你当前项目认证不是用户名密码模式，而是手机号模式
- `POST /api/auth/login-with-code` 当前实际请求体只需要 `phone`

## 基础用例

### 用例 1：健康检查成功

- 名称：`健康检查成功`
- 方法：`GET`
- URL：`{{dev}}/api/health`
- 请求头：无
- 请求体：无

断言：

1. 类型：`STATUS`
   表达式：`200`
2. 类型：`JSONPATH`
   表达式：`/status`
   期望值：`ok`

### 用例 2：用户登录失败

- 名称：`用户登录失败`
- 方法：`POST`
- URL：`{{dev}}/api/auth/login-with-code`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "12345"
}
```

断言：

1. 类型：`STATUS`
   表达式：`400`

### 用例 3：用户登录成功

- 名称：`用户登录成功`
- 方法：`POST`
- URL：`{{dev}}/api/auth/login-with-code`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "{{demo_phone}}"
}
```

断言：

1. 类型：`STATUS`
   表达式：`200`
2. 类型：`JSONPATH`
   表达式：`/phone`
   期望值：`{{demo_phone}}`
3. 类型：`CONTAINS`
   表达式：`token`

## 需要前置脚本的用例

### 用例 4：先检查服务再登录

- 名称：`先检查服务再登录`
- 方法：`POST`
- URL：`{{dev}}/api/auth/login-with-code`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "{{demo_phone}}"
}
```

前置步骤：

1. 类型：`HTTP`
   方法：`GET`
   URL：`{{dev}}/api/health`
   失败终止：开启

断言：

1. 类型：`STATUS`
   表达式：`200`
2. 类型：`CONTAINS`
   表达式：`token`

### 用例 5：先检查服务再验证注册参数错误

- 名称：`先检查服务再验证注册参数错误`
- 方法：`POST`
- URL：`{{dev}}/api/auth/register-with-org`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "12345",
  "orgName": "测试企业空间",
  "description": "非法手机号校验"
}
```

前置步骤：

1. 类型：`HTTP`
   方法：`GET`
   URL：`{{dev}}/api/health`
   失败终止：开启

断言：

1. 类型：`STATUS`
   表达式：`400`

## 脚本库

先在脚本库里新建下面 2 个函数。

### 脚本 1：build_register_phone_payload

- 脚本名称：`随机企业注册数据生成器`
- 函数名：`build_register_phone_payload`

脚本内容：

```python
import time

def build_register_phone_payload():
    suffix = str(int(time.time() * 1000))[-8:]
    return {
        "dynamic_phone": "13" + suffix[-9:],
        "dynamic_org_name": "演示企业_" + suffix[-6:]
    }
```

### 脚本 2：build_login_phone_payload

- 脚本名称：`随机手机号登录数据生成器`
- 函数名：`build_login_phone_payload`

脚本内容：

```python
import time

def build_login_phone_payload():
    suffix = str(int(time.time() * 1000))[-8:]
    return {
        "dynamic_phone": "15" + suffix[-9:]
    }
```

## 需要脚本库的用例

### 用例 6：脚本生成随机手机号后注册成功

- 名称：`脚本生成随机手机号后注册企业空间成功`
- 方法：`POST`
- URL：`{{dev}}/api/auth/register-with-org`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "{{dynamic_phone}}",
  "orgName": "{{dynamic_org_name}}",
  "description": "脚本库动态造数注册"
}
```

前置步骤：

1. 类型：`FUNCTION`
   函数名：`build_register_phone_payload`
   参数：留空
   失败终止：开启

断言：

1. 类型：`STATUS`
   表达式：`200`
2. 类型：`JSONPATH`
   表达式：`/phone`
   期望值：`{{dynamic_phone}}`
3. 类型：`CONTAINS`
   表达式：`token`

### 用例 7：脚本生成随机手机号后登录成功

- 名称：`脚本生成随机手机号后登录成功`
- 方法：`POST`
- URL：`{{dev}}/api/auth/login-with-code`
- 请求头：
  - `Content-Type: application/json`

请求体：

```json
{
  "phone": "{{dynamic_phone}}"
}
```

前置步骤：

1. 类型：`FUNCTION`
   函数名：`build_login_phone_payload`
   参数：留空
   失败终止：开启

断言：

1. 类型：`STATUS`
   表达式：`200`
2. 类型：`JSONPATH`
   表达式：`/phone`
   期望值：`{{dynamic_phone}}`

## 最推荐你录入的 6 个

1. `健康检查成功`
2. `用户登录失败`
3. `用户登录成功`
4. `先检查服务再登录`
5. `脚本生成随机手机号后注册企业空间成功`
6. `脚本生成随机手机号后登录成功`

## 演示建议

- 如果要展示基础能力：演示 `健康检查成功` 和 `用户登录失败`
- 如果要展示前置步骤：演示 `先检查服务再登录`
- 如果要展示动态造数和脚本库：演示 `脚本生成随机手机号后注册企业空间成功`
