import zipfile, json, io, os, sys

SRC = r"E:\PCL\PCL2\.minecraft\versions\Lets go cobblemon master [6.0.2fix]\mods\[优化-区块加载]c2me-fabric-mc1.21.1-0.3.0+alpha.0.362.jar.disabled"
DST = r"E:\PCL\PCL2\.minecraft\versions\Lets go cobblemon master [6.0.2fix]\mods\[优化-区块加载]c2me-fabric-mc1.21.1-0.3.0+alpha.0.362.jar"

def java_req_of(jar_bytes):
    """Return depends.java requirement of a nested jar, or None."""
    try:
        z = zipfile.ZipFile(io.BytesIO(jar_bytes))
        with z.open("fabric.mod.json") as f:
            m = json.load(f)
        return (m.get("depends") or {}).get("java")
    except Exception as e:
        return None

zin = zipfile.ZipFile(SRC)
fmj = json.loads(zin.read("fabric.mod.json").decode("utf-8"))

# map nested jar file -> bytes
nested = {}
for j in fmj.get("jars", []):
    name = j["file"]
    try:
        nested[name] = zin.read(name)
    except KeyError:
        pass

print("=== nested modules & java requirement ===")
drop = set()
for name, data in nested.items():
    req = java_req_of(data)
    flag = ""
    if req:
        # crude: any requirement mentioning 22+ or 23/24/25
        digits = "".join(ch for ch in req if ch.isdigit())
        if digits and int(digits[:2]) >= 22:
            flag = "  <== DROP (needs newer java)"
            drop.add(name)
    print(f"  {req!s:18}  {name}{flag}")

# rewrite
kept_jars = [j for j in fmj.get("jars", []) if j["file"] not in drop]
fmj["jars"] = kept_jars
print(f"\n dropping {len(drop)} nested jar(s); keeping {len(kept_jars)}")

with zipfile.ZipFile(DST, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename in drop:
            continue
        data = zin.read(item.filename)
        if item.filename == "fabric.mod.json":
            data = json.dumps(fmj, ensure_ascii=False, indent=2).encode("utf-8")
        zout.writestr(item, data)

zin.close()
print("\nwritten:", DST, os.path.getsize(DST), "bytes")
