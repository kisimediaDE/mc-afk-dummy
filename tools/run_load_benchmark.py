"""Run the isolated integration benchmark. Requires prepared local Paper test servers."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import threading
import time

parser = argparse.ArgumentParser()
parser.add_argument('--target', choices=['26.2', '26.3'], required=True)
parser.add_argument('--variant', choices=['before', 'after'], required=True)
parser.add_argument('--java', required=True)
parser.add_argument('--run-id', default='', help='Optional suffix for a fresh repeat run')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
baseline_version = {'26.2': '1.0.0', '26.3': '2.0.0'}[args.target]
release_version = {'26.2': '1.1.0', '26.3': '2.1.0'}[args.target]
version = baseline_version if args.variant == 'before' else release_version
assert not args.run_id or args.run_id.isalnum(), 'run-id must be alphanumeric'
label = f'{args.target}-{args.variant}' + (f'-{args.run_id}' if args.run_id else '')
source = root / '.integration' / ('server' if args.target == '26.2' else 'server-26.3')
server = root / '.integration' / ('load-' + label)
assert not server.exists(), f'Refusing to reuse benchmark world: {server}'
server.mkdir()
for name in ['paper.jar', 'eula.txt', 'server.properties', 'bukkit.yml', 'spigot.yml']:
    shutil.copy2(source / name, server / name)
for name in ['libraries', 'versions', 'cache', 'config']:
    if (source / name).exists(): shutil.copytree(source / name, server / name)
properties = (server / 'server.properties').read_text()
replacements = {'level-name': 'benchmark-world', 'level-seed': '20260925',
                'server-port': '25587', 'server-ip': '127.0.0.1', 'enable-rcon': 'false',
                'pause-when-empty-seconds': '-1', 'sync-chunk-writes': 'false'}
lines = [f'{line.split("=", 1)[0]}={replacements[line.split("=", 1)[0]]}'
         if '=' in line and line.split('=', 1)[0] in replacements else line
         for line in properties.splitlines()]
(server / 'server.properties').write_text('\n'.join(lines) + '\n')
plugins = server / 'plugins'
plugins.mkdir()
(plugins / 'AFKDummy').mkdir()
shutil.copy2(source / 'plugins/AFKDummy/config.yml', plugins / 'AFKDummy/config.yml')
jar = (root / 'build/libs' if args.variant == 'before' else root / 'build' / args.target / 'libs') / f'AFKDummyLimited-{version}.jar'
shutil.copy2(jar, plugins / jar.name)
shutil.copy2(root / 'build' / args.target / 'libs' / f'AFKDummy-Probe-{release_version}.jar', plugins / f'AFKDummy-Probe-{release_version}.jar')
metadata = {'target': args.target, 'variant': args.variant,
            'runId': args.run_id or 'first',
            'pluginSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
            'serverSha256': hashlib.sha256((server / 'paper.jar').read_bytes()).hexdigest(),
            'java': args.java, 'heap': '-Xms512M -Xmx1536M', 'warmupTicks': 200, 'sampleTicks': 400,
            'seed': 20260925, 'viewDistance': 3, 'simulationDistance': 3}
out = root / 'docs' / 'benchmarks'
out.mkdir(exist_ok=True)
log = open(server / 'benchmark.log', 'w', encoding='utf8')
messages = queue.Queue()
process = subprocess.Popen([args.java, '-Xms512M', '-Xmx1536M', '-jar', 'paper.jar', '--nogui'],
    cwd=server, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
    text=True, encoding='utf8', errors='replace',
    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
def reader():
    for line in process.stdout:
        log.write(line); log.flush(); messages.put(line)
reader_thread = threading.Thread(target=reader, daemon=True)
reader_thread.start()
def send(command):
    process.stdin.write(command + '\n'); process.stdin.flush()
def wait(marker, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try: line = messages.get(timeout=1)
        except queue.Empty:
            if process.poll() is not None: raise RuntimeError('Server exited')
            continue
        if 'BENCH ' in line or marker in line: print(line.strip(), flush=True)
        if 'PROBE FAILED' in line or 'Error occurred' in line or 'generated an exception' in line:
            raise RuntimeError(line)
        if marker in line: return
    raise TimeoutError(marker)
try:
    wait('Done (', 120)
    wait('No dummy sessions to restore', 30)
    send('afkprobe bench ' + label)
    wait('BENCH DONE ' + label, 300)
    results = json.loads((plugins / 'AFKDummyProbe' / f'benchmark-{label}.json').read_text())
    assert len(results) == 5 and all(r['ticks'] == 400 for r in results)
    assert all(r['validMobs'] == r['dummies'] * 32 for r in results)
    (out / f'{label}.json').write_text(json.dumps({'metadata': metadata, 'results': results}, indent=2) + '\n')
finally:
    if process.poll() is None:
        send('stop')
        try: process.wait(timeout=60)
        except subprocess.TimeoutExpired:
            process.terminate(); process.wait(timeout=15)
    reader_thread.join(timeout=5)
    log.close()
print(f'BENCH SAVED {out / (label + ".json")}', flush=True)
