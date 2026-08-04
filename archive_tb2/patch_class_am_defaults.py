"""Patch Class_am.class: HS Luong default OFF, SPGame default 20."""
import sys
import pathlib

target = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path("build/tb2_step1_patched/Class_am.class")
data = bytearray(target.read_bytes())

# --- Find <clinit> method ---
# Pattern 1: iconst_1 (0x04) + putstatic Field ap:Z (0xB3 + 2 bytes cp index)
# At PC 108-109 in <clinit>, bytes: 04 B3 00 95  (iconst_1, putstatic #149)
# cp index for ap is #149 = 0x0095

# Pattern 2: bipush 40 (0x10 0x28) + putstatic Field y:I (0xB3 + 2 bytes cp index)
# At PC 330-332, bytes: 10 28 B3 00 F1  (bipush 40, putstatic #241)
# cp index for y is #241 = 0x00F1

# Search for pattern: iconst_1 + putstatic(ap) followed by bipush 29 + putstatic(aq)
# ap = #149 (0x0095), aq = #151 (0x0097)
pat_hs = bytes([0x04, 0xB3, 0x00, 0x95, 0x10, 0x1D, 0xB3, 0x00, 0x97])
# 0x04=iconst_1, 0xB3=putstatic, 0x0095=#149(ap), 0x10=bipush, 0x1D=29, 0xB3=putstatic, 0x0097=#151(aq)

idx_hs = data.find(pat_hs)
if idx_hs < 0:
    print("ERROR: HS Luong pattern not found!")
    sys.exit(1)
print(f"Found HS Luong at offset {idx_hs}: iconst_1 -> iconst_0")
data[idx_hs] = 0x03  # iconst_0

# Search for pattern: bipush 40 + putstatic(y)
# y = #241 (0x00F1)
pat_sp = bytes([0x10, 0x28, 0xB3, 0x00, 0xF1])
# 0x10=bipush, 0x28=40, 0xB3=putstatic, 0x00F1=#241(y)

idx_sp = data.find(pat_sp)
if idx_sp < 0:
    print("ERROR: SPGame pattern not found!")
    sys.exit(1)
print(f"Found SPGame at offset {idx_sp}: bipush 40 -> bipush 20")
data[idx_sp + 1] = 0x14  # 20

target.write_bytes(data)
print(f"Patched {target} ({len(data)} bytes)")
