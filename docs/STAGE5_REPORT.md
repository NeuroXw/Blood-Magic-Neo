# 阶段 5 启动：血球、基础印记与灵魂网络

日期：2026-10-25（Asia/Shanghai）。固定目标 Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。
阶段 4 悬浮物品渲染已完成、编译通过，174 项既有断言重启通过，快照在 `docs/archive/stage4`。
初始阶段 4 报告以 `STAGE4_REPORT.original.md` 保留；更新后的报告记录渲染接口、保护与验证边界。

## 原版阅读与范围

上游锁定 `35f0188665e648d981095b6933cc75b0ca96eca5`（1.20.1）。阅读：
`ItemBloodOrb`、`BloodOrb`、`IBindable`、`ItemBindableBase`、`ItemSigil`、`ItemSigilBase`、
`ItemSigilDivination`、`ItemSigilAir`、`ItemSigilWater/Lava/Void`、`SoulNetwork`、`BloodAltar`、
`BloodMagicItems`、`ConfigManager` 及对应资源。

本次“基础印记”具体为占卜、先知、空气、水、熔岩、虚空六种。高级切换式印记、持有/传送印记、
炼金阵、炼金台、流体容器能力不属于本次实现。原版六种印记的制作均属于炼金阵配方；
没有将这些 JSON 错装为普通配方，也没有擅自发明替代制作方式。物品可由创造物品栏/命令获得。

阶段 1 的血球无功能占位及阶段 2 的仅绑定实现，已替换为实际功能类；对应待办和旧占位说明已移除。
注册表中原本没有印记空类，现新增真实实现。`ItemBase` 仍用于普通材料，是有效基础物品类，不应删除。

## 五级血球

新增 `BloodOrb` 不可变定义、`BloodMagicBloodOrbs` 同步自定义注册表 `bloodmagic:blood_orb`。
五个物品保持原 ID 和堆叠上限 1，仍兼容已有 `BLOOD_ORB_TIER` 与 `BINDING` 数据组件。

| 血球 | 等级 | 基础网络容量 LP | 祭坛充能 LP/刻 |
| --- | ---: | ---: | ---: |
| weakbloodorb | 1 | 5,000 | 2 |
| apprenticebloodorb | 2 | 25,000 | 5 |
| magicianbloodorb | 3 | 150,000 | 15 |
| masterbloodorb | 4 | 1,000,000 | 25 |
| archmagebloodorb | 5 | 10,000,000 | 50 |

右键在服务端首次绑定到 UUID；名称只用于显示。已有绑定不会被借用者覆盖。主手/副手同样处理。
手持血球消耗使用者生命，向绑定者网络补充最多 200 LP；不是从网络扣 LP。仅拥有者使用自己的血球
记录网络血球等级。等级保留已使用过的最高值，低级血球不能降低余额或最高等级。
若命令删除等级组件，回退到注册物品的固有等级，避免渲染提示或充能时访问无效等级。

返还物使用 26.1 `getCraftingRemainder(ItemInstance)`，返回 `ItemStackTemplate` 并保留组件，
覆盖可变 ItemStack 和不可变模板两种原生输入。不使用已删除的旧 Forge 返还物方法。

## 灵魂网络与扣费

`SoulNetwork` 为不可变 UUID/LP/最高血球等级快照；`SoulNetworks` 是实际 `SavedData`，以 Codec 保存。
从服务器主世界的 SavedDataStorage 获取，同一世界的所有维度共享同一实例；不缓存 Player/Level，
不依赖拥有者在线，不用静态 Map 跨服务器泄露数据。所有改动经过统一入口并 setDirty。

fill 返回实际接收量，容量/负值检查使用 long 中间值；consume 全额成功或完全不扣。
网络最高可保存 Integer.MAX_VALUE，数据 codec 拒绝负 LP 或非法等级。
绑定仍使用持久化和网络 codec 均已注册的数据组件，未新增允许客户端直接写 LP 的数据包。

印记首先消费绑定者网络 LP；不足时按原版规则保留该余额，改消耗**使用者完整费用**的生命值。
每 100 LP 对应 1 点生命，向上取整（50 LP 消耗 1 点生命）。创造模式不扣 LP/生命。
自定义 `bloodmagic:sacrifice` 伤害类型及 tags 绕过护甲、效果、附魔减伤和受击冷却，无击退。
服务端 `hurtServer` 实际拒绝伤害时，不授予 LP/效果；支付后世界操作返回失败或抛出异常时退回本次支付。

明确行为调整：满容量血球不扣血；仅补充剩余容量；生命/吸收不足以安全支付时拒绝使用，避免致死。
原版血球可能在满容量时继续伤害玩家，本移植有意收紧；印记也不会照搬原版“扣费失败仍放置流体”路径。
假玩家、旁观者、死亡玩家不能绑定/激活物品。普通祭坛献祭保持此前实现。

## 祭坛充能

真实 ticker 中先匹配现有配方；没有配方且槽内为已绑定血球时，将主槽 LP 转入对应 UUID 网络。
未绑定、槽空、储量空、网络满、结构无法确认时不转移；血球和组件不消耗。
保持上游任意等级祭坛可为任意等级血球充能的行为，不新增等级门槛。

速度符文作用于基础 fillRate；球容量符文按每强度 +2% 增加该祭坛的网络充能上限。
只扣网络 fill 实际接受量，支持最后 1 LP 容量与离线拥有者；更换低容量血球不截断已有余额。
充能时不冒充配方进度；GUI 的祭坛 LP 由既有 ContainerData 同步，血球由槽位和 BE 包同步。

## 基础印记

| 印记 | 每次 LP | 当前行为 |
| --- | ---: | --- |
| 占卜 | 0 | 显示绑定者网络 LP/等级，或祭坛等级/储量/容量 |
| 先知 | 0 | 同上，并显示合成进度和输入/输出缓冲值 |
| 空气 | 50 | 沿视线设置 1.7 速度，清 fallDistance，服务端向使用者发送速度包 |
| 水 | 100 | 放置水源或为支持的方块含水 |
| 熔岩 | 1,000 | 放置熔岩源 |
| 虚空 | 50 | 移除流体源或抽干含水方块，保留方块主体 |

读数印记通过 onItemUseFirst 优先于祭坛插入处理，不会把读数印记塞入祭坛。
流体逻辑检查已加载区块、交互距离、世界边界、世界/玩家使用权限和目标可放置性，先支付再执行。
放置复用原生 BucketItem.emptyContents，含水和环境蒸发规则由 26.1 原生 API 处理。
自定义储罐能力、HUD 编辑界面、配置项迁移和高级印记仍待后续。当前费用为原版默认值。
此次没有实测下界蒸发或第三方含水方块，只确认原生委托调用编译正确。

六个名称/PNG 取自同一上游快照，新增 26.1 items 定义与模型。中英文说明、费用、绑定者和读数均完整。
动态提示使用客户端 ItemTooltipEvent，不调用废弃 appendHoverText，也不从客户端读取服务端网络。

## 26.1 API 核对

| 内容 | 当前实现 |
| --- | --- |
| BER | 两个泛型；createRenderState/extractRenderState/submit；CameraRenderState 在 state.level 包 |
| 物品模型 | ItemModelResolver.updateForTopItem → ItemStackRenderState.submit |
| SavedData | Identifier + Codec 的 SavedDataType；SavedDataStorage.computeIfAbsent；不覆写旧 save(CompoundTag) |
| 物品使用结果 | InteractionResult，无 InteractionResultHolder |
| 返还物 | ItemInstance → ItemStackTemplate，组件保留 |
| 玩家消息 | sendOverlayMessage / sendSystemMessage，旧 displayClientMessage 不存在 |
| 伤害 | 服务端 hurtServer，注册 damage_type JSON + tags |
| 速度包 | ClientboundSetEntityMotionPacket 为 record，id()/movement() |
| 流体操作 | BucketPickup.pickupBlock 接收 LivingEntity；BucketItem.emptyContents 提供 ItemStack 扩展重载 |

依据为精确本地 patched JAR 的 javap 签名/字节码、NeoForge 同版本 sources JAR，以及实际编译和运行。
核对记录在 `build/stage4-render-api`、`build/stage5-api`。在线检索没有返回可用正文，未据此推断版本兼容性。

## 验证

```powershell
.\scripts\verify-stage5-orbs.ps1 -Restart
# 单次
.\build-local.ps1 -GradleTasks @('build','runServer','-Pstage5Smoke=true')
```

使用独立 `run/stage5-smoke`，源码在 `src/stage5Smoke`，不进入发行 JAR。
保留 174 项既有断言，增加 128 项，共 **302 项**。
唯一基础夹具适配：旧持久化夹具现在装有可工作的血球，因此预先填满它的对应网络，仍验证原有闲置储存；
充能行为由独立新夹具验证，没有删除或跳过旧断言。

测试以真实 ServerPlayer 与 ServerGamePacketListenerImpl 配合模拟连接运行。ServerboundUseItemPacket
和 ServerboundUseItemOnPacket 均经真实 codec 往返后进入服务端处理器；物品组件和空气速度包也验证 wire codec。
覆盖五级绑定、自己/借用者、副手、创造/旁观/假玩家、满额与 1 LP 空间、伤害拒绝、低生命、完整 LP/生命扣费、
失败效果退款、移除等级组件、返还物组件、离线/跨维度网络、整数边界、保存 codec、实际保存重启，
水/熔岩/虚空/含水、冒险权限、空目标和真正使用包避免双扣费。结构中的真实速度/球符文也接入充能测试。
自然世界 ticker 在三次真实数据包 reload 期间继续充能，检查祭坛 + 网络 LP 守恒及物品保留。

首轮 `build-stage5-smoke-initial.log` 的 297 项运行期断言通过，但 Gradle 收尾仍匹配旧阶段 4 标记导致构建失败；
已修正结果检查，不将这轮列为完整成功。扩展测试后：

- `build-stage5-smoke-pass1.log`：302 项，RESTART persistence verified，BUILD SUCCESSFUL。
- 最终一键验证日志和发行哈希见本文下方的交付记录。

Gradle `test NO-SOURCE` 不等于测试通过；实际断言由专用服务端执行，旧 PASS 在每轮运行前删除，
缺少新 PASS 会令 Gradle 失败。没有伪称真实 socket 握手、图形客户端或 GPU 验收。

## 剩余边界

阶段 4 渲染已编译、服务端隔离已验证；仍须图形客户端确认物品高度/光照/模型显示和 GUI 操作。
完整阶段 5 的高级印记、HUD、网络事件/ticket 历史、配置界面、炼金阵及制作链留待后续。
祭坛仍用此前方块模型；本次完成的是悬浮物品渲染。旧 1.20.1 世界 NBT 导入未实现。
工程没有 Git 元数据，修改保存在共享工作区，没有创建 Git 提交。

## 交付记录

- `build-stage5-smoke-pass1.log`：2026-10-25 21:12:15，302 项断言、世界/网络重启通过，构建成功（45 秒）。
- `build-stage5-verify-pass1.log`：2026-10-25 21:14:15，302 项断言、世界/网络重启再次通过，构建成功（53 秒）。
- `verify-stage5-orbs.log`：一键验证成功；272 个 JSON、90 个物品定义、23 套方块资源、GUI 贴图尺寸、双语占位符和发行内容通过。
- `build-stage5-final.log`：最终构建成功；两个最终服务端日志没有 ERROR/FATAL，存在既有 offline-mode/性能计数器/命令歧义和 reload 运行落后提示。
- 发行包：`build/libs/bloodmagic-neoforge-0.1.0-stage5.jar`，319,548 字节。
- SHA-256：`2B6C4E5533630CDDA5CFCB0163B9CB4CDBFA13EB53E9126DCAE7601A24FF2FD5`。
- 实际网络存档：`run/stage5-smoke/world/dimensions/minecraft/overworld/data/bloodmagic/soul_networks.dat`。
- 阶段 4 归档共 413 个受校验文件，SHA256SUMS 已逐项复核；阶段 4 JAR 哈希仍为 `1EE58D8E5D17E6B4359472D2C62201E5D56980DBC6FBD88FA338AA4D46205CAA`。

旧 `scripts/verify-stage4-gui.ps1` 现在转到当前阶段 5 验证，避免在当前工程运行历史脚本而误开普通服务端。
直接传入旧 `-Pstage4Smoke` 会给出明确迁移提示；需要 174 项旧版验收时使用归档源码。
