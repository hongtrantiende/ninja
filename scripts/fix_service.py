"""
Fix Service.class: Remove broken SplitPatcher.checkSplit hook.
The old patch inserted 9 bytes at the start of gameAA(short,String)
with a wrong ifeq offset, causing stack mismatch -> game freeze.

Fix: Replace the 9 injected bytes with NOPs (0x00) to neutralize the patch.
"""
import struct, sys

def fix_service(class_file):
    with open(class_file, 'rb') as f:
        data = bytearray(f.read())

    # Parse CP to find method gameAA(S, String)
    magic, minor, major, cp_count = struct.unpack('>IHHH', data[:10])
    
    pos = 10
    utf8_map = {}
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack('>H', data[pos:pos+2])[0]
            val = data[pos+2:pos+2+length].decode('latin1')
            utf8_map[i] = val
            pos += 2 + length
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag in (7, 8):
            pos += 2
        elif tag in (9, 10, 11, 12):
            pos += 4
        i += 1

    cp_end_pos = pos

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

    # Parse methods
    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2

    fixed = False
    for m in range(methods_count):
        m_flags, m_name_idx, m_desc_idx, m_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        name = utf8_map.get(m_name_idx, '')
        desc = utf8_map.get(m_desc_idx, '')
        pos += 8
        for _ in range(m_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            attr_name = utf8_map.get(attr_name_idx, '')

            if name == 'gameAA' and desc == '(SLjava/lang/String;)V' and attr_name == 'Code':
                max_stack, max_locals, code_len = struct.unpack('>HHI', data[pos+6:pos+14])
                code_start = pos + 14
                
                # Verify: first 9 bytes should be our patch
                # iload_1(0x1b) aload_2(0x2c) invokestatic(0xb8 XX XX) ifeq(0x99 00 05) return(0xb1)
                if data[code_start] == 0x1b and data[code_start+1] == 0x2c and data[code_start+2] == 0xb8:
                    print(f"Found patched gameAA(short, String) at code offset {code_start}")
                    print(f"  Original 9 bytes: {' '.join(f'{b:02x}' for b in data[code_start:code_start+9])}")
                    
                    # Replace 9 bytes with NOPs (0x00 = nop in JVM)
                    for k in range(9):
                        data[code_start + k] = 0x00  # nop
                    
                    print(f"  Replaced with:    {' '.join(f'{b:02x}' for b in data[code_start:code_start+9])}")
                    
                    # Also fix exception table: revert offsets -9
                    exc_table_pos = code_start + code_len
                    exc_count = struct.unpack('>H', data[exc_table_pos:exc_table_pos+2])[0]
                    exc_pos = exc_table_pos + 2
                    print(f"  Exception table ({exc_count} entries):")
                    for ei in range(exc_count):
                        start_pc, end_pc, handler_pc, catch_type = struct.unpack('>HHHH', data[exc_pos:exc_pos+8])
                        print(f"    [{ei}] from={start_pc} to={end_pc} handler={handler_pc}")
                        exc_pos += 8
                    
                    fixed = True
                    print("[OK] Fixed! 9 bytes replaced with NOPs.")
                else:
                    print(f"Method gameAA(short, String) found but first bytes don't match patch pattern:")
                    print(f"  {' '.join(f'{b:02x}' for b in data[code_start:code_start+9])}")
                    
            pos += 6 + attr_len
        if fixed:
            break

    if fixed:
        with open(class_file, 'wb') as f:
            f.write(data)
        print(f"[OK] Saved {class_file}")
    else:
        print("[FAIL] Could not find or fix the patched method!")

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/unpacked/Service.class'
    fix_service(target)
