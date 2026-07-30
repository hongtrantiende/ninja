import struct, sys, os

def patch_service(class_file):
    with open(class_file, 'rb') as f:
        data = bytearray(f.read())
        
    magic, minor, major, cp_count = struct.unpack('>IHHH', data[:10])
    print(f"Original CP Count: {cp_count}")
    
    # We will add 6 CP entries:
    # 1. Utf8 "SplitPatcher" -> cp_count
    # 2. Class -> cp_count + 1
    # 3. Utf8 "checkSplit" -> cp_count + 2
    # 4. Utf8 "(SLjava/lang/String;)Z" -> cp_count + 3
    # 5. NameAndType -> cp_count + 4
    # 6. Methodref -> cp_count + 5
    
    idx_utf8_class = cp_count
    idx_class = cp_count + 1
    idx_utf8_name = cp_count + 2
    idx_utf8_desc = cp_count + 3
    idx_nat = cp_count + 4
    idx_methodref = cp_count + 5
    new_cp_count = cp_count + 6
    
    # Build new CP bytes
    new_cp_bytes = bytearray()
    
    # 1. Utf8 "SplitPatcher"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len("SplitPatcher")))
    new_cp_bytes.extend("SplitPatcher".encode('latin1'))
    
    # 2. Class -> idx_utf8_class
    new_cp_bytes.append(7)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_class))
    
    # 3. Utf8 "checkSplit"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len("checkSplit")))
    new_cp_bytes.extend("checkSplit".encode('latin1'))
    
    # 4. Utf8 "(SLjava/lang/String;)Z"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len("(SLjava/lang/String;)Z")))
    new_cp_bytes.extend("(SLjava/lang/String;)Z".encode('latin1'))
    
    # 5. NameAndType -> idx_utf8_name, idx_utf8_desc
    new_cp_bytes.append(12)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_name))
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_desc))
    
    # 6. Methodref -> idx_class, idx_nat
    new_cp_bytes.append(10)
    new_cp_bytes.extend(struct.pack('>H', idx_class))
    new_cp_bytes.extend(struct.pack('>H', idx_nat))
    
    # Find CP end pos
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
    print(f"CP End Pos: {cp_end_pos}")
    
    # Insert new CP bytes at cp_end_pos
    data[cp_end_pos:cp_end_pos] = new_cp_bytes
    # Update CP count in header
    data[8:10] = struct.pack('>H', new_cp_count)
    
    # Shift pos after CP insert
    pos = cp_end_pos + len(new_cp_bytes)
    
    # Parse interfaces
    access_flags, this_class, super_class, interfaces_count = struct.unpack('>HHHH', data[pos:pos+8])
    pos += 8 + interfaces_count * 2
    
    # Parse fields
    fields_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(fields_count):
        f_flags, f_name_idx, f_desc_idx, f_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        pos += 8
        for _ in range(f_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            pos += 6 + attr_len
            
    # Parse methods
    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    
    patched = False
    for m in range(methods_count):
        m_flags, m_name_idx, m_desc_idx, m_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        name = utf8_map.get(m_name_idx, '')
        desc = utf8_map.get(m_desc_idx, '')
        pos += 8
        for _ in range(m_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            attr_name = utf8_map.get(attr_name_idx, '')
            if name == 'gameAA' and desc == '(SLjava/lang/String;)V' and attr_name == 'Code':
                print(f"Patching method gameAA(short, String) at pos {pos}...")
                
                # Code attribute format:
                # attr_name_idx (2), attr_len (4), max_stack (2), max_locals (2), code_len (4), code (...), exception_table_len (2), ...
                max_stack, max_locals, code_len = struct.unpack('>HHI', data[pos+6:pos+14])
                
                # New bytecode:
                # iload_1 (0x1a)
                # aload_2 (0x2c)
                # invokestatic idx_methodref (0xb8 MSB LSB)
                # ifeq +5 (0x99 0x00 0x05)
                # return (0xb1)
                
                new_bytecode = bytearray()
                new_bytecode.append(0x1b) # iload_1 (first parameter: short id)
                new_bytecode.append(0x2c) # aload_2 (second parameter: String quantity)
                new_bytecode.append(0xb8) # invokestatic
                new_bytecode.extend(struct.pack('>H', idx_methodref))
                new_bytecode.extend([0x99, 0x00, 0x05]) # ifeq +5
                new_bytecode.append(0xb1) # return
                
                patch_len = len(new_bytecode) # 9 bytes
                
                # Update Code attribute len (+9)
                data[pos+2:pos+6] = struct.pack('>I', attr_len + patch_len)
                # Update code_len (+9)
                data[pos+10:pos+14] = struct.pack('>I', code_len + patch_len)
                
                # Insert bytecode at beginning of code array (pos+14)
                code_start_pos = pos + 14
                data[code_start_pos:code_start_pos] = new_bytecode
                
                # Fix exception table offsets
                exc_table_start = code_start_pos + patch_len + code_len
                exc_table_len = struct.unpack('>H', data[exc_table_start:exc_table_start+2])[0]
                exc_pos = exc_table_start + 2
                for _ in range(exc_table_len):
                    start_pc, end_pc, handler_pc, catch_type = struct.unpack('>HHHH', data[exc_pos:exc_pos+8])
                    struct.pack_into('>HHHH', data, exc_pos, start_pc + patch_len, end_pc + patch_len, handler_pc + patch_len, catch_type)
                    exc_pos += 8
                    
                patched = True
                print("✅ Successfully patched gameAA(short, String)!")
                break
            pos += 6 + attr_len
        if patched:
            break
            
    if not patched:
        print("❌ Could not find gameAA(short, String) method to patch!")
        sys.exit(1)
        
    with open(class_file, 'wb') as f:
        f.write(data)

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/unpacked/Service.class'
    patch_service(target)
