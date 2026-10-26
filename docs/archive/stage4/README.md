# 阶段 4 归档（2026-10-25）

本目录保存完成祭坛悬浮物品渲染后的阶段 4 快照。
`STAGE4_REPORT.original.md` 是收尾前的报告原件，`STAGE4_REPORT.md` 包含渲染增补。
`src/` 包含发行源码/资源和 174 项服务端测试；`stage4-render-api/` 为精确版本 API 核对记录。
`build-stage4-render-smoke.log` 记录 174 项断言及重启通过；发行 JAR 和校验值一并保存。
图形客户端画面没有实测。

归档内源码、构建配置和 scripts 属于阶段 4。当前工作区已切换阶段 5；
需要复现旧阶段时将本目录复制到单独目录，并将构建产物目录保持为默认的 build/。
`build-local.ps1` 可显式传入本机 Java 25 路径。历史报告位于 docs/。
`SHA256SUMS.txt` 列出文件校验值（不包含该清单自身）。
