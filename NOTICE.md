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
and loot/tag paths. Stage 5 implements blood-orb soul-network behavior.
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

Stage 4 completion adds a 26.1 extraction/submission block entity renderer,
checked against the exact Minecraft/NeoForge runtime's CampfireRenderer API.
Stage 5 adapts ItemBloodOrb, BloodOrb, IBindable, ItemSigil/ItemSigilBase,
ItemSigilDivination, ItemSigilAir, ItemSigilWater/Lava/Void, SoulNetwork and
BloodAltar charging logic from the same upstream commit. Six sigil textures
and names are from that snapshot. Soul networks now use codec-based SavedData,
bindings use data components, and fluid effects use current vanilla bucket APIs.
See docs/STAGE5_REPORT.md for compatibility boundaries and deliberate differences.

Stage 6 adapts AlchemyArray/TileAlchemyArray/RenderAlchemyArray, ArcaneAshes,
AlchemyTable, SoulForge/TartaricForge recipes, Demon Will/Tartaric Gems,
SoulSnare, RitualDiviner, MasterRitualStone and the Water, GreenGrove and
Crushing rituals from the same pinned snapshot. Selected textures and recipes
retain upstream attribution; the snare status icon reuses the upstream item
art. The machine GUIs and simplified table/forge geometry are new port code.
Persistence, codecs, permission events and rendering use native 26.1 APIs.
See docs/STAGE6_REPORT.md and docs/stage6-content.json for scope and differences.

This is an independent port, not an official release or an endorsement by
the upstream authors.

Stage 7 altar visual fixes restore models/block/blockaltar.obj,
models/block/blockaltar.mtl and textures/models/altar.png from the same
upstream commit 35f0188665e648d981095b6933cc75b0ca96eca5. The original PNG
is stored as textures/block/altar.png for atlas discovery, and the MTL
uses the model's #default texture slot for the 26.1 NeoForge OBJ loader.
The port restores the upstream block shape and item display transform,
with air-space lighting for the suspended item.

The build skeleton and entry-point pattern are adapted from NeoForged's
MDK-26.1.2-ModDevGradle, commit 070b9d2. Its MIT notice is retained in
TEMPLATE_LICENSE.txt. Gradle wrapper scripts and JAR are copied without modification;
the wrapper properties add a SHA-256 check and a longer network timeout.
