"""
Script to inspect GSAS & iSpace status and verify Chrome DevTools connection
"""

import httpx
import asyncio
from rich.console import Console
from rich.table import Table

console = Console()

TARGETS = [
    ("GSAS 教务系统", "https://gsas.bnbu.edu.cn"),
    ("iSpace 教学平台", "https://ispace.bnbu.edu.cn"),
    ("Chrome DevTools 调试端点", "http://127.0.0.1:9222/json/version"),
]


async def inspect_all():
    table = Table(title="BNBU 目标服务与 Chrome 调试状态检测")
    table.add_column("服务名称", style="cyan")
    table.add_column("目标 URL", style="blue")
    table.add_column("状态", style="bold")
    table.add_column("详情说明")

    async with httpx.AsyncClient(verify=False, timeout=5.0) as client:
        for name, url in TARGETS:
            try:
                resp = await client.get(url)
                if resp.status_code == 200:
                    status_style = "[green]正常 (200)[/green]"
                    info = "可正常访问"
                    if "9222" in url:
                        data = resp.json()
                        info = f"已连接: {data.get('Browser', 'Chrome')}"
                else:
                    status_style = f"[yellow]HTTP {resp.status_code}[/yellow]"
                    info = f"返回码异常"
            except httpx.ConnectError:
                if "9222" in url:
                    status_style = "[dim]未启动[/dim]"
                    info = "运行 scripts/launch_debug_chrome.ps1 即可启动调试浏览器"
                else:
                    status_style = "[red]连接失败[/red]"
                    info = "无法建立网络连接"
            except Exception as e:
                status_style = "[red]错误[/red]"
                info = str(e)

            table.add_row(name, url, status_style, info)

    console.print(table)


if __name__ == "__main__":
    asyncio.run(inspect_all())
