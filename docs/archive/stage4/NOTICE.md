# Attribution

Blood Magic is by WayofTime and its contributors.
Upstream: https://github.com/WayofTime/BloodMagic/tree/1.20.1
The requested license for this independent port is CC-BY-4.0:
https://creativecommons.org/licenses/by/4.0/

Stage 1 adapts the registry declarations, ItemBase, BlockBloodRune,
BloodRuneType, IBloodRune and the main creative tab from upstream commit
35f0188665e648d981095b6933cc75b0ca96eca5 (1.20.1 branch). Selected item/block
models, textures and English/Chinese translations are from the same snapshot.
The upstream authors and translation contributors retain credit.
The code targets NeoForge 26.1.2.122; resources use current item definitions
and loot/tag paths. Blood-orb soul-network behavior remains pending.
See docs/STAGE1_REPORT.md and docs/stage1-content.json for scope.

Stage 3 additionally adapts TileAltar, BloodAltar, AltarTier, AltarUtil,
AltarComponent, AltarUpgrade, RecipeBloodAltar, BloodAltarRecipeSerializer,
BloodAltarRecipeProvider and BloodMagicCorePlugin from the same commit.
Ten altar recipe resources and the dungeon_metal block model/texture are
adapted from that snapshot. The port uses native 26.1 Recipe codecs and
ItemStackTemplate, tag-based structural components and explicit health sacrifice.
See docs/STAGE3_REPORT.md for gameplay differences and remaining scope.

Stage 4 adds AltarMenu/AltarScreen and a code-drawn altar GUI texture.
The pinned upstream has no altar menu or screen; its ContainerSoulForge,
ScreenSoulForge and BlockAltar were inspected as reference patterns.
The new menu uses the 26.1 Container/Slot APIs and NeoForge menu synchronization.
See docs/STAGE4_REPORT.md; the texture is reproducible with scripts/generate-altar-gui.ps1.

This is an independent port, not an official release or an endorsement by
the upstream authors.

The build skeleton and entry-point pattern are adapted from NeoForged's
MDK-26.1.2-ModDevGradle, commit 070b9d2. Its MIT notice is retained in
TEMPLATE_LICENSE.txt. Gradle wrapper scripts and JAR are copied without modification;
the wrapper properties add a SHA-256 check and a longer network timeout.
