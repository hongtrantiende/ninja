"""Patch GameCanvas.class for Black Background (Nền Đen 1.4.8).

Replaces PC 0 of GameCanvas.gameAA(mGraphics) with 'goto 26' (A7 00 1A 00 00 00).
This forces GameCanvas.gameAA to fill the screen with solid black (0x000000)
and return immediately without drawing any background images/sky/clouds.
"""
import sys, pathlib

target_path = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path("build/unpacked/GameCanvas.class")

data = bytearray(target_path.read_bytes())

# Original pattern at PC 0 of GameCanvas.gameAA(mGraphics):
# B2 00 A0 99 00 2B B2 00 7A 10 0A 70 10 07 A4 00 0C 2A 12 A5 B6 00 A6 A7 00 08 2A 03 B6 00 A6 2A 03 03 B2 00 A4 B2 00 A7 B6 00 A8 A7
pat = bytes([
    0xB2, 0x00, 0xA0, 0x99, 0x00, 0x2B, 0xB2, 0x00, 0x7A, 0x10, 0x0A, 0x70,
    0x10, 0x07, 0xA4, 0x00, 0x0C, 0x2A, 0x12, 0xA5, 0xB6, 0x00, 0xA6, 0xA7,
    0x00, 0x08, 0x2A, 0x03, 0xB6, 0x00, 0xA6, 0x2A, 0x03, 0x03, 0xB2, 0x00,
    0xA4, 0xB2, 0x00, 0xA7, 0xB6, 0x00, 0xA8, 0xA7
])

pos = data.find(pat)
if pos < 0:
    # Check if already patched: A7 00 1A 00 00 00
    patched_pat = bytes([0xA7, 0x00, 0x1A, 0x00, 0x00, 0x00, 0xB2, 0x00, 0x7A])
    if data.find(patched_pat) >= 0:
        print("⚡ GameCanvas.class already patched for black background!")
        sys.exit(0)
    print("❌ Error: Pattern not found in GameCanvas.class")
    sys.exit(1)

print(f"Found GameCanvas.gameAA at offset {pos}")
# Replace 'B2 00 A0 99 00 2B' with 'A7 00 1A 00 00 00' (goto 26 + 3 nops)
data[pos:pos+6] = bytes([0xA7, 0x00, 0x1A, 0x00, 0x00, 0x00])

target_path.write_bytes(data)
print(f"✅ Successfully patched {target_path} for Black Background (Nền Đen)!")
