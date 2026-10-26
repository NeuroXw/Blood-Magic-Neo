# 阶段 1：原版文件清单（注册系统与基础内容）

已核对并缓存 Blood Magic 上游 `1.20.1` 分支提交
`35f0188665e648d981095b6933cc75b0ca96eca5`。
本地参考目录：`../../.references/bloodmagic-original/WayofTime-BloodMagic-35f0188/`。
阶段 0 尚未迁移下列实现。以下 Java 路径均相对于
`src/main/java/wayoftime/bloodmagic/`。

## 第一批：普通物品、简单方块、创造模式标签页

| 原版文件 | 用途 |
| --- | --- |
| `BloodMagic.java` | 注册总线绑定、初始化顺序、命名空间 |
| `common/item/BloodMagicItems.java` | 保留原注册 ID，筛选石板、材料等基础物品 |
| `common/item/ItemBase.java` | 基础物品属性、堆叠数、说明文字 |
| `common/block/BloodMagicBlocks.java` | 基础方块及对应方块物品 ID、属性 |
| `common/registries/BloodMagicCreativeTabs.java` | 标签页 ID、名称、物品排序 |
| `common/block/BlockBloodRune.java` | 若第一批包含符文方块，检查符文属性 |
| `block/enums/BloodRuneType.java` | 符文类型枚举，与上一项配套 |
| `altar/IBloodRune.java` | 符文接口，与上一项配套 |

注意：原版石板直接注册为 `ItemBase`，不存在需要迁移的独立 `ItemSlate.java`。
原注册 ID 包括 `blankslate`、`reinforcedslate`、`infusedslate`、`demonslate`、
`etherealslate`。原 `core/RegistrarBloodMagic.java` 基本为已注释的旧代码，
不能把它误当成 1.20.1 的主要注册入口。

## 同步准备的资源

- `src/generated/resources/assets/bloodmagic/lang/en_us.json`：英文语言键。
- `src/main/resources/packs/translations/assets/bloodmagic/lang/`：中文等翻译。
- `src/main/resources/assets/bloodmagic/textures/`：所选物品和方块贴图。
- `src/main/resources/assets/bloodmagic/models/` 及 `blockstates/`：现有模型和状态。
- `src/generated/resources/` 中所选内容的模型、状态、标签、掉落表和配方。
- `common/data/GeneratorItemModels.java`、`GeneratorBlockStates.java`、
  `GeneratorLanguage.java`、`GeneratorItemTags.java`、`GeneratorBlockTags.java`、
  `GeneratorLootTable.java`：作为原数据生成规则参考。
- 项目根目录 `LICENSE.md`、`build.gradle`、`gradle.properties` 及
  `src/main/resources/META-INF/mods.toml`：许可、原依赖和元数据参考。

旧 JSON 路径和格式需要按 26.1.2 实际规范检查，不应直接全量复制。

## 若阶段 1 包含自定义注册表

再准备 `common/registration/` 全目录，特别是：

- `WrappedDeferredRegister.java`、`WrappedForgeDeferredRegister.java`、
  `WrappedRegistryObject.java`、`INamedEntry.java`。
- `impl/BloodOrbDeferredRegister.java`、`impl/BloodOrbRegistryObject.java`。
- `common/item/BloodOrb.java`、`core/registry/OrbRegistry.java`。

血球绑定、灵魂网络、祭坛、流体、容器、网络包和实体各有独立依赖。
建议第一批验收仅覆盖基础注册内容；扩展这些子系统前另做依赖清单。
