#!/usr/bin/env python3
import sys
import os

def search_strings(target_dir, keyword):
    print(f"🔍 Đang tìm kiếm chuỗi '{keyword}' trong {target_dir}...")
    matches = 0
    keyword_bytes = keyword.encode('utf-8')
    for root, _, files in os.walk(target_dir):
        for f in files:
            if f.endswith('.class') or f.endswith('.txt') or f.endswith('.bin'):
                filepath = os.path.join(root, f)
                try:
                    with open(filepath, 'rb') as file:
                        content = file.read()
                        if keyword_bytes in content:
                            print(f"  [FOUND] {filepath}")
                            matches += 1
                except Exception as e:
                    pass
    print(f"✨ Hoàn tất. Tìm thấy tại {matches} tệp.")

def replace_string(filepath, old_str, new_str):
    if len(old_str) != len(new_str):
        print(f"⚠️ Cảnh báo: Độ dài chuỗi mới ('{new_str}') khác độ dài chuỗi cũ ('{old_str}'). Hãy cẩn thận với hằng số bytecode.")
    old_bytes = old_str.encode('utf-8')
    new_bytes = new_str.encode('utf-8')
    with open(filepath, 'rb') as f:
        data = f.read()
    if old_bytes not in data:
        print(f"❌ Không tìm thấy chuỗi '{old_str}' trong {filepath}")
        return
    new_data = data.replace(old_bytes, new_bytes)
    with open(filepath, 'wb') as f:
        f.write(new_data)
    print(f"✅ Đã thay thế thành công '{old_str}' -> '{new_str}' trong {filepath}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Sử dụng:")
        print("  python3 patch_string.py search <keyword> <directory>")
        print("  python3 patch_string.py replace <old_str> <new_str> <file>")
        sys.exit(1)
    
    cmd = sys.argv[1]
    if cmd == "search":
        search_strings(sys.argv[3] if len(sys.argv) > 3 else ".", sys.argv[2])
    elif cmd == "replace" and len(sys.argv) >= 4:
        replace_string(sys.argv[4], sys.argv[2], sys.argv[3])
