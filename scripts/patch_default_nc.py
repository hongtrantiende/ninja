"""
Patch Code.class: mac dinh n10 c40
  gameBI = false → true   (bat ngang)
  gameBJ = 40   → 10      (tam ngang = 10)
  gameBK = false → true   (bat cao)
  gameBL = 40   → 40      (giu nguyen)
"""
import os

CLASS_FILE = os.path.join("build", "unpacked", "Code.class")

with open(CLASS_FILE, "rb") as f:
    data = bytearray(f.read())

patches = 0

# Tim putstatic refs
print("Scanning for putstatic field refs...")
field_offsets = {}
for i in range(len(data) - 3):
    if data[i] == 0xB3:  # putstatic
        ref = (data[i+1] << 8) | data[i+2]
        if ref in [277, 280, 283, 286]:
            prev = data[i-1] if i > 0 else 0
            prev2 = data[i-2] if i > 1 else 0
            field_offsets.setdefault(ref, []).append(i)
            print(f"  putstatic #{ref} at {i}, prev bytes: 0x{prev2:02X} 0x{prev:02X}")

# Patch gameBI (#277): iconst_0 → iconst_1
for off in field_offsets.get(277, []):
    if data[off-1] == 0x03:  # iconst_0
        data[off-1] = 0x04   # iconst_1
        patches += 1
        print(f"Patched gameBI=true at {off-1}")

# Patch gameBJ (#280): bipush 40 → bipush 10
for off in field_offsets.get(280, []):
    if data[off-2] == 0x10 and data[off-1] == 0x28:  # bipush 40
        data[off-1] = 0x0A  # 10
        patches += 1
        print(f"Patched gameBJ=10 at {off-2}")

# Patch gameBK (#283): iconst_0 → iconst_1
for off in field_offsets.get(283, []):
    if data[off-1] == 0x03:  # iconst_0
        data[off-1] = 0x04   # iconst_1
        patches += 1
        print(f"Patched gameBK=true at {off-1}")

# gameBL (#286): giu 40 — khong can patch

print(f"\nTotal patches: {patches}")
if patches >= 3:
    with open(CLASS_FILE, "wb") as f:
        f.write(data)
    print("Code.class patched OK! Default: n10 c40")
else:
    print("WARNING: khong du patch!")
