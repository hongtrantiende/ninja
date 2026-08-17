#!/usr/bin/env python3
"""
Patch GameScr.class: them offset y cho doan HS luong / Loc Do
de day xuong thap hon, tranh che do.
Tim pattern NOP truoc 'HS luong' va thay bang iinc 2, OFFSET
"""
import sys

OFFSET = 30  # So pixel day xuong (co the chinh)

with open(sys.argv[1], 'rb') as f:
    data = bytearray(f.read())

# Tim chuoi "HS l" trong constant pool (UTF8: 48 53 20 6c)
marker = b'HS l'
idx = data.find(marker)
if idx < 0:
    print("Khong tim thay chuoi 'HS luong' trong class!")
    sys.exit(1)
print(f"Tim thay 'HS l' tai offset {idx}")

# Tim ldc_w instruction tro toi chuoi nay trong bytecode
# Pattern: getstatic mFont.tahoma_7_yellow (b2 XX XX) truoc ldc_w
# Tim nguoc tu vi tri chuoi de tim doan code

# Tim pattern: nhieu byte 00 (NOP) lien tiep roi getstatic (b2)
# Trong bytecode, NOP = 0x00, iinc = 0x84
# Ta can tim chuoi NOP... NOP getstatic(b2) ngay truoc "HS luong"

# Tim tat ca vi tri cua b2 (getstatic) trong code
# Va ldc_w (13) tro toi index cua "HS luong"

# Approach: tim pattern NOP NOP NOP ... NOP getstatic (nhieu NOP lien tiep)
# roi thay 3 NOP cuoi (truoc getstatic) bang: iinc 2 OFFSET (84 02 XX)

# Tim vi tri code_attribute
code_attr_marker = b'\x00\x04Code'  # "Code" attribute name
# Tim method paint/gameAA... 

# Cach don gian: tim pattern byte 00 00 00 00 b2 ngay truoc HS luong
# Scan nguoc tu cuoi file
found = False
for pos in range(len(data) - 10, 0, -1):
    # Tim getstatic (b2) theo sau boi aload_1 (2b), new (bb), dup (59), invokespecial (b7)
    # Va ldc_w (13) voi index tro toi "HS luong"
    if (data[pos] == 0xb2 and 
        pos >= 3 and 
        data[pos-1] == 0x00 and data[pos-2] == 0x00 and data[pos-3] == 0x00 and
        pos + 14 < len(data) and
        data[pos+3] == 0x2b and  # aload_1
        data[pos+4] == 0xbb and  # new
        data[pos+7] == 0x59 and  # dup
        data[pos+8] == 0xb7 and  # invokespecial
        data[pos+11] == 0x13):   # ldc_w
        # Kiem tra ldc_w tro toi constant co chua "HS l"
        cp_idx = (data[pos+12] << 8) | data[pos+13]
        # Tim constant pool entry
        # Cach don gian: kiem tra xem sau ldc_w co chuoi "HS" khong
        # Kiem tra vung nay co gan voi marker "HS l" khong
        print(f"Tim thay getstatic tai byte offset {pos}, ldc_w cp_index={cp_idx}")
        
        # Thay 3 NOP truoc getstatic bang iinc 2, OFFSET
        data[pos-3] = 0x84  # iinc
        data[pos-2] = 0x02  # local var 2 (= y)
        data[pos-1] = OFFSET & 0xFF  # offset value
        found = True
        print(f"Da patch: iinc 2, {OFFSET} tai byte offset {pos-3}")
        break

if not found:
    print("Khong tim thay vi tri patch!")
    sys.exit(1)

with open(sys.argv[1], 'wb') as f:
    f.write(data)
print("Patch thanh cong!")
