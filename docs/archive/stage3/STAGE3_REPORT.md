# 阶段 3：祭坛逻辑、多方块与合成配方

日期：2026-10-25（Asia/Shanghai）。目标：Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。

## 实现范围

按符文检测、原生配方匹配、交互与 LP 合成的顺序实现。没有硬编码配方回退：已提供 10 条正式 JSON。
此前 22 个方块、83 个物品全部保留；为五级结构与大法师血球配方补入上游 `dungeon_metal` 狱锻块，
现在是 23 个方块、84 个物品。追加其方块/物品模型、贴图、掉落表、翻译和挖掘标签。

### 原版阅读依据

锁定提交仍为 `35f0188665e648d981095b6933cc75b0ca96eca5`，本地目录：
`../../.references/bloodmagic-original/WayofTime-BloodMagic-35f0188`。

实际读取了以下类，而非仅按旧教程猜测接口：

- `common/tile/TileAltar.java`：单槽实体、ticker、供血与存储委派。
- `altar/BloodAltar.java`：配方循环、消耗 LP、断血退度、批量合成、冷却、符文公式。
- `altar/AltarTier.java`、`AltarComponent.java`、`AltarUtil.java`、`AltarUpgrade.java`、`ComponentType.java`。
- `block/enums/BloodRuneType.java`、`altar/IBloodRune.java`。
- `recipe/RecipeBloodAltar.java`、`common/recipe/serializer/BloodAltarRecipeSerializer.java`、
  `common/recipe/BloodAltarRecipeProvider.java` 和生成的配方 JSON。
- `common/block/BlockAltar.java`、`common/item/ItemSacrificialDagger.java`、`ConfigManager.java`。
- `impl/BloodMagicCorePlugin.java`：结构组件的实际注册，尤其五级顶石及替代发光方块。

## 多方块检测

`AltarStructure` 单独保存坐标与有效槽位，`BloodAltarBlockEntity.refreshStructure()` 使用结果更新等级、
主槽和缓冲容量。`AltarUpgrades` 按 `IBloodRune.getRuneCount` 累积强度，并提供速度、容量、退度和献祭公式。

| 等级 | 符文结构 | 有效升级槽 | 顶石/柱体 |
| --- | --- | ---: | --- |
| 1 | 无 | 0 | 无 |
| 2 | 下方一格的 3×3 环，排除中心 | 4 | 四角用于成型，但不提供加成 |
| 3 | 再加 y=-2、半径 3 的四条边，每边 5 块 | 28 | (±3, -1..0, ±3) 柱体，y=1 发光顶石 |
| 4 | 再加 y=-3、半径 5 的四条边，每边 7 块 | 56 | (±5, -2..1, ±5) 柱体，y=2 血石顶石 |
| 5 | 再加 y=-4、半径 8 的四条边，每边 13 块 | 108 | (±8, -4, ±8) 狱锻块 |
| 6 | 坐标已保留 | 184 | `bloodmagic:altar/crystal` 默认空，需数据包扩展 |

五级结构的组件枚举名虽然叫 BEACON，但该上游提交的实际有效方块是狱锻块及其地牢变种，
不是原版信标。新增 `dungeon_metal` 满足主要路径，尚未迁移的四种变体在标签中设为 optional。
发光顶石标签保留荧石、海晶灯、菌光体和三种蛙明灯；血石标签允许两种原有血石方块。

符文加成：

- 速度倍率 `1 + 0.20 × SPEED`；每 tick 扣除的 LP 与进度同步增加，总配方 LP 需求不变。
- 容量倍率 `(1 + 0.20 × CAPACITY) × 1.075^AUGMENTED_CAPACITY`。
- 主槽和缓冲分别从 10,000 和 1,000 LP 乘容量倍率；降级时按新容量截断。
- 自我献祭倍率 `1 + 0.10 × SELF_SACRIFICE`；断血退度乘 `0.85^EFFICIENCY`。
- 强化符文按强度 2 计算。其他符文类型已计数，其依赖的缓冲、血球、充能玩法仍为后续范围。

每 20 tick 重新检测，插入/献祭时立即检测。扫描只调用已加载区块读取，不强制生成或加载区块。
已有高等级因相关区块未加载而无法判断时暂停合成、保留容量和 LP；已加载位置确定缺失时正常降级。
方块被移除后，周期检测的正常响应延迟上限为 20 tick。

## 26.1 原生配方体系

`BloodAltarRecipe implements Recipe<BloodAltarRecipeInput>`；输入封装单槽物品和当前祭坛等级。
`BloodMagicRecipes` 通过 DeferredRegister 注册 `bloodmagic:altar` 的 RecipeType 和 RecipeSerializer。

| 原版或旧版形式 | 本项目实际 26.1.2 API |
| --- | --- |
| `Recipe<Container>`、外部 API 自行匹配 | `Recipe<BloodAltarRecipeInput>`，服务端 RecipeManager 匹配物品、等级、批量边界 |
| `RecipeSerializer` 接口 + fromJson/fromNetwork/toNetwork | `RecipeSerializer` 是 final record，构造传入 MapCodec 与 StreamCodec |
| Gson 手动字段解析 | RecordCodecBuilder.mapCodec；数值范围直接由 Codec 校验 |
| 配方加载时创建 ItemStack 输出 | 使用不可变 ItemStackTemplate；运行期 `create()` 生成结果 |
| `assemble(input, registries)` 示例 | 26.1.2 为 `assemble(input)` |
| 配方对象持有 ResourceLocation ID | RecipeHolder 持有 `ResourceKey<Recipe<?>>`；命名 ID 为 Identifier |
| `Ingredient.fromJson({"tag": "forge:stone"})` | `Ingredient.CODEC`，输入 `"#c:stones"`；物品也可直接写 ID 字符串 |
| 输出 `{"item":"bloodmagic:blankslate"}` | 输出 `{"id":"bloodmagic:blankslate","count":1}`，由 ItemStackTemplate.CODEC 解析 |
| `data/bloodmagic/recipes` | 单数目录 `data/bloodmagic/recipe` |
| BloodMagicAPI 注册结构组件 BlockState 列表 | 可由数据包扩展的方块标签 + IBloodRune 检测 |
| `Block.use(...)` | `useItemOn(...)` 与 `useWithoutItem(...)` 两个交互入口 |

首次运行实际暴露了 ItemStack 生命周期差异：配方 JSON 加载早于默认组件就绪，使用 ItemStack.CODEC
会报 `Item ... does not have components yet`。对照锁定 JAR 中 ShapedRecipe/ShapelessRecipe 后改为
ItemStackTemplate.CODEC/STREAM_CODEC；MapCodec 解码期间不访问默认组件、不创建 ItemStack。
匹配和产出发生在服务器运行期，才检查输出最大堆叠数。

Recipe 的 PlacementInfo 为 NOT_PLACEABLE，isSpecial 为 true；本阶段不把祭坛塞入工作台配方书。
StreamCodec 使用 RegistryFriendlyByteBuf、Ingredient.CONTENTS_STREAM_CODEC 与 ItemStackTemplate.STREAM_CODEC。
配方网络 codec 已验证，但这不代表实现了 JEI 或客户端完整配方列表同步界面。

### 已迁移数据

字段 `upgradeLevel` 保持原版 0..5，在 codec 内转换为运行期 1..6。其余原版数值字段也保留。

| 配方文件 | 输入 → 输出 | 最低等级 | LP | 消耗/tick | 断血退度/tick |
| --- | --- | ---: | ---: | ---: | ---: |
| altar/slate | 石头标签 → 空白石板 | 1 | 1000 | 5 | 5 |
| altar/reinforcedslate | 空白 → 强化石板 | 2 | 2000 | 5 | 5 |
| altar/imbuedslate | 强化 → 注魔石板 | 3 | 5000 | 15 | 10 |
| altar/demonicslate | 注魔 → 恶魔石板 | 4 | 15000 | 20 | 20 |
| altar/etherealslate | 恶魔 → 空灵石板 | 5 | 30000 | 40 | 100 |
| altar/weakbloodorb | 钻石标签 → 弱小血球 | 1 | 2000 | 5 | 1 |
| altar/apprenticebloodorb | 红石块标签 → 学徒血球 | 2 | 5000 | 5 | 5 |
| altar/magicianbloodorb | 金块标签 → 魔法师血球 | 3 | 25000 | 20 | 20 |
| altar/masterbloodorb | 弱小血之碎片 → 大师血球 | 4 | 40000 | 30 | 50 |
| altar/archmagebloodorb | 狱锻块 → 大法师血球 | 5 | 80000 | 50 | 100 |

其他上游祭坛配方依赖尚未迁移的工具、流体桶、传送器、炼金瓶等内容，明确留待对应模块迁移。
不存在 `TODO: JSON Data` 的硬编码替代路径。

## 交互、供血与执行

持物右键放入一件；空手右键取出，背包满时掉落剩余物品。已有物品时拒绝覆盖。
生存模式扣除实际放入物品，创造模式保留手持物品。旁观者、跨世界和超出交互距离的操作被拒绝。
客户端只返回交互预测结果，真正的库存、生命值和 LP 变更均在服务端执行。

潜行空手右键调用 `sacrificeHealth`：最多请求 2 点魔法伤害，按**实际减少的生命值** × 100 LP 转换，
再应用自我献祭符文倍率。无敌、伤害被取消或完全被吸收时不会凭空产生 LP。
生命值不高于 2 时拒绝，满槽不扣血；创造模式每次基础供血 200 LP。
这是本阶段明确的临时交互设计，不宣称已迁移献祭匕首、玩家 SoulNetwork 或生物献祭事件。
原版符文只是升级组件，没有独立 LP 储存，故不存在“从符文抽取 LP”的假实现。

进度单位为 LP。每 tick 实际消费 `min(倍率后消耗速率, 主槽余量, 本次剩余需求)`，
完成之前保留原料，完成后替换为输出。空槽、无配方、等级不足时不扣血。
缺血按 drainRate 退度，已经消耗的 LP 不返还。内部支持整批配方，需求与结果数量同步乘输入数量，
拒绝 LP 整数溢出或输出超堆叠；右键默认只放一件，避免将多颗钻石批量变成不可堆叠血球。
成功后等待 30 tick 再匹配下一配方，以支持原版石板逐级合成。

保存 recipe ID、不可变配方定义、已支付进度和完成冷却。每 tick 从当前 RecipeManager 解析配方，
同 ID 定义改变、物品改变、总需求改变、配方消失或等级不足时清除旧进度。合法未完成配方重启后继续，
不重新扣除已支付的 LP。数值状态沿用阶段 2 的同步包，物品变化沿用原版方块实体更新包。

额外修正了重载身份问题：Ingredient.equals 会比较 Named HolderSet 对象，重载后同一标签可以对应新对象。
原生 Ingredient 改为比较 unwrap 后的标签键或直接物品集合，避免未修改配方重载时误清空进度。
第三方自定义 Ingredient 若没有提供稳定的 equals，采取保守的进度重置策略。

## 验证与结果

独立测试命令：

```powershell
.\build-local.ps1 -GradleTasks @('build','runServer','-Pstage3Smoke=true')
```

测试源码从 `src/stage2Smoke` 升级为 `src/stage3Smoke`，保留并更新原有 45 项基础断言，追加 81 项，
合计 126 项服务端集成断言。旧 Stub“不修改进度”检查改为“空闲祭坛保留存储”，与真实玩法一致。
`test NO-SOURCE` 不冒称 JUnit 通过；本报告所述检查由真实专用服务端加载测试 source set 执行。

第一轮通过日志：

```text
[20:33:04] STAGE3_SMOKE PASS: 126 assertions; WRITE persistence fixture created
BUILD SUCCESSFUL in 1m 9s
```

日志为 `../build-stage3-smoke-write.log`，退出码 0，无 ERROR/FATAL。
第二轮使用同一世界和同一命令，实际加载上一轮保存的进行中配方并继续扣除后续 LP：

```text
[20:34:23] STAGE3_SMOKE PASS: 126 assertions; RESTART persistence verified
BUILD SUCCESSFUL in 53s
```

日志为 `../build-stage3-smoke-restart.log`，退出码 0，无 ERROR/FATAL。
最新通过标记位于 `../run/stage3-smoke/stage3-smoke-result.txt`。

关闭测试开关后实际执行用户要求的普通命令：

```powershell
.\build-local.ps1 -GradleTasks @('build','runServer')
```

运行目录确认为 `run`，仅加载 main 输出；20:35:37 到达 `Done (0.662s)`，20:36:13 经控制台 `stop`
正常保存所有维度并退出。结果 `BUILD SUCCESSFUL in 1m 5s`，退出码 0。
普通服务端完整日志为 `../run-server-stage3.log`，控制台退出结果为 `../build-stage3-server-result.log`。
无 ERROR/FATAL；保留环境已有的 Windows 性能计数器、原版命令歧义、offline-mode 及世界模组版本变更提示。
独立测试在连续 reload 时出现过运行落后提示，不影响断言结果。

验证内容包括：

- 原有数据组件、网络 codec、序列化、默认值、边界、物品防御性复制、掉落与服务端隔离。
- 独立坐标搭建一至五级结构，升级槽数、无效邻近符文、二级角落、强化符文、替代顶石、液体柱体、
  五级狱锻块、符文拆除后等级/容量下降、未加载区块不强载也不削减 LP。
- 10 条实际数据包配方加载；每条 Recipe.CODEC JSON 往返；Recipe.STREAM_CODEC 注册表分派往返；
  非法数值输入、最低等级、错误物品、输出超堆叠和 LP 整数溢出。
- 石头消耗恰好 1,000 LP 产出石板；两件原料扣 2,000 LP；速度符文最后一 tick 只扣剩余 LP；
  无血不合成、断血退度、换原料清进度、低等级拒绝、物品输出不重复。
- 模拟生存玩家经真实方块 useItemOn/useWithoutItem 入口放入原料，献祭生命值供血，完成并取出石板；
  低血量、满槽、旁观者和远距离行为；自我献祭符文加成。
- 真正的 ticker 推进完成；未完成配方的 NBT 恢复及真实世界重启续作。
- 在独立测试世界建立测试数据包并实际 reload 三次，验证配方内容修改、匹配消失、原配方恢复；
  另一个未修改配方在这三次 reload 期间保持 LP 守恒。结束时删除测试配方覆盖文件。

测试每轮先清理 PASS 标记，完成所有断言后才写入新标记并保存停服；缺标记使 Gradle 失败，
避免 Minecraft 线程崩溃但进程仍返回 0 的假阳性。测试类和测试数据包不进入发行 JAR。
资源验证结果：254 个 JSON、84 个物品定义、23 套方块状态/掉落/挖掘标签、模型贴图引用与双语翻译均通过。

### 本轮排错记录

首次运行发现 ItemStack 提前创建导致 JSON 配方加载失败，以及容量公式使用 double 后截断产生一 LP 精度偏差；
分别改用 ItemStackTemplate 和与上游一致的 float 乘法截断。
FakePlayer 默认无敌且连接未完成加载，测试夹具显式解除无敌并 markClientLoaded，保留真实伤害计算路径。
重启世界时实体分区异步就绪，掉落实体验证移到初始世界 tick 后执行，并排除之前失败运行残留的实体。
实际 reload 检查发现并修正了 Named HolderSet 身份变化造成的进度误清零。
早期失败日志与 crash-reports 保留，不作为验收通过证据。

## 最终产物

- `../build/libs/bloodmagic-neoforge-0.1.0-stage3.jar`
- 大小：266,833 字节。
- SHA-256：`A3089B4EB168275D0219D87EFF83978FA28699CDDD9DCA39425BDE0F81C730F7`。
- JAR 内包含 10 条祭坛配方，未包含服务端测试类。
- 工程没有 .git 元数据；已直接修改共享工作区，没有创建提交。

## 剩余范围

- 祭坛形状、物品渲染、GUI、音效粒子及图形客户端操作验收。
- 灵魂网络 SavedData、血球充能、自定义 BloodOrb 注册表。
- 原版献祭匕首、祭刀/生物献祭、伤害事件与香炉加成；当前采用直接右键自我献祭。
- 流体/物品自动化能力、缓冲槽转移、位移/加速度/充能符文实际行为、红石输出。
- 尚未注册内容对应的其他祭坛 JSON；六级默认顶石继续与上游一致地不启用。
- 1.20.1 旧世界 NBT 转换。现有阶段 2 状态可读取，但无活动配方身份的旧进度不会被当成已支付的新配方进度。

## API 核对来源

以精确本地依赖 `build/moddev/artifacts/minecraft-patched-26.1.2.122.jar` 的 javap 签名/字节码、
同版本 NeoForge universal JAR，以及实际编译/运行结果为准。检查了 Recipe、RecipeSerializer、RecipeInput、
RecipeManager、RecipeHolder、Ingredient、ItemStackTemplate、ShapedRecipe、ShapelessRecipe、
BlockBehaviour、ServerPlayer、FakePlayer 和注册表/配方标签。
在线文档工具未返回可用正文，不把其他版本文档当作本项目 API 的验证依据。
