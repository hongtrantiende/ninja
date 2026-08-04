"""Patch Class_er.class: force coolDown = 100ms (0.1s) at every READ.

Replaces all 3 occurrences of 'aload_0 + getfield e:I' (4 bytes each)
with 'bipush 100 + nop + nop' (4 bytes each). Exact same size, no structural changes.

Result: cooldown animation still plays but finishes in 0.1s.
"""
import sys, pathlib

src = pathlib.Path(sys.argv[1])
dst = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else src

data = bytearray(src.read_bytes())

# Pattern: 2A B4 00 3A (aload_0 + getfield #58 e:I)
# getfield e:I has cp index #58 = 0x003A
pat = bytes([0x2A, 0xB4, 0x00, 0x3A])

count = 0
idx = 0
while idx < len(data):
    pos = data.find(pat, idx)
    if pos < 0:
        break
    count += 1
    print(f"  #{count} at offset {pos}: {' '.join(f'{data[i]:02x}' for i in range(pos, pos+6))}")
    # Replace: bipush 100 (10 64) + nop (00) + nop (00)
    data[pos] = 0x10      # bipush
    data[pos + 1] = 0x64  # 100
    data[pos + 2] = 0x00  # nop
    data[pos + 3] = 0x00  # nop
    print(f"       -> {' '.join(f'{data[i]:02x}' for i in range(pos, pos+6))}")
    idx = pos + 4

print(f"\nPatched {count} occurrences of getfield e:I -> bipush 100")
dst.write_bytes(data)
print(f"Saved {dst} ({len(data)} bytes)")
