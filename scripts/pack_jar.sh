#!/usr/bin/env bash
set -e

OUTPUT_NAME="${1:-Aeharuna_modded.jar}"
BUILD_DIR="/root/ninja/build/unpacked"
OUTPUT_PATH="/root/ninja/$OUTPUT_NAME"

if [ ! -d "$BUILD_DIR" ]; then
    echo "Lỗi: Chưa có thư mục giải nén $BUILD_DIR. Hãy chạy ./scripts/unpack_jar.sh trước."
    exit 1
fi

# Đảm bảo META-INF/MANIFEST.MF luôn tồn tại
MANIFEST_FILE="$BUILD_DIR/META-INF/MANIFEST.MF"
if [ ! -f "$MANIFEST_FILE" ]; then
    echo "⚠️ Thiếu META-INF/MANIFEST.MF, đang tự động tạo file MANIFEST chuẩn..."
    mkdir -p "$BUILD_DIR/META-INF"
    cat << 'EOF' > "$MANIFEST_FILE"
Manifest-Version: 1.0
Ant-Version: Apache Ant 1.9.7
Created-By: 1.8.0_202-b08 (Oracle Corporation)
MIDlet-1: Aeharuna ,/icon.png,GameMidlet
MIDlet-Vendor: Aeharuna
MIDlet-Version: 1.4.8
MIDlet-Name: Aeharuna
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.1

EOF
fi

echo "⚙️ Đang đóng gói lại file JAR: $OUTPUT_PATH..."
rm -f "$OUTPUT_PATH"

cd "$BUILD_DIR"
if command -v jar >/dev/null 2>&1; then
    jar cfm "$OUTPUT_PATH" META-INF/MANIFEST.MF .
elif command -v zip >/dev/null 2>&1; then
    zip -q -0 -r "$OUTPUT_PATH" META-INF/MANIFEST.MF . -x "META-INF/MANIFEST.MF"
else
    echo "ℹ️ zip/jar binary chưa có, sử dụng Python zipfile engine..."
    python3 -c "
import zipfile, os

build_dir = '$BUILD_DIR'
output_path = '$OUTPUT_PATH'

with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    manifest_path = os.path.join(build_dir, 'META-INF', 'MANIFEST.MF')
    if os.path.exists(manifest_path):
        zf.write(manifest_path, 'META-INF/MANIFEST.MF')
    for root, dirs, files in os.walk(build_dir):
        for f in files:
            rel = os.path.relpath(os.path.join(root, f), build_dir)
            if rel != 'META-INF/MANIFEST.MF':
                zf.write(os.path.join(root, f), rel)
"
fi

echo "✅ Đóng gói thành công $OUTPUT_PATH!"
ls -lh "$OUTPUT_PATH"

# Tự động đồng bộ sang thư mục Download/Extransion-TTC
for SYNC_DIR in "/storage/emulated/0/Download/Extransion-TTC" "/root/Extransion-TTC"; do
    if [ -d "$SYNC_DIR" ]; then
        cp "$OUTPUT_PATH" "$SYNC_DIR/$OUTPUT_NAME"
        echo "🔄 Đã đồng bộ sang $SYNC_DIR/$OUTPUT_NAME"
    fi
done

