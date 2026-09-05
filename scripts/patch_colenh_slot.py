#!/usr/bin/env python3
import sys

def patch_tilemap_colenh(file_path):
    with open(file_path, "rb") as f:
        data = bytearray(f.read())

    # Pattern: bipush 14 (0x10 0x0e), bipush 29 (0x10 0x1d), iconst_2 (0x05)
    pattern = bytes([0x10, 0x0e, 0x10, 0x1d, 0x05])
    idx = data.find(pattern)

    if idx == -1:
        # Check if already patched to 28 (0x1c)
        pattern_patched = bytes([0x10, 0x0e, 0x10, 0x1c, 0x05])
        if data.find(pattern_patched) != -1:
            print(f"[+] {file_path} is already patched (slot 28).")
            return
        else:
            print(f"[-] Error: Could not find target pattern in {file_path}")
            sys.exit(1)

    data[idx + 3] = 0x1c  # Change slot index from 29 -> 28 (Row 5, Col 5)
    
    with open(file_path, "wb") as f:
        f.write(data)

    print(f"[+] Successfully patched {file_path}: changed Goshu Cổ Lệnh slot from 29 -> 28 (Row 5, Col 5).")

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else "build/unpacked/TileMap.class"
    patch_tilemap_colenh(path)
