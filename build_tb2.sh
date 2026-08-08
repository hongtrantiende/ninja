#!/bin/bash
set -euo pipefail

ROOT="/root/ninja"
BASE="$ROOT/archive_tb2/tb2_step1_base"
SRC="$ROOT/archive_tb2/tb2_step1_src"
TOOLS_SRC="$ROOT/archive_tb2/tb2_step1_tools"
CLASSES="$ROOT/archive_tb2/tb2_step1_classes"
PATCHED="$ROOT/archive_tb2/tb2_step1_patched"
STUBS="$ROOT/stubs"
JAVASSIST="$ROOT/tools/javassist.jar"
STUBS_OUT="$ROOT/build/stubs_compiled"

echo "=== [1/5] Compile J2ME stubs ==="
rm -rf "$STUBS_OUT"
mkdir -p "$STUBS_OUT"
find "$STUBS" -name "*.java" | xargs javac --release 8 -encoding UTF-8 -d "$STUBS_OUT"
echo "    OK: stubs compiled"

echo "=== [2/5] Compile TB2 source files ==="
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
javac --release 8 -encoding UTF-8 \
  -cp "$BASE:$STUBS_OUT" \
  -d "$CLASSES" \
  "$SRC"/*.java
echo "    OK: $(ls "$CLASSES"/*.class | wc -l) class files"

echo "=== [3/5] Compile PatchMenuStep1 ==="
javac --release 8 -encoding UTF-8 \
  -cp "$JAVASSIST:$BASE:$CLASSES" \
  -d "$TOOLS_SRC" \
  "$TOOLS_SRC/PatchMenuStep1.java"
echo "    OK: patcher compiled"

echo "=== [4/5] Run patcher (Javassist) ==="
rm -rf "$PATCHED"
mkdir -p "$PATCHED"
java -cp "$JAVASSIST:$TOOLS_SRC:$CLASSES:$BASE" PatchMenuStep1 \
  "$BASE" "$CLASSES" "$PATCHED"
echo "    OK: patched classes:"
ls "$PATCHED"/*.class 2>/dev/null || echo "    (none in root)"
ls "$PATCHED"/ 

echo "=== [5/5] Pack JAR ==="
python3 "$ROOT/archive_tb2/pack_tb2_build.py"

echo ""
echo "=== BUILD COMPLETE ==="
