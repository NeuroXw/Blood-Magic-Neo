# Stage 0 API and build sources

Retrieved on 2026-10-25 from the primary repositories. The official 26.1.2 MDK
archive is cached outside this project in ../.references/mdk.

## Official template

https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle/tree/070b9d2

- `build.gradle`: ModDevGradle 2.0.148, Java toolchain 25, `neoForge.runs`, and
  `neoForge.mods` source-set binding.
- `settings.gradle`: plugin portal and Foojay resolver 1.0.0.
- `gradle.properties`: Minecraft 26.1.2 and NeoForge 26.1.2.122.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.2.1.
- `src/main/java/com/example/examplemod/ExampleMod.java`: NeoForge `@Mod`,
  constructor injection of `IEventBus`, `FMLCommonSetupEvent`, and SLF4J logging.
- `src/main/templates/META-INF/neoforge.mods.toml`: current metadata structure
  and required dependency declarations. We do not add old loader-version fields.

## Deliberate skeleton changes

- Removed all sample blocks, items, creative tabs, configuration and data generation.
- Kept only client/server runs and common lifecycle logging.
- Put metadata directly in `src/main/resources/META-INF/neoforge.mods.toml` as
  requested; Gradle `processResources` expands version and identity properties.
- Restricted Minecraft to `[26.1.2]` and NeoForge to `[26.1.2.122,26.1.3)`.
- No ForgeGradle, mappings configuration, reobfuscation, or NeoVitae dependency.
- Wrapper timeout increased to 60 seconds; Gradle 9.2.1 SHA-256 is pinned to
  `72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f`,
  verified against the digest in the official Gradle GitHub release API:
  https://api.github.com/repos/gradle/gradle-distributions/releases/tags/v9.2.1

## Blood Magic reference for stage 1

https://github.com/WayofTime/BloodMagic/tree/35f0188665e648d981095b6933cc75b0ca96eca5

This is the upstream `1.20.1` branch snapshot inspected for the next-stage file
inventory. It is not added as a runtime dependency.

## Official ModDevGradle binary pipeline

https://github.com/neoforged/ModDevGradle#disabling-decompilation-and-recompilation

The official README documents `neoForge.enable { version = ...;
disableRecompilation = true }` (available since MDG 2.0.124). Stage 0 uses this
supported pipeline to avoid full Minecraft source generation on the local 8 GB
machine. Set `-Pgenerate_minecraft_sources=true` to restore the default source
generation pipeline for later API inspection. This does not skip mod compilation.

## Asset download helper toolchain

Inspected the published `net.neoforged:moddev-gradle:2.0.148` JAR with `javap`.
`net.neoforged.nfrtgradle.DownloadAssets` extends `NeoFormRuntimeTask`, whose
public `getJavaExecutable()` returns `Property<String>`. Its default toolchain
is Java 21. The skeleton sets this property from the Java 25 toolchain launcher,
so preparing run configurations does not require an additional JDK 21 download.
The mod and Minecraft runtime also use Java 25.
