"""
patch_effectauto.py — Fix ArrayIndexOutOfBoundsException: length=20; index=40
in EffectAuto.class.

EffectAuto co mang arrEffAtutoTemplate = new EffAtutoTemp[20].
Server gui effect ID lon hon 20 (vd: 40) => crash lien tuc.
Patch: Tang kich thuoc mang tu 20 len 100.

Usage: python3 patch_effectauto.py build/unpacked/EffectAuto.class
"""
import sys, os

def patch_effectauto(class_path):
    with open(class_path, 'rb') as f:
        data = bytearray(f.read())

    # Tim bipush 20 (0x10 0x14) + anewarray (0xbd) 
    # Day la static initializer: new EffAtutoTemp[20]
    pattern_found = False
    for i in range(len(data) - 2):
        if data[i] == 0x10 and data[i+1] == 20 and data[i+2] == 0xbd:
            print(f"Found 'bipush 20 + anewarray' at offset {i}")
            # Thay bipush 20 (0x10 0x14) thanh bipush 100 (0x10 0x64)
            data[i+1] = 100  # 100 elements thay vi 20
            print(f"  Patched: bipush 20 -> bipush 100")
            pattern_found = True
            break

    if not pattern_found:
        # Kiem tra xem da patch chua
        for i in range(len(data) - 2):
            if data[i] == 0x10 and data[i+1] == 100 and data[i+2] == 0xbd:
                print("EffectAuto.class da duoc patch truoc do (size=100), bo qua!")
                return
        print("ERROR: Khong tim thay pattern 'bipush 20 + anewarray' trong EffectAuto.class!")
        sys.exit(1)

    with open(class_path, 'wb') as f:
        f.write(data)

    print(f"✅ Patched EffectAuto.class: arrEffAtutoTemplate[20] -> arrEffAtutoTemplate[100]")
    print(f"   Fix: ArrayIndexOutOfBoundsException length=20 index=40")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 patch_effectauto.py <path/to/EffectAuto.class>")
        sys.exit(1)
    patch_effectauto(sys.argv[1])
