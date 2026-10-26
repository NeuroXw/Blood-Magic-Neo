# 阶段 4：祭坛 GUI 与交互

日期：2026-10-25（Asia/Shanghai）。固定目标：Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。

阶段 3 已归档至 `archive/stage3`，保留报告原件、126 项断言的两轮日志、普通服务端日志、
阶段 3 测试源码、资源清单、发行 JAR 和 SHA-256。原 `STAGE3_REPORT.md` 保持不变。

## 原版阅读与范围说明

锁定上游仍为 `35f0188665e648d981095b6933cc75b0ca96eca5`（1.20.1）。
对完整源码按文件名和类名检索，**该版本没有 AltarMenu、AltarScreen、ContainerAltar 或 ScreenAltar**。
实际祭坛由 `common/block/BlockAltar.java` 直接右键插取，`TileAltar` / `BloodAltar` 提供单槽与合成状态。
本次阅读了该路径，并参考上游 `ContainerSoulForge.java` 的槽位/Shift 转移、
`ScreenSoulForge.java` 的背景/进度布局。新增的 `AltarMenu` / `AltarScreen` 不是声称迁移了不存在的类。

## 实现

### 服务端菜单和库存

- `common/menu/AltarMenu.java` 继承 AbstractContainerMenu，菜单索引 0 为祭坛共享输入/输出槽；
  1..27 是背包（Inventory 9..35），28..36 是快捷栏（Inventory 0..8）。
- 祭坛使用 `new Slot(container, 0, 26, 42)`；玩家格使用 `addStandardInventorySlots(inventory, 8, 122)`。
- `BloodAltarBlockEntity` 实现 Container 和 MenuProvider，提供 getDisplayName/createMenu；
  完成 size/isEmpty/getItem/setItem/removeItem/removeItemNoUpdate/clearContent/stillValid。
- `getItem` 按原版 Slot 约定返回活引用，保留 `getStoredItem` 防御性副本作为一般读取接口。
  原版 Shift 合并会直接 grow/shrink 活栈，再调用 setChanged。实体比较物品、数量和组件快照，
  仅在库存真的变化时清理旧配方与已付进度；普通 LP 脏标记不会清进度。读取 NBT 时重建此快照，保留重启进度。
- Shift 双向移动遵守物品堆叠上限，支持部分转移，满背包不掉出或删除物品；数字键交换保留血球绑定等组件。
  菜单允许放置任意物品，是否可合成由原生配方判定。Shift 放入多颗钻石等情况仍受阶段 3 的结果最大堆叠检查约束。
- 服务器 createMenu 检查实体仍存在、世界、玩家生命与旁观状态和交互距离。
  菜单有效性使用 Container.stillValidBlockEntity（交互距离属性加默认 4 格缓冲），
  开窗入口仍使用无缓冲方块交互距离。移除或同坐标替换实体使旧菜单失效。
  clicked/quickMoveStack 自身也检查有效性，防止直接调用旧实例修改库存。
- 多个玩家共享同一实体容器；关闭菜单不清空祭坛库存，拆除掉落仍走原有逻辑。

### 右键优先级

普通空手右键**始终优先打开 GUI**，无论祭坛空槽、已有原料或已有产物。
原阶段 3 的空手直接取出被 GUI 点击/Shift 取出替代。
潜行空手右键仍自我献祭，不打开 GUI；持物右键空祭坛仍只放入一件，生存模式扣除一件。
持物右键已有物品时不覆盖。空栈 useItemOn 返回 PASS，让原版只通过 useWithoutItem 开一次菜单。
客户端仅返回预测结果，库存、LP、献祭和开窗实际操作留在服务端。

### 菜单注册与同步

`BloodMagicMenus.MENUS = DeferredRegister.create(Registries.MENU, "bloodmagic")`，注册 ID `bloodmagic:altar`。
本菜单使用 `new MenuType<>(AltarMenu::new, FeatureFlags.DEFAULT_FLAGS)`；服务端 `player.openMenu(provider)`。
客户端工厂使用 `SimpleContainer(1)` 和 `SimpleContainerData(6)`，不查询客户端 BE，
因此不需要额外 BlockPos 开窗数据，也不会依赖区块/BE 包先到。

六个 ContainerData 槽依次为主槽 LP、容量、等级、已支付进度、所需 LP、是否活动。
服务端 get 读取实体状态，set 不写入实体；客户端 set 只更新自己的数组。
`addDataSlots` 仍创建 DataSlot。打开时发送完整库存及六项状态；之后由 broadcastChanges 增量同步。
物品走原版容器内容/槽位包，携带所有 ItemStack 数据组件。GUI 不依赖阶段 2 的追踪区块 LP 包；
该旧包继续服务世界实体状态，二者都读取同一服务端状态。

**大数值细节：**本地 patched JAR 的 ServerPlayer$1.broadcastDataValue 检查
`neoforge:advanced_container_set_data` 通道，并在双方具备通道时发送 NeoForge 内建
AdvancedContainerSetDataPayload。其值使用 VAR_INT，覆盖 32 位 int。
没有通道时才回退到 ClientboundContainerSetDataPacket，其 wire codec 仍读写 signed short。
本模组要求客户端也安装对应 NeoForge/模组；不宣称原版 DataSlot 包已经变成 32 位。
生产代码仅调用公共 addDataSlots，不直接依赖内部扩展 payload 类；集成检查读取该类验证 wire codec。

### 客户端界面

`client/BloodMagicClient` 使用 `@EventBusSubscriber(value = Dist.CLIENT)` 和 RegisterMenuScreensEvent
绑定菜单到 `client/screen/AltarScreen`，公共入口不引用客户端类，专用服务端成功加载。

176×204 背景 PNG 随 JAR 打包，可通过 `scripts/generate-altar-gui.ps1` 重建；
图案由代码绘制，没有伪称使用原版不存在的祭坛 GUI 贴图。
包括标题、等级、共享物品槽、背包/快捷栏、合成进度条/百分比、活动/缺血状态、
LP 储量与容量数字、血量条和完整数值悬停提示；保留基类物品提示、拿取与拖动处理。

使用 extractBackground/extractLabels/extractTooltip 与 GuiGraphicsExtractor，背景 blit 显式指定
RenderPipelines.GUI_TEXTURED 和真实纹理尺寸。基类构造函数接收界面宽高（26.1 的尺寸字段为 final）。
进度条用 long 中间值计算再 clamp，避免高容量溢出；暂未收到同步时分母为零返回空条；
较长 LP 数字缩放至可用宽度。图形代码有迁移注释和中英文翻译。

## 与 1.20.1 的核心差异

| 项目 | 1.20.1 / 旧 Forge 写法 | 本项目 26.1.2 |
| --- | --- | --- |
| 容器基类 / Slot | AbstractContainerMenu；Slot(Container, index, x, y) | 继续可用，无需换掉 Slot；新增标准玩家格辅助方法可直接使用 |
| MenuType 注册 | DeferredRegister + MenuType，常用 IForgeMenuType.create | NeoForge DeferredRegister/DeferredHolder + Registries.MENU；简单工厂仍用 MenuType + FeatureFlags |
| 额外开窗数据 | IForgeMenuType / FriendlyByteBuf / NetworkHooks.openScreen | 需要时用 IMenuTypeExtension.create、RegistryFriendlyByteBuf 与 player.openMenu(provider, writer)；本菜单无需额外数据 |
| Container | 核心库存方法；startOpen/stopOpen 接收 Player | 核心方法保留，open/close 接收 ContainerUser；Iterable、SlotProvider 和扩展接口提供默认实现 |
| 数值同步 | ContainerData/DataSlot，原版包 16 位，常拆高低位或自定义包 | DataSlot 未删除；NeoForge 协商扩展包支持完整 int，原版回退仍 16 位 |
| 点击枚举 | ClickType | ContainerInput；真实服务端点击包还包含状态 ID 与 HashedStack 预测信息 |
| GUI 绑定 | 常见 FMLClientSetupEvent 中 MenuScreens.register | 客户端 RegisterMenuScreensEvent.register |
| GUI 绘制 | GuiGraphics + renderBg/renderLabels/renderTooltip | GuiGraphicsExtractor + extractBackground/extractLabels/extractTooltip；显式渲染管线 |
| 资源 ID | ResourceLocation | Identifier |

## 验证

首先单独编译服务端 Menu，再写并编译 Screen，最后编译测试 source set：
`build-stage4-menu-compile.log`、`build-stage4-screen-compile.log`、`build-stage4-test-compile.log` 均成功。

一键脚本：

```powershell
.\scripts\verify-stage4-gui.ps1 -Restart
```

直接运行单轮：

```powershell
.\build-local.ps1 -GradleTasks @('build','runServer','-Pstage4Smoke=true')
```

测试使用独立 `run/stage4-smoke` 世界，`src/stage4Smoke` 不进入发行 JAR。
保留并适配 126 项阶段 3 检查，加 48 项菜单检查，共 **174 项**。
每轮启动先清除旧 PASS 标记，完成全部断言并保存世界才生成新标记；缺少标记令 Gradle 失败。
Gradle 的 `test NO-SOURCE` 不算单元测试通过，这里的断言由真实专用服务端执行。

菜单检查使用真实 ServerPlayer 和 ServerGamePacketListenerImpl；仅测试连接截获出站数据包、
模拟完成 NeoForge 通道协商，不使用禁用 openMenu 的 FakePlayer。
从真实方块右键入口调用 ServerPlayer.openMenu/initMenu，验证开窗包类型、标题和 ID。
Shift/普通点击通过 ServerboundContainerClickPacket 的实际 codec 往返后交给服务端处理器；
内容/槽位/扩展数值包也经过 codec 往返，送进独立客户端菜单模型。
这验证服务端权威状态、数据包和菜单镜像，不等于真实 socket 联机或图形客户端验收。

新增覆盖：初次打开、空/非空祭坛优先级、37 格映射、Shift 双向转移/重复取出/快捷栏/部分合并、
满背包、不同物品拒绝覆盖、普通拾取、数字键交换、组件保留、双人共享、关闭保留、
拆除与同位置替换失效、远距离/旁观拒绝、潜行献祭、实际 ticker 合成完成后的物品和进度同步。
数值覆盖 70,000/80,000/100,000/200,000 LP 及接近 Integer.MAX_VALUE 的初始/增量同步，
验证无截断、条宽无溢出、客户端镜像和菜单 setData 不能修改实体 LP。
既有世界重启、三次真实数据包 reload 与 LP 守恒断言继续运行。

首次运行 `build-stage4-smoke-initial.log`：

```text
[20:47:34] STAGE4_SMOKE PASS: 174 assertions; WRITE persistence fixture created
BUILD SUCCESSFUL in 1m 5s
```

随后在同一世界执行 `scripts/verify-stage4-gui.ps1` 单轮，实际重启结果
（`build-stage4-smoke-pass1.log`）：

```text
[20:49:02] STAGE4_SMOKE PASS: 174 assertions; RESTART persistence verified
BUILD SUCCESSFUL in 44s
```

两轮退出码均为 0，无 ERROR/FATAL。环境已有 Windows 性能计数器、原版命令歧义和 offline-mode 警告；
连续 reload 有运行落后提示，不影响断言。脚本结果见 `verify-stage4-gui.log`：
254 个 JSON、84 个物品定义、23 套方块资源、GUI 176×204 贴图、双语 GUI 翻译和占位符均通过。
发行 JAR 包含 AltarMenu、AltarScreen、客户端绑定类和 GUI 贴图，未包含测试类。

## 发行产物

- `build/libs/bloodmagic-neoforge-0.1.0-stage4.jar`
- 大小：278,126 字节。
- SHA-256：`E7EBA48C58ED1F832C298F131BA49748128F4A26DFFC0D06794965FFD486A3FB`。
- 工程没有 .git 元数据；修改已保存在共享工作区，没有创建 Git 提交。

## 验证边界和后续

Screen 已按精确 26.1.2 依赖完整编译，GUI 背景 PNG 已直接查看并检查布局与尺寸。
当前无图形客户端测试环境，因此**未验证游戏内实际画面、鼠标命中/拖动、GUI 缩放、真实客户端握手和联机**。
没有把背景贴图预览当作游戏截图，也没有声称已完成 GPU 渲染验收。

下一阶段仍可继续血球功能、灵魂网络及物品渲染。世界内祭坛模型/悬浮物品渲染、自动化能力、
缓冲转移和原版献祭匕首等剩余项沿用阶段 3 的边界。

API 核对依据为本地精确 `minecraft-patched-26.1.2.122.jar` javap 签名/字节码、
同版本 NeoForge sources JAR 和实际编译/运行结果。核对文件提取到 `build/stage4-api`。
在线检索未返回可用正文，因此未借用其他版本网页作为 26.1.2 的确认依据。
