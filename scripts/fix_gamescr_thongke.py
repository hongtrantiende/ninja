"""
Fix GameScr constant pool: rename ThongKe.paint -> ThongKe.render
'paint' might conflict with J2ME Canvas.paint on J2ME Loader
"""
import struct

with open('build/unpacked/GameScr.class', 'rb') as f:
    data = bytearray(f.read())

# Find and replace the UTF-8 "paint" entries that are used for ThongKe
# We need to find EXACTLY the right entries (the ones at CP#6364 and CP#6369)
# Since we know the bytes, search for the pattern: tag=1, len=5, "paint"
# But we need to be careful not to replace OTHER "paint" entries

# Parse constant pool to find the specific entries
offset = 10
cp_count = struct.unpack('>H', data[8:10])[0]

paint_entries = []  # (offset_of_string_bytes, length)
i = 1
while i < cp_count:
    tag = data[offset]
    offset += 1
    if tag == 1:  # UTF-8
        length = struct.unpack('>H', data[offset:offset+2])[0]
        offset += 2
        value = data[offset:offset+length]
        text = value.decode('utf-8', errors='replace')
        if text == 'paint' and i > 6000:  # Only the ones we added (high CP index)
            paint_entries.append((i, offset, length))
            print(f'Found CP#{i}: UTF8 "paint" at byte offset {offset}')
        offset += length
    elif tag == 7 or tag == 8:
        offset += 2
    elif tag == 9 or tag == 10 or tag == 11 or tag == 12:
        offset += 4
    elif tag == 3 or tag == 4:
        offset += 4
    elif tag == 5 or tag == 6:
        offset += 8
        i += 1
    elif tag == 15:
        offset += 3
    elif tag == 16:
        offset += 2
    elif tag == 18:
        offset += 4
    i += 1

# Replace "paint" with "draaw" (same length = 5) in the high-index CP entries
# This avoids any conflict with Canvas.paint
new_name = b'draaw'
for cp_idx, byte_offset, length in paint_entries:
    data[byte_offset:byte_offset+length] = new_name
    print(f'Replaced CP#{cp_idx} "paint" -> "draaw"')

with open('build/unpacked/GameScr.class', 'wb') as f:
    f.write(data)

print(f'Done! Patched {len(paint_entries)} entries.')
