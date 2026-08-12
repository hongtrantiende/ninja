import os
import sys
import glob
import shutil
import zipfile
import subprocess

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

cmd_src = ["javac", "--release", "8", "-encoding", "UTF-8", "-cp", f"{unpacked_dir};{stubs_dir}", "-d", unpacked_dir] + src_files
subprocess.run(cmd_src, check=True)

print("=== 3. Run Idempotent Python Patches ===")
# 3a. Downgrade Java 8 class version (52.0 -> 45.3) for J2ME Loader compatibility
subprocess.run(["python", "scripts/patch_class_j2me.py", unpacked_dir], check=True)

# 3b. Fix EffectAuto array size 20 -> 100
effect_auto_class = os.path.join(unpacked_dir, "EffectAuto.class")
if os.path.exists(effect_auto_class):
    subprocess.run(["python", "scripts/patch_effectauto.py", effect_auto_class], check=True)

print("=== 4. Pack Aeharuna.jar ===")
for root_d, dirs_d, files_d in os.walk(unpacked_dir):
    for f in files_d:
        if f.endswith(".bak") or "bak_effects" in f:
            os.remove(os.path.join(root_d, f))

manifest_file = os.path.join(unpacked_dir, "META-INF", "MANIFEST.MF")
subprocess.run(["jar", "cfm", jar_path, manifest_file, "."], cwd=unpacked_dir, check=True)

print(f"=== SUCCESS! Aeharuna.jar created: {os.path.getsize(jar_path)} bytes ===")
