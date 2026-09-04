#!/usr/bin/env python3
"""
Script doi IP va Port server trong LoginScr.class va GameMidlet.class.
Ho tro ca file .jar truc tiep lan thu muc da unpack.

Cach dung:
  python3 scripts/patch_server_ip.py [jar_hoac_dir] [new_ip] [new_port]

Vi du:
  python3 scripts/patch_server_ip.py SVJenny.jar 160.250.130.241 15555
  python3 scripts/patch_server_ip.py build/unpacked 160.250.130.241 15555
  python3 scripts/patch_server_ip.py Aeharuna_share.jar 160.250.130.241 15555
"""
import os
import sys
import struct
import zipfile
import shutil
import tempfile

DEFAULT_IP = "160.250.130.241"
DEFAULT_PORT = 15555


def patch_cp_entry(data, entry_idx, new_bytes):
    """Patch a specific UTF-8 constant pool entry in class file bytes."""
    data = bytearray(data)
    cp_count = struct.unpack(">H", data[8:10])[0]
    idx = 10
    target_offset = -1
    target_data_start = -1
    old_len = 0

    i = 1
    while i < cp_count:
        tag = data[idx]
        if tag == 1:
            length = struct.unpack(">H", data[idx + 1 : idx + 3])[0]
            if i == entry_idx:
                target_offset = idx
                target_data_start = idx + 3
                old_len = length
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
            raise ValueError(f"Unknown CP tag {tag} at {idx}")
        i += 1

    if target_offset < 0:
        raise ValueError(f"CP entry #{entry_idx} not found")

    new_len = len(new_bytes)
    struct.pack_into(">H", data, target_offset + 1, new_len)
    new_data = data[:target_data_start] + new_bytes + data[target_data_start + old_len:]
    return bytes(new_data)


def patch_loginscr_bytes(data, new_ip=None, new_port=None):
    """Patch IP (CP#746) and Port (sipush in clinit) in LoginScr.class bytes."""
    # 1. Patch IP in CP#746
    if new_ip:
        data = patch_cp_entry(data, 746, new_ip.encode("utf-8"))
        print(f"  [OK] LoginScr: da doi IP server thanh [{new_ip}]")

    # 2. Patch Port in clinit
    if new_port is not None:
        data = bytearray(data)
        port_bytes = struct.pack(">H", int(new_port))
        # sipush opcode 0x11 followed by 2 bytes port, then iastore 0x4f
        pattern = b"\x11"
        pos = 0
        patched = False
        while True:
            idx = data.find(pattern, pos)
            if idx == -1:
                break
            if idx + 3 < len(data) and data[idx + 3] == 0x4F:  # iastore
                data[idx + 1 : idx + 3] = port_bytes
                patched = True
                print(f"  [OK] LoginScr: da doi listPort thanh [{new_port}]")
                break
            pos = idx + 1

        if not patched:
            print("  [WARN] Khong tim thay vi tri sipush listPort trong LoginScr!")
        data = bytes(data)

    return data


def patch_gamemidlet_bytes(data, new_port=None):
    """Patch default PORT in GameMidlet.class bytes."""
    if new_port is None:
        return data

    data = bytearray(data)
    port_bytes = struct.pack(">H", int(new_port))
    pattern = b"\x11"
    pos = 0
    patched = False
    while True:
        idx = data.find(pattern, pos)
        if idx == -1:
            break
        if idx + 3 < len(data) and data[idx + 3] == 0xB3:  # putstatic
            data[idx + 1 : idx + 3] = port_bytes
            patched = True
            print(f"  [OK] GameMidlet: da doi PORT mac dinh thanh [{new_port}]")
            break
        pos = idx + 1

    if not patched:
        print("  [WARN] Khong tim thay vi tri sipush PORT trong GameMidlet!")

    return bytes(data)


def patch_jar(jar_path, new_ip, new_port):
    """Patch classes directly inside a jar file."""
    print(f"=== Patching JAR: {jar_path} ===")
    tmp_dir = tempfile.mkdtemp()
    try:
        with zipfile.ZipFile(jar_path, "r") as zin:
            zin.extractall(tmp_dir)

        loginscr_path = os.path.join(tmp_dir, "LoginScr.class")
        if os.path.exists(loginscr_path):
            with open(loginscr_path, "rb") as f:
                cdata = f.read()
            cdata = patch_loginscr_bytes(cdata, new_ip, new_port)
            with open(loginscr_path, "wb") as f:
                f.write(cdata)

        gamemidlet_path = os.path.join(tmp_dir, "GameMidlet.class")
        if os.path.exists(gamemidlet_path):
            with open(gamemidlet_path, "rb") as f:
                cdata = f.read()
            cdata = patch_gamemidlet_bytes(cdata, new_port)
            with open(gamemidlet_path, "wb") as f:
                f.write(cdata)

        tmp_jar = os.path.join(tmp_dir, "repacked.jar")
        manifest_file = os.path.join(tmp_dir, "META-INF", "MANIFEST.MF")
        if os.path.exists(manifest_file):
            import subprocess
            subprocess.run(["jar", "cfm", tmp_jar, manifest_file, "."], cwd=tmp_dir, check=True)
        else:
            with zipfile.ZipFile(tmp_jar, "w", zipfile.ZIP_DEFLATED) as zout:
                for root, _, files in os.walk(tmp_dir):
                    for f in files:
                        if f == "repacked.jar":
                            continue
                        full_p = os.path.join(root, f)
                        rel_p = os.path.relpath(full_p, tmp_dir)
                        zout.write(full_p, rel_p)

        shutil.move(tmp_jar, jar_path)
        print(f"=== SUCCESS! Da cap nhat {jar_path} ({os.path.getsize(jar_path)} bytes) ===")
    finally:
        shutil.rmtree(tmp_dir)


def patch_dir(target_dir, new_ip, new_port):
    """Patch classes in an extracted directory."""
    print(f"=== Patching directory: {target_dir} ===")
    loginscr_path = os.path.join(target_dir, "LoginScr.class")
    if os.path.exists(loginscr_path):
        with open(loginscr_path, "rb") as f:
            cdata = f.read()
        cdata = patch_loginscr_bytes(cdata, new_ip, new_port)
        with open(loginscr_path, "wb") as f:
            f.write(cdata)

    gamemidlet_path = os.path.join(target_dir, "GameMidlet.class")
    if os.path.exists(gamemidlet_path):
        with open(gamemidlet_path, "rb") as f:
            cdata = f.read()
        cdata = patch_gamemidlet_bytes(cdata, new_port)
        with open(gamemidlet_path, "wb") as f:
            f.write(cdata)


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else "SVJenny.jar"
    new_ip = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_IP
    new_port = int(sys.argv[3]) if len(sys.argv) > 3 else DEFAULT_PORT

    if os.path.isfile(target) and target.endswith(".jar"):
        patch_jar(target, new_ip, new_port)
    elif os.path.isdir(target):
        patch_dir(target, new_ip, new_port)
    else:
        print(f"Target khong hop le: {target}")
        sys.exit(1)


if __name__ == "__main__":
    main()
