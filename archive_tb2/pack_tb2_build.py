"""
Pack TB2 JAR from unpacked base directory.
Replaces pack_tb2_step1.py for environments without the original JAR.
Rules:
  - agent.txt MUST be the first entry
  - META-INF/MANIFEST.MF MUST be the last entry
  - No javax/ entries allowed
  - Overrides patched classes and adds new mod classes
"""
import os
import pathlib
import zipfile

root = pathlib.Path(__file__).resolve().parent.parent
base = root / "archive_tb2" / "tb2_step1_base"
patched = root / "archive_tb2" / "tb2_step1_patched"
classes = root / "archive_tb2" / "tb2_step1_classes"
output = root / "Aeharuna_148TB2_NamMod.jar"
temporary = output.with_suffix(".jar.tmp")

# Patched base classes (overrides)
overrides = {}
if patched.exists():
    for f in patched.rglob("*.class"):
        rel = f.relative_to(patched).as_posix()
        overrides[rel] = f

# New mod classes
new_classes = {}
if classes.exists():
    for f in classes.rglob("*.class"):
        name = f.name
        # Skip PatchMenuStep1 - it's a build tool, not a game class
        if name.startswith("PatchMenuStep1"):
            continue
        new_classes[name] = f

print(f"Overrides: {list(overrides.keys())}")
print(f"New classes: {list(new_classes.keys())}")

# Collect all entries from base directory
entries = []
for dirpath, dirnames, filenames in os.walk(base):
    for fn in filenames:
        full = pathlib.Path(dirpath) / fn
        rel = full.relative_to(base).as_posix()
        # Skip javax/ entries
        if rel.startswith("javax/"):
            continue
        entries.append(rel)

# Sort entries: agent.txt first, META-INF/MANIFEST.MF last
manifest_entry = "META-INF/MANIFEST.MF"
entries_sorted = []
if "agent.txt" in entries:
    entries_sorted.append("agent.txt")
    entries.remove("agent.txt")
if manifest_entry in entries:
    entries.remove(manifest_entry)
entries_sorted.extend(sorted(entries))

# Insert new mod classes BEFORE manifest
for name in sorted(new_classes.keys()):
    if name not in entries_sorted:
        entries_sorted.append(name)

# Manifest must be last
entries_sorted.append(manifest_entry)

# Build the JAR
with zipfile.ZipFile(temporary, "w") as jar:
    for entry in entries_sorted:
        if entry in overrides:
            data = overrides[entry].read_bytes()
        elif entry in new_classes:
            data = new_classes[entry].read_bytes()
        else:
            src = base / entry
            data = src.read_bytes()
        jar.writestr(entry, data, compress_type=zipfile.ZIP_DEFLATED)

# Verify
with zipfile.ZipFile(temporary, "r") as jar:
    names = jar.namelist()
    assert names[0] == "agent.txt", f"First entry must be agent.txt, got {names[0]}"
    assert names[-1] == manifest_entry, f"Last entry must be {manifest_entry}, got {names[-1]}"
    assert jar.testzip() is None, "ZIP integrity check failed"
    for name in list(overrides) + list(new_classes):
        assert name in names, f"Missing entry: {name}"
    javax_entries = [n for n in names if n.startswith("javax/")]
    assert len(javax_entries) == 0, f"JAR contains javax/ entries: {javax_entries}"
    print(f"Verification OK: {len(names)} entries")

os.replace(temporary, output)
print(f"Packed: {output} ({output.stat().st_size:,} bytes)")
