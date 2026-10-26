# 阶段 1：注册系统与基础内容

日期：2026-10-25（Asia/Shanghai）。
目标固定为 Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。
输入依据：STAGE1_INPUTS.md 和本地上游 1.20.1 提交
35f0188665e648d981095b6933cc75b0ca96eca5。

## 实现范围

本阶段按输入文档的“第一批”范围迁移，未把上游完整玩法注册表全部替换为假实现。

| 内容 | 数量 | 状态 |
| --- | ---: | --- |
| 原版直接使用 ItemBase 的有效注册 | 56 | 已迁移，包括 5 级石板、材料、试剂、催化剂、矿物碎片/砂等 |
| 血符文 | 19 | 已迁移：10 个一级、9 个二级；类型和强度与原版一致 |
| 血石方块 | 2 | 普通方块实现，保留原属性 |
| 祭坛 | 1 | 普通实心方块占位 |
| 方块物品 | 22 | 与方块使用相同 ID，可取得、放置 |
| 血球物品 | 5 | 无功能占位，堆叠上限为 1 |
| 主创造模式标签页 | 1 | bloodmagic:bloodmagic |
| 符文方块类型 codec | 1 | bloodmagic:blood_rune，保留 rune_type、rune_strength、properties |

合计 22 个方块、83 个物品。完整字段/ID 清单见 stage1-content.json。
普通物品中的 slate_vial 仍为 16 堆叠，其余普通材料为 64。
infusedslate、demonslate、dislocationrune 等原版拼写均保留。
EFFICIENCY 枚举值保留，但上游没有对应方块注册，本阶段也不虚构新 ID。

请求的六个源文件均已落地；另补充 IBloodRune、
ItemBlockBloodRune、BloodMagicCreativeTabs，并修改 BloodMagic 入口。
原 RegistrarBloodMagic 几乎全为旧注释代码；本移植将其明确改作
IEventBus 的统一绑定入口。注册顺序为方块类型/方块、物品、创造模式标签页；
静态初始化期间不提前调用方块或物品 holder.get()。

## API 核对与迁移

以下依据本地精确版本源码/JAR 核对，不以较旧的 1.21 示例代替 26.1.2 API：

- 官方 26.1.2 MDK，提交 070b9d2：
  ../../.references/mdk/NeoForgeMDKs-MDK-26.1.2-ModDevGradle-070b9d2/
  中的 ExampleMod.java。
- Maven 坐标 net.neoforged:neoforge:26.1.2.122:sources：
  net/neoforged/neoforge/registries/DeferredRegister.java。
- build/moddev/artifacts/minecraft-patched-26.1.2.122.jar：
  使用 Java 25 javap 检查 Item、Block、BlockBehaviour、Item.Properties、
  ItemLore、StringRepresentable 和 Registries；并读取原版资源 JSON。

| 1.20.1 用法 | 本次 26.1.2 实现 |
| --- | --- |
| net.minecraftforge.registries | net.neoforged.neoforge.registries |
| DeferredRegister<Item/Block> + ForgeRegistries | 专用 DeferredRegister.Items / .Blocks；createItems / createBlocks |
| RegistryObject<T> | DeferredItem<T> / DeferredBlock<T>；通用注册使用 DeferredHolder |
| ResourceLocation | Identifier.fromNamespaceAndPath；BloodMagic.rl 返回 Identifier |
| 构造函数内部创建无 ID 的 Properties | 构造函数接收 Properties；registerItem/registerBlock 在构造前调用 setId(ResourceKey.create(...)) |
| 方块物品独立手写注册 ID | registerSimpleBlockItem(holder)，符文物品从 holder ID 派生并延迟取方块 |
| 独立方块物品语言键容易变成 item.* | useBlockDescriptionPrefix()，沿用 block.bloodmagic.* |
| Block.appendHoverText / 旧 Item.appendHoverText | 方块物品和 ItemBase 使用 DataComponents.LORE；文字灰色且非斜体 |
| 单独 models/item 文件 | 增加 assets/bloodmagic/items/<id>.json，使用 minecraft:model 定义 |
| loot_tables、tags/blocks | loot_table、tags/block |

registerItem/registerBlock 的实际源码先设置 ResourceKey，再调用对象工厂。
不能简单把旧无参 ItemBase/BlockBloodRune 构造方法直接包进 register(name, supplier)。

首次编译发现 Item.appendHoverText 的新版签名同样带有 Deprecated，
因此最终使用静态 lore 数据组件，未通过压制警告继续调用旧 hook。
编译启用了 -Xlint:deprecation；最终源码编译没有废弃 API 警告。
符文额外使用已注册的 MapCodec，使类型及强度不会在 codec 中丢失。

## 资源

仅复制所选内容需要的上游模型、贴图及中英翻译，递归包含模型父级和贴图依赖。
资源均位于 src/main/resources，当前无数据生成器任务依赖。

- 83 份新版物品定义，22 份方块状态，22 份自身掉落表。
- 符文/血石保留原版材质和模型。
- 祭坛暂用 blankrune 贴图的立方体模型；名称和说明均标明占位。
- 血球保留原贴图，但说明明确表示尚无绑定和 LP 功能。
- 所有 22 个方块均加入 minecraft:mineable/pickaxe。
- 中英文包含物品名、方块名、标签页名以及说明文字。
- 尚未迁移任何合成配方，也未复制依赖其他子系统的旧 JSON。

资源检查命令：

~~~powershell
.\scripts\verify-stage1-resources.ps1
~~~

实际结果：235 个 JSON 全部解析成功；83 个物品定义、22 套方块状态/掉落表、
挖掘标签、模型/贴图引用及 en_us/zh_cn 语言键均通过检查。
日志：../verify-stage1-resources.log。

## TODO 占位与后续边界

代码注释均使用 // TODO: Stage 2 或 // TODO: Stage 3。
这里的阶段号用于后续工作分组，不表示这些玩法已完成。

| 阶段 | 位置 | 遗留内容 |
| --- | --- | --- |
| Stage 2 | BloodMagicBlocks.BLOOD_ALTAR、BloodMagicItems.BLOOD_ALTAR_ITEM | 替换普通方块占位，迁移祭坛形状、方块实体、LP 存储、交互、菜单、配方、模型/渲染 |
| Stage 2 | RegistrarBloodMagic.register | 接入祭坛方块实体、菜单、配方序列化器；恢复相应获取/合成配方 |
| Stage 3 | BloodMagicItems 的 5 个血球 | 替换 ItemBase 占位，迁移 ItemBloodOrb、BloodOrb 自定义注册表、拥有者组件、灵魂网络/LP 行为 |
| Stage 3 | RegistrarBloodMagic.register | 接入血球注册表、流体、自定义数据组件、网络系统 |
| Stage 3 | BloodMagicBlocks | 其余机器、仪式、地牢方块及菜单当前未注册，随对应子系统迁移 |
| Stage 3 | BloodMagicItems | 印记、工具/装备、流体桶、路由、地牢等复杂物品当前未注册 |
| Stage 3 | BloodMagicCreativeTabs | 补回 bloodmagic_upgrades、bloodmagic_decorative 及有组件数据的物品变体 |

没有创建空壳血球注册表、方块实体或菜单来冒充已实现的子系统。
IBloodRune、BloodRuneType 和符文类型/强度查询为实际实现，并非占位；
祭坛对其加成的消费逻辑仍属于 Stage 2。

## 验证与边界

指定构建命令已执行：

~~~powershell
.\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('build')
~~~

gradle.properties 已设置 org.gradle.parallel=false、org.gradle.workers.max=1；
保留 -Xmx2G 和二进制开发流程，避免 Minecraft 全量反编译/重编译。
Java 源码实际通过 compileJava，最终构建记录见 ../build-stage1.log。
项目暂未引入 JUnit/GameTest；test NO-SOURCE 属预期，不将其称为单元测试通过。

开发服务端也实际执行 runServer：
临时 JavaExec 配置将服务端堆上限设为 1 GiB、ActiveProcessorCount 设为 2，
并转发标准输入；Gradle 使用单次 512 MiB daemon，结束后自动退出。
测试使用已有 run/world，未进入图形客户端。
本次尝试以 JavaExec.workingDir 指定独立目录被 ModDevGradle 的运行配置覆盖，
实际工作目录由日志确认为 run；没有把本次报告为独立测试世界验证。

关键日志（2026-10-25，Asia/Shanghai）：

~~~text
[19:36:02] Blood Magic stage 1 entry point loaded
[19:36:03] Blood Magic stage 1 common setup complete: 22 blocks, 83 items
[19:36:10] Done (0.530s)! For help, type "help"
[19:36:48] Stopping server
BUILD SUCCESSFUL in 1m 33s
~~~

通过控制台 stop 正常存盘退出，runServer 退出码为 0。
服务端日志已保存到 ../run-server-stage1.log。
日志中存在 Windows 性能计数器警告、原版命令歧义警告和
开发世界版本从 stage0 升到 stage1 的提示，没有模组注册或数据包加载错误。
尝试的 debug report 子命令被当前版本拒绝；未产出该调试报告，不作为验收依据。

验收覆盖编译/打包、服务端注册与加载、数据包加载、资源静态检查、正常停服。
未验证图形客户端的模型烘焙、创造模式 UI 或实际操作；未验证祭坛/血球等占位玩法。

## 最终产物

最终指定 build 命令：BUILD SUCCESSFUL in 11s，退出码 0。
Java 代码在此前的完整 compileJava 构建中实际编译成功（5s，无废弃 API 警告）；
最终一次构建重新打包了更新后的阶段元数据与署名。

- 产物：build/libs/bloodmagic-neoforge-0.1.0-stage1.jar
- 大小：218662 字节
- SHA-256：912c6f911eff65e8cc62ed5c44503aabdd3b54a84d1db6fc2dbc0f070b0c3db9
- JAR 检查：请求的全部类及接口/标签页、83 个物品定义、
  22 个方块状态、展开后的 stage1 元数据及许可/署名文件均存在。
