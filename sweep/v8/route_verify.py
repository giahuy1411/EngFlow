import json, re, sys
from pathlib import Path

root = Path(__file__).resolve().parents[2]
d = json.loads((root / ".specify/specs/audit-v8-full/evidence/route-inventory.json").read_text(encoding="utf-8"))
paths = [r["path"] for r in d["routes"]]
print("parsed routes  :", len(paths), " unique:", len(set(paths)))

src = (root / "frontend/src/router/index.js").read_text(encoding="utf-8")
src = re.sub(r"//[^\n]*", "", src)
src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
literals = re.findall(r"path:\s*'([^']*)'", src)
print("source literals:", len(literals), " unique:", len(set(literals)))

parsed = set(paths)
srclit = set(literals)
# source child paths are relative ('dashboard'); parsed are absolute ('/admin/dashboard')
relative = {p for p in srclit if not p.startswith("/")}
print("relative child paths in source:", sorted(relative))
missing = srclit - parsed - relative
print("in source but NOT parsed :", sorted(missing) if missing else "none")
extra = parsed - srclit - {"/admin/" + p for p in relative}
print("parsed but NOT in source :", sorted(extra) if extra else "none")
ok = not missing and not extra and len(paths) == len(set(paths))
print("VERDICT:", "CONSISTENT" if ok else "MISMATCH")
sys.exit(0 if ok else 1)
