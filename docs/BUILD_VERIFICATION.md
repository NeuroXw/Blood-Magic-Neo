# 阶段 0 验证记录

日期：2026-10-25（Asia/Shanghai）。

## 构建环境

- Windows / PowerShell。
- JDK：Oracle Java `25.0.5.0.3`，位于 `../.tools/jdk-25.0.5.0.3`。
- Gradle Wrapper：9.2.1，校验值固定于 wrapper properties。
- ModDevGradle：2.0.148。
- Minecraft：26.1.2；NeoForge：26.1.2.122。
- 代理：本次构建临时使用 `127.0.0.1:6789`，未写入全局 Gradle 配置。

## 编译与打包

首次成功构建实际执行了 `compileJava` 和 `jar`：

```text
> Task :compileJava
> Task :jar
> Task :build
BUILD SUCCESSFUL in 1m 13s
4 actionable tasks: 3 executed, 1 up-to-date
```

最终配置修正后执行：

```powershell
.\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('clean','build')
```

```text
> Task :clean
> Task :processResources
> Task :createMinecraftArtifacts
> Task :compileJava FROM-CACHE
> Task :jar
> Task :build
BUILD SUCCESSFUL in 9s
5 actionable tasks: 4 executed, 1 from cache
```

完整终端输出：项目根目录 `build.log`。

产物：`build/libs/bloodmagic-neoforge-0.1.0-stage0.jar`（8,731 字节）。
SHA-256：`1f7fc5d5d8f014136fccb1a171f97a67bcd4fdb84d576d9a41a13a2a202b8f36`。
两次构建的产物哈希一致。

已通过 JDK `jar` / `javap` 工具核对：

- 包含 `wayoftime/bloodmagic/BloodMagic.class`，class major version 为 69（Java 25）。
- 类上存在 `net.neoforged.fml.common.Mod(value="bloodmagic")` 注解。
- `META-INF/neoforge.mods.toml` 已展开版本及模组属性。
- JAR 包含 CC-BY-4.0 许可、上游署名和官方 MDK 的 MIT 声明。

阶段 0 没有单元测试，Gradle 的 `test NO-SOURCE` 属于预期结果。

## 开发服务端启动

执行 `runServer`，实际加载 Minecraft 和 NeoForge，并生成测试世界。
关键日志摘录：

```text
Blood Magic 0.1.0-stage0 (bloodmagic)
Minecraft 26.1.2 (minecraft)
NeoForge 26.1.2.122 (neoforge)
[19:17:11] Blood Magic stage 0 entry point loaded
[19:17:11] Blood Magic stage 0 common setup complete
[19:17:28] Done (10.533s)! For help, type "help"
```

完整记录：根目录 `run-server.log` 和 `run/logs/latest.log`。
首次运行曾打印缺少 `server.properties`，之后自动创建配置并成功进入 Done 状态。
验证完成后终止了本次测试服务端进程；由于终端标准输入已关闭，无法发送 stop，
因此 `run-server.log` 末尾的 `runServer FAILED / exit value -1` 是主动终止产生的，
不是模组启动崩溃。最终 `clean build` 的退出码为 0。

验证范围：开发服务端启动、模组生命周期、JAR 内容及 Java 字节码。
尚未验证图形客户端或普通发行服务端的安装部署；未迁移任何玩法子系统。

## 已处理的问题

1. 默认 Java 为 17：将 Java 25 放在工作目录，并由本地脚本选择。
2. GitHub / Maven 直连超时：官方模板经 GitHub API 获取；用户启用代理后通过官方仓库下载依赖。
3. 打包许可文件的 Gradle 多参数闭包产生自依赖：改为显式 `files(...)` 输入。
4. 官方源码生成流程在本机 8 GB 内存环境中开销较大：主动中止该流程，改用
   ModDevGradle 官方二进制流程；`-Pgenerate_minecraft_sources=true` 可恢复源码生成。
5. 资源下载辅助任务默认寻找 Java 21：通过已核对的 `javaExecutable` 属性将其统一为 Java 25。

上述 API 与构建配置依据详见 `PORTING_SOURCES.md`。
