"""
patch_gamescr_chat.py — Patch GameScr.class:
  Code.gameAF(String)Z  →  ChatRouter.checkAll(String)Z

Thay Methodref cua invokestatic Code.gameAF(Ljava/lang/String;)Z
bang invokestatic ChatRouter.checkAll(Ljava/lang/String;)Z

Chi patch DUNG 1 call site (trong GameScr, noi xu ly chat input).
An toan chay lai nhieu lan (detect neu da patch).
"""
import struct, sys

def patch_gamescr_chat(class_file):
    with open(class_file, 'rb') as f:
        data = bytearray(f.read())

    if b"ChatRouter" in data:
        print("⚡ GameScr.class đã có ChatRouter, bỏ qua!")
        return

    magic, minor, major, cp_count = struct.unpack('>IHHH', data[:10])
    print(f"CP Count: {cp_count}")

    # === Them 6 entry moi vao constant pool ===
    idx_utf8_class = cp_count       # UTF8 "ChatRouter"
    idx_class = cp_count + 1        # Class ChatRouter
    idx_utf8_name = cp_count + 2    # UTF8 "checkAll"
    idx_utf8_desc = cp_count + 3    # UTF8 "(Ljava/lang/String;)Z"
    idx_nat = cp_count + 4          # NameAndType checkAll:(Ljava/lang/String;)Z
    idx_mr = cp_count + 5           # Methodref ChatRouter.checkAll
    new_cp_count = cp_count + 6

    new_cp_bytes = bytearray()

    # 1. UTF8 "ChatRouter"
    name_bytes = b"ChatRouter"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(name_bytes)))
    new_cp_bytes.extend(name_bytes)

    # 2. Class -> ChatRouter
    new_cp_bytes.append(7)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_class))

    # 3. UTF8 "checkAll"
    method_bytes = b"checkAll"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(method_bytes)))
    new_cp_bytes.extend(method_bytes)

    # 4. UTF8 "(Ljava/lang/String;)Z"
    desc_bytes = b"(Ljava/lang/String;)Z"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(desc_bytes)))
    new_cp_bytes.extend(desc_bytes)

    # 5. NameAndType checkAll:(Ljava/lang/String;)Z
    new_cp_bytes.append(12)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_name))
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_desc))

    # 6. Methodref ChatRouter.checkAll
    new_cp_bytes.append(10)
    new_cp_bytes.extend(struct.pack('>H', idx_class))
    new_cp_bytes.extend(struct.pack('>H', idx_nat))

    # === Parse constant pool de tim Code.gameAF(Ljava/lang/String;)Z ===
    pos = 10
    utf8_map = {}
    class_map = {}
    nat_map = {}
    mr_list = []

    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack('>H', data[pos:pos+2])[0]
            val = data[pos+2:pos+2+length].decode('latin1')
            utf8_map[i] = val
            pos += 2 + length
        elif tag == 7:
            name_idx = struct.unpack('>H', data[pos:pos+2])[0]
            class_map[i] = name_idx
            pos += 2
        elif tag == 12:
            name_idx, desc_idx = struct.unpack('>HH', data[pos:pos+4])
            nat_map[i] = (name_idx, desc_idx)
            pos += 4
        elif tag == 10:
            class_idx, nat_idx = struct.unpack('>HH', data[pos:pos+4])
            mr_list.append((i, class_idx, nat_idx))
            pos += 4
        elif tag in (3, 4, 9, 11):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag == 8:
            pos += 2
        elif tag == 15:  # MethodHandle
            pos += 3
        elif tag == 16:  # MethodType
            pos += 2
        elif tag == 18:  # InvokeDynamic
            pos += 4
        i += 1

    # Tim Methodref Code.gameAF(Ljava/lang/String;)Z
    target_mr_idx = -1
    for mr_idx, c_idx, nat_idx in mr_list:
        c_name_idx = class_map.get(c_idx)
        c_name = utf8_map.get(c_name_idx)
        if c_name == 'Code':
            n_idx, d_idx = nat_map.get(nat_idx, (0, 0))
            name = utf8_map.get(n_idx)
            desc = utf8_map.get(d_idx)
            if name == 'gameAF' and desc == '(Ljava/lang/String;)Z':
                target_mr_idx = mr_idx
                print(f"Found Code.gameAF(String)Z at CP#{mr_idx}")
                break

    if target_mr_idx == -1:
        print("❌ Khong tim thay Methodref Code.gameAF(Ljava/lang/String;)Z")
        return

    # === Insert CP entries ===
    cp_end_pos = pos
    data[cp_end_pos:cp_end_pos] = new_cp_bytes
    data[8:10] = struct.pack('>H', new_cp_count)

    # === Scan bytecode: thay invokestatic Code.gameAF → ChatRouter.checkAll ===
    pos = cp_end_pos + len(new_cp_bytes)

    # Skip access_flags, this_class, super_class, interfaces
    access_flags, this_class, super_class, interfaces_count = struct.unpack('>HHHH', data[pos:pos+8])
    pos += 8 + interfaces_count * 2

    # Skip fields
    fields_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(fields_count):
        pos += 6
        attr_count = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
        for _ in range(attr_count):
            attr_len = struct.unpack('>I', data[pos+2:pos+6])[0]
            pos += 6 + attr_len

    # Scan methods for invokestatic Code.gameAF
    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2

    # invokestatic = 0xb8, target_mr_idx
    target_bytes = bytearray([0xb8]) + struct.pack('>H', target_mr_idx)
    replace_bytes = bytearray([0xb8]) + struct.pack('>H', idx_mr)

    patched_count = 0
    for m in range(methods_count):
        m_flags, m_name_idx, m_desc_idx, m_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        pos += 8
        for _ in range(m_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            attr_name = utf8_map.get(attr_name_idx, '')

            if attr_name == 'Code':
                max_stack, max_locals, code_len = struct.unpack('>HHI', data[pos+6:pos+14])
                code_start = pos + 14
                code_bytes = data[code_start:code_start+code_len]

                new_code = code_bytes.replace(target_bytes, replace_bytes)
                if new_code != code_bytes:
                    count = code_bytes.count(target_bytes)
                    patched_count += count
                    data[code_start:code_start+code_len] = new_code
                    mname = utf8_map.get(m_name_idx, '?')
                    print(f"  Patched {count}x in method {mname}")

            pos += 6 + attr_len

    if patched_count == 0:
        print("⚠️ Khong tim thay invokestatic Code.gameAF trong bytecode!")
    else:
        print(f"✅ Patched {patched_count} call(s): Code.gameAF → ChatRouter.checkAll")

    with open(class_file, 'wb') as f:
        f.write(data)

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/unpacked/GameScr.class'
    patch_gamescr_chat(target)
