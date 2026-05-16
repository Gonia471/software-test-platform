# 简单权限测试脚本

$BaseUrl = "http://localhost:8080/api"
$DevToken = "dev-token"

Write-Host "=== 测试1: 开发者模式查看组织 ===" -ForegroundColor Cyan

$headers = @{
    "Authorization" = "Bearer $DevToken"
    "Accept" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations" -Headers $headers -Method Get
    Write-Host "状态码: 200" -ForegroundColor Green
    Write-Host "返回的组织数量: $($response.Count)" -ForegroundColor Green
    
    foreach ($org in $response) {
        Write-Host "  - ID: $($org.id), 名称: $($org.name), 成员数: $($org.memberCount)" -ForegroundColor Gray
    }
    
    if ($response.Count -gt 0) {
        Write-Host "✓ 开发者模式可以查看所有组织" -ForegroundColor Green
    } else {
        Write-Host "✗ 开发者模式没有看到任何组织" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 请求失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试2: 查看组织成员 ===" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/organizations/9/members" -Headers $headers -Method Get
    Write-Host "状态码: 200" -ForegroundColor Green
    Write-Host "组织ID=9的成员数量: $($response.Count)" -ForegroundColor Green
    
    foreach ($member in $response) {
        Write-Host "  成员: $($member.username), 角色: $($member.role)" -ForegroundColor Gray
    }
    
    Write-Host "✓ 成功查看组织成员" -ForegroundColor Green
} catch {
    Write-Host "✗ 请求失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试3: 查看UI测试用例 ===" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/test-cases" -Headers $headers -Method Get
    Write-Host "状态码: 200" -ForegroundColor Green
    Write-Host "UI测试用例数量: $($response.Count)" -ForegroundColor Green
    
    if ($response.Count -gt 0) {
        foreach ($case in $response) {
            Write-Host "  用例: $($case.name), 组织ID: $($case.organizationId)" -ForegroundColor Gray
        }
    } else {
        Write-Host "  没有UI测试用例" -ForegroundColor Yellow
    }
    
    Write-Host "✓ 成功查看UI测试用例" -ForegroundColor Green
} catch {
    Write-Host "✗ 请求失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试4: 查看API测试集合 ===" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api-test/collections" -Headers $headers -Method Get
    Write-Host "状态码: 200" -ForegroundColor Green
    Write-Host "API测试集合数量: $($response.Count)" -ForegroundColor Green
    
    if ($response.Count -gt 0) {
        foreach ($collection in $response) {
            Write-Host "  集合: $($collection.name), 组织ID: $($collection.organizationId)" -ForegroundColor Gray
        }
    } else {
        Write-Host "  没有API测试集合" -ForegroundColor Yellow
    }
    
    Write-Host "✓ 成功查看API测试集合" -ForegroundColor Green
} catch {
    Write-Host "✗ 请求失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Yellow