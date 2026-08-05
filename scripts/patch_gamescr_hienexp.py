import struct
import sys

def patch_gamescr():
    filename = "build/unpacked/GameScr.class"
    with open(filename, "rb") as f:
        data = bytearray(f.read())

    # Read CP count
    cp_count = struct.unpack(">H", data[8:10])[0]
    print(f"Original CP Count: {cp_count}")

    # We need to add Constant Pool entries for ThongKe.paint:(LmGraphics;)V
    # 1. Utf8 "ThongKe"
    # 2. Class -> ThongKe
    # 3. Utf8 "paint"
    # 4. Utf8 "(LmGraphics;)V"
    # 5. NameAndType -> paint:(LmGraphics;)V
    # 6. Methodref -> ThongKe.paint:(LmGraphics;)V

    cp1_utf8_class = len("ThongKe").to_bytes(2, 'big') + b"ThongKe"
    entry1 = b"\x01" + cp1_utf8_class
    idx1 = cp_count

    entry2 = b"\x07" + idx1.to_bytes(2, 'big')
    idx2 = cp_count + 1

    cp3_utf8_name = len("paint").to_bytes(2, 'big') + b"paint"
    entry3 = b"\x01" + cp3_utf8_name
    idx3 = cp_count + 2

    cp4_utf8_sig = len("(LmGraphics;)V").to_bytes(2, 'big') + b"(LmGraphics;)V"
    entry4 = b"\x01" + cp4_utf8_sig
    idx4 = cp_count + 3

    entry5 = b"\x0c" + idx3.to_bytes(2, 'big') + idx4.to_bytes(2, 'big')
    idx5 = cp_count + 4

    entry6 = b"\x0a" + idx2.to_bytes(2, 'big') + idx5.to_bytes(2, 'big')
    methodref_idx = cp_count + 5

    print(f"New Methodref Index for ThongKe.paint: {methodref_idx}")

    # Find position where CP ends
    pos = 10
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack(">H", data[pos:pos+2])[0]
            pos += 2 + length
        elif tag in (3, 4): pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1 # Long and Double take two CP entries!
        elif tag in (7, 8, 16): pos += 2
        elif tag in (9, 10, 11, 12, 18): pos += 4
        elif tag == 15: pos += 3
        else:
            print(f"Unknown tag {tag} at cp {i} pos {pos}")
            return False
        i += 1

    cp_end_pos = pos
    print(f"CP End Position: {cp_end_pos}")

    new_entries = entry1 + entry2 + entry3 + entry4 + entry5 + entry6
    new_data = bytearray()
    new_data.extend(data[:8])
    new_data.extend((cp_count + 6).to_bytes(2, 'big'))
    new_data.extend(data[10:cp_end_pos])
    new_data.extend(new_entries)
    new_data.extend(data[cp_end_pos:])

    # Find the target location of hienexp check in bytecode
    # Pattern for getstatic SetAuto.hienexp (b2 0d 7c 99 00 a0)
    target_pattern = b"\xb2\x0d\x7c\x99\x00\xa0"
    target_pos = new_data.find(target_pattern)
    if target_pos == -1:
        # Retry finding without fixed offset
        print("Searching for b2 ?? ?? 99 00 a0...")
        import re
        m = re.search(rb"\xb2..\x99\x00\xa0\xb2", new_data)
        if m:
            target_pos = m.start()

    if target_pos == -1:
        print("Could not find SetAuto.hienexp bytecode pattern!")
        return False

    print(f"Found target bytecode pattern at offset: {target_pos}")

    # Patch instructions:
    # 0: b2 0d 7c (getstatic SetAuto.hienexp:Z)
    # 3: 99 00 0b (ifeq +11 -> jump past our call if false)
    # 6: 2b       (aload_1 -> mGraphics g)
    # 7: b8 xx xx (invokestatic ThongKe.paint:(LmGraphics;)V)
    # 10: a7 00 a0 (goto +160 -> jump past all original 2-line rendering code)

    goto_offset = 0x00a0 - 13 # 0x00a0 is the original ifeq jump distance
    patch_bytes = bytearray()
    patch_bytes.extend(b"\xb2\x0d\x7c") # getstatic SetAuto.hienexp:Z
    patch_bytes.extend(b"\x99\x00\x0b") # ifeq +11
    patch_bytes.extend(b"\x2b")         # aload_1 (mGraphics g)
    patch_bytes.extend(b"\xb8" + methodref_idx.to_bytes(2, 'big')) # invokestatic ThongKe.paint
    patch_bytes.extend(b"\xa7" + goto_offset.to_bytes(2, 'big'))    # goto +147

    # Fill remaining bytes up to 6 + 160 with NOP (0x00)
    total_orig_len = 6 + 0x00a0
    patch_len = len(patch_bytes)
    nop_len = total_orig_len - patch_len
    patch_bytes.extend(b"\x00" * nop_len)

    new_data[target_pos:target_pos + total_orig_len] = patch_bytes
    print("Successfully patched GameScr.class!")

    with open(filename, "wb") as f:
        f.write(new_data)
    return True

if __name__ == "__main__":
    success = patch_gamescr()
    if not success:
        sys.exit(1)
