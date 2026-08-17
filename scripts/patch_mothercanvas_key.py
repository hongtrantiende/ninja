"""Patch MotherCanvas.class: hook ShortcutHandler.handleKey vao keyPressed.

Thay invokevirtual GameGraphics.gameAA(I)V
bang invokestatic ShortcutHandler.handleKey(LGameGraphics;I)V

Chi patch 1 call site (trong keyPressed).
"""
import struct, sys

def patch_mothercanvas(class_file):
    with open(class_file, 'rb') as f:
        data = bytearray(f.read())

    if b"ShortcutHandler" in data:
        print("Already patched!")
        return

    magic, minor, major, cp_count = struct.unpack('>IHHH', data[:10])
    print(f"CP Count: {cp_count}")

    # Them 6 entries moi
    idx_utf8_class = cp_count
    idx_class = cp_count + 1
    idx_utf8_name = cp_count + 2
    idx_utf8_desc = cp_count + 3
    idx_nat = cp_count + 4
    idx_mr = cp_count + 5
    new_cp_count = cp_count + 6

    new_cp_bytes = bytearray()

    # 1. UTF8 "ShortcutHandler"
    name = b"ShortcutHandler"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(name)))
    new_cp_bytes.extend(name)

    # 2. Class
    new_cp_bytes.append(7)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_class))

    # 3. UTF8 "handleKey"
    method = b"handleKey"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(method)))
    new_cp_bytes.extend(method)

    # 4. UTF8 "(LGameGraphics;I)V"
    desc = b"(LGameGraphics;I)V"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(desc)))
    new_cp_bytes.extend(desc)

    # 5. NameAndType
    new_cp_bytes.append(12)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_name))
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_desc))

    # 6. Methodref
    new_cp_bytes.append(10)
    new_cp_bytes.extend(struct.pack('>H', idx_class))
    new_cp_bytes.extend(struct.pack('>H', idx_nat))

    # Parse CP
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
        elif tag == 15:
            pos += 3
        elif tag == 16:
            pos += 2
        elif tag == 18:
            pos += 4
        i += 1

    # Tim GameGraphics.gameAA(I)V
    target_mr_idx = -1
    for mr_idx, c_idx, nat_idx in mr_list:
        c_name_idx = class_map.get(c_idx)
        c_name = utf8_map.get(c_name_idx)
        if c_name == 'GameGraphics':
            n_idx, d_idx = nat_map.get(nat_idx, (0, 0))
            name = utf8_map.get(n_idx)
            desc = utf8_map.get(d_idx)
            if name == 'gameAA' and desc == '(I)V':
                target_mr_idx = mr_idx
                print(f"Found GameGraphics.gameAA(I)V at CP#{mr_idx}")
                break

    if target_mr_idx == -1:
        print("ERROR: GameGraphics.gameAA(I)V not found")
        return

    # Insert CP entries
    cp_end_pos = pos
    data[cp_end_pos:cp_end_pos] = new_cp_bytes
    data[8:10] = struct.pack('>H', new_cp_count)

    # Scan bytecode - replace ONLY in keyPressed method
    pos = cp_end_pos + len(new_cp_bytes)
    access_flags, this_class, super_class, interfaces_count = struct.unpack('>HHHH', data[pos:pos+8])
    pos += 8 + interfaces_count * 2

    fields_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(fields_count):
        pos += 6
        attr_count = struct.unpack('>H', data[pos:pos+2])[0]
        pos += 2
        for _ in range(attr_count):
            attr_len = struct.unpack('>I', data[pos+2:pos+6])[0]
            pos += 6 + attr_len

    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2

    # invokevirtual GameGraphics.gameAA(I)V -> invokestatic ShortcutHandler.handleKey
    target_bytes = bytearray([0xb6]) + struct.pack('>H', target_mr_idx)
    replace_bytes = bytearray([0xb8]) + struct.pack('>H', idx_mr)

    patched_count = 0
    for m in range(methods_count):
        m_flags, m_name_idx, m_desc_idx, m_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        pos += 8
        mname = utf8_map.get(m_name_idx, '?')
        mdesc = utf8_map.get(m_desc_idx, '?')

        for _ in range(m_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            attr_name = utf8_map.get(attr_name_idx, '')

            if attr_name == 'Code' and mname == 'keyPressed':
                max_stack, max_locals, code_len = struct.unpack('>HHI', data[pos+6:pos+14])
                code_start = pos + 14
                code_bytes = data[code_start:code_start+code_len]

                new_code = code_bytes.replace(target_bytes, replace_bytes, 1)
                if new_code != code_bytes:
                    patched_count += 1
                    data[code_start:code_start+code_len] = new_code
                    print(f"  Patched keyPressed: GameGraphics.gameAA -> ShortcutHandler.handleKey")

            pos += 6 + attr_len

    if patched_count == 0:
        print("WARNING: No calls patched in keyPressed!")
    else:
        print(f"OK: Patched {patched_count} call(s)")

    with open(class_file, 'wb') as f:
        f.write(data)

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/nso148_unpacked/MotherCanvas.class'
    patch_mothercanvas(target)
