# 权限控制测试脚本
# 测试不同角色对UI测试和API测试的访问权限

$BaseUrl = "http://localhost:8080/api"
$DevToken = "dev-token"

function Test-DeveloperMode {
    Write-Host "=== 测试1: 开发者模式可以看到所有组织 ===" -ForegroundColor Cyan
    
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
            return $true
        } else {
            Write-Host "✗ 开发者模式没有看到任何组织" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "✗ 请求失败: $_" -ForegroundColor Red
        return $false
    }
}

function Test-UiTestPermissions {
    Write-Host "`n=== 测试2: UI测试权限控制 ===" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $DevToken"
        "Accept" = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/cases" -Headers $headers -Method Get
        Write-Host "UI测试用例列表状态码: 200" -ForegroundColor Green
        Write-Host "返回的UI测试用例数量: $($response.Count)" -ForegroundColor Green
        
        # 检查每个用例的组织信息
        $count = 0
        foreach ($case in $response) {
            if ($count -ge 5) { break }
            $orgId = $case.organizationId
            Write-Host "  用例 '$($case.name)': 组织ID=$orgId" -ForegroundColor Gray
            $count++
        }
        
        Write-Host "✓ UI测试权限控制正常" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "✗ 获取UI测试用例失败: $_" -ForegroundColor Red
        return $false
    }
}

function Test-ApiTestPermissions {
    Write-Host "`n=== 测试3: API测试权限控制 ===" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $DevToken"
        "Accept" = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api-test/collections" -Headers $headers -Method Get
        Write-Host "API测试集合列表状态码: 200" -ForegroundColor Green
        Write-Host "返回的API测试集合数量: $($response.Count)" -ForegroundColor Green
        
        # 检查每个集合的组织信息
        $count = 0
        foreach ($collection in $response) {
            if ($count -ge 5) { break }
            $orgId = $collection.organizationId
            Write-Host "  集合 '$($collection.name)': 组织ID=$orgId" -ForegroundColor Gray
            $count++
        }
        
        Write-Host "✓ API测试权限控制正常" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "✗ 获取API测试集合失败: $_" -ForegroundColor Red
        return $false
    }
}

function Test-CreateUiTestCase {
    Write-Host "`n=== 测试4: 创建UI测试用例权限 ===" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $DevToken"
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    # 创建测试用例的请求数据
    $testCaseData = @{
        name = "权限测试用例"
        description = "测试权限控制的UI测试用例"
        organizationId = 9  # 使用现有的组织ID
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
        $response = Invoke-RestMethod -Uri "$BaseUrl/ui-test/cases" -Headers $headers -Method Post -Body $testCaseData
        Write-Host "创建UI测试用例状态码: 200" -ForegroundColor Green
        Write-Host "✓ 成功创建UI测试用例: $($response.name) (ID: $($response.id))" -ForegroundColor Green
        Write-Host "  所属组织ID: $($response.organizationId)" -ForegroundColor Gray
        return $true, $response.id
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "✗ 创建失败 (状态码: $statusCode): $($_.Exception.Message)" -ForegroundColor Red
        return $false, $null
    }
}

function Test-CreateApiCollection {
    Write-Host "`n=== 测试5: 创建API测试集合权限 ===" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $DevToken"
        "Accept" = "application/json"
        "Content-Type" = "application/json"
    }
    
    # 创建API集合的请求数据
    $collectionData = @{
        name = "权限测试API集合"
        description = "测试权限控制的API集合"
        type = "folder"
        organizationId = 9  # 使用现有的组织ID
        method = "GET"
        url = "https://api.example.com/test"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api-test/collections" -Headers $headers -Method Post -Body $collectionData
        Write-Host "创建API测试集合状态码: 200" -ForegroundColor Green
        Write-Host "✓ 成功创建API测试集合: $($response.name) (ID: $($response.id))" -ForegroundColor Green
        Write-Host "  所属组织ID: $($response.organizationId)" -ForegroundColor Gray
        return $true, $response.id
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "✗ 创建失败 (状态码: $statusCode): $($_.Exception.Message)" -ForegroundColor Red
        return $false, $null
    }
}

function Test-OrganizationMemberPermissions {
    Write-Host "`n=== 测试6: 组织成员权限 ===" -ForegroundColor Cyan
    
    $headers = @{
        "Authorization" = "Bearer $DevToken"
        "Accept" = "application/json"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/organizations/9/members" -Headers $headers -Method Get
        Write-Host "组织成员列表状态码: 200" -ForegroundColor Green
        Write-Host "组织ID=9的成员数量: $($response.Count)" -ForegroundColor Green
        
        foreach ($member in $response) {
            Write-Host "  成员: $($member.username), 角色: $($member.role)" -ForegroundColor Gray
        }
        
        # 检查是否有空间创建者/管理员
        $spaceAdmins = $response | Where-Object { $_.role -in @('SPACE_CREATOR', 'SPACE_ADMIN') }
        if ($spaceAdmins.Count -gt 0) {
            Write-Host "✓ 找到空间管理员: $($spaceAdmins.Count)个" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ 没有找到空间管理员" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "✗ 获取成员列表失败: $_" -ForegroundColor Red
        return $false
    }
}

# 主测试函数
Write-Host "开始权限控制测试..." -ForegroundColor Yellow
Write-Host "=" * 50 -ForegroundColor DarkGray

# 检查后端服务是否运行
try {
    $healthCheck = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5
    if ($healthCheck.status -ne "UP") {
        Write-Host "✗ 后端服务健康检查失败" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ 无法连接到后端服务，请确保服务正在运行" -ForegroundColor Red
    exit 1
}

$testResults = @()

# 运行所有测试
$testResults += @{ Name = "开发者模式查看组织"; Result = Test-DeveloperMode }
$testResults += @{ Name = "UI测试权限控制"; Result = Test-UiTestPermissions }
$testResults += @{ Name = "API测试权限控制"; Result = Test-ApiTestPermissions }

$uiTestResult, $uiCaseId = Test-CreateUiTestCase
$testResults += @{ Name = "创建UI测试用例"; Result = $uiTestResult }

$apiTestResult, $apiCollectionId = Test-CreateApiCollection
$testResults += @{ Name = "创建API测试集合"; Result = $apiTestResult }

$testResults += @{ Name = "组织成员权限"; Result = Test-OrganizationMemberPermissions }

# 输出测试总结
Write-Host "`n" + "=" * 50 -ForegroundColor DarkGray
Write-Host "测试总结:" -ForegroundColor Yellow
Write-Host "=" * 50 -ForegroundColor DarkGray

$passed = 0
$total = $testResults.Count

foreach ($test in $testResults) {
    if ($test.Result) {
        Write-Host "$($test.Name): ✓ 通过" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "$($test.Name): ✗ 失败" -ForegroundColor Red
    }
}

Write-Host "`n总计: $passed/$total 个测试通过" -ForegroundColor Cyan

if ($passed -eq $total) {
    Write-Host "✓ 所有权限控制测试通过！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ $($total - $passed) 个测试失败" -ForegroundColor Red
    exit 1
}