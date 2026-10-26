# 阶段 6：大型机器与合成网络

完成日期：2026-10-25。目标固定为 Minecraft **26.1.2**、NeoForge **26.1.2.122**、Java 25。
新增炼金阵、炼金台、地狱熔炉和基础仪式；发行包共 33 个方块、114 个物品、5 种方块实体、3 种菜单、4 种数据组件。
41 条配方按类型分为：17 条祭坛、6 条炼金阵、6 条炼金台、3 条地狱熔炉、9 条工作台配方。
这是基础功能迁移，未宣称完整复刻原版所有自动化、意志和仪式功能。

原版基准为本地 `../../.references/bloodmagic-original/WayofTime-BloodMagic-35f0188`，提交
`35f0188665e648d981095b6933cc75b0ca96eca5`。核对过 `TileAlchemyArray`、`BlockAlchemyArray`、
`RenderAlchemyArray`、`AlchemyArrayEffectCrafting`、`ItemArcaneAshes` 及配方提供器，随后核对
`TileAlchemyTable`、`TileSoulForge`、`BlockSoulForge`、意志/结晶和灵魂陷阱代码，最后核对
`ItemRitualDiviner`、`TileMasterRitualStone`、`RitualWater`、`RitualGreenGrove`、`RitualCrushing`。
API 依据当前构建使用的 patched Minecraft JAR、NeoForge 源码 JAR 及 `build/stage6-api` 下的 javap 记录。

原版炼金阵和地狱熔炉都是单方块，未增加虚构的外壳结构。真正的结构判断覆盖炼金阵支撑、炼金台两半和仪式符文。
Ritual Diviner 是占卜工具，Master Ritual Stone 才是核心方块。绘阵“粉笔”对应原版 20 次耐久的奥术灰烬。

炼金阵先实现服务端配方、存取、支撑检查和编译，再接入渲染。两槽按顺序匹配，200 tick 后产生一件印记并移除阵。
缺支撑时材料只掉落一次；改变原料或相同 ID 的配方定义会重置进度；保持不变的定义支持保存和重载续作。
占卜、水、熔岩、虚空、空气、先知六种印记均使用原版配方和阵纹贴图。

原版标准阵配方直接耗费 **0 LP**，LP 在制作石板和试剂时支付。1.20.1 的灰烬和试剂配方属于炼金台，
因此补齐了两格炼金台、六原料槽、绑定血球槽、输出槽、GUI 和返还桶逻辑，没有启用上游注释掉的旧熔炉配方。
另补祭坛和空白符文工作台配方，解决从普通生存材料进入印记链的缺口。
阵配方可选 `lpCost`（默认 0）；费用在产物生成时支付，生成事件取消或抛出异常时退款；取消时保留原料。

| 炼金台产物 | 材料 | 血球最低等级 | LP | 时长 |
|---|---|---:|---:|---:|
| 奥术灰烬 | 红石、白色染料、火药、煤/木炭 | 1 | 500 | 200 tick |
| 水试剂 | 糖、水桶、水桶 | 1 | 300 | 200 tick |
| 熔岩试剂 | 熔岩桶、红石、圆石、煤块 | 1 | 1,000 | 200 tick |
| 虚空试剂 | 桶、线、线、火药 | 2 | 1,000 | 200 tick |
| 空气试剂 | 恶魂之泪、羽毛、羽毛 | 2 | 2,000 | 200 tick |
| 视觉试剂 | 荧石粉、玻璃、玻璃、占卜印记 | 1 | 500 | 200 tick |

六种试剂/灰烬均有真实配方执行和精确扣费断言，合计 5,300 LP。水印记另有完整串联测试：
工作台祭坛 → 祭坛石板/弱血球 → 工作台空白符文 → 二级祭坛/学徒血球 → 工作台炼金台 → 灰烬和试剂 → 炼金阵产物。
工作台血球返还绑定组件。先知印记的视觉试剂还需要占卜印记；六种阵配方均单独验证 200 tick 和材料消耗。

地狱熔炉有四输入、意志槽和输出槽。Demon Will 用持久化及联网数据组件保存小数，拒绝负数、NaN、无穷和越界值。
原始意志可通过灵魂陷阱捕获的怪物死亡掉落；右键可将背包中的原始意志转入结晶。
模拟扣费和输出容量检查完成后一次提交，输出满时不扣意志，升级结晶保留扣费后的剩余值。

| 结晶 | 容量 | 合成最低意志 | 实际消耗 | 时长 |
|---|---:|---:|---:|---:|
| 微弱 | 64 | 1 | 1 | 100 tick |
| 次级 | 256 | 60 | 20 | 100 tick |
| 普通 | 1,024 | 240 | 50 | 100 tick |

炼金台、熔炉提供原版 Container/WorldlyContainer 自动化入口：上面/侧面输入原料，下面提取产物。
血球与意志槽手动管理；本轮没有承诺第三方物品/流体 capability 兼容。

仪式占卜棒选择图案、逐块消耗空白仪式石搭建，铭文工具可转换符文。普通棒支持前两种，暮色棒支持全部三种。
激活水晶绑定拥有者，完整结构一次扣激活费；重复激活不重复收费。拥有者离线可继续使用 UUID 灵魂网络。

| 仪式 | 符文构型（相对核心） | 激活 / 效果 LP | 行为 |
|---|---|---|---|
| 满泉 | y=0，半径 1 的四角水符文 | 500 / 25 | 正上方空位生成水源，已有水不收费；水蒸发环境不执行 |
| 绿林 | y=0，半径 1 四角地、正交水 | 5,000 / 20 | y=2 的 3×3 CropBlock；每 20 tick 按原版基础概率尝试生长，实际变化才扣费 |
| 采掘 | y=0：r1 正交地、r2 正交火、r2 四角暮色；y=1：r2 正交风 | 5,000 / 7 | 核心下方 3×3×3，每 40 tick 最多破坏一块，钻石镐战利品送入上方容器 |

所有符文区块已加载才判断有效性；未加载暂停且不强制加载，错误/缺失符文停用。红石暂停保留激活状态，解除后不再收激活费。
放置和采掘尊重当前保护事件；被拒绝不产生效果、不扣 LP。采掘跳过不可破坏方块、流体、方块实体和仪式方块。

26.1 相关迁移如下，均以本地固定运行库核对并编译/执行。

| 接口变化 | 本轮处理 |
|---|---|
| `BlockEntityRenderer<BE, State>`，`createRenderState` / `extractRenderState` / `submit` | 提取阵纹、朝向和两个物品渲染状态；提交阶段不读取可变方块实体 |
| `SubmitNodeCollector.submitCustomGeometry`、`renderer.rendertype.RenderTypes` | 阵纹四边形通过延迟提交绘制，回调只使用本帧几何参数 |
| `ItemModelResolver` 与 `ItemStackRenderState.submit` | 阵中物品走新版渲染管线；灵魂陷阱复用新版 `ThrownItemRenderer` |
| 方块实体/实体存档改用 `ValueInput` / `ValueOutput` | 物品、拥有者、配方定义、进度及仪式状态明确编码；绑定和意志使用数据组件 |
| `BlockEntity.preRemoveSideEffects` | 库存掉落集中在一次移除钩子；炼金台副半块不持有库存，方块战利品限定主半块 |
| `DirectionProperty` 不再使用，方向为 `EnumProperty<Direction>` | 炼金台朝向与 `BedPart` 区分主副；只有主半块提供方块实体和 ticker |
| `updateShape` 含 `LevelReader`、`ScheduledTickAccess` 等新参数 | 只安排延迟结构检查；相邻区块未加载时等待，避免把未知结构当成损坏 |
| 配方 `MapCodec` + `StreamCodec`，结果 `ItemStackTemplate` | 数据包加载不提前构造依赖尚未就绪默认组件的 ItemStack；在运行时生成产物 |
| 投掷物位于 `projectile.throwableitemprojectile`；构造器增加 ItemStack | 新灵魂陷阱构造方式和原生生成/同步；EntityType 的 build 使用 ResourceKey |
| MobEffectInstance 接受 Holder | 捕获状态传注册 holder，不再传裸 effect 对象 |
| `hurtAndBreak(..., InteractionHand)` | 灰烬和铭文工具使用实际交互手，验证副手及创造模式 |
| 旧 `BlockEvent.BreakEvent` 已移除 | 使用 `event.level.block.BreakBlockEvent`；取消事件有真实服务端断言 |
| `dimensionType().ultraWarm()` 路径替换 | 使用 `EnvironmentAttributes.WATER_EVAPORATES` 查询目标位置环境 |
| 服务端光照由 `ThreadedLevelLightEngine` 调度 | 测试使用 `waitForPendingTasks` / `tryScheduleUpdate` 异步等待，禁止直接 `runLightUpdates` |
| GUI 使用 `GuiGraphicsExtractor` | 两种机器界面使用 extractBackground/extractLabels；数值采用 NeoForge 菜单 int 同步 |

主要实现入口：

- [炼金阵](../src/main/java/wayoftime/bloodmagic/common/block/entity/AlchemyArrayBlockEntity.java)、[渲染器](../src/main/java/wayoftime/bloodmagic/client/render/AlchemyArrayRenderer.java)。
- [炼金台结构](../src/main/java/wayoftime/bloodmagic/common/block/BlockAlchemyTable.java)、[库存与生命周期](../src/main/java/wayoftime/bloodmagic/common/block/entity/MachineInventory.java)。
- [地狱熔炉](../src/main/java/wayoftime/bloodmagic/common/block/entity/HellfireForgeBlockEntity.java)、[意志组件](../src/main/java/wayoftime/bloodmagic/will/DemonWill.java)。
- [仪式核心](../src/main/java/wayoftime/bloodmagic/common/block/entity/MasterRitualBlockEntity.java)、[结构定义](../src/main/java/wayoftime/bloodmagic/ritual/BasicRitual.java)。
- [资源生成脚本](../scripts/prepare-stage6.py)、[完整清单](stage6-content.json)、[服务端测试源码](../src/stage6Smoke/java/wayoftime/bloodmagic/test)。

正式验收命令：`./scripts/verify-stage6-machines.ps1 -Restart`。2026-10-25 的结果：

| 检查 | 结果 |
|---|---|
| `build`（含客户端源码编译） | 两轮 BUILD SUCCESSFUL |
| 第一轮独立服务端 | 477 assertions，RESTART persistence verified |
| 第二轮独立服务端 | **480 assertions**，RESTART persistence verified；增加新炼金台存档的三项检查 |
| 存档重启 | 祭坛、网络、阵的 70 tick/拥有者/原料、熔炉的 40 tick/5.25 意志、仪式绑定/激活状态、两格炼金台 80 tick 续作 |
| 炼金台重启付款 | 剩余 120 tick 完成，1,000 LP 降至 500 LP，未重复收费 |
| 实际数据包重载 | 祭坛原有重载测试及炼金阵三次重载；同 ID 修改定义重置，移除覆盖恢复原配方 |
| 阵 LP | 余额不足暂停、完成扣一次 70 LP、实体生成取消退款、移除后不再次扣费/出产物 |
| 多方块与保护 | 悬空阵退材料、炼金台缺半块暂停/拆除只掉一桌一库存、仪式缺符文/红石/区块/保护事件 |
| 资源 | 383 个 JSON 解析；114 个物品定义，33 个 blockstate，32 种方块掉落/工具标签与无物品炼金阵特例；贴图、模型、双语名称和占位符 |
| 发行包 | 关键类/配方/贴图齐全；不含 `wayoftime/bloodmagic/test/` 测试类 |

`test` Gradle 任务本身是 NO-SOURCE；这里的测试结论来自真实 `runServer -Pstage6Smoke=true` 的断言，
不是把空单元测试当作通过。Gradle 每次删除旧 PASS，缺少新 PASS 必须失败；全部断言和保存完成后才提交下一次的持久化基线。
开发中一次中断在重建旧祭坛测试后退出，导致实际进度 250 与早先快照 315 不同；读盘核对后修正测试基线的提交时机，保留了严格重启比较。
回归还修正空气印记非整角朝向时 look vector 的浮点长度误差，先 normalize 再乘 1.7，测试固定了非整角视线。

日志：[第一轮](../build-stage6-verify-pass1.log)、[第二轮](../build-stage6-verify-pass2.log)、[完整验收](../build-stage6-verification.log)。
日志中的 Yggdrasil 公钥网络获取失败发生在离线测试环境，未影响服务端玩法断言或构建退出结果。

发行包：`build/libs/bloodmagic-neoforge-0.1.0-stage6.jar`，680,831 字节。
SHA-256：`B28A601C31D821FF513EDB7F33C1D15792B673C9BBD6B1A210562B91410CE96B`。

尚未完成的范围：

- 没有运行图形客户端；阵纹、投掷物和 GUI 只完成编译、资源核查及独立服务端隔离验证，需游戏中检查视觉、鼠标操作和联机表现。
- 炼金台和熔炉使用简化模型，没有迁移原版完整 OBJ 和全部动画。
- 只有原始意志和三档结晶；彩色/环境意志网络、所有意志配方、宝石自动拾取和相关装备未迁移。
- 仪式仅以上三种；绿林只支持原版 CropBlock，不包含 AgriCraft、全部植物、范围编辑、环境意志强化和罐体供水。
- 熔岩水晶仅作为激活水晶制作前置，LP 熔炉燃料行为尚未实现。高级仪式所需四级及以上材料的完整获取链仍受前阶段未迁移内容限制。
- 普通漏斗入口已实现，但血球/意志槽为手动管理，没有第三方 capability/JEI 集成；更完整的合成网络仍需后续阶段。
- 不提供 1.20.1 世界旧 NBT 的导入转换；配方标签只迁移当前这批所需原版/通用材料。
