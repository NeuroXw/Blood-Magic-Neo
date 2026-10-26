# 阶段 7：发行准备结果

日期：2026-10-25（Asia/Shanghai）。本轮完成元数据定稿、干净构建、正式包审计和图形客户端验收指南；图形客户端的实际操作由下一轮人工验收执行。

## 发行文件

- 路径：`D:\GPT\mods\bloodmagic-neoforge\build\libs\BloodMagicNeo-1.0.0-beta.jar`
- 大小：691,633 字节（含用户提供的Logo）。
- SHA-256：`E2BA6DF2830ADFCB95730B2331DAAA53CF06C59B9582A88C6238687F896A520D`。
- 校验文件：`build/libs/BloodMagicNeo-1.0.0-beta.jar.sha256`。
- 固定验收环境：Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。

## 修改范围

- `gradle.properties`：名称 `Blood Magic: Neo`，版本 `1.0.0-beta`；`build.gradle` 将归档基础名设置为 `BloodMagicNeo`。
- `src/main/resources/META-INF/neoforge.mods.toml`：保留构建时名称/版本展开，作者包含 WayofTime、NeuroXw 和 Blood Magic contributors；credits 与 description 明确标注 `Unofficial port. Based on Blood Magic by WayofTime. Licensed under CC-BY-4.0.`。描述更新为实际 beta 功能，移除阶段3的过时说明。
- `README.md`：更新当前发行名称/路径、构建审计命令和图形验收入口。
- `scripts/verify-stage6-machines.ps1`：根据当前 `mod_version` 选择新命名的发行包，避免继续检查旧阶段6 JAR。
- `scripts/verify-stage7-release.ps1`：新增实际JAR审计及SHA-256输出。
- `docs/STAGE7_CLIENT_ACCEPTANCE_GUIDE.md`：图形验收的完整操作、预期、异常判据、日志采集和记录模板。
- `src/main/resources/logo.png`：用户提供的 `C:\Users\Administrator\Desktop\logo.icon` 实际为64×64 JPEG，转换为同尺寸PNG；元数据新增 `logoFile="logo.png"` 和 `logoBlur=false`，用于Mods详情页。源文件保留。

## 已执行的验证

| 项目 | 本轮结果 | 日志/记录 |
|---|---|---|
| `.\build-local.ps1 -GradleTasks @('clean','build')` | BUILD SUCCESSFUL；clean清掉旧build输出，正式Java编译输出由Gradle缓存恢复 | `build-stage7-final.log` |
| `.\scripts\verify-stage6-machines.ps1 -Restart` 第一轮 | 480 assertions，RESTART persistence verified，BUILD SUCCESSFUL | `build-stage7-verify-pass1.log` |
| 同上第二轮 | 480 assertions，RESTART persistence verified，BUILD SUCCESSFUL | `build-stage7-verify-pass2.log` |
| 资源/双语/贴图校验 | 383 JSON、114物品定义、33 blockstate、32方块物品掉落及无物品炼金阵特例通过 | `build-stage7-regression.log` |
| Logo加入后的 `.\build-local.ps1 -GradleTasks @('build')` | BUILD SUCCESSFUL；Java输出未变化，重新处理资源并打包 | `build-stage7-logo.log` |
| Logo加入后的 `.\scripts\verify-stage7-release.ps1` | PASS；602个文件、92个正式class、41条配方，Logo与元数据引用匹配 | `build-stage7-logo-audit.log`、`build/reports/stage7-release-audit.json` |
| 图形客户端、真实鼠标、实际客户端联机 | **未执行，待人工验收** | `docs/STAGE7_CLIENT_ACCEPTANCE_GUIDE.md` |

Gradle的普通`test`任务仍为`NO-SOURCE`，不能作为单元测试通过的证据。两轮480项结论来自真实独立服务端中的现有Smoke断言，包含机器、配方、守恒、结构、保护事件和保存重启。它们通过开发运行配置加载源码输出，不是用户外部启动器安装JAR后的图形测试。

## 正式包与测试代码的边界

`src/stage5Smoke`、`src/stage6Smoke`、`docs/archive`和`run`保留用于历史记录与回归；它们没有被放入发行JAR。没有为“清理”删除测试世界或历史证据。

本轮直接读取发行JAR并执行以下审计：

1. 每个正式编译class与处理后的资源必须存在，且逐文件SHA-256与main构建输出一致。
2. 允许额外打包的内容只有清单及`LICENSE.md`、`NOTICE.md`、`TEMPLATE_LICENSE.txt`。
3. 拒绝测试包、Smoke类、开发目录、脚本、日志、源码、嵌套JAR及任何未在正式输出清单中的文件。
4. 检查class中是否引用`wayoftime/bloodmagic/test/`或`STAGE*_SMOKE`标记；结果为0。
5. 从JAR内读取元数据，核对显示名、版本、作者、许可和非官方署名，确保没有未展开占位符。

首次干净构建及两轮服务端回归对应加入Logo前的发行哈希 `5F0798991860B791BFE951061632B0E9A16C8E55C066D4A2DD48589E8A495A99`；原包审计保留在 `build-stage7-release-audit.log`。随后按用户要求加入Logo，只调整图像资源与元数据，重新build并完成包审计，得到本文顶部的新哈希。此次资源修改没有重跑服务端回归；回归测试class未混入任何正式包。

## 后续验收入口

打开`docs/STAGE7_CLIENT_ACCEPTANCE_GUIDE.md`，先按第3节完成首轮核心流程，再执行其余编号用例。所有人工结果初始为“未测”。记录阻断问题、修复并复测后，再决定是否发布；本轮没有对外上传或发布。
