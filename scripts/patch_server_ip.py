#!/usr/bin/env python3
"""
Patch server IP trong LoginScr.class (constant pool UTF-8 entry).

Cách dùng:
  python scripts/patch_server_ip.py build/unpacked/LoginScr.class <new_ip_or_hostname>

Ví dụ:
  python scripts/patch_server_ip.py build/unpacked/LoginScr.class aeharuna.com
"""
import sys
import struct

OLD_IP = b"103.77.214.194"


def patch_ip(class_path, new_ip_str):
    new_ip = new_ip_str.encode("utf-8")
    old_len = len(OLD_IP)
    new_len = len(new_ip)

    data = bytearray(open(class_path, "rb").read())

    # --- Parse constant pool to find the UTF-8 entry ---
    cp_count = struct.unpack(">H", data[8:10])[0]
    idx = 10
    target_offset = -1
    target_entry_start = -1

    i = 1
    while i < cp_count:
        tag = data[idx]
        if tag == 1:  # UTF-8
            length = struct.unpack(">H", data[idx + 1 : idx + 3])[0]
            s = data[idx + 3 : idx + 3 + length]
            if s == OLD_IP:
                target_offset = idx  # tag position
                target_entry_start = idx + 3  # data start
                print(f"Tim thay IP [{OLD_IP.decode()}] tai CP#{i}, offset={idx}")
                break
            idx += 3 + length
        elif tag in (7, 8, 16, 19, 20):
            idx += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            idx += 5
        elif tag in (5, 6):
            idx += 9
            i += 1
        elif tag == 15:
            idx += 4
        else:
            print(f"Loi: Unknown CP tag {tag} tai offset {idx}")
            sys.exit(1)
        i += 1

    if target_offset < 0:
        print(f"Khong tim thay IP [{OLD_IP.decode()}] trong constant pool!")
        sys.exit(1)

    # --- Patch: thay length + data ---
    # Structure: tag(1) + length(2) + utf8_bytes(length)
    # Old total: 1 + 2 + old_len
    # New total: 1 + 2 + new_len

    # Update length field (2 bytes big-endian at target_offset+1)
    struct.pack_into(">H", data, target_offset + 1, new_len)

    # Replace data bytes
    old_entry_end = target_entry_start + old_len
    new_data = data[:target_entry_start] + new_ip + data[old_entry_end:]

    open(class_path, "wb").write(new_data)

    diff = new_len - old_len
    print(f"Da doi [{OLD_IP.decode()}] -> [{new_ip_str}] (size {old_len} -> {new_len}, diff={diff:+d})")
    print(f"File saved: {class_path} ({len(new_data)} bytes)")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python patch_server_ip.py <LoginScr.class> <new_ip_or_hostname>")
        sys.exit(1)
    patch_ip(sys.argv[1], sys.argv[2])
