#!/usr/bin/env python3
"""
patch_controller_globalchat.py — Inject AutoBossNotice.onReceiveMessage()
vào Controller.class tại 2 đoạn xử lý GLOBALCHAT.

Đoạn 1 (thông báo Server): Sau offset 1014 (ChatManager.gameAA), trước 1017 (Code.gameAB check)
  → inject: aload_2; invokestatic AutoBossNotice.onReceiveMessage(String)V

Đoạn 2 (chat người chơi kênh TG): Sau offset 1656 (ChatManager.gameAA), trước 1659 (blockGlobalChat)
  → inject: aload 25; invokestatic AutoBossNotice.onReceiveMessage(String)V

Strategy: Tìm bytecode pattern, thêm CP entry cho AutoBossNotice, chèn 2 lệnh invoke.
"""
import struct, sys, os

def read_u2(data, off): return struct.unpack('>H', data[off:off+2])[0]
def read_u4(data, off): return struct.unpack('>I', data[off:off+4])[0]
def write_u2(data, off, val):
    data[off:off+2] = struct.pack('>H', val)
def write_u4(data, off, val):
    data[off:off+4] = struct.pack('>I', val)

def find_cp_end(data):
    """Tìm vị trí kết thúc Constant Pool"""
    cp_count = read_u2(data, 8)
    off = 10
    i = 1
    while i < cp_count:
        tag = data[off]
        if tag == 1:  # CONSTANT_Utf8
            length = read_u2(data, off + 1)
            off += 3 + length
        elif tag in (7, 8, 16, 19, 20):  # Class, String, MethodType, Module, Package
            off += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            # Integer, Float, Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
            off += 5
        elif tag in (5, 6):  # Long, Double
            off += 9
            i += 1  # takes 2 slots
        elif tag == 15:  # MethodHandle
            off += 4
        else:
            print(f"Unknown CP tag {tag} at offset {off}")
            sys.exit(1)
        i += 1
    return off, cp_count

def find_utf8_index(data, cp_count, text):
    """Tìm index của UTF8 entry"""
    off = 10
    i = 1
    while i < cp_count:
        tag = data[off]
        if tag == 1:
            length = read_u2(data, off + 1)
            s = data[off+3:off+3+length].decode('utf-8', errors='replace')
            if s == text:
                return i
            off += 3 + length
        elif tag in (7, 8, 16, 19, 20):
            off += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            off += 5
        elif tag in (5, 6):
            off += 9
            i += 1
        elif tag == 15:
            off += 4
        else:
            off += 3
        i += 1
    return -1

def add_cp_entries(data, cp_end, cp_count):
    """
    Thêm các CP entries cần thiết:
    1. Utf8 "AutoBossNotice"
    2. Class -> AutoBossNotice
    3. Utf8 "onReceiveMessage"
    4. Utf8 "(Ljava/lang/String;)V"
    5. NameAndType -> onReceiveMessage:(Ljava/lang/String;)V
    6. Methodref -> AutoBossNotice.onReceiveMessage:(Ljava/lang/String;)V
    """
    new_entries = bytearray()
    new_indices = {}

    # Check nếu đã có sẵn
    existing_class_name = find_utf8_index(data, cp_count, "AutoBossNotice")
    existing_method_name = find_utf8_index(data, cp_count, "onReceiveMessage")
    existing_desc = find_utf8_index(data, cp_count, "(Ljava/lang/String;)V")

    next_idx = cp_count

    # 1. Utf8 "AutoBossNotice"
    if existing_class_name >= 0:
        new_indices['class_name_idx'] = existing_class_name
    else:
        text = b"AutoBossNotice"
        new_entries += bytes([1]) + struct.pack('>H', len(text)) + text
        new_indices['class_name_idx'] = next_idx
        next_idx += 1

    # 2. Class -> AutoBossNotice
    new_entries += bytes([7]) + struct.pack('>H', new_indices['class_name_idx'])
    new_indices['class_idx'] = next_idx
    next_idx += 1

    # 3. Utf8 "onReceiveMessage"
    if existing_method_name >= 0:
        new_indices['method_name_idx'] = existing_method_name
    else:
        text = b"onReceiveMessage"
        new_entries += bytes([1]) + struct.pack('>H', len(text)) + text
        new_indices['method_name_idx'] = next_idx
        next_idx += 1

    # 4. Utf8 "(Ljava/lang/String;)V"
    if existing_desc >= 0:
        new_indices['desc_idx'] = existing_desc
    else:
        text = b"(Ljava/lang/String;)V"
        new_entries += bytes([1]) + struct.pack('>H', len(text)) + text
        new_indices['desc_idx'] = next_idx
        next_idx += 1

    # 5. NameAndType -> onReceiveMessage:(Ljava/lang/String;)V
    new_entries += bytes([12]) + struct.pack('>H', new_indices['method_name_idx']) + struct.pack('>H', new_indices['desc_idx'])
    new_indices['nat_idx'] = next_idx
    next_idx += 1

    # 6. Methodref -> AutoBossNotice.onReceiveMessage
    new_entries += bytes([10]) + struct.pack('>H', new_indices['class_idx']) + struct.pack('>H', new_indices['nat_idx'])
    new_indices['methodref_idx'] = next_idx
    next_idx += 1

    # Insert CP entries
    result = bytearray(data[:cp_end]) + new_entries + bytearray(data[cp_end:])

    # Update cp_count
    write_u2(result, 8, next_idx)

    return result, new_indices, len(new_entries)

def find_pattern(code, pattern_bytes, start=0):
    """Tìm pattern trong bytecode"""
    return code.find(pattern_bytes, start)

def patch_controller(filepath):
    with open(filepath, 'rb') as f:
        data = bytearray(f.read())

    cp_end, cp_count = find_cp_end(data)
    print(f"CP Count: {cp_count}, CP End: {cp_end}")

    # Thêm CP entries
    data, indices, cp_added_bytes = add_cp_entries(data, cp_end, cp_count)
    methodref_idx = indices['methodref_idx']
    print(f"Added CP entries, methodref index: {methodref_idx}")
    print(f"CP added bytes: {cp_added_bytes}")

    # Tìm method gameAA trong Controller (method lớn nhất, chứa packet handler)
    # Tìm tất cả Code attributes và patch bên trong
    
    # Pattern 1: Đoạn thông báo Server (aload_2 rồi invokevirtual ChatManager.gameAA)
    # Bytecode tại offset 1013-1017:
    #   1013: aload_2                        -> 0x2c
    #   1014: invokevirtual #93              -> 0xb6 00 5d
    #   1017: getstatic #9 (Code.gameAB)     -> 0xb2 00 09
    # Sau invokevirtual #93, trước getstatic #9, inject:
    #   aload_2                              -> 0x2c
    #   invokestatic #methodref_idx          -> 0xb8 XX XX
    
    # Pattern 2: Đoạn chat người chơi kênh TG
    #   1654: aload 25                       -> 0x19 19
    #   1656: invokevirtual #93              -> 0xb6 00 5d
    #   1659: getstatic #147 (blockGlobalChat) -> 0xb2 00 93
    # Sau invokevirtual #93, trước getstatic #147, inject:
    #   aload 25                             -> 0x19 19
    #   invokestatic #methodref_idx          -> 0xb8 XX XX

    # Scan all Code attributes để tìm và patch
    # Controller chỉ có 1 method lớn - gameAA(Message)
    
    # Tìm tất cả "Code" attributes
    code_attr_utf8 = find_utf8_index(data, read_u2(data, 8), "Code")
    if code_attr_utf8 < 0:
        print("ERROR: Cannot find 'Code' Utf8 in CP")
        sys.exit(1)
    
    # Tìm Code attribute dài nhất (method gameAA)
    patched = 0
    search_start = 0
    
    # Tìm pattern bytes cho 2 đoạn
    # Pattern 1: invokevirtual ChatManager.gameAA + getstatic Code.gameAB
    # 0xb6 00 5d b2 00 09 (invokevirtual #93, getstatic #9)
    # Nhưng CP indices có thể thay đổi sau khi thêm entries...
    # CP indices KHÔNG thay đổi vì ta thêm cuối CP, chỉ cp_count tăng
    
    # Tìm trong toàn bộ file data
    # Pattern 1: aload_2 + invokevirtual #93 rồi getstatic #9  
    # Hex: 2c b6 00 5d b2 00 09
    p1 = bytes([0x2c, 0xb6, 0x00, 0x5d, 0xb2, 0x00, 0x09])
    
    pos1 = data.find(p1)
    if pos1 >= 0:
        # Chèn sau 2c b6 00 5d (4 bytes), trước b2 00 09
        insert_pos1 = pos1 + 4
        inject1 = bytes([0x2c, 0xb8]) + struct.pack('>H', methodref_idx)
        data = data[:insert_pos1] + bytearray(inject1) + data[insert_pos1:]
        patched += 1
        print(f"Pattern 1 patched at offset {pos1} (Server notification)")
        
        # Cập nhật code_length của Code attribute chứa pattern này
        # Sẽ xử lý bên dưới
    else:
        print("WARNING: Pattern 1 not found!")
    
    # Pattern 2: aload 25 + invokevirtual #93 rồi getstatic #147 (blockGlobalChat)
    # Hex: 19 19 b6 00 5d b2 00 93
    p2 = bytes([0x19, 0x19, 0xb6, 0x00, 0x5d, 0xb2, 0x00, 0x93])
    
    pos2 = data.find(p2)
    if pos2 >= 0:
        # Chèn sau 19 19 b6 00 5d (5 bytes), trước b2 00 93
        insert_pos2 = pos2 + 5
        inject2 = bytes([0x19, 0x19, 0xb8]) + struct.pack('>H', methodref_idx)
        data = data[:insert_pos2] + bytearray(inject2) + data[insert_pos2:]
        patched += 1
        print(f"Pattern 2 patched at offset {pos2} (Player world chat)")
    else:
        print("WARNING: Pattern 2 not found!")
    
    if patched == 0:
        print("ERROR: No patterns matched! Aborting.")
        sys.exit(1)
    
    # Cần update code_length trong Code attribute
    # Tìm Code attribute bằng cách scan file cho attribute name = "Code" (cp index)
    # Mỗi lần chèn N bytes, code_length += N và attribute_length += N
    # Cũng cần update tất cả branch offsets nhưng đây rất phức tạp...
    
    # Cách an toàn hơn: KHÔNG chèn bytecode trực tiếp, mà thay thế bytecode hiện có
    # Thay vì chèn, ta sẽ THAY ĐỔI cách tiếp cận:
    # Thay vì inject vào Controller (rất rủi ro do branch offset),
    # ta inject vào ChatManager.gameAA(String, String, String) vì hàm đó đơn giản hơn
    
    print(f"\nWARNING: Direct bytecode insertion into Controller is RISKY due to branch offsets.")
    print(f"Switching to safer approach: patch ChatManager instead.")
    
    # Khôi phục data gốc
    with open(filepath, 'rb') as f:
        data = bytearray(f.read())
    
    return patch_chat_manager(data, filepath)

def patch_chat_manager(data_unused, controller_path):
    """
    Patch ChatManager.gameAA(String, String, String) để gọi AutoBossNotice.onReceiveMessage(content)
    Hàm này được gọi cho MỌI tin nhắn chat (GLOBALCHAT, PUBLICCHAT, PARTYCHAT, CLANCHAT)
    → Đơn giản hơn Controller, ít branch offsets
    """
    chatmgr_path = os.path.join(os.path.dirname(controller_path), "ChatManager.class")
    
    with open(chatmgr_path, 'rb') as f:
        data = bytearray(f.read())
    
    cp_end, cp_count = find_cp_end(data)
    print(f"\nChatManager CP Count: {cp_count}, CP End: {cp_end}")
    
    # Thêm CP entries
    data, indices, cp_added = add_cp_entries(data, cp_end, cp_count)
    methodref_idx = indices['methodref_idx']
    print(f"Added CP entries to ChatManager, methodref index: {methodref_idx}")
    
    # Tìm method gameAA(String, String, String)V
    # Decompile cho thấy nó nhận 3 tham số String
    # Trong instance method: this=aload_0, tab=aload_1, sender=aload_2, content=aload_3
    
    # Cách inject: Thêm ở ĐẦU method body:
    #   aload_3           -> 0x2d (content, tham số thứ 3)
    #   invokestatic #idx -> 0xb8 XX XX
    # Total: 4 bytes thêm vào đầu method code
    
    # Tìm method gameAA:(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    new_cp_count = read_u2(data, 8)
    
    # Tìm Utf8 index cho tên và descriptor
    method_name_utf8 = find_utf8_index(data, new_cp_count, "gameAA")
    method_desc_utf8 = find_utf8_index(data, new_cp_count, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V")
    code_utf8 = find_utf8_index(data, new_cp_count, "Code")
    
    if method_name_utf8 < 0 or method_desc_utf8 < 0:
        print(f"ERROR: Cannot find method gameAA with 3 String params")
        print(f"  method_name_utf8={method_name_utf8}, method_desc_utf8={method_desc_utf8}")
        sys.exit(1)
    
    print(f"Method name idx: {method_name_utf8}, desc idx: {method_desc_utf8}, code idx: {code_utf8}")
    
    # Parse class structure to find the method
    new_cp_end, _ = find_cp_end(data)
    pos = new_cp_end
    
    # access_flags, this_class, super_class
    pos += 6
    
    # interfaces
    iface_count = read_u2(data, pos)
    pos += 2 + iface_count * 2
    
    # fields
    field_count = read_u2(data, pos)
    pos += 2
    for _ in range(field_count):
        pos += 6  # access, name, desc
        attr_count = read_u2(data, pos)
        pos += 2
        for _ in range(attr_count):
            pos += 2  # attr name
            attr_len = read_u4(data, pos)
            pos += 4 + attr_len
    
    # methods
    method_count = read_u2(data, pos)
    pos += 2
    
    target_code_offset = -1
    
    for m in range(method_count):
        m_access = read_u2(data, pos)
        m_name = read_u2(data, pos + 2)
        m_desc = read_u2(data, pos + 4)
        pos += 6
        
        attr_count = read_u2(data, pos)
        pos += 2
        
        for a in range(attr_count):
            a_name = read_u2(data, pos)
            a_len = read_u4(data, pos + 2)
            
            if m_name == method_name_utf8 and m_desc == method_desc_utf8 and a_name == code_utf8:
                target_code_offset = pos
                print(f"Found target method Code attribute at offset {pos}")
            
            pos += 6 + a_len
    
    if target_code_offset < 0:
        print("ERROR: Cannot find Code attribute for gameAA(String,String,String)V")
        sys.exit(1)
    
    # Parse Code attribute
    # Format: attr_name(2) + attr_length(4) + max_stack(2) + max_locals(2) + code_length(4) + code[...]
    code_attr_pos = target_code_offset
    attr_name_idx = read_u2(data, code_attr_pos)
    attr_length = read_u4(data, code_attr_pos + 2)
    max_stack = read_u2(data, code_attr_pos + 6)
    max_locals = read_u2(data, code_attr_pos + 8)
    code_length = read_u4(data, code_attr_pos + 10)
    code_start = code_attr_pos + 14
    
    print(f"Code attr: max_stack={max_stack}, max_locals={max_locals}, code_length={code_length}")
    
    # Inject bytes at the beginning of code
    inject = bytearray([
        0x2d,                   # aload_3 (content string - 3rd param)
        0xb8,                   # invokestatic
    ]) + struct.pack('>H', methodref_idx)
    
    inject_len = len(inject)
    
    # Insert inject bytes at code_start
    data = data[:code_start] + inject + data[code_start:]
    
    # Update code_length
    write_u4(data, code_attr_pos + 10, code_length + inject_len)
    
    # Update attr_length
    write_u4(data, code_attr_pos + 2, attr_length + inject_len)
    
    # Update max_stack if needed (we push 1 String, invokestatic pops it)
    if max_stack < 2:
        write_u2(data, code_attr_pos + 6, 2)
    
    # Write back
    with open(chatmgr_path, 'wb') as f:
        f.write(data)
    
    print(f"SUCCESS: Injected {inject_len} bytes into ChatManager.gameAA(String,String,String)V")
    print(f"AutoBossNotice.onReceiveMessage() will now receive ALL chat messages!")
    return True

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <path_to_unpacked_dir>")
        sys.exit(1)
    
    unpacked_dir = sys.argv[1]
    controller_path = os.path.join(unpacked_dir, "Controller.class")
    
    if not os.path.exists(controller_path):
        print(f"ERROR: {controller_path} not found")
        sys.exit(1)
    
    patch_controller(controller_path)
