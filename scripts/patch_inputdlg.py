import struct, sys, os

def patch_inputdlg(class_file):
    with open(class_file, 'rb') as f:
        data = bytearray(f.read())

    if b"checkInputDlg" in data:
        print("⚡ InputDlg.class đã được patch với SplitPatcher trước đó, bỏ qua!")
        return
        
    magic, minor, major, cp_count = struct.unpack('>IHHH', data[:10])
    
    idx_utf8_class = cp_count
    idx_class = cp_count + 1
    
    idx_utf8_name = cp_count + 2
    idx_utf8_desc = cp_count + 3
    idx_nat = cp_count + 4
    idx_mr = cp_count + 5
    
    new_cp_count = cp_count + 6
    
    new_cp_bytes = bytearray()
    
    # 1. Utf8 "SplitPatcher"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len("SplitPatcher")))
    new_cp_bytes.extend("SplitPatcher".encode('latin1'))
    
    # 2. Class
    new_cp_bytes.append(7)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_class))
    
    # 3. Utf8 "checkInputDlg"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len("checkInputDlg")))
    new_cp_bytes.extend("checkInputDlg".encode('latin1'))
    
    # 4. Utf8 "(Ljava/lang/String;LCommand;)Z"
    desc_str = "(Ljava/lang/String;LCommand;)Z"
    new_cp_bytes.append(1)
    new_cp_bytes.extend(struct.pack('>H', len(desc_str)))
    new_cp_bytes.extend(desc_str.encode('latin1'))
    
    # 5. NAT
    new_cp_bytes.append(12)
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_name))
    new_cp_bytes.extend(struct.pack('>H', idx_utf8_desc))
    
    # 6. Methodref
    new_cp_bytes.append(10)
    new_cp_bytes.extend(struct.pack('>H', idx_class))
    new_cp_bytes.extend(struct.pack('>H', idx_nat))
    
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
    data[cp_end_pos:cp_end_pos] = new_cp_bytes
    data[8:10] = struct.pack('>H', new_cp_count)
    
    pos = cp_end_pos + len(new_cp_bytes)
    
    access_flags, this_class, super_class, interfaces_count = struct.unpack('>HHHH', data[pos:pos+8])
    pos += 8 + interfaces_count * 2
    
    fields_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for _ in range(fields_count):
        f_flags, f_name_idx, f_desc_idx, f_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        pos += 8
        for _ in range(f_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            pos += 6 + attr_len
            
    methods_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    
    patched_count = 0
    for m in range(methods_count):
        m_flags, m_name_idx, m_desc_idx, m_attr_count = struct.unpack('>HHHH', data[pos:pos+8])
        name = utf8_map.get(m_name_idx, '')
        desc = utf8_map.get(m_desc_idx, '')
        pos += 8
        for _ in range(m_attr_count):
            attr_name_idx, attr_len = struct.unpack('>HI', data[pos:pos+6])
            attr_name = utf8_map.get(attr_name_idx, '')
            
            if name == 'gameAA' and desc == '(Ljava/lang/String;LCommand;I)V' and attr_name == 'Code':
                max_stack, max_locals, code_len = struct.unpack('>HHI', data[pos+6:pos+14])
                
                bc = bytearray()
                bc.append(0x2b) # aload_1 (String)
                bc.append(0x2c) # aload_2 (Command)
                bc.append(0xb8) # invokestatic
                bc.extend(struct.pack('>H', idx_mr))
                bc.extend([0x99, 0x00, 0x04]) # ifeq +4
                bc.append(0xb1) # return
                
                patch_len = len(bc) # 9 bytes
                data[pos+2:pos+6] = struct.pack('>I', attr_len + patch_len)
                data[pos+10:pos+14] = struct.pack('>I', code_len + patch_len)
                code_start = pos + 14
                data[code_start:code_start] = bc
                
                exc_start = code_start + patch_len + code_len
                exc_len = struct.unpack('>H', data[exc_start:exc_start+2])[0]
                exc_pos = exc_start + 2
                for _ in range(exc_len):
                    spc, epc, hpc, ctype = struct.unpack('>HHHH', data[exc_pos:exc_pos+8])
                    struct.pack_into('>HHHH', data, exc_pos, spc + patch_len, epc + patch_len, hpc + patch_len, ctype)
                    exc_pos += 8
                patched_count += 1
                pos += patch_len

            pos += 6 + attr_len
            
    print(f"✅ Patched {patched_count} InputDlg methods successfully!")
    with open(class_file, 'wb') as f:
        f.write(data)

if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/unpacked/InputDlg.class'
    patch_inputdlg(target)
