#!/usr/bin/env bash
set -e

JAR_FILE="${1:-Aeharuna.jar}"
BUILD_DIR="/root/ninja/build/unpacked"

if [ ! -f "/root/ninja/$JAR_FILE" ] && [ ! -f "$JAR_FILE" ]; then
    echo "Lỗi: Không tìm thấy file $JAR_FILE"
    exit 1
fi

TARGET_JAR="/root/ninja/$JAR_FILE"
[ -f "$JAR_FILE" ] && TARGET_JAR="$JAR_FILE"

echo "📦 Đang giải nén $TARGET_JAR vào $BUILD_DIR..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
unzip -q "$TARGET_JAR" -d "$BUILD_DIR"

# Đảm bảo MANIFEST.MF luôn tồn tại
if [ ! -f "$BUILD_DIR/META-INF/MANIFEST.MF" ]; then
    if [ -f "/root/Aeharuna.jar" ]; then
        unzip -q -o "/root/Aeharuna.jar" "META-INF/MANIFEST.MF" -d "$BUILD_DIR" 2>/dev/null || true
    fi
    if [ ! -f "$BUILD_DIR/META-INF/MANIFEST.MF" ]; then
        mkdir -p "$BUILD_DIR/META-INF"
        cat << 'EOF' > "$BUILD_DIR/META-INF/MANIFEST.MF"
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
fi

echo "✅ Giải nén thành công! Kích thước file giải nén:"
du -sh "$BUILD_DIR"

