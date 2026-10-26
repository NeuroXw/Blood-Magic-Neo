# 阶段 2：数据与同步示例

目标固定为 Minecraft 26.1.2 / NeoForge 26.1.2.122 / Java 25。
示例中的写操作在服务端主线程执行。实现位于 src/main/java/wayoftime/bloodmagic。

## 血球等级与绑定

```java
ItemStack orb = new ItemStack(BloodMagicItems.WEAK_BLOOD_ORB.get());

// 五种血球的默认等级分别为 1..5；没有绑定组件表示尚未绑定。
int tier = BloodOrbData.getTier(orb); // 1
BloodOrbData.setTier(orb, 3);
BloodOrbData.bind(orb, player.getUUID(), player.getGameProfile().name());
Optional<Binding> binding = BloodOrbData.getBinding(orb);
binding.ifPresent(value -> {
    UUID ownerId = value.ownerId();
    String displayName = value.ownerName();
});
BloodOrbData.unbind(orb);
```

对应的原生组件调用：

```java
orb.set(BloodMagicDataComponents.BLOOD_ORB_TIER.get(), 3);
int tier = orb.getOrDefault(BloodMagicDataComponents.BLOOD_ORB_TIER.get(), 0);

orb.set(BloodMagicDataComponents.BINDING.get(),
        new Binding(player.getUUID(), player.getGameProfile().name()));
Binding owner = orb.get(BloodMagicDataComponents.BINDING.get()); // 允许 null，表示未绑定
orb.remove(BloodMagicDataComponents.BINDING.get());
```

优先使用 BloodOrbData 写入：它检查物品类型和等级范围。
等级和 Binding 都是不可变值；修改时用 set/remove 替换组件，不能原地修改已放入组件的数据。
UUID 为身份依据，名称仅为显示快照，不承担权限判断。
ItemBloodOrb.use 已接入首次右键绑定，客户端不直接修改组件，已有绑定不会被右键覆盖。

注册 ID：bloodmagic:blood_orb_tier 和 bloodmagic:binding。
两者均配置 persistent(Codec) 和 networkSynchronized(StreamCodec)，由 ItemStack 的保存及
物品同步路径处理，无须另外发送“绑定包”。其中 binding 的保存字段为 id（UUID）和 name。
设置等级目前只改变数据；灵魂网络、实际血球容量及升级规则仍待后续阶段实现。

上游 1.20.1 的拥有者存放于 ItemStack NBT 的 binding 子项，但血球等级来自 BloodOrb 对象，
并非所有血球数据原本都在 ItemStack NBT 中。本次为五种血球增加了默认等级组件。
本阶段没有旧版 NBT 自动升级器，不声称可以直接导入 1.20.1 世界。

## 祭坛存储与序列化

```java
if (level.getBlockEntity(pos) instanceof BloodAltarBlockEntity altar) {
    int accepted = altar.fillMainTank(500);
    int drained = altar.drainMainTank(100);
    AltarState snapshot = altar.getAltarState();
    altar.setStoredItem(orb); // 保存防御性副本，并触发物品同步
    ItemStack copy = altar.getStoredItem(); // 修改返回值后必须再次 setStoredItem
}
```

AltarState 保存主 LP、主容量、输入/输出缓冲 LP、缓冲容量、祭坛等级、
已消耗的配方 LP、配方总需求 LP、活动标记。progress 的单位是 LP，不是 tick；
requiredBlood 表示整次操作的总需求，未来配方逻辑需要处理批量物品乘数。
默认主容量 10,000 LP、缓冲容量 1,000 LP、等级 1；负数和越界值在入口归一化。
空配方的 progress 为 0、active 为 false。数据变更调用 setChanged，并标记待同步。
测试/未来配方可用 setAltarState(new AltarState(...)) 原子替换完整状态。

**26.1.2 的覆盖签名如下，不能直接使用 1.21.1 的双参数 NBT 示例：**

```java
@Override
protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    altarState = AltarState.load(input.childOrEmpty("altar"));
    storedItem = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
}

@Override
protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    altarState.save(output.child("altar"));
    output.store("item", ItemStack.OPTIONAL_CODEC, storedItem);
}
```

ValueInput/ValueOutput 的编解码上下文携带注册表信息。ValueInput.lookup() 可取得
HolderLookup.Provider；只有手工创建 TagValueInput/TagValueOutput 或调用下列入口时
才显式传递 provider：

```java
HolderLookup.Provider registries = serverLevel.registryAccess();
CompoundTag saved = altar.saveWithFullMetadata(registries);
BlockEntity restored = BlockEntity.loadStatic(pos, blockState, saved, registries);

var problems = new ProblemReporter.Collector();
ValueInput input = TagValueInput.create(problems, registries, saved);
altar.loadWithComponents(input);
// 在测试中检查 problems.isEmpty()；不要默默忽略编解码失败。
```

getUpdateTag(HolderLookup.Provider) 返回 saveWithoutMetadata(provider)。
getUpdatePacket() 返回 ClientboundBlockEntityDataPacket.create(this)，要求实体已关联世界。
NeoForge 默认的 handleUpdateTag(ValueInput) / onDataPacket(Connection, ValueInput)
会调用 loadWithComponents，无须重写旧版 CompoundTag 接收函数。

首次跟踪区块用完整更新标签取得状态和单槽物品，后续物品变化也走原版方块实体包。
基础存储尚未接入菜单、漏斗或管道；方块移除会掉落保存的物品。

## 进度网络

AltarProgressPayload 是 CustomPacketPayload record，由维度 ResourceKey、BlockPos、AltarState 构成。
TYPE 的 ID 为 bloodmagic:altar_progress，协议版本为 "1"。

```java
// 在模组事件总线上监听 RegisterPayloadHandlersEvent：
event.registrar("1").executesOn(HandlerThread.MAIN).playToClient(
        AltarProgressPayload.TYPE,
        AltarProgressPayload.STREAM_CODEC,
        BloodMagicNetwork::handleAltarProgress); // 实现中的私有方法

// 服务端已发生变更时，向正在跟踪所在区块的玩家发送：
PacketDistributor.sendToPlayersTrackingChunk(serverLevel, ChunkPos.containing(pos),
        new AltarProgressPayload(serverLevel.dimension(), pos, altar.getAltarState()));
```

事件的实际包名是 net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent。
没有 SimpleChannel、递增消息编号、NetworkEvent.Context#setPacketHandled。
StreamCodec 描述数据流格式，playToClient 只注册服务端到客户端方向。
处理器显式在游戏主线程运行；如果改用 HandlerThread.NETWORK，世界访问必须经
IPayloadContext.enqueueWork 调度到主线程。

客户端处理器通过 context.player().level() 获取当前世界，不引用 Minecraft 客户端专用类，
因此注册类可以在专用服务端加载。它检查逻辑端、维度、区块已加载以及祭坛实体类型。
getChunkNow 不会为了一个网络包加载新区块。卸载/已移除实体的包直接忽略，重新跟踪时由
完整标签恢复；正常变更按需发送，最多每 5 tick 一次，静止祭坛不会持续刷包。

复杂玩法在 tickGameplayStub() 中保持 TODO，不会虚构配方进度或扣除 LP。
客户端实际连接、换维度及重新跟踪的端到端操作还需图形客户端验证。

## 重放服务端集成检查

正常启动命令（控制台输入 stop 结束）：

```powershell
.\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('build','runServer')
```

可选集成检查使用单独的 run/stage2-smoke 世界，自动保存并停服。首次运行写入
测试祭坛，第二次运行验证世界重启后保留的字段和血球组件：

```powershell
$env:ORG_GRADLE_PROJECT_stage2Smoke = 'true'
try {
    .\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('build','runServer')
    .\build-local.ps1 -ProxyHost 127.0.0.1 -GradleTasks @('build','runServer')
} finally {
    Remove-Item Env:ORG_GRADLE_PROJECT_stage2Smoke
}
```

src/stage2Smoke 仅在该开关启用时加入开发模组；它不进入发行 JAR，也不在普通 runServer
中自动运行。Gradle 会清理前次通过标记，并要求本次生成 STAGE2_SMOKE PASS，防止服务端
线程崩溃但进程退出码为 0 造成误报。具体检查与执行日志见 STAGE2_REPORT.md。
