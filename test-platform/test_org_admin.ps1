# 组织管理员权限测试脚本

$BaseUrl = "http://localhost:8080/api"
$DevToken = "dev-token"

Write-Host "=== 测试: 组织管理员权限 ===" -ForegroundColor Cyan

# 1. 首先使用开发者模式创建一个新的组织
Write-Host "`n1. 创建新组织..." -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $DevToken"
    "Accept" = "application/json"
    "Content-Type" = "application/json"
}

$newOrgData = @{
    name = "测试组织_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    description = "用于测试组织管理员权限的组织"
    color = "#FF6B6B"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Headers $headers -Method Post -Body $newOrgData
    $newOrgId = $response.id
    Write-Host "✓ 成功创建新组织: $($response.name) (ID: $newOrgId)" -ForegroundColor Green
} catch {
    Write-Host "✗ 创建组织失败: $_" -ForegroundColor Red
    exit 1
}

# 2. 注册一个新用户作为组织管理员
Write-Host "`n2. 注册新用户作为组织管理员..." -ForegroundColor Yellow

$newUserPhone = "13800138$(Get-Random -Minimum 1000 -Maximum 9999)"
$newUserData = @{
    phone = $newUserPhone
    orgName = "组织管理员测试组织"
    description = "测试组织管理员权限"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/auth/register-with-org" -Headers $headers -Method Post -Body $newUserData
    $newUserToken = $response.token
    $newUsername = $response.username
    Write-Host "✓ 成功注册新用户: $newUsername (电话: $newUserPhone)" -ForegroundColor Green
    Write-Host "  用户Token: $($newUserToken.Substring(0, 20))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ 注册用户失败: $_" -ForegroundColor Red
    exit 1
}

# 3. 使用新用户的token查看组织列表
Write-Host "`n3. 使用新用户查看组织列表..." -ForegroundColor Yellow

$newUserHeaders = @{
    "Authorization" = "Bearer $newUserToken"
    "Accept" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Headers $newUserHeaders -Method Get
    Write-Host "新用户看到的组织数量: $($response.Count)" -ForegroundColor Green
    
    if ($response.Count -eq 1) {
        Write-Host "✓ 组织管理员只能看到自己的组织" -ForegroundColor Green
        $userOrg = $response[0]
        Write-Host "  组织名称: $($userOrg.name), ID: $($userOrg.id)" -ForegroundColor Gray
    } else {
        Write-Host "✗ 组织管理员看到了 $($response.Count) 个组织，应该只能看到1个" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 获取组织列表失败: $_" -ForegroundColor Red
}

# 4. 测试新用户创建UI测试用例
Write-Host "`n4. 测试组织管理员创建UI测试用例..." -ForegroundColor Yellow

$uiTestCaseData = @{
    name = "组织管理员创建的UI测试用例"
    description = "测试组织管理员权限的UI测试用例"
    organizationId = $userOrg.id
    steps = @(
        @{
            action = "navigate"
            target = "https://example.com"
            value = ""
            description = "访问示例网站"
        }
    )
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/test-cases" -Headers $newUserHeaders -Method Post -Body $uiTestCaseData
    Write-Host "✓ 组织管理员成功创建UI测试用例: $($response.name)" -ForegroundColor Green
    Write-Host "  用例ID: $($response.id), 组织ID: $($response.organizationId)" -ForegroundColor Gray
} catch {
    Write-Host "✗ 创建UI测试用例失败: $_" -ForegroundColor Red
}

# 5. 测试新用户创建API测试集合
Write-Host "`n5. 测试组织管理员创建API测试集合..." -ForegroundColor Yellow

$apiCollectionData = @{
    name = "组织管理员创建的API集合"
    description = "测试组织管理员权限的API集合"
    type = "folder"
    organizationId = $userOrg.id
    method = "GET"
    url = "https://api.example.com/test"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api-test/collections" -Headers $newUserHeaders -Method Post -Body $apiCollectionData
    Write-Host "✓ 组织管理员成功创建API测试集合: $($response.name)" -ForegroundColor Green
    Write-Host "  集合ID: $($response.id), 组织ID: $($response.organizationId)" -ForegroundColor Gray
} catch {
    Write-Host "✗ 创建API测试集合失败: $_" -ForegroundColor Red
}

# 6. 测试新用户尝试访问其他组织的用例（应该失败）
Write-Host "`n6. 测试组织管理员尝试访问其他组织的用例..." -ForegroundColor Yellow

try {
    # 尝试访问原始组织的用例（ID=9）
    $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/test-cases" -Headers $newUserHeaders -Method Get
    Write-Host "新用户看到的UI测试用例数量: $($response.Count)" -ForegroundColor Green
    
    # 检查是否只能看到自己组织的用例
    $ownOrgCases = $response | Where-Object { $_.organizationId -eq $userOrg.id }
    $otherOrgCases = $response | Where-Object { $_.organizationId -ne $userOrg.id }
    
    if ($otherOrgCases.Count -eq 0) {
        Write-Host "✓ 组织管理员只能看到自己组织的用例" -ForegroundColor Green
    } else {
        Write-Host "✗ 组织管理员看到了其他组织的用例: $($otherOrgCases.Count) 个" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 获取UI测试用例失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Yellow