"""Reproducible port of stage 6 resources from the pinned, locally cached upstream."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UP = ROOT.parent / '.references/bloodmagic-original/WayofTime-BloodMagic-35f0188/src'
RES = ROOT / 'src/main/resources'

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def copy_resource(rel):
    source = UP / 'main/resources' / rel
    if not source.exists(): source = UP / 'generated/resources' / rel
    dest = RES / rel
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, dest)

def item(name):
    copy_resource(f'assets/bloodmagic/textures/item/{name}.png')
    write(RES / f'assets/bloodmagic/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'bloodmagic:item/{name}'}})
    write(RES / f'assets/bloodmagic/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'bloodmagic:item/{name}'}})

# Exact tag migration: common 26.1 tags, or explicit vanilla alternatives where upstream tags no longer exist.
TAG_MAP = {'forge:stone': 'minecraft:stone', 'forge:glass': 'minecraft:glass', 'forge:gunpowder': 'minecraft:gunpowder',
           'forge:dyes/white': 'minecraft:white_dye', 'forge:cobblestone': '#c:cobblestones', 'forge:feathers': 'minecraft:feather',
           'forge:string': 'minecraft:string', 'forge:rods/wooden': 'minecraft:stick', 'forge:obsidian': 'minecraft:obsidian'}
def ingredient(old):
    if isinstance(old, list): return [ingredient(v) for v in old]
    if old.get('type') == 'bloodmagic:bloodorb':
        return '#bloodmagic:blood_orbs/tier_' + str(old['orb_tier'])
    if 'item' in old: return old['item']
    tag = old['tag']
    return TAG_MAP.get(tag, '#' + tag.replace('forge:', 'c:'))

def recipe(rel):
    r = read(UP / f'generated/resources/data/bloodmagic/recipes/{rel}.json')
    for key in ['baseinput', 'addedinput']:
        if key in r: r[key] = ingredient(r[key])
    if 'input' in r: r['input'] = ingredient(r['input']) if isinstance(r['input'], dict) else [ingredient(x) for x in r['input']]
    if 'key' in r: r['key'] = {k: ingredient(v) for k, v in r['key'].items()}
    if 'ingredients' in r: r['ingredients'] = [ingredient(x) for x in r['ingredients']]
    if 'input0' in r: r['input'] = [ingredient(r.pop(f'input{i}')) for i in range(4) if f'input{i}' in r]
    for key in ['output', 'result']:
        if key in r:
            r[key]['id'] = r[key].pop('item')
            r[key].pop('nbt', None)  # Arcane ashes Damage:0 is its native default.
    write(RES / f'data/bloodmagic/recipe/{rel}.json', r)

def block_item(name):
    write(RES / f'assets/bloodmagic/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'bloodmagic:item/{name}'}})
    write(RES / f'assets/bloodmagic/models/item/{name}.json', {'parent': f'bloodmagic:block/{name}'})

def loot(name, condition=None):
    entry = {'type': 'minecraft:item', 'name': 'bloodmagic:' + name}
    conditions = [{'condition': 'minecraft:survives_explosion'}]
    if condition: conditions.append(condition)
    write(RES / f'data/bloodmagic/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry], 'conditions': conditions}]})

def cube_element(start, end, texture):
    return {'from': start, 'to': end, 'faces': {face: {'texture': texture} for face in ['up', 'down', 'north', 'south', 'east', 'west']}}

def phase1():
    item('arcaneashes')
    for name in ['divinationsigil', 'watersigil', 'lavasigil', 'voidsigil', 'airsigil', 'seersigil']:
        recipe('array/' + name)
        tex = 'sightsigil' if name == 'seersigil' else name
        copy_resource(f'assets/bloodmagic/textures/models/alchemyarrays/{tex}.png')
    for name in ['arcane_ash', 'reagent_water', 'reagent_lava', 'reagent_void', 'reagent_air', 'reagent_sight']: recipe('alchemytable/' + name)
    recipe('alchemy_table')
    # Required to enter the existing altar/slate/orb chain from a fresh survival world.
    recipe('blood_altar'); recipe('blood_rune_blank')
    block_item('alchemytable')
    write(RES / 'assets/bloodmagic/models/block/alchemytable.json', {'parent': 'minecraft:block/block',
        'textures': {'top': 'minecraft:block/smooth_stone', 'leg': 'minecraft:block/dark_oak_planks', 'particle': 'minecraft:block/smooth_stone'},
        'elements': [cube_element([0, 12, 0], [16, 16, 16], '#top'), cube_element([2, 0, 2], [5, 12, 5], '#leg'), cube_element([11, 0, 11], [14, 12, 14], '#leg')]})
    write(RES / 'assets/bloodmagic/blockstates/alchemytable.json', {'variants': {'': {'model': 'bloodmagic:block/alchemytable'}}})
    write(RES / 'assets/bloodmagic/models/block/alchemyarray.json', {'textures': {'particle': 'minecraft:block/white_concrete'}, 'elements': []})
    write(RES / 'assets/bloodmagic/blockstates/alchemyarray.json', {'variants': {'': {'model': 'bloodmagic:block/alchemyarray'}}})
    loot('alchemytable', {'condition': 'minecraft:block_state_property', 'block': 'bloodmagic:alchemytable', 'properties': {'part': 'foot'}})
    names = {'item.bloodmagic.arcaneashes': ('Arcane Ashes', '奥术灰烬'), 'block.bloodmagic.alchemyarray': ('Alchemy Array', '炼金阵'),
             'block.bloodmagic.alchemytable': ('Alchemy Table', '炼金台'), 'gui.bloodmagic.machine.lp': ('Network: %s LP', '网络：%s LP'),
             'gui.bloodmagic.machine.will': ('Will: %s', '意志：%s'),
             'gui.bloodmagic.machine.status.0': ('Ready', '就绪'), 'gui.bloodmagic.machine.status.1': ('Requires a bound orb / will', '需要绑定血球／意志'),
             'gui.bloodmagic.machine.status.2': ('Insufficient LP / will', 'LP／意志不足'), 'gui.bloodmagic.machine.status.3': ('Output is full', '输出槽已满'),
             'gui.bloodmagic.machine.status.4': ('Incomplete structure', '结构不完整')}
    languages(names)

def languages(entries):
    for i, lang in enumerate(['en_us', 'zh_cn']):
        path = RES / f'assets/bloodmagic/lang/{lang}.json'
        data = read(path)
        data.update({key: val[i] for key, val in entries.items()})
        write(path, data)

def phase2():
    for name in ['basemonstersoul', 'soulgempetty', 'soulgemlesser', 'soulgemcommon', 'soulsnare']: item(name)
    for name in ['pettytartaricgem', 'lessertartaricgem', 'commontartaricgem']: recipe('soulforge/' + name)
    recipe('soul_forge'); recipe('altar/soul_snare'); block_item('soulforge'); loot('soulforge')
    copy_resource('assets/bloodmagic/textures/block/soulforge.png')
    # Reuse upstream snare artwork as the small status-effect icon.
    icon = RES / 'assets/bloodmagic/textures/mob_effect/soul_snare.png'
    icon.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(RES / 'assets/bloodmagic/textures/item/soulsnare.png', icon)
    write(RES / 'assets/bloodmagic/models/block/soulforge.json', {'parent': 'minecraft:block/block', 'textures': {'all': 'bloodmagic:block/soulforge', 'particle': '#all'}, 'elements': [cube_element([1,0,1], [15,12,15], '#all')]})
    write(RES / 'assets/bloodmagic/blockstates/soulforge.json', {'variants': {'': {'model': 'bloodmagic:block/soulforge'}}})
    languages({'block.bloodmagic.soulforge': ('Hellfire Forge', '地狱熔炉'), 'item.bloodmagic.basemonstersoul': ('Demon Will', '恶魔意志'),
       'item.bloodmagic.soulgempetty': ('Petty Tartaric Gem', '微弱的恶魔结晶'), 'item.bloodmagic.soulgemlesser': ('Lesser Tartaric Gem', '次级恶魔结晶'),
       'item.bloodmagic.soulgemcommon': ('Common Tartaric Gem', '普通恶魔结晶'), 'item.bloodmagic.soulsnare': ('Rudimentary Snare', '简易灵魂陷阱'),
       'entity.bloodmagic.soulsnare': ('Soul Snare', '灵魂陷阱'), 'effect.bloodmagic.soul_snare': ('Snared Soul', '灵魂捕获'),
       'tooltip.bloodmagic.demon_will': ('Will: %s / %s; right-click to transfer to a gem', '意志：%s / %s；右键转入结晶')})

def phase3():
    for name in ['ritualdiviner','ritualdivinerdusk','activationcrystalweak','lavacrystal'] + [t+'scribetool' for t in ['water','fire','earth','air','dusk']]: item(name)
    for name in ['ritualstone','waterritualstone','earthritualstone','fireritualstone','airritualstone','duskritualstone','masterritualstone']:
        block_item(name); loot(name)
        copy_resource('assets/bloodmagic/textures/block/'+name+'.png')
        copy_resource('assets/bloodmagic/models/block/'+name+'.json')
        write(RES / f'assets/bloodmagic/blockstates/{name}.json', {'variants': {'': {'model': 'bloodmagic:block/'+name}}})
    for name in ['ritual_stone_blank','ritual_stone_master','ritual_diviner_0','ritual_diviner_1','lava_crystal','weak_activation_crystal']:
        recipe(name)
    for name in ['water','earth','fire','air','dusk']: recipe('altar/'+name+'_tool')
    orbs=['weakbloodorb','apprenticebloodorb','magicianbloodorb','masterbloodorb','archmagebloodorb']
    for tier in [1,2,3]:
        write(RES / f'data/bloodmagic/tags/item/blood_orbs/tier_{tier}.json', {'replace':False,'values':['bloodmagic:'+o for o in orbs[tier-1:]]})
    names={'ritualstone':('Ritual Stone','仪式石'),'waterritualstone':('Water Ritual Stone','水之仪式石'),'earthritualstone':('Earth Ritual Stone','地之仪式石'),
           'fireritualstone':('Fire Ritual Stone','火之仪式石'),'airritualstone':('Air Ritual Stone','风之仪式石'),'duskritualstone':('Dusk Ritual Stone','暮色仪式石'),
           'masterritualstone':('Master Ritual Stone','主仪式石')}
    entries={'block.bloodmagic.'+k:v for k,v in names.items()}
    entries.update({'item.bloodmagic.ritualdiviner':('Ritual Diviner','仪式占卜棒'),'item.bloodmagic.ritualdivinerdusk':('Dusk Ritual Diviner','暮色仪式占卜棒'),
        'item.bloodmagic.activationcrystalweak':('Weak Activation Crystal','弱效激活水晶'),'item.bloodmagic.lavacrystal':('Lava Crystal','熔岩水晶'),
        'tooltip.bloodmagic.activationcrystal':('Right-click a complete ritual to activate; sneak-click to stop.','右键完整仪式激活，潜行右键停止。'),
        'ritual.bloodmagic.water':('Ritual of the Full Spring','满泉仪式'),'ritual.bloodmagic.green_grove':('Ritual of the Green Grove','绿林仪式'),
        'ritual.bloodmagic.crushing':('Ritual of the Crusher','采掘仪式'),
        'message.bloodmagic.ritual.selected':('Selected: %s','已选择：%s'), 'message.bloodmagic.ritual.ready':('Runes complete; use an activation crystal.','符文完整，请使用激活水晶。'),
        'message.bloodmagic.ritual.changed':('Ritual state updated.','仪式状态已更新。'),'message.bloodmagic.ritual.failed':('Check runes, owner, redstone and network LP.','请检查符文、拥有者、红石信号和网络 LP。'),
        'tooltip.bloodmagic.diviner':('Air right-click to select; use on the master to place one rune.','空中右键选择仪式；右键主仪式石逐个放置符文。'),
        'tooltip.bloodmagic.ashes':('Draw on a solid top face; add reagent, then slate. Sneak empty-hand to retrieve.','在完整顶面绘阵；先放试剂、后放石板。潜行空手取回材料。')})
    for key,en,zh in [('water','Water','水'),('earth','Earth','地'),('fire','Fire','火'),('air','Air','风'),('dusk','Dusk','暮色')]: entries['item.bloodmagic.'+key+'scribetool']=(en+' Inscription Tool',zh+'之铭文工具')
    languages(entries)
    # All machine and ritual blocks have proper tool tags; the chalk array deliberately has no block item.
    path=RES/'data/minecraft/tags/block/mineable/pickaxe.json'
    data=read(path) if path.exists() else {'replace':False,'values':[]}
    for name in list(names)+['alchemytable','soulforge']:
        if 'bloodmagic:'+name not in data['values']: data['values'].append('bloodmagic:'+name)
    write(path,data)

def manifest():
    data = read(ROOT / 'docs/stage5-content.json')
    known_blocks = {v['id'] for v in data['blocks']}
    known_items = {v['id'] for v in data['items']} | known_blocks
    for path in sorted((RES / 'assets/bloodmagic/blockstates').glob('*.json')):
        if path.stem not in known_blocks:
            data['blocks'].append({'id':path.stem, 'kind':'ritual' if 'ritual' in path.stem else 'machine',
                                   'hasItem':path.stem != 'alchemyarray'})
    block_ids = {v['id'] for v in data['blocks']}
    for path in sorted((RES / 'assets/bloodmagic/items').glob('*.json')):
        if path.stem not in known_items | block_ids:
            data['items'].append({'id':path.stem, 'kind':'stage6'})
    data['stage'] = 6
    data['recipes'] = sorted(str(p.relative_to(RES / 'data/bloodmagic/recipe')).replace('\\','/')
                             for p in (RES / 'data/bloodmagic/recipe').rglob('*.json'))
    write(ROOT / 'docs/stage6-content.json', data)

if __name__ == '__main__':
    phase1()
    phase2()
    phase3()
    manifest()
    print('Stage 6 machine/ritual resources and manifest generated from pinned upstream')
