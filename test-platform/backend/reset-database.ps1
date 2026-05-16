# ============================================================
# 数据库重置脚本（仅在需要完全重置时使用）
# ============================================================
# 
# 使用方法：双击运行此脚本，或在 PowerShell 中执行
#   .\reset-database.ps1
#
# 注意：执行前请确保后端已关闭
# ============================================================

Write-Host "============================================"
Write-Host "  数据库重置脚本"
Write-Host "============================================"
Write-Host ""

# 确认操作
$confirmation = Read-Host "即将删除所有数据并重新初始化，是否继续？(y/n)"
if ($confirmation -ne "y" -and $confirmation -ne "Y") {
    Write-Host "操作已取消"
    exit 0
}

# 停止 Java 进程
Write-Host "[1/4] 正在停止后端服务..."
taskkill /F /IM java.exe 2>$null
Start-Sleep -Milliseconds 500

# 删除数据库文件
Write-Host "[2/4] 正在删除旧数据库文件..."
$dataDir = "d:\03 study\11_毕设\test-platform\backend\data"
Remove-Item -Force "$dataDir\test_platform.mv.db" -ErrorAction SilentlyContinue
Remove-Item -Force "$dataDir\test_platform.trace.db" -ErrorAction SilentlyContinue
Write-Host "  已删除旧数据库文件"

# 重新创建数据库
Write-Host "[3/4] 正在重新初始化数据库..."
Write-Host "  请在数据库删除后手动启动后端以重新初始化"
Write-Host "  启动命令: mvn spring-boot:run"

Write-Host ""
Write-Host "[4/4] 完成！"
Write-Host ""
Write-Host "============================================"
Write-Host "  下一步："
Write-Host "  1. 启动后端: mvn spring-boot:run"
Write-Host "  2. 启动前端: npm run dev"
Write-Host "  3. 访问 http://localhost:5173"
Write-Host "============================================"

# 打开后端目录
explorer.exe "d:\03 study\11_毕设\test-platform\backend"