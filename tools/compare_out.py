import json
import sys
from pathlib import Path

expected, actual = Path(sys.argv[1]), Path(sys.argv[2])
names = sorted(p.name for p in expected.glob("*.json"))
problems = 0
for name in names:
    other = actual / name
    if not other.exists():
        print("MISSING", name)
        problems += 1
    elif json.loads((expected / name).read_text(encoding="utf-8")) != json.loads(other.read_text(encoding="utf-8")):
        print("DIFFERENT", name)
        problems += 1
extra = sorted(p.name for p in actual.glob("*.json") if p.name not in names)
for name in extra:
    print("EXTRA", name)
    problems += 1
print("same" if problems == 0 else f"{problems} problem(s)", f"({len(names)} files compared)")
sys.exit(1 if problems else 0)