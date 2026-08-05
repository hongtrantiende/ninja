#!/usr/bin/env python3
"""
patch_char_skip_effects.py — Patch Char.gameAB(SkillPaint, int) 
de skip hieu ung skill khi Code.timBG = true.

Tim method gameAB(LSkillPaint;I)V trong Char.class.
Chen vao dau method: getstatic Code.timBG -> ifeq ORIGINAL -> return

Do Char.class KHONG co ref den Code.timBG, ta them Fieldref vao Constant Pool.
"""
import struct
import sys
import os
import shutil

# ==================== HELPER FUNCTIONS ====================

def read_u1(data, offset):
    return data[offset], offset + 1

def read_u2(data, offset):
    return struct.unpack_from('>H', data, offset)[0], offset + 2

def read_u4(data, offset):
    return struct.unpack_from('>I', data, offset)[0], offset + 4

def write_u2(data, offset, value):
    struct.pack_into('>H', data, offset, value)

def write_u4(data, offset, value):
    struct.pack_into('>I', data, offset, value)

# ==================== CONSTANT POOL PARSER ====================

CP_TAGS = {
    1: 'Utf8', 3: 'Integer', 4: 'Float', 5: 'Long', 6: 'Double',
    7: 'Class', 8: 'String', 9: 'Fieldref', 10: 'Methodref',
    11: 'InterfaceMethodref', 12: 'NameAndType',
    15: 'MethodHandle', 16: 'MethodType', 18: 'InvokeDynamic',
}

def parse_cp(data, offset, cp_count):
    """Parse constant pool entries. Returns list of (tag, offset, length) tuples."""
    entries = [None]  # CP index 0 is unused
    i = 1
    while i < cp_count:
        tag = data[offset]
        entry_start = offset
        offset += 1
        if tag == 1:  # Utf8
            length = struct.unpack_from('>H', data, offset)[0]
            offset += 2 + length
        elif tag in (3, 4):  # Integer, Float
            offset += 4
        elif tag in (5, 6):  # Long, Double
            offset += 8
            entries.append((tag, entry_start, offset - entry_start))
            entries.append(None)  # takes 2 slots
            i += 2
            continue
        elif tag in (7, 8, 16):  # Class, String, MethodType
            offset += 2
        elif tag in (9, 10, 11):  # Fieldref, Methodref, InterfaceMethodref
            offset += 4
        elif tag == 12:  # NameAndType
            offset += 4
        elif tag == 15:  # MethodHandle
            offset += 3
        elif tag == 18:  # InvokeDynamic
            offset += 4
        else:
            raise ValueError(f"Unknown CP tag {tag} at offset {entry_start}, index {i}")
        entries.append((tag, entry_start, offset - entry_start))
        i += 1
    return entries, offset

def get_utf8(data, entries, index):
    """Get UTF-8 string from CP index."""
    if index < 1 or index >= len(entries) or entries[index] is None:
        return None
    tag, off, length = entries[index]
    if tag != 1:
        return None
    str_len = struct.unpack_from('>H', data, off + 1)[0]
    return data[off + 3:off + 3 + str_len].decode('utf-8', errors='replace')

def find_or_add_utf8(data_array, entries, text):
    """Find existing UTF-8 entry or add new one. Returns index."""
    for i, e in enumerate(entries):
        if e is not None and e[0] == 1:
            if get_utf8(bytes(data_array), entries, i) == text:
                return i, data_array
    # Add new Utf8 entry
    encoded = text.encode('utf-8')
    new_entry = bytes([1]) + struct.pack('>H', len(encoded)) + encoded
    # Will be appended later
    return None, new_entry

def find_class_ref(data, entries, class_name):
    """Find Class_info CP index by name."""
    for i, e in enumerate(entries):
        if e is not None and e[0] == 7:  # Class
            name_idx = struct.unpack_from('>H', data, e[1] + 1)[0]
            name = get_utf8(data, entries, name_idx)
            if name == class_name:
                return i
    return None

def find_nat(data, entries, name, desc):
    """Find NameAndType CP index."""
    for i, e in enumerate(entries):
        if e is not None and e[0] == 12:
            n_idx = struct.unpack_from('>H', data, e[1] + 1)[0]
            d_idx = struct.unpack_from('>H', data, e[1] + 3)[0]
            n = get_utf8(data, entries, n_idx)
            d = get_utf8(data, entries, d_idx)
            if n == name and d == desc:
                return i
    return None

def find_fieldref(data, entries, class_name, field_name, field_desc):
    """Find Fieldref CP index."""
    class_idx = find_class_ref(data, entries, class_name)
    if class_idx is None:
        return None
    nat_idx = find_nat(data, entries, field_name, field_desc)
    if nat_idx is None:
        return None
    for i, e in enumerate(entries):
        if e is not None and e[0] == 9:
            c = struct.unpack_from('>H', data, e[1] + 1)[0]
            n = struct.unpack_from('>H', data, e[1] + 3)[0]
            if c == class_idx and n == nat_idx:
                return i
    return None

# ==================== MAIN PATCHER ====================

def patch_char_class(filepath):
    with open(filepath, 'rb') as f:
        original = f.read()
    
    data = bytearray(original)
    
    # Verify magic
    magic = struct.unpack_from('>I', data, 0)[0]
    if magic != 0xCAFEBABE:
        print(f"ERROR: Not a valid class file (magic={hex(magic)})")
        return False
    
    # Parse header
    cp_count = struct.unpack_from('>H', data, 8)[0]
    print(f"Constant pool count: {cp_count}")
    
    entries, cp_end = parse_cp(bytes(data), 10, cp_count)
    print(f"Parsed {len(entries)-1} CP entries, CP ends at offset {cp_end}")
    
    # Check if Code.timBG fieldref already exists
    timBG_ref = find_fieldref(bytes(data), entries, "Code", "timBG", "Z")
    
    if timBG_ref is not None:
        print(f"Found Code.timBG Fieldref at CP#{timBG_ref}")
    else:
        print("Code.timBG not found in CP. Need to add entries.")
        # We need:
        # 1. Utf8 "Code" (may exist)
        # 2. Class_info -> "Code"
        # 3. Utf8 "timBG" (may exist)
        # 4. Utf8 "Z" (may exist)
        # 5. NameAndType -> timBG:Z
        # 6. Fieldref -> Code.timBG:Z
        
        # Find or prepare needed utf8 entries
        new_cp_bytes = bytearray()
        new_cp_count = 0
        
        # Track new indices starting from cp_count
        next_idx = cp_count
        
        # 1. "Code" utf8
        code_utf8_idx = None
        for i, e in enumerate(entries):
            if e is not None and e[0] == 1 and get_utf8(bytes(data), entries, i) == "Code":
                code_utf8_idx = i
                break
        if code_utf8_idx is None:
            code_utf8_idx = next_idx
            new_cp_bytes += bytes([1]) + struct.pack('>H', 4) + b'Code'
            next_idx += 1
            new_cp_count += 1
            print(f"  Adding Utf8 'Code' at CP#{code_utf8_idx}")
        else:
            print(f"  Found Utf8 'Code' at CP#{code_utf8_idx}")
        
        # 2. Class_info for "Code"
        code_class_idx = find_class_ref(bytes(data), entries, "Code")
        if code_class_idx is None:
            code_class_idx = next_idx
            new_cp_bytes += bytes([7]) + struct.pack('>H', code_utf8_idx)
            next_idx += 1
            new_cp_count += 1
            print(f"  Adding Class 'Code' at CP#{code_class_idx}")
        else:
            print(f"  Found Class 'Code' at CP#{code_class_idx}")
        
        # 3. "timBG" utf8
        timBG_utf8_idx = None
        for i, e in enumerate(entries):
            if e is not None and e[0] == 1 and get_utf8(bytes(data), entries, i) == "timBG":
                timBG_utf8_idx = i
                break
        if timBG_utf8_idx is None:
            timBG_utf8_idx = next_idx
            new_cp_bytes += bytes([1]) + struct.pack('>H', 5) + b'timBG'
            next_idx += 1
            new_cp_count += 1
            print(f"  Adding Utf8 'timBG' at CP#{timBG_utf8_idx}")
        else:
            print(f"  Found Utf8 'timBG' at CP#{timBG_utf8_idx}")
        
        # 4. "Z" utf8
        z_utf8_idx = None
        for i, e in enumerate(entries):
            if e is not None and e[0] == 1 and get_utf8(bytes(data), entries, i) == "Z":
                z_utf8_idx = i
                break
        if z_utf8_idx is None:
            z_utf8_idx = next_idx
            new_cp_bytes += bytes([1]) + struct.pack('>H', 1) + b'Z'
            next_idx += 1
            new_cp_count += 1
            print(f"  Adding Utf8 'Z' at CP#{z_utf8_idx}")
        else:
            print(f"  Found Utf8 'Z' at CP#{z_utf8_idx}")
        
        # 5. NameAndType timBG:Z
        timBG_nat_idx = next_idx
        new_cp_bytes += bytes([12]) + struct.pack('>H', timBG_utf8_idx) + struct.pack('>H', z_utf8_idx)
        next_idx += 1
        new_cp_count += 1
        print(f"  Adding NameAndType timBG:Z at CP#{timBG_nat_idx}")
        
        # 6. Fieldref Code.timBG:Z
        timBG_ref = next_idx
        new_cp_bytes += bytes([9]) + struct.pack('>H', code_class_idx) + struct.pack('>H', timBG_nat_idx)
        next_idx += 1
        new_cp_count += 1
        print(f"  Adding Fieldref Code.timBG:Z at CP#{timBG_ref}")
        
        # Insert new CP entries before cp_end
        data = data[:cp_end] + new_cp_bytes + data[cp_end:]
        
        # Update cp_count
        new_total = cp_count + new_cp_count
        struct.pack_into('>H', data, 8, new_total)
        print(f"  CP count updated: {cp_count} -> {new_total}")
        
        # Re-parse to get updated entries
        entries, cp_end = parse_cp(bytes(data), 10, new_total)
    
    # ==================== FIND gameAB(LSkillPaint;I)V METHOD ====================
    
    # Skip: access_flags(2) + this_class(2) + super_class(2)
    pos = cp_end + 6
    
    # Interfaces
    iface_count, pos = read_u2(data, pos)
    pos += iface_count * 2
    
    # Fields
    field_count, pos = read_u2(data, pos)
    for _ in range(field_count):
        pos += 6  # access_flags + name + descriptor
        attr_count, pos = read_u2(data, pos)
        for _ in range(attr_count):
            pos += 2  # attr name
            attr_len, pos = read_u4(data, pos)
            pos += attr_len
    
    # Methods
    method_count, pos = read_u2(data, pos)
    print(f"\nMethods: {method_count}")
    
    target_method_pos = None
    
    for m in range(method_count):
        m_start = pos
        m_access, pos = read_u2(data, pos)
        m_name_idx, pos = read_u2(data, pos)
        m_desc_idx, pos = read_u2(data, pos)
        m_attr_count, pos = read_u2(data, pos)
        
        m_name = get_utf8(bytes(data), entries, m_name_idx)
        m_desc = get_utf8(bytes(data), entries, m_desc_idx)
        
        code_attr_pos = None
        
        for _ in range(m_attr_count):
            a_name_idx, pos = read_u2(data, pos)
            a_len, pos = read_u4(data, pos)
            a_name = get_utf8(bytes(data), entries, a_name_idx)
            if a_name == "Code":
                code_attr_pos = pos
            pos += a_len
        
        if m_name == "gameAB" and m_desc == "(LSkillPaint;I)V":
            print(f"  FOUND target: gameAB(LSkillPaint;I)V at method #{m}")
            target_method_pos = code_attr_pos
    
    if target_method_pos is None:
        print("ERROR: Could not find gameAB(LSkillPaint;I)V!")
        return False
    
    # ==================== PATCH THE METHOD ====================
    # Code_attribute structure:
    #   u2 max_stack
    #   u2 max_locals
    #   u4 code_length
    #   u1 code[code_length]
    #   ...
    
    code_pos = target_method_pos
    max_stack = struct.unpack_from('>H', data, code_pos)[0]
    max_locals = struct.unpack_from('>H', data, code_pos + 2)[0]
    code_length = struct.unpack_from('>I', data, code_pos + 4)[0]
    bytecode_start = code_pos + 8
    
    print(f"\n  max_stack={max_stack}, max_locals={max_locals}, code_length={code_length}")
    print(f"  bytecode starts at offset {bytecode_start}")
    
    # We insert at the BEGINNING of the method:
    #   getstatic Code.timBG (3 bytes: B2 xx xx)
    #   ifeq +4            (3 bytes: 99 00 04)  -- if false, skip return
    #   return              (1 byte: B1)
    # Total: 7 bytes inserted
    
    INSERT_BYTES = bytearray([
        0xB2, (timBG_ref >> 8) & 0xFF, timBG_ref & 0xFF,  # getstatic Code.timBG
        0x99, 0x00, 0x04,  # ifeq +4 (skip return if timBG==false)
        0xB1,              # return (skip effect rendering)
    ])
    INSERT_LEN = len(INSERT_BYTES)
    
    print(f"  Inserting {INSERT_LEN} bytes at bytecode start")
    print(f"  getstatic CP#{timBG_ref} (Code.timBG)")
    print(f"  ifeq +4")
    print(f"  return")
    
    # Insert bytes
    data = data[:bytecode_start] + INSERT_BYTES + data[bytecode_start:]
    
    # Update code_length
    new_code_length = code_length + INSERT_LEN
    struct.pack_into('>I', data, code_pos + 4, new_code_length)
    
    # Update Code attribute length (6 bytes before code_pos in the attr header)
    # The Code attribute length is at code_pos - 4 (u4 attribute_length)
    code_attr_len_pos = code_pos - 4
    old_attr_len = struct.unpack_from('>I', data, code_attr_len_pos)[0]
    struct.pack_into('>I', data, code_attr_len_pos, old_attr_len + INSERT_LEN)
    
    # Update max_stack if needed (getstatic pushes 1)
    if max_stack < 1:
        struct.pack_into('>H', data, code_pos, 1)
    
    # ==================== FIX STACK MAP TABLE ====================
    # We need to shift all offsets in StackMapTable by INSERT_LEN
    # The StackMapTable is after: code[new_code_length] + exception_table
    
    smt_search_pos = bytecode_start + new_code_length
    
    # Exception table
    exc_count = struct.unpack_from('>H', data, smt_search_pos)[0]
    smt_search_pos += 2
    
    # Fix exception table offsets
    for e in range(exc_count):
        epos = smt_search_pos + e * 8
        for field_off in [0, 2, 4]:  # start_pc, end_pc, handler_pc
            old_val = struct.unpack_from('>H', data, epos + field_off)[0]
            struct.pack_into('>H', data, epos + field_off, old_val + INSERT_LEN)
    
    smt_search_pos += exc_count * 8
    
    # Code attributes (look for StackMapTable)
    code_attrs_count = struct.unpack_from('>H', data, smt_search_pos)[0]
    smt_search_pos += 2
    
    for ca in range(code_attrs_count):
        ca_name_idx = struct.unpack_from('>H', data, smt_search_pos)[0]
        ca_len = struct.unpack_from('>I', data, smt_search_pos + 2)[0]
        ca_name = get_utf8(bytes(data), entries, ca_name_idx)
        
        if ca_name == "StackMapTable":
            print(f"\n  StackMapTable found at offset {smt_search_pos}")
            smt_data_pos = smt_search_pos + 6
            num_entries = struct.unpack_from('>H', data, smt_data_pos)[0]
            print(f"  StackMapTable entries: {num_entries}")
            
            # Only fix the FIRST entry's offset_delta (it's relative to method start)
            if num_entries > 0:
                first_entry_pos = smt_data_pos + 2
                frame_type = data[first_entry_pos]
                
                if frame_type <= 63:
                    # same_frame: offset_delta = frame_type
                    new_val = frame_type + INSERT_LEN
                    if new_val <= 63:
                        data[first_entry_pos] = new_val
                        print(f"  Fixed same_frame: {frame_type} -> {new_val}")
                    else:
                        # Need to convert to same_frame_extended
                        # This is complex — for now, just rebuild
                        print(f"  WARNING: same_frame overflow {new_val}, converting to same_frame_extended")
                        # Replace frame_type byte with: 251(same_frame_extended) + u2(new_val)
                        replacement = bytes([251]) + struct.pack('>H', new_val)
                        data = data[:first_entry_pos] + replacement + data[first_entry_pos + 1:]
                        # Update StackMapTable attribute length
                        old_smt_len = struct.unpack_from('>I', data, smt_search_pos + 2)[0]
                        struct.pack_into('>I', data, smt_search_pos + 2, old_smt_len + 2)
                        # Update Code attribute length
                        old_code_attr = struct.unpack_from('>I', data, code_attr_len_pos)[0]
                        struct.pack_into('>I', data, code_attr_len_pos, old_code_attr + 2)
                        
                elif 64 <= frame_type <= 127:
                    # same_locals_1_stack_item: offset = frame_type - 64
                    old_delta = frame_type - 64
                    new_delta = old_delta + INSERT_LEN
                    if new_delta <= 63:
                        data[first_entry_pos] = new_delta + 64
                        print(f"  Fixed same_locals_1_stack: delta {old_delta} -> {new_delta}")
                    else:
                        print(f"  WARNING: same_locals_1_stack overflow, may need conversion")
                        # Fallback: try setting it
                        data[first_entry_pos] = min(new_delta + 64, 127)
                        
                elif frame_type == 247:
                    # same_locals_1_stack_item_extended
                    old_delta = struct.unpack_from('>H', data, first_entry_pos + 1)[0]
                    struct.pack_into('>H', data, first_entry_pos + 1, old_delta + INSERT_LEN)
                    print(f"  Fixed extended: delta {old_delta} -> {old_delta + INSERT_LEN}")
                    
                elif 248 <= frame_type <= 250:
                    # chop_frame
                    old_delta = struct.unpack_from('>H', data, first_entry_pos + 1)[0]
                    struct.pack_into('>H', data, first_entry_pos + 1, old_delta + INSERT_LEN)
                    print(f"  Fixed chop_frame: delta {old_delta} -> {old_delta + INSERT_LEN}")
                    
                elif frame_type == 251:
                    # same_frame_extended
                    old_delta = struct.unpack_from('>H', data, first_entry_pos + 1)[0]
                    struct.pack_into('>H', data, first_entry_pos + 1, old_delta + INSERT_LEN)
                    print(f"  Fixed same_frame_extended: delta {old_delta} -> {old_delta + INSERT_LEN}")
                    
                elif 252 <= frame_type <= 254:
                    # append_frame
                    old_delta = struct.unpack_from('>H', data, first_entry_pos + 1)[0]
                    struct.pack_into('>H', data, first_entry_pos + 1, old_delta + INSERT_LEN)
                    print(f"  Fixed append_frame: delta {old_delta} -> {old_delta + INSERT_LEN}")
                    
                elif frame_type == 255:
                    # full_frame
                    old_delta = struct.unpack_from('>H', data, first_entry_pos + 1)[0]
                    struct.pack_into('>H', data, first_entry_pos + 1, old_delta + INSERT_LEN)
                    print(f"  Fixed full_frame: delta {old_delta} -> {old_delta + INSERT_LEN}")
            break
        
        smt_search_pos += 6 + ca_len
    
    # ==================== WRITE ====================
    backup = filepath + '.bak_effects'
    if not os.path.exists(backup):
        shutil.copy2(filepath, backup)
        print(f"\n  Backup: {backup}")
    
    with open(filepath, 'wb') as f:
        f.write(data)
    
    print(f"\n  PATCHED: {filepath}")
    print(f"  Char.gameAB(SkillPaint,int) now skips when Code.timBG=true")
    return True


if __name__ == '__main__':
    class_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), 
                               '..', 'build', 'unpacked', 'Char.class')
    class_file = os.path.normpath(class_file)
    
    if not os.path.exists(class_file):
        print(f"ERROR: File not found: {class_file}")
        sys.exit(1)
    
    print(f"Patching: {class_file}")
    if patch_char_class(class_file):
        print("\nSUCCESS!")
    else:
        print("\nFAILED!")
        sys.exit(1)
