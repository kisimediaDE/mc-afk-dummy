"""Package a built Community release with an explicit source allowlist."""
from pathlib import Path
import hashlib
import argparse
import json
import re
import shutil
import xml.etree.ElementTree as ET
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--target-paper', choices=['26.2', '26.3'], default='26.3')
target = parser.parse_args().target_paper
version = {'26.2': '1.1.0', '26.3': '2.1.0'}[target]
build = ROOT / 'build' / target
name = f'AFKDummyLimited-{version}'
jar = build / 'libs' / f'{name}.jar'
release = ROOT / 'dist' / name
release.mkdir(parents=True, exist_ok=True)

suites = []
for path in sorted((build / 'test-results/test').glob('TEST-*.xml')):
    suite = ET.parse(path).getroot()
    suites.append({'suite': suite.get('name'), **{k: int(suite.get(k, '0')) for k in ('tests', 'failures', 'errors', 'skipped')}})
assert suites and all(s['failures'] == s['errors'] == 0 for s in suites), 'Tests must pass before packaging'
results_name = f'unit-test-results-{target}.json'
(ROOT / 'docs' / results_name).write_text(json.dumps(suites, indent=2) + '\n', encoding='utf-8')

with ZipFile(jar) as z:
    names = z.namelist()
    for required in ('plugin.yml', 'META-INF/LICENSE', 'META-INF/NOTICE', 'META-INF/THIRD_PARTY_NOTICES.md', 'META-INF/licenses/Apache-2.0.txt'):
        assert required in names, f'Missing {required}'
    assert not any('bstats' in p.lower() or '/probe/' in p for p in names), 'Test/telemetry classes must not ship'
    descriptor = z.read('plugin.yml').decode()
    assert f"version: '{version}'" in descriptor and 'name: AFKDummy\n' in descriptor
    assert f"api-version: '{target}'" in descriptor

top = ['README.md', 'CHANGELOG.md', 'LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md']
docs = ['UPSTREAM.md', 'ABNAHME.md', 'PERFORMANCE.md', results_name]
docs += [p.relative_to(ROOT / 'docs').as_posix() for p in sorted((ROOT / 'docs/benchmarks').glob('*')) if p.suffix in ('.json', '.md')]
docs += [p.relative_to(ROOT / 'docs').as_posix() for p in sorted((ROOT / 'docs/releases').glob('*.md'))]
shutil.copy2(jar, release / jar.name)
for file in top:
    shutil.copy2(ROOT / file, release / file)
for directory in ('docs', 'licenses'):
    (release / directory).mkdir(exist_ok=True)
    files = [ROOT / directory / f for f in docs] if directory == 'docs' else list((ROOT / directory).glob('*.txt'))
    for file in files:
        destination = release / directory / file.relative_to(ROOT / directory)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, destination)

# Include only project sources, build tools and public documentation, never server data/caches.
source_files = [ROOT / f for f in top + ['build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', '.gitignore']]
for directory in ('src', 'gradle', 'licenses', 'tools', '.github'):
    source_files += [p for p in (ROOT / directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
source_files += [ROOT / 'docs' / f for f in docs]
with ZipFile(release / f'{name}-sources.zip', 'w', ZIP_DEFLATED) as archive:
    for file in sorted(source_files):
        archive.write(file, file.relative_to(ROOT).as_posix())

hashes = []
for file in sorted(release.iterdir()):
    if file.suffix in ('.jar', '.zip'):
        hashes.append(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.name}')
(release / 'SHA256SUMS').write_text('\n'.join(hashes) + '\n', encoding='utf-8')
print(json.dumps({'release': str(release), 'tests': sum(s['tests'] for s in suites), 'hashes': hashes}, indent=2))
