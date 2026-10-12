import zipfile, json, os, sys, re

MODS = r"E:\PCL\PCL2\.minecraft\versions\Lets go cobblemon master [6.0.2fix]\mods"
KEYWORDS = ["膏药", "特性膏药", "特效膏药", "ability_patch", "ability_capsule"]

hits = []
for name in os.listdir(MODS):
    if not name.lower().endswith(".jar"):
        continue
    path = os.path.join(MODS, name)
    try:
        if os.path.getsize(path) > 250 * 1024 * 1024:
            continue
        z = zipfile.ZipFile(path)
    except Exception:
        continue
    for e in z.namelist():
        if not re.match(r"assets/.+/lang/(zh_cn|en_us)\.json$", e):
            continue
        try:
            data = json.loads(z.read(e).decode("utf-8"))
        except Exception:
            continue
        for k, v in data.items():
            s = f"{k} = {v}"
            for kw in KEYWORDS:
                if kw in s or kw.lower() in k.lower():
                    hits.append((name, e, s))
    z.close()

print(f"=== {len(hits)} hits ===")
for h in hits[:80]:
    print(f"[{h[0]}] {h[1]}\n    {h[2]}")
