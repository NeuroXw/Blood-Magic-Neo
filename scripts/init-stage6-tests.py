from pathlib import Path
import shutil

root = Path(__file__).resolve().parents[1]
source = root / 'src/stage5Smoke/java/wayoftime/bloodmagic/test'
dest = root / 'src/stage6Smoke/java/wayoftime/bloodmagic/test'
dest.mkdir(parents=True, exist_ok=True)
for path in source.glob('*.java'):
    target = dest / path.name.replace('Stage5', 'Stage6')
    if target.exists(): continue
    content = path.read_text(encoding='utf-8-sig').replace('Stage5', 'Stage6').replace('STAGE5', 'STAGE6').replace('stage5', 'stage6')
    if path.name == 'Stage5SmokeChecks.java':
        content = content.replace('assertions += OrbSigilChecks.begin(level);', 'assertions += OrbSigilChecks.begin(level);\n        assertions += ArrayChecks.begin(level);')
        content = content.replace('if (!AltarReloadChecks.afterTick(level)) return;', 'if (!AltarReloadChecks.afterTick(level)) return;\n        if (!ArrayChecks.afterTick(level)) return;')
        content = content.replace('assertions += OrbSigilChecks.finish(level);', 'assertions += OrbSigilChecks.finish(level);\n        assertions += ArrayChecks.finish(level);')
    target.write_text(content, encoding='utf-8')
run = root / 'run/stage6-smoke'
run.mkdir(exist_ok=True)
for name in ['eula.txt', 'server.properties']:
    if not (run / name).exists() and (root / 'run/stage5-smoke' / name).exists(): shutil.copy2(root / 'run/stage5-smoke' / name, run / name)
print('Stage 6 independent server harness initialized; prior suite preserved')
