"""One-time stage 5 resource/source-set migration from the archived stage 4 tree."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UPSTREAM = ROOT.parent / '.references/bloodmagic-original/WayofTime-BloodMagic-35f0188/src'
RES = ROOT / 'src/main/resources'
IDS = ['divinationsigil', 'seersigil', 'airsigil', 'watersigil', 'lavasigil', 'voidsigil']

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def replace(path, pairs):
    text = path.read_text(encoding='utf-8-sig')
    for old, new in pairs:
        text = text.replace(old, new)
    path.write_text(text, encoding='utf-8')

for item in IDS:
    write(RES / f'assets/bloodmagic/items/{item}.json', {'model': {'type': 'minecraft:model', 'model': f'bloodmagic:item/{item}'}})
    write(RES / f'assets/bloodmagic/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'bloodmagic:item/{item}'}})
    shutil.copy2(UPSTREAM / f'main/resources/assets/bloodmagic/textures/item/{item}.png', RES / f'assets/bloodmagic/textures/item/{item}.png')

manifest = read(ROOT / 'docs/stage3-content.json')
manifest['items'] += [{'id': item, 'kind': 'sigil', 'stackSize': 1} for item in IDS]
write(ROOT / 'docs/stage5-content.json', manifest)
for tag in ['bypasses_armor', 'bypasses_effects', 'bypasses_enchantments', 'bypasses_cooldown', 'no_impact']:
    write(RES / f'data/minecraft/tags/damage_type/{tag}.json', {'replace': False, 'values': ['bloodmagic:sacrifice']})

EN = {
    'tooltip.bloodmagic.orb.desc': 'Right-click to bind and sacrifice health to charge the owner’s soul network.',
    'tooltip.bloodmagic.currentOwner': 'Bound to: %s',
    'tooltip.bloodmagic.unbound': 'Unbound: right-click to bind',
    'tooltip.bloodmagic.orb.stats': 'Tier %s · Capacity %s LP · Charge %s LP/tick',
    'tooltip.bloodmagic.sigil.cost': 'Cost: %s LP per use (health if insufficient)',
    'tooltip.bloodmagic.sigil.divination.desc': 'Read altar or bound soul network LP.',
    'tooltip.bloodmagic.sigil.seer.desc': 'Read altar LP, recipe progress and buffers.',
    'tooltip.bloodmagic.sigil.air.desc': 'Propel the user in the direction they are looking.',
    'tooltip.bloodmagic.sigil.water.desc': 'Place a water source or waterlog a block.',
    'tooltip.bloodmagic.sigil.lava.desc': 'Place a lava source.',
    'tooltip.bloodmagic.sigil.void.desc': 'Remove the targeted fluid source.',
    'message.bloodmagic.altar': 'Altar tier %s · %s / %s LP',
    'message.bloodmagic.altar.progress': 'Progress %s / %s LP · Input %s · Output %s',
    'message.bloodmagic.network': '%s’s soul network: %s LP · Orb tier %s',
    'death.attack.sacrifice': '%1$s sacrificed too much life essence'
}
ZH = dict(zip(EN, [
    '右键绑定并献祭生命，为拥有者的灵魂网络充能。', '绑定者：%s', '未绑定：右键绑定到当前玩家',
    '等级 %s · 容量 %s LP · 充能 %s LP/刻', '每次消耗：%s LP（不足时消耗生命）',
    '查看祭坛或绑定者灵魂网络的 LP。', '查看祭坛的 LP、合成进度和缓冲储量。', '沿视线方向推动使用者。',
    '放置水源，或为方块含水。', '放置熔岩源。', '移除所指向的流体源。',
    '祭坛等级 %s · %s / %s LP', '进度 %s / %s LP · 输入 %s · 输出 %s',
    '%s 的灵魂网络：%s LP · 血球等级 %s', '%1$s 献祭了过多生命源质'
]))
for locale, entries in [('en_us', EN), ('zh_cn', ZH)]:
    path = RES / f'assets/bloodmagic/lang/{locale}.json'
    lang = read(path)
    suffix = 'packs/translations/' if locale == 'zh_cn' else ''
    original_path = UPSTREAM / f'main/resources/{suffix}assets/bloodmagic/lang/{locale}.json'
    if not original_path.exists():
        original_path = UPSTREAM / f'generated/resources/assets/bloodmagic/lang/{locale}.json'
    original = read(original_path)
    for item in IDS:
        lang[f'item.bloodmagic.{item}'] = original[f'item.bloodmagic.{item}']
    lang.update(entries)
    lang.pop('tooltip.bloodmagic.stage2.orb', None)
    write(path, lang)

replace(ROOT / 'scripts/verify-stage1-resources.ps1', [('docs/stage3-content.json','docs/stage5-content.json'), ('tooltip.bloodmagic.stage2.orb','tooltip.bloodmagic.orb.desc')])
replace(ROOT / 'gradle.properties', [('0.1.0-stage4','0.1.0-stage5')])
replace(ROOT / 'src/main/java/wayoftime/bloodmagic/BloodMagic.java', [('stage 4','stage 5')])
replace(ROOT / 'build.gradle', [('stage4','stage5'), ('stage 4','stage 5'), ('Stage 4','Stage 5')])
destination = ROOT / 'src/stage5Smoke'
if not destination.exists():
    shutil.copytree(ROOT / 'src/stage4Smoke', destination)
    for path in destination.rglob('*.java'):
        replace(path, [('Stage4','Stage5'), ('STAGE4','STAGE5'), ('stage4','stage5')])
    (destination / 'java/wayoftime/bloodmagic/test/Stage4SmokeChecks.java').rename(destination / 'java/wayoftime/bloodmagic/test/Stage5SmokeChecks.java')
run = ROOT / 'run/stage5-smoke'
run.mkdir(exist_ok=True)
for name in ['eula.txt', 'server.properties']:
    if not (run / name).exists() and (ROOT / f'run/stage4-smoke/{name}').exists():
        shutil.copy2(ROOT / f'run/stage4-smoke/{name}', run / name)
print('Stage 5 resources and test harness prepared')
