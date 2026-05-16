/**
 * CURL 命令解析器
 * 支持解析常见的 curl 命令格式，自动识别接口信息
 */

export function parseCurl(curlCommand) {
  if (!curlCommand || typeof curlCommand !== 'string') {
    throw new Error('请输入有效的 curl 命令')
  }

  const result = {
    method: 'GET',
    url: '',
    headers: [],
    params: [],
    bodyType: 'none',
    bodyRaw: '',
    bodyRawType: 'json',
    bodyForm: [],
    authType: 'none',
    authConfig: {},
  }

  // 清理命令，去除换行符和多余的空格
  let cmd = curlCommand.trim()

  // 移除开头的 curl 关键词
  cmd = cmd.replace(/^curl\s+/i, '')

  // 处理引号包裹的整个命令
  if ((cmd.startsWith('"') && cmd.endsWith('"')) || (cmd.startsWith("'") && cmd.endsWith("'"))) {
    cmd = cmd.slice(1, -1)
  }

  // 使用状态机解析
  let currentPart = ''
  let inSingleQuote = false
  let inDoubleQuote = false
  let backslash = false

  for (let i = 0; i < cmd.length; i++) {
    const char = cmd[i]

    if (backslash) {
      currentPart += char
      backslash = false
      continue
    }

    if (char === '\\') {
      backslash = true
      continue
    }

    if (char === "'" && !inDoubleQuote) {
      inSingleQuote = !inSingleQuote
      continue
    }

    if (char === '"' && !inSingleQuote) {
      inDoubleQuote = !inDoubleQuote
      continue
    }

    if (char === ' ' && !inSingleQuote && !inDoubleQuote) {
      if (currentPart.trim()) {
        processPart(currentPart.trim(), result)
      }
      currentPart = ''
      continue
    }

    currentPart += char
  }

  // 处理最后一部分
  if (currentPart.trim()) {
    processPart(currentPart.trim(), result)
  }

  // 清理 URL 中的多余引号
  result.url = result.url.replace(/^['"]|['"]$/g, '')

  // 自动检测 content-type 设置 bodyType
  autoDetectBodyType(result)

  // 自动检测认证类型
  autoDetectAuth(result)

  return result
}

function processPart(part, result) {
  // 解析方法 (-X, --request)
  if (part.match(/^-(X|--request)$/i)) {
    return // 方法会在下一个部分处理
  }
  if (part.match(/^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$/i)) {
    result.method = part.toUpperCase()
    return
  }

  // 解析 URL
  if (part.match(/^https?:\/\//i) || part.startsWith('{{')) {
    result.url = part
    return
  }

  // 解析 -X/--request 后面的方法
  const prevParts = []
  let i = 0
  const parts = part.split(' ')
  if (['-X', '--request'].includes(parts[0]?.toLowerCase())) {
    if (parts[1] && parts[1].match(/^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$/i)) {
      result.method = parts[1].toUpperCase()
      return
    }
  }

  // 解析 header (-H, --header)
  if (part.match(/^-(H|--header)$/i)) {
    return // Header 会在下一个部分处理
  }
  if (part.startsWith('-H ') || part.startsWith('--header ')) {
    const headerValue = part.replace(/^-H\s+/i, '').replace(/^--header\s+/i, '').replace(/^['"]|['"]$/g, '')
    parseHeader(headerValue, result)
    return
  }

  // 解析 data (-d, --data, --data-raw)
  if (part.match(/^-d$|--data|--data-raw|--data-binary|--data-json/i)) {
    return // Data 会在下一个部分处理
  }
  if (part.startsWith('-d ') || part.match(/^--data|^--data-raw|^--data-binary|^--data-json/i)) {
    const dataValue = part.replace(/^-d\s+/i, '').replace(/--data(-raw|-binary|-json)?\s+/i, '').replace(/^['"]|['"]$/g, '')
    parseBody(dataValue, result)
    return
  }

  // 解析 URL 参数 (-G, --get)
  if (part.match(/^--get|-G$/i)) {
    result.method = 'GET'
    return
  }

  // 解析 user (-u, --user)
  if (part.match(/^-u$|--user$/i)) {
    return
  }
  if (part.startsWith('-u ') || part.startsWith('--user ')) {
    const userValue = part.replace(/^-u\s+/i, '').replace(/^--user\s+/i, '').replace(/^['"]|['"]$/g, '')
    parseBasicAuth(userValue, result)
    return
  }

  // 解析 bearer token
  if (part.startsWith('--oauth2-bearer ') || part.startsWith('-oauth2-bearer ')) {
    const token = part.replace(/--oauth2-bearer\s+/i, '').replace(/-oauth2-bearer\s+/i, '').replace(/^['"]|['"]$/g, '')
    result.authType = 'bearer'
    result.authConfig = { token }
    return
  }

  // 如果是带引号的URL
  if ((part.startsWith("'") || part.startsWith('"')) && !part.includes(' ')) {
    const cleanPart = part.replace(/^['"]|['"]$/g, '')
    if (cleanPart.match(/^https?:\/\//i)) {
      result.url = cleanPart
      return
    }
  }
}

function parseHeader(headerStr, result) {
  const colonIndex = headerStr.indexOf(':')
  if (colonIndex === -1) return

  const key = headerStr.substring(0, colonIndex).trim()
  const value = headerStr.substring(colonIndex + 1).trim()

  // 特殊处理 Content-Type
  if (key.toLowerCase() === 'content-type') {
    if (value.includes('application/json')) {
      result.bodyType = 'raw'
      result.bodyRawType = 'json'
    } else if (value.includes('application/x-www-form-urlencoded')) {
      result.bodyType = 'x-www-form-urlencoded'
    } else if (value.includes('multipart/form-data')) {
      result.bodyType = 'form-data'
    }
    result.headers.push({ key, value, enabled: true })
    return
  }

  result.headers.push({ key, value, enabled: true })
}

function parseBody(bodyStr, result) {
  if (!bodyStr) return

  // 自动切换为 POST 方法
  if (result.method === 'GET') {
    result.method = 'POST'
  }

  // 尝试解析 JSON
  try {
    const json = JSON.parse(bodyStr)
    result.bodyType = 'raw'
    result.bodyRawType = 'json'
    result.bodyRaw = bodyStr
    return
  } catch (e) {
    // 不是 JSON
  }

  // 尝试解析为 form-urlencoded
  if (bodyStr.includes('=') && bodyStr.includes('&')) {
    result.bodyType = 'x-www-form-urlencoded'
    const pairs = bodyStr.split('&')
    pairs.forEach(pair => {
      const [key, value] = pair.split('=')
      result.bodyForm.push({
        key: decodeURIComponent(key || ''),
        value: decodeURIComponent(value || ''),
        enabled: true
      })
    })
    return
  }

  // 默认为 raw text
  result.bodyType = 'raw'
  result.bodyRawType = 'text'
  result.bodyRaw = bodyStr
}

function parseBasicAuth(userStr, result) {
  try {
    const decoded = atob(userStr)
    const [username, ...passwordParts] = decoded.split(':')
    const password = passwordParts.join(':')
    result.authType = 'basic'
    result.authConfig = { username, password }
  } catch (e) {
    result.authType = 'basic'
    result.authConfig = { username: userStr, password: '' }
  }
}

function autoDetectBodyType(result) {
  // 如果有 body 且没有明确设置 bodyType
  if (result.bodyRaw && result.bodyType === 'none') {
    // 尝试检测 content-type
    const contentTypeHeader = result.headers.find(
      h => h.key.toLowerCase() === 'content-type'
    )
    if (contentTypeHeader) {
      if (contentTypeHeader.value.includes('application/json')) {
        result.bodyType = 'raw'
        result.bodyRawType = 'json'
      } else if (contentTypeHeader.value.includes('application/x-www-form-urlencoded')) {
        result.bodyType = 'x-www-form-urlencoded'
      }
    } else {
      // 尝试自动检测
      result.bodyType = 'raw'
      try {
        JSON.parse(result.bodyRaw)
        result.bodyRawType = 'json'
      } catch (e) {
        result.bodyRawType = 'text'
      }
    }
  }
}

function autoDetectAuth(result) {
  // 检查 Authorization header
  const authHeader = result.headers.find(
    h => h.key.toLowerCase() === 'authorization'
  )

  if (authHeader) {
    if (authHeader.value.toLowerCase().startsWith('bearer ')) {
      result.authType = 'bearer'
      result.authConfig = { token: authHeader.value.substring(7) }
      // 移除 headers 中的 Authorization，因为已经在 authConfig 中
      result.headers = result.headers.filter(h => h.key.toLowerCase() !== 'authorization')
    } else if (authHeader.value.toLowerCase().startsWith('basic ')) {
      result.authType = 'basic'
      try {
        const decoded = atob(authHeader.value.substring(6))
        const [username, ...passwordParts] = decoded.split(':')
        const password = passwordParts.join(':')
        result.authConfig = { username, password }
      } catch (e) {
        result.authConfig = { username: '', password: '' }
      }
      result.headers = result.headers.filter(h => h.key.toLowerCase() !== 'authorization')
    } else if (authHeader.value.toLowerCase().startsWith('apikey ')) {
      result.authType = 'apikey'
      result.authConfig = { key: 'X-API-Key', value: authHeader.value.substring(7) }
      result.headers = result.headers.filter(h => h.key.toLowerCase() !== 'authorization')
    }
  }
}

export function curlToRequest(curlCommand) {
  try {
    const parsed = parseCurl(curlCommand)
    return {
      success: true,
      data: parsed,
    }
  } catch (error) {
    return {
      success: false,
      error: error.message,
    }
  }
}

/**
 * 检测输入是否看起来像 curl 命令
 */
export function isCurlCommand(input) {
  if (!input || typeof input !== 'string') return false
  const trimmed = input.trim()
  return trimmed.startsWith('curl ') || trimmed.startsWith('curl\n')
}

/**
 * 将请求对象转换为 curl 命令
 */
export function requestToCurl(request) {
  if (!request) return ''

  const parts = ['curl']

  // 方法
  if (request.method && request.method !== 'GET') {
    parts.push(`-X ${request.method}`)
  }

  // URL
  let url = request.url || ''
  // 添加 URL 参数
  if (request.params && request.params.length > 0) {
    const enabledParams = request.params.filter(p => p.enabled !== false && p.key)
    if (enabledParams.length > 0) {
      const queryString = enabledParams
        .map(p => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value || '')}`)
        .join('&')
      if (url.includes('?')) {
        url += '&' + queryString
      } else {
        url += '?' + queryString
      }
    }
  }

  // 引用 URL（处理特殊字符）
  if (url.includes(' ') || url.includes('&') || url.includes('?')) {
    parts.push(`'${url}'`)
  } else {
    parts.push(`'${url}'`)
  }

  // Headers
  if (request.headers && request.headers.length > 0) {
    const enabledHeaders = request.headers.filter(h => h.enabled !== false && h.key)
    enabledHeaders.forEach(header => {
      parts.push(`-H '${header.key}: ${header.value || ''}'`)
    })
  }

  // Auth
  if (request.authType === 'bearer' && request.authConfig?.token) {
    parts.push(`-H 'Authorization: Bearer ${request.authConfig.token}'`)
  } else if (request.authType === 'basic' && request.authConfig?.username) {
    const password = request.authConfig.password || ''
    parts.push(`-u '${request.authConfig.username}:${password}'`)
  } else if (request.authType === 'apikey' && request.authConfig?.key) {
    parts.push(`-H '${request.authConfig.key}: ${request.authConfig.value || ''}'`)
  }

  // Body
  if (request.bodyType === 'raw' && request.bodyRaw) {
    // 根据 bodyRawType 添加 Content-Type
    if (request.bodyRawType === 'json') {
      const hasContentType = request.headers?.some(
        h => h.enabled !== false && h.key.toLowerCase() === 'content-type'
      )
      if (!hasContentType) {
        parts.push("-H 'Content-Type: application/json'")
      }
    }
    parts.push(`-d '${request.bodyRaw.replace(/'/g, "'\\''")}'`)
  } else if (request.bodyType === 'x-www-form-urlencoded' && request.bodyForm) {
    const enabledForm = request.bodyForm.filter(f => f.key)
    if (enabledForm.length > 0) {
      const formData = enabledForm
        .map(f => `${encodeURIComponent(f.key)}=${encodeURIComponent(f.value || '')}`)
        .join('&')
      parts.push(`-d '${formData}'`)
    }
  } else if (request.bodyType === 'form-data' && request.bodyForm) {
    const enabledForm = request.bodyForm.filter(f => f.key)
    if (enabledForm.length > 0) {
      const formData = enabledForm
        .map(f => `${encodeURIComponent(f.key)}=${encodeURIComponent(f.value || '')}`)
        .join('&')
      parts.push(`-d '${formData}'`)
    }
  }

  return parts.join(' \\\n  ')
}
