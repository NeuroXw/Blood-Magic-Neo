# 阶段 2：方块实体、数据与网络

日期：2026-10-25（Asia/Shanghai）。
目标固定为 Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。
阶段 1 的 22 个方块、83 个物品及 ID 均保留。

## 结论

血之祭坛已从普通方块替换为具有实际存储和同步路径的 BlockAltar + BloodAltarBlockEntity。
新增 1 个 BlockEntityType、2 个 DataComponentType 和 1 个客户端方向的 CustomPacketPayload。
五种血球拥有默认等级，支持服务端首次右键绑定；配方与灵魂网络尚未实现。

指定命令已在关闭测试开关的普通开发环境中执行：

```powershell
.\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('build','runServer')
```

结果为 BUILD SUCCESSFUL in 1m 1s，退出码 0；服务端到达 Done 后通过控制台 stop 保存退出。
最终普通启动和两次成功的集成检查日志均无 ERROR / FATAL。
另在独立测试世界运行了两轮集成检查，每轮 45 个断言通过，第二轮验证了真正的世界重启持久化。

## 上游输入及版本纠正

先读取了 STAGE1_REPORT.md。上游仍使用本地锁定提交
35f0188665e648d981095b6933cc75b0ca96eca5，位于
../.references/bloodmagic-original/WayofTime-BloodMagic-35f0188。

该提交没有名为 TileBloodAltar.java 的文件。实际读取的对应实现是：

- common/tile/TileAltar.java：祭坛实体、单槽继承、存储委派与 ticker。
- altar/BloodAltar.java：LP 主槽/缓冲槽、等级、进度、配方状态与升级逻辑。
- common/tile/TileInventory.java、common/tile/base/TileBase.java：物品存储、NBT 和方块实体更新包。
- common/item/ItemBloodOrb.java、ItemBindableBase.java、BloodOrb.java、core/data/Binding.java。
- network/BloodMagicPacketHandler.java：原 SimpleChannel 注册方式。

上游祭坛同步走 TileBase 的 ClientboundBlockEntityDataPacket / 更新标签，没有独立的
“祭坛进度包”类。本次新增专用的 AltarProgressPayload，并保留完整初始同步。
上游 binding 为 ItemStack NBT；等级原本来自 BloodOrb 对象，本次给物品增加默认等级组件。

特别纠正：26.1.2 的 saveAdditional/loadAdditional 已使用 ValueOutput/ValueInput。
`CompoundTag + HolderLookup.Provider` 双参数是较早版本的形式，不是本项目的覆盖签名。

## 已实现范围

| 内容 | 实现 |
| --- | --- |
| 祭坛方块 | BlockAltar extends BaseEntityBlock；保持 bloodmagic:altar ID，新增对应 MapCodec 注册 |
| 方块实体 | BloodMagicBlockEntities.BLOOD_ALTAR，ID bloodmagic:altar；延迟引用方块，绑定创建工厂 |
| 祭坛状态 | 不可变 AltarState，保存主 LP/容量、输入/输出缓冲 LP、缓冲容量、等级、进度、总需求及活动标记 |
| 基础存储 | 默认 10,000 LP / 1,000 LP 缓冲；正负值与上下界校验；fillMainTank/drainMainTank 返回实际数量 |
| 单槽物品 | ItemStack.OPTIONAL_CODEC 持久化，读写均防御性复制；方块移除时掉落一次 |
| 序列化 | super.loadAdditional/saveAdditional；altar 子项及 item 字段；缺失字段恢复默认 |
| 初始同步 | getUpdateTag(provider) 保存完整状态；原版 BE 更新包同步物品；默认 ValueInput 接收路径 |
| 后续进度同步 | S2C CustomPacketPayload：维度、位置和数值状态；变更后发送，最多每 5 tick 一次 |
| 客户端应用 | 游戏主线程；检查逻辑端、维度、已加载区块及实体类型，不因收包加载区块 |
| 血球等级 | bloodmagic:blood_orb_tier；五种现有物品默认 1..5，范围检查，堆叠上限 1 |
| 玩家绑定 | bloodmagic:binding；不可变 Binding(UUID, String)，UUID 为身份、名称为显示快照 |
| 血球使用 | 服务端首次右键绑定；已有绑定不被右键覆盖；ItemStack 自带组件同步 |
| 玩法 Stub | tickGameplayStub 保留明确 TODO，不推进虚假合成进度、不自动消耗 LP |

progress 及 requiredBlood 的单位是 LP，不是 tick；本次 requiredBlood 定义为整次操作的总需求。
尚未开放玩家向祭坛放入/取出物品的交互，存储 API 可供后续逻辑调用。
祭坛仍沿用阶段 1 的立方体视觉资源，名称和中英文说明已更新，避免宣称数据存储未实现。

## 核心 API 差异

以下以本地精确版本 JAR、NeoForge sources JAR 和实际编译/运行结果为依据。

| 旧实现或旧版本示例 | 本次 26.1.2 实现 |
| --- | --- |
| TileBase.load(CompoundTag) / saveAdditional(CompoundTag) | protected loadAdditional(ValueInput) / saveAdditional(ValueOutput) |
| 较早的 saveAdditional/loadAdditional(CompoundTag, HolderLookup.Provider) | 此双参数覆盖签名已不存在；ValueInput/ValueOutput 携带编解码上下文，ValueInput.lookup() 可取 provider |
| 直接使用 CompoundTag 的 get/put 和旧 ItemStack NBT | ValueInput.read / childOrEmpty / getIntOr；ValueOutput.store / child / putInt；ItemStack.OPTIONAL_CODEC 使用注册表上下文 |
| 无参数 getUpdateTag() | getUpdateTag(HolderLookup.Provider)；调用 saveWithoutMetadata(provider) |
| onDataPacket(Connection, ClientboundBlockEntityDataPacket) | NeoForge 默认 onDataPacket(Connection, ValueInput)，handleUpdateTag 同样接收 ValueInput |
| BlockEntityType.Builder.of(...).build(...) 示例 | 注册 new BlockEntityType<>(factory, validBlocks...)，由 DeferredRegister 延迟构造 |
| ItemStack.getTag/getOrCreateTag 中的可变 binding | 注册 DataComponentType，persistent(Codec) + networkSynchronized(StreamCodec)；ItemStack.get/getOrDefault/set/remove |
| 可变 Binding 字段、INBTSerializable | 不可变 record + UUIDUtil.CODEC/STREAM_CODEC；修改时整体替换，避免破坏组件缓存/比较 |
| SimpleChannel + 数字消息编号 + encode/decode/handle | CustomPacketPayload.Type + StreamCodec，RegisterPayloadHandlersEvent 注册 playToClient |
| NetworkEvent.Context / setPacketHandled | IPayloadContext；HandlerThread.MAIN 明确主线程调度；如改网络线程则 enqueueWork |
| ResourceLocation / new ChunkPos(BlockPos) | Identifier / ChunkPos.containing(BlockPos) |
| hasChunkAt(BlockPos) | 客户端使用 getChunkSource().getChunkNow(x >> 4, z >> 4)，避免弃用及意外加载 |

HolderLookup.Provider 仍出现在 saveWithFullMetadata、saveWithoutMetadata、getUpdateTag、
BlockEntity.loadStatic、TagValueInput.create 和 TagValueOutput.createWithContext 等边界入口。
不能为通过编译而自行添加一个没有被游戏调用的双参数 saveAdditional/loadAdditional 重载。

网络协议版本为 "1"，只注册客户端方向，没有允许客户端直接修改祭坛 LP 或进度的入口。
处理器通过 context.player().level() 访问客户端世界，无客户端专用类引用，专用服务端可加载。
进入区块时发送完整标签；后续数值快照由 PacketDistributor.sendToPlayersTrackingChunk 分发。
物品变化使用原版 BE 更新包，不在进度包里重复发送整个 ItemStack。

读写示例、注册及重放命令见 STAGE2_API_EXAMPLES.md，实际 helper 为 BloodOrbData.java。

## 验证证据

### 编译与资源

- 主代码 compileJava 实际执行成功，最终没有废弃 API 警告。
- 集成检查源码 compileStage2SmokeJava 实际执行成功，没有废弃 API 警告。
- test NO-SOURCE 仍属预期；本报告中的 45 项检查是服务端集成断言，不冒称 JUnit 测试。
- verify-stage1-resources.ps1 已更新为当前说明键并通过：235 个 JSON，83 个物品定义，
  22 套方块状态/掉落表，挖掘标签、模型贴图引用及中英文翻译均通过。
- JAR 含新增类和展开后的 stage2 元数据，不含 src/stage2Smoke 的测试类。

### 独立测试世界

设置临时环境变量 ORG_GRADLE_PROJECT_stage2Smoke=true 后，执行相同的 build/runServer 命令。
ModDevGradle 使用正式的 RunModel.gameDirectory 配置，日志确认实际目录为 run/stage2-smoke。
测试源码只在开关开启时加入开发运行环境；普通发行 JAR 不包含测试代码。

两轮关键日志：

```text
[19:56:09] STAGE2_SMOKE PASS: 45 assertions; WRITE persistence fixture created
BUILD SUCCESSFUL in 55s

[19:57:37] STAGE2_SMOKE PASS: 45 assertions; RESTART persistence verified
BUILD SUCCESSFUL in 41s
```

两次均退出码 0，正常保存后停服。原始日志为：

- ../build-stage2-smoke-write.log
- ../build-stage2-smoke-restart.log
- ../run/stage2-smoke/stage2-smoke-result.txt（最近一轮通过标记）

实际检查包括：注册类型/网络 codec、5 种血球默认等级和未绑定状态、组件复制与移除、
非法等级拒绝、LP 填充/抽取边界和整数溢出、库存防御性复制、压缩 NBT 文件保存/读取、
方块实体类型恢复、初始更新标签、缺字段默认值、越界存档归一化、带 registry context 的
ItemStack/自定义组件网络往返、payload 网络往返、服务端状态不可被客户端处理器修改、
方块放置自动创建实体、ticker 连接、物品移除掉落及组件保留、10 tick 后 Stub 不篡改数据，
以及重启后真实区块中的祭坛状态和物品保持一致。

测试祭坛保存在独立世界 (0, 100, 0)，重启比较值为：主 LP 4,321、容量 20,000、
输入/输出缓冲 123/456、缓冲容量 2,000、等级 3、进度 750 / 2,000、活动 true；
单槽内为绑定 Stage2Tester 测试 UUID 的血球，等级组件为 3。

### 普通服务端：指定原始命令

关闭测试开关后再执行用户指定命令，日志确认工作目录为 run，加载列表仅含 main 输出。
使用已有 run/world，主进程通过控制台 stop 正常退出：

```text
[19:58:15] Blood Magic stage 2 entry point loaded
[19:58:16] Blood Magic stage 2 common setup complete: 22 blocks, 83 items, 1 block entity types, 2 data components
[19:58:21] Done (0.500s)! For help, type "help"
[19:58:57] Stopping server
BUILD SUCCESSFUL in 1m 1s
```

终端日志：../build-stage2-server.log。
无终端折行的服务端日志副本：../run-server-stage2.log。
最终普通运行与两次成功集成运行无 ERROR / FATAL；存在 Windows 性能计数器、
原版命令歧义、开发 offline-mode 提示。普通运行另有 stage1 → stage2 的世界模组版本提示。

### 排错记录与验证边界

首次编译修正了事件包名、ChunkPos 构造形式和废弃的 hasChunkAt。
首次集成运行的测试代码对未关联世界的实体调用 getUpdatePacket，缺少 registryAccess 导致失败；
已将检查改到真实放置的实体。首次新目录启动还生成了缺失的 server.properties。
该次失败日志保留为 ../build-stage2-smoke-initial-failed.log，不作为验收成功记录。

观察到 Minecraft 服务端线程崩溃仍可返回退出码 0，因此测试模式增加本次通过标记校验：
运行前清理旧标记，运行后缺少 STAGE2_SMOKE PASS 则 Gradle 失败。最终没有只凭 BUILD SUCCESSFUL
判断运行正常，而是同时检查实际断言标记、Done、正常保存和 ERROR/FATAL 日志。

没有启动图形客户端，未验证真实客户端连接/协议协商、换维度/区块重跟踪操作、
右键绑定的视觉体验或渲染；本次网络验证覆盖注册、编解码和服务端隔离，不等于端到端联机验收。
没有实现或验证从 1.20.1 世界直接升级。

## 遗留 TODO

| 位置 | 后续内容 |
| --- | --- |
| BloodAltarBlockEntity.tickGameplayStub | 配方执行/恢复、符文与多方块等级计算、缓冲转移、充能、红石状态 |
| BlockAltar | 祭坛形状/模型、物品插入取出交互、菜单及渲染 |
| RegistrarBloodMagic | 菜单、祭坛配方类型/序列化器及配方资源 |
| BloodAltarBlockEntity | NeoForge 物品/流体能力、生命精华流体、献祭钩子 |
| ItemBloodOrb | BloodOrb 自定义注册表、灵魂网络 SavedData/LP、实际容量/伤害行为、动态拥有者提示 |
| 存档兼容 | 旧 bloodAltar/Items/binding NBT 的明确版本化升级方案；阶段 1 没有祭坛 BE 数据可迁移 |
| 客户端验收 | 实际两端同步、换维度、区块卸载重进、右键绑定、模型/界面 |

复杂玩法没有被假实现替代为“已完成”；代码中的 tickGameplayStub 和上述相关类均保留 TODO。
其他机器、仪式、地牢、印记及装备继续沿用阶段 1 的后续迁移边界。

## API 核对来源

精确依赖本地文件：

- build/moddev/artifacts/minecraft-patched-26.1.2.122.jar：通过 Java 25 javap 检查
  BlockEntity、BlockEntityType、ValueInput、ValueOutput、TagValueInput、TagValueOutput、
  BaseEntityBlock、ItemStack、DataComponentType.Builder、UUIDUtil、RegistryFriendlyByteBuf、ChunkPos。
- ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.122/
  ba67ce156e2bb5f3c485de8488cc01b0c263e341/neoforge-26.1.2.122-sources.jar：
  PayloadRegistrar、IPayloadContext、PacketDistributor、IBlockEntityExtension 源码。
- 同版本 universal JAR：RegisterPayloadHandlersEvent 的实际包位置、HandlerThread、
  NetworkRegistry、ConnectionType。
- ModDevGradle 2.0.148 JAR：RunModel.gameDirectory、ModModel.sourceSet、addModdingDependenciesTo。

在线文档工具本次未返回可用页面；所有签名以本地已锁定的依赖及实编译/实运行为准。

## 最终产物

- build/libs/bloodmagic-neoforge-0.1.0-stage2.jar
- 大小：238,573 字节。
- SHA-256：82693a11a37d18c4286043e624a98889efdbab3471f182938fe68deac85dadb3。
- 本项目目录没有 .git 元数据；本次直接修改工作区，未创建提交。
