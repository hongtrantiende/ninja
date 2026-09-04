import os
import sys
import glob
import shutil
import zipfile
import subprocess
import struct

root = os.path.abspath(os.path.dirname(__file__))
build_dir = os.path.join(root, "build")
unpacked_dir = os.path.join(build_dir, "unpacked")
stubs_dir = os.path.join(build_dir, "stubs_compiled")
jar_path = os.path.join(root, "Aeharuna.jar")

print("=== 1. Clean & Unpack current Aeharuna.jar ===")
if os.path.exists(build_dir):
    shutil.rmtree(build_dir)
os.makedirs(unpacked_dir, exist_ok=True)
os.makedirs(stubs_dir, exist_ok=True)

with zipfile.ZipFile(jar_path, 'r') as z:
    z.extractall(unpacked_dir)

print("=== 2. Find and Compile Stubs & Src ===")
stubs_files = glob.glob(os.path.join(root, "stubs", "**", "*.java"), recursive=True)
src_files = glob.glob(os.path.join(root, "src", "**", "*.java"), recursive=True)

cmd_stubs = ["javac", "--release", "8", "-encoding", "UTF-8", "-d", stubs_dir] + stubs_files
subprocess.run(cmd_stubs, check=True)

cp_path = os.pathsep.join([unpacked_dir, stubs_dir])
cmd_src = ["javac", "--release", "8", "-encoding", "UTF-8", "-cp", cp_path, "-d", unpacked_dir] + src_files
subprocess.run(cmd_src, check=True)

print("=== 3. Run Idempotent Python Patches ===")
# 3a. Downgrade Java 8 class version (52.0 -> 45.3) for J2ME Loader compatibility
subprocess.run(["python", "scripts/patch_class_j2me.py", unpacked_dir], check=True)

# 3b. Fix EffectAuto array size 20 -> 100
effect_auto_class = os.path.join(unpacked_dir, "EffectAuto.class")
if os.path.exists(effect_auto_class):
    subprocess.run(["python", "scripts/patch_effectauto.py", effect_auto_class], check=True)

# 3c. Patch MotherCanvas for EcoMode (paint + touch wake up)
mothercanvas_class = os.path.join(unpacked_dir, "MotherCanvas.class")
if os.path.exists(mothercanvas_class):
    subprocess.run(["python", "scripts/patch_mothercanvas_eco.py", mothercanvas_class], check=True)

# 3d. Patch GameScr for watermark (Aeharuna -> NinjaNamod)
def patch_cp_utf8(class_path, old_bytes, new_bytes):
    data = bytearray(open(class_path, "rb").read())
    cp_count = struct.unpack(">H", data[8:10])[0]
    idx = 10
    target_offset = -1
    target_data_start = -1
    old_len = len(old_bytes)
    new_len = len(new_bytes)

    i = 1
    while i < cp_count:
        tag = data[idx]
        if tag == 1:
            length = struct.unpack(">H", data[idx+1:idx+3])[0]
            s = bytes(data[idx+3:idx+3+length])
            if s == old_bytes:
                target_offset = idx
                target_data_start = idx + 3
                break
            idx += 3 + length
        elif tag in (7, 8, 16, 19, 20): idx += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): idx += 5
        elif tag in (5, 6): idx += 9; i += 1
        elif tag == 15: idx += 4
        else: raise ValueError(f"Unknown tag {tag} at offset {idx}")
        i += 1

    if target_offset < 0:
        return False

    struct.pack_into(">H", data, target_offset + 1, new_len)
    new_data = data[:target_data_start] + new_bytes + data[target_data_start + old_len:]
    open(class_path, "wb").write(new_data)
    print(f"Patched {os.path.basename(class_path)}: {old_bytes} -> {new_bytes}")
    return True

gamescr_class = os.path.join(unpacked_dir, "GameScr.class")
if not patch_cp_utf8(gamescr_class, b"Aeharuna", b"NinjaNamod"):
    patch_cp_utf8(gamescr_class, b"nammod", b"NinjaNamod")

# 3e. Patch LoginScr (Aeharuna -> NinjaNamod)
loginscr_class = os.path.join(unpacked_dir, "LoginScr.class")
if os.path.exists(loginscr_class):
    for old_s in [b"NSO Aeharuna", b"SVJenny", b"Aeharuna"]:
        if patch_cp_utf8(loginscr_class, old_s, b"NinjaNamod"):
            break
    for old_s in [b"AEharuna ", b"SVJenny "]:
        if patch_cp_utf8(loginscr_class, old_s, b"NinjaNamod "):
            break
    for old_s in [b"Aeharuna1 ", b"SVJenny1 "]:
        if patch_cp_utf8(loginscr_class, old_s, b"NinjaNamod1 "):
            break

# 3f. Patch Server IP & Port (160.250.130.241:15555)
sys.path.insert(0, os.path.join(root, "scripts"))
import patch_server_ip
patch_server_ip.patch_dir(unpacked_dir, "160.250.130.241", 15555)

# Update MANIFEST.MF: Rename to NinjaNamod
manifest_file = os.path.join(unpacked_dir, "META-INF", "MANIFEST.MF")
manifest_content = """Manifest-Version: 1.0
MIDlet-1: NinjaNamod,/icon.png,GameMidlet
MIDlet-Vendor: NinjaNamod
MIDlet-Version: 1.4.8
MIDlet-Name: NinjaNamod
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.1

"""
with open(manifest_file, "w", encoding="utf-8") as f:
    f.write(manifest_content)

print("=== 4. Pack Aeharuna.jar ===")
for root_d, dirs_d, files_d in os.walk(unpacked_dir):
    for f in files_d:
        if f.endswith(".bak") or "bak_effects" in f:
            os.remove(os.path.join(root_d, f))

subprocess.run(["jar", "cfm", jar_path, manifest_file, "."], cwd=unpacked_dir, check=True)
print(f"=== SUCCESS! Aeharuna.jar created: {os.path.getsize(jar_path)} bytes ===")

ninjanamod_jar_path = os.path.join(root, "NinjaNamod.jar")
shutil.copyfile(jar_path, ninjanamod_jar_path)
print(f"=== Created copy: NinjaNamod.jar: {os.path.getsize(ninjanamod_jar_path)} bytes ===")

download_dir = "/storage/emulated/0/Download"
if os.path.exists(download_dir):
    shutil.copyfile(jar_path, os.path.join(download_dir, "Aeharuna.jar"))
    shutil.copyfile(jar_path, os.path.join(download_dir, "NinjaNamod.jar"))
    print(f"=== Copied to {download_dir}/Aeharuna.jar and NinjaNamod.jar ===")
