# PowerShell script to launch Chrome with remote debugging enabled for GSAS & iSpace
# 启动开启了远程调试端口 (9222) 的 Chrome 浏览器，以便通过 chrome-devtools-mcp 实时捕获网络请求与 DOM

$chromePath = "C:\Program Files\Google\Chrome\Application\chrome.exe"
if (-not (Test-Path $chromePath)) {
    $chromePath = "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
}

if (-not (Test-Path $chromePath)) {
    Write-Error "Chrome 浏览器未找到，请确认已安装 Google Chrome。"
    exit 1
}

$debugDir = "$env:TEMP\b-plan-chrome-debug"
if (-not (Test-Path $debugDir)) {
    New-Item -ItemType Directory -Path $debugDir -Force | Out-Null
}

Write-Host "正在启动 Chrome 调试模式 (Port: 9222)..." -ForegroundColor Cyan
Write-Host "调试缓存目录: $debugDir" -ForegroundColor Gray
Write-Host "正在打开: https://gsas.bnbu.edu.cn 和 https://ispace.bnbu.edu.cn" -ForegroundColor Green

Start-Process -FilePath $chromePath -ArgumentList @(
    "--remote-debugging-port=9222",
    "--user-data-dir=$debugDir",
    "https://gsas.bnbu.edu.cn",
    "https://ispace.bnbu.edu.cn"
)

Write-Host "Chrome 已启动！可通过 chrome-devtools-mcp 连接进行网页与接口调试。" -ForegroundColor Green
