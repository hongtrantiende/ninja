#!/usr/bin/env python3
"""
Patch compiled .class files for J2ME compatibility:
1. Downgrade class version from 52.0 (Java 8) to 45.3 (Java 1.1)
2. Strip StackMapTable attributes (Java 7+) that break J2ME Loader DEX converter

Usage: python patch_class_j2me.py <class_file_or_directory>
"""

import struct
import sys
import os

def read_u1(data, offset):
    return data[offset], offset + 1

def read_u2(data, offset):
    return struct.unpack('>H', data[offset:offset+2])[0], offset + 2

def read_u4(data, offset):
    return struct.unpack('>I', data[offset:offset+4])[0], offset + 4

def write_u2(data, offset, value):
    struct.pack_into('>H', data, offset, value)

def write_u4(data, offset, value):
    struct.pack_into('>I', data, offset, value)

def get_utf8_from_cp(data, cp_entries, index):
    """Get UTF-8 string from constant pool by index."""
    if index < 1 or index >= len(cp_entries):
        return None
    entry = cp_entries[index]
    if entry is None:
        return None
    tag, offset = entry
    if tag != 1:  # CONSTANT_Utf8
        return None
    length, off = read_u2(data, offset)
    return data[off:off+length].decode('utf-8', errors='replace')

def parse_constant_pool(data, offset, cp_count):
    """Parse constant pool to find UTF-8 entries for attribute name lookup."""
    cp_entries = [None] * cp_count  # index 0 is unused
    i = 1
    while i < cp_count:
        tag = data[offset]
        cp_entries[i] = (tag, offset + 1)
        offset += 1
        if tag == 1:  # CONSTANT_Utf8
            length, offset = read_u2(data, offset)
            offset += length
        elif tag == 3 or tag == 4:  # Integer, Float
            offset += 4
        elif tag == 5 or tag == 6:  # Long, Double (takes 2 slots)
            offset += 8
            i += 1  # skip next slot
        elif tag == 7 or tag == 8:  # Class, String
            offset += 2
        elif tag == 9 or tag == 10 or tag == 11:  # Fieldref, Methodref, InterfaceMethodref
            offset += 4
        elif tag == 12:  # NameAndType
            offset += 4
        elif tag == 15:  # MethodHandle
            offset += 3
        elif tag == 16:  # MethodType
            offset += 2
        elif tag == 18:  # InvokeDynamic
            offset += 4
        else:
            print(f"  WARNING: Unknown CP tag {tag} at index {i}")
            break
        i += 1
    return cp_entries, offset

def strip_stackmaptable_from_attributes(data, offset, cp_entries):
    """
    Process attributes, removing StackMapTable entries by zeroing them out.
    Returns (new_data_segment, bytes_consumed_from_original).
    """
    attrs_count, offset = read_u2(data, offset)
    result = bytearray()
    result += struct.pack('>H', attrs_count)  # will be updated later
    new_count = 0
    
    for _ in range(attrs_count):
        attr_name_idx, off2 = read_u2(data, offset)
        attr_length, off3 = read_u4(data, off2)
        attr_end = off3 + attr_length
        
        name = get_utf8_from_cp(data, cp_entries, attr_name_idx)
        
        if name == 'StackMapTable' or name == 'StackMap':
            # Skip this attribute entirely
            offset = attr_end
            continue
        
        if name == 'Code':
            # Code attribute contains sub-attributes — need to recurse
            # Code: max_stack(2) + max_locals(2) + code_length(4) + code + 
            #       exception_table_length(2) + exceptions + attributes
            code_start = off3
            max_stack, co = read_u2(data, code_start)
            max_locals, co = read_u2(data, co)
            code_length, co = read_u4(data, co)
            co += code_length  # skip bytecode
            exc_table_length, co = read_u2(data, co)
            co += exc_table_length * 8  # each exception entry is 8 bytes
            
            # Now co points to sub-attributes of Code
            sub_attrs_offset = co
            
            # Build new Code attribute without StackMapTable
            code_prefix = data[code_start:sub_attrs_offset]
            sub_result = strip_stackmaptable_from_attributes(data, sub_attrs_offset, cp_entries)
            
            new_code_data = bytearray(code_prefix) + sub_result[0]
            
            # Write attribute header
            result += struct.pack('>H', attr_name_idx)
            result += struct.pack('>I', len(new_code_data))
            result += new_code_data
            
            offset = attr_end
            new_count += 1
            continue
        
        # Keep other attributes as-is
        result += data[offset:attr_end]
        offset = attr_end
        new_count += 1
    
    # Update attribute count
    struct.pack_into('>H', result, 0, new_count)
    return result, offset

def patch_class_file(filepath):
    """Patch a single .class file for J2ME compatibility."""
    with open(filepath, 'rb') as f:
        data = bytearray(f.read())
    
    # Verify magic
    magic = struct.unpack('>I', data[0:4])[0]
    if magic != 0xCAFEBABE:
        return False, "Not a class file"
    
    minor = struct.unpack('>H', data[4:6])[0]
    major = struct.unpack('>H', data[6:8])[0]
    
    if major <= 45:
        return False, f"Already version {major}.{minor}"
    
    # Parse constant pool
    cp_count, cp_offset = read_u2(data, 8)
    cp_entries, after_cp = parse_constant_pool(data, cp_offset, cp_count)
    
    # Skip access_flags(2) + this_class(2) + super_class(2)
    offset = after_cp + 6
    
    # Skip interfaces
    iface_count, offset = read_u2(data, offset)
    offset += iface_count * 2
    
    # Process fields
    fields_count, offset = read_u2(data, offset)
    fields_start = offset - 2
    
    # Build new class file
    # Header (magic + version) + constant pool + access+this+super+interfaces
    new_data = bytearray(data[:fields_start + 2])  # up to and including fields_count
    
    # Patch version in new_data
    struct.pack_into('>H', new_data, 4, 3)   # minor = 3
    struct.pack_into('>H', new_data, 6, 45)  # major = 45
    
    # Process fields (strip StackMapTable from field attributes)
    for _ in range(fields_count):
        field_start = offset
        access, offset = read_u2(data, offset)
        name_idx, offset = read_u2(data, offset)
        desc_idx, offset = read_u2(data, offset)
        new_data += data[field_start:offset]
        
        stripped, offset_after = strip_stackmaptable_from_attributes(data, offset, cp_entries)
        new_data += stripped
        offset = offset_after
    
    # Process methods
    methods_count, offset = read_u2(data, offset)
    new_data += struct.pack('>H', methods_count)
    
    stripped_count = 0
    for _ in range(methods_count):
        method_start = offset
        access, offset = read_u2(data, offset)
        name_idx, offset = read_u2(data, offset)
        desc_idx, offset = read_u2(data, offset)
        new_data += data[method_start:offset]
        
        stripped, offset_after = strip_stackmaptable_from_attributes(data, offset, cp_entries)
        new_data += stripped
        
        old_size = offset_after - offset
        new_size = len(stripped)
        if new_size < old_size:
            stripped_count += 1
        offset = offset_after
    
    # Class attributes
    stripped, offset_after = strip_stackmaptable_from_attributes(data, offset, cp_entries)
    new_data += stripped
    
    with open(filepath, 'wb') as f:
        f.write(new_data)
    
    saved = len(data) - len(new_data)
    return True, f"{major}.{minor} -> 45.3, stripped {stripped_count} StackMapTable(s), saved {saved} bytes"


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else 'build/unpacked'
    
    if os.path.isfile(target):
        files = [target]
    else:
        files = [os.path.join(target, f) for f in os.listdir(target) if f.endswith('.class')]
    
    # Only patch our mod classes
    mod_classes = {
        'ThongKe', 'TsBoost', 'Auto', 'AutoSanBoss', 'ChatRouter', 'Code',
        'AutoPickup', 'AutoBossEvent', 'BossRadar', 'GhostBoss',
        'ThongTinBoss', 'NamMod', 'MapScanner', 'AutoLevel', 'SplitPatcher',
        'ShortcutHandler', 'SanBossHolder', 'MultiSkillAttack', 'AutoGaoDa',
        'AutoDoiDiem', 'AutoFilter', 'AutoNhanDa', 'AutoTsXa',
        'AutoTuLuyen', 'AutoVipMap', 'AutoVT55', 'InfoMe', 'mResources',
        'Skill', 'AutoFakePkb', 'BossConfig', 'ExploitConfig', 'TsConfig',
        'AutoSuicide', 'AutoBossNotice', 'AutoTAQ',
        'AutoRollTAQDung', 'AutoRollTAQClone', 'AutoLogin'
    }
    
    patched = 0
    for filepath in files:
        basename = os.path.splitext(os.path.basename(filepath))[0]
        # Include inner classes too (e.g., AutoSanBoss$1)
        base_class = basename.split('$')[0]
        if base_class not in mod_classes:
            continue
        
        success, msg = patch_class_file(filepath)
        status = "OK" if success else "SKIP"
        print(f"  [{status}] {basename}.class: {msg}")
        if success:
            patched += 1
    
    print(f"\nDone! Patched {patched} class files for J2ME compatibility.")


if __name__ == '__main__':
    main()
