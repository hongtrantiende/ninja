"""
Patch GameScr.class: Dich chuyen HS Luong + Loc Do xuong duoi 20px.

Thay 3 byte NOP (nop nop nop) tai offset 776-778 thanh iinc 2, 20
de tang y-coord truoc khi render "HS luong:" va "Loc Do:".
"""

import struct
import os

CLASS_FILE = os.path.join("build", "unpacked", "GameScr.class")

# Doc file
with open(CLASS_FILE, "rb") as f:
    data = bytearray(f.read())

# Tim method code area chua "HS l" string
# Tu bytecode analysis: offset 785 bat dau render "HS luong:"
# Truoc do co cac NOP tu offset ~760-784 (khu vuc da patch truoc)
# Can chen iinc 2, 20 vao 3 NOP tai cuoi day nop block

# Tim pattern: nhieu NOP lien tuc roi getstatic mFont.tahoma_7_yellow
# Pattern: 00 00 00 B2 (nop nop nop getstatic)
# Offset 782-785 trong code_array: nop nop nop getstatic

# Tim vi tri code_attribute trong class file
# Scan tim method co HS luong string reference
# Tim CP entry cho "HS l" string

# Tim string "HS l" trong constant pool
hs_str_bytes = None
for encoding in ['utf-8']:
    try:
        hs_encoded = "HS lượng:".encode('utf-8')
        idx = data.find(hs_encoded)
        if idx >= 0:
            hs_str_bytes = hs_encoded
            print(f"Found 'HS luong:' at file offset {idx}")
            break
    except:
        pass

if hs_str_bytes is None:
    print("ERROR: Cannot find HS luong string in class file!")
    exit(1)

# Tim Code attribute cua paint method (method lon nhat)
# Tim tat ca Code attributes va chon cai lon nhat
code_attr_name = b'\x04Code'  # UTF8 length 4 + "Code"

# Parse class file header
magic = struct.unpack('>I', data[0:4])[0]
assert magic == 0xCAFEBABE, "Not a valid class file"

minor = struct.unpack('>H', data[4:6])[0]
major = struct.unpack('>H', data[6:8])[0]
cp_count = struct.unpack('>H', data[8:10])[0]

print(f"Class file: magic={hex(magic)}, version={major}.{minor}, cp_count={cp_count}")

# Parse constant pool
cp_offsets = [0] * cp_count  # 1-indexed
pos = 10
i = 1
while i < cp_count:
    cp_offsets[i] = pos
    tag = data[pos]
    if tag == 1:  # UTF8
        length = struct.unpack('>H', data[pos+1:pos+3])[0]
        pos += 3 + length
    elif tag in (3, 4, 9, 10, 11, 12):  # Int, Float, Fieldref, Methodref, InterfaceMethodref, NameAndType
        pos += 5
    elif tag in (5, 6):  # Long, Double
        pos += 9
        i += 1  # Takes 2 slots
    elif tag in (7, 8, 16, 19, 20):  # Class, String, MethodType, Module, Package
        pos += 3
    elif tag == 15:  # MethodHandle
        pos += 4
    elif tag == 17:  # Dynamic
        pos += 5
    elif tag == 18:  # InvokeDynamic
        pos += 5
    else:
        print(f"Unknown CP tag {tag} at index {i}, pos {pos}")
        exit(1)
    i += 1

# Skip access_flags, this_class, super_class
pos_after_cp = pos
pos += 6  # access_flags(2) + this_class(2) + super_class(2)

# Skip interfaces
iface_count = struct.unpack('>H', data[pos:pos+2])[0]
pos += 2 + iface_count * 2

# Skip fields
field_count = struct.unpack('>H', data[pos:pos+2])[0]
pos += 2
for fi in range(field_count):
    pos += 6  # access_flags + name_index + descriptor_index
    attr_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for ai in range(attr_count):
        pos += 2  # attr_name_index
        attr_len = struct.unpack('>I', data[pos:pos+4])[0]
        pos += 4 + attr_len

# Parse methods
method_count = struct.unpack('>H', data[pos:pos+2])[0]
pos += 2

# Find the biggest Code attribute (= paint method)
biggest_code_offset = -1
biggest_code_length = 0
biggest_code_array_offset = -1

for mi in range(method_count):
    m_access = struct.unpack('>H', data[pos:pos+2])[0]
    m_name_idx = struct.unpack('>H', data[pos+2:pos+4])[0]
    m_desc_idx = struct.unpack('>H', data[pos+4:pos+6])[0]
    pos += 6
    m_attr_count = struct.unpack('>H', data[pos:pos+2])[0]
    pos += 2
    for ai in range(m_attr_count):
        a_name_idx = struct.unpack('>H', data[pos:pos+2])[0]
        a_len = struct.unpack('>I', data[pos+2:pos+6])[0]
        # Check if this is a Code attribute
        cp_off = cp_offsets[a_name_idx]
        if data[cp_off] == 1:  # UTF8
            utf_len = struct.unpack('>H', data[cp_off+1:cp_off+3])[0]
            utf_str = data[cp_off+3:cp_off+3+utf_len]
            if utf_str == b'Code' and a_len > biggest_code_length:
                biggest_code_length = a_len
                biggest_code_offset = pos + 6  # Start of Code attribute data
                # Code attr: max_stack(2) + max_locals(2) + code_length(4) = 8 bytes before code array
                code_len = struct.unpack('>I', data[pos+6+4:pos+6+8])[0]
                biggest_code_array_offset = pos + 6 + 8
                print(f"Method {mi}: Code attribute at {pos}, code_length={code_len}")
        pos += 6 + a_len

print(f"\nBiggest Code attribute: offset={biggest_code_offset}, code_array={biggest_code_array_offset}")

# Now find the NOP block right before "HS luong:" rendering
# In the code array, bytecode offset 785 = getstatic mFont.tahoma_7_yellow
# Before it are NOPs from ~760 to 784 (25 NOPs)
# We need to find these in the actual file

code_start = biggest_code_array_offset

# Strategy: Find the sequence of bytes that matches:
# ... many 0x00 (nop) ... B2 xx xx (getstatic mFont.tahoma_7_yellow) ... 
# followed by ldc_w for "HS luong:" string

# Let's scan for the HS luong CP index
# Find CP# for "HS luong:" string
hs_cp_index = -1
for ci in range(1, cp_count):
    cp_off = cp_offsets[ci]
    if data[cp_off] == 1:  # UTF8
        utf_len = struct.unpack('>H', data[cp_off+1:cp_off+3])[0]
        utf_bytes = data[cp_off+3:cp_off+3+utf_len]
        if hs_str_bytes in utf_bytes:
            hs_cp_index = ci
            print(f"Found HS string at CP#{ci}")
            break

if hs_cp_index < 0:
    # Try String constant that references the UTF8
    for ci in range(1, cp_count):
        cp_off = cp_offsets[ci]
        if data[cp_off] == 8:  # String
            ref_idx = struct.unpack('>H', data[cp_off+1:cp_off+3])[0]
            ref_off = cp_offsets[ref_idx]
            if data[ref_off] == 1:
                utf_len = struct.unpack('>H', data[ref_off+1:ref_off+3])[0]
                utf_bytes = data[ref_off+3:ref_off+3+utf_len]
                if hs_str_bytes in utf_bytes:
                    hs_cp_index = ci
                    print(f"Found HS String constant at CP#{ci}")
                    break

# Now find ldc_w #hs_cp_index in the code array
# ldc_w = 0x13 followed by 2-byte index
ldc_w_bytes = bytes([0x13]) + struct.pack('>H', hs_cp_index)
print(f"Looking for ldc_w bytes: {ldc_w_bytes.hex()}")

# Search in code array
code_len_val = struct.unpack('>I', data[biggest_code_offset+4:biggest_code_offset+8])[0]
code_data = data[code_start:code_start+code_len_val]

ldc_pos = code_data.find(ldc_w_bytes)
if ldc_pos < 0:
    print("ERROR: Cannot find ldc_w for HS string in code!")
    exit(1)

print(f"Found ldc_w at bytecode offset {ldc_pos}")

# Now go backwards from ldc_pos to find the NOP block
# The getstatic before ldc_w is at ldc_pos - 6 (getstatic=3 bytes + aload_1=1 byte + new StringBuffer=3 bytes)
# Actually: getstatic(3) aload_1(1) new(3) dup(1) invokespecial(3) ldc_w(3)
# So getstatic is at ldc_pos - 11
# Before getstatic there should be NOPs

nop_end = ldc_pos - 11  # getstatic position
print(f"Expected NOP block ends before bytecode offset {nop_end}")

# Count NOPs backwards
nop_count = 0
pos_check = nop_end - 1
while pos_check >= 0 and code_data[pos_check] == 0x00:
    nop_count += 1
    pos_check -= 1

nop_start = nop_end - nop_count
print(f"Found {nop_count} NOPs from bytecode offset {nop_start} to {nop_end-1}")

# Replace 3 NOPs at the END of the NOP block with: iinc 2, 20
# iinc = 0x84, index=0x02, const=0x14 (20 decimal)
# That's 3 bytes: 84 02 14

if nop_count < 3:
    print(f"ERROR: Not enough NOPs ({nop_count}) to patch!")
    exit(1)

# Patch at nop_end - 3 (last 3 NOPs before getstatic)
patch_offset = code_start + nop_end - 3
print(f"\nPatching 3 bytes at file offset {patch_offset}")
print(f"Before: {data[patch_offset:patch_offset+3].hex()}")

data[patch_offset] = 0x84  # iinc
data[patch_offset + 1] = 0x02  # local var 2 (y)
data[patch_offset + 2] = 0x14  # +20 pixels

print(f"After:  {data[patch_offset:patch_offset+3].hex()}")
print("Patched: iinc 2, 20 (y += 20px before HS luong)")

# Write patched file
with open(CLASS_FILE, "wb") as f:
    f.write(data)

print(f"\nDone! GameScr.class patched successfully.")
print("HS Luong + Loc Do se duoc day xuong them 20px.")
