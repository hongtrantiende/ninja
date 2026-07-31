import zipfile, os, sys

def pack_jar(output_name="Aeharuna.jar"):
    build_dir = "/root/ninja/build/unpacked"
    output_path = os.path.join("/root/ninja", output_name)
    
    # 1. Patch GameScr.class bang patch_gamescr_menu.py
    gamescr_class = os.path.join(build_dir, "GameScr.class")
    if os.path.exists(gamescr_class):
        try:
            import patch_gamescr_menu
            patch_gamescr_menu.patch_gamescr(gamescr_class)
        except Exception as e:
            print(f"⚠️ Lỗi khi patch GameScr.class: {e}")

    print(f"⚙️ Đang đóng gói file JAR: {output_path}...")
    if os.path.exists(output_path):
        os.remove(output_path)
        
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        manifest_path = os.path.join(build_dir, 'META-INF', 'MANIFEST.MF')
        if os.path.exists(manifest_path):
            zf.write(manifest_path, 'META-INF/MANIFEST.MF')
        for root, dirs, files in os.walk(build_dir):
            for f in files:
                rel = os.path.relpath(os.path.join(root, f), build_dir)
                if rel != 'META-INF/MANIFEST.MF':
                    zf.write(os.path.join(root, f), rel)
                    
    print(f"✅ Đóng gói thành công {output_path}! (Kích thước: {os.path.getsize(output_path)} bytes)")
    
    # Đồng bộ sang các thư mục xuất bản
    for sync_dir in ["/storage/emulated/0/Download/Extransion-TTC", "/root/Extransion-TTC"]:
        if os.path.exists(sync_dir):
            target = os.path.join(sync_dir, output_name)
            with open(output_path, 'rb') as src_f, open(target, 'wb') as dst_f:
                dst_f.write(src_f.read())
            print(f"🔄 Đã đồng bộ sang {target}")

if __name__ == "__main__":
    out_file = sys.argv[1] if len(sys.argv) > 1 else "Aeharuna.jar"
    pack_jar(out_file)
