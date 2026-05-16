# 简单组织权限测试脚本

$BaseUrl = "http://localhost:8080/api"
$DevToken = "dev-token"

Write-Host "=== 组织权限测试 ===" -ForegroundColor Cyan

# 1. 使用开发者模式查看所有组织
Write-Host "`n1. 开发者模式查看组织:" -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $DevToken"
    "Accept" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Headers $headers -Method Get
    Write-Host "组织数量: $($response.Count)" -ForegroundColor Green
    
    foreach ($org in $response) {
        Write-Host "  ID: $($org.id), 名称: $($org.name)" -ForegroundColor Gray
    }
} catch {
    Write-Host "失败: $_" -ForegroundColor Red
}

# 2. 查看组织成员
Write-Host "`n2. 查看组织成员:" -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations/9/members" -Headers $headers -Method Get
    Write-Host "成员数量: $($response.Count)" -ForegroundColor Green
    
    foreach ($member in $response) {
        Write-Host "  用户: $($member.username), 角色: $($member.role)" -ForegroundColor Gray
    }
} catch {
    Write-Host "失败: $_" -ForegroundColor Red
}

# 3. 测试UI测试权限
Write-Host "`n3. UI测试权限:" -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/test-cases" -Headers $headers -Method Get
    Write-Host "UI测试用例数量: $($response.Count)" -ForegroundColor Green
} catch {
    Write-Host "失败: $_" -ForegroundColor Red
}

# 4. 测试API测试权限
Write-Host "`n4. API测试权限:" -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api-test/collections" -Headers $headers -Method Get
    Write-Host "API测试集合数量: $($response.Count)" -ForegroundColor Green
} catch {
    Write-Host "失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Yellow