import struct

with open('build/unpacked/GameScr.class', 'rb') as f:
    data = f.read()

offset = 10
cp_count = struct.unpack('>H', data[8:10])[0]
print(f'Constant pool count: {cp_count}')

i = 1
while i < cp_count:
    tag = data[offset]
    offset += 1
    if tag == 1:  # UTF-8
        length = struct.unpack('>H', data[offset:offset+2])[0]
        offset += 2
        value = data[offset:offset+length]
        text = value.decode('utf-8', errors='replace')
        offset += length
        if 'ThongKe' in text or text == 'paint' or text == 'PAINT' or text == 'mGraphics':
            print(f'  CP#{i}: UTF8 = "{text}" (len={length}, bytes={value.hex()})')
    elif tag == 7:  # Class
        idx = struct.unpack('>H', data[offset:offset+2])[0]
        offset += 2
    elif tag == 9 or tag == 10 or tag == 11:
        offset += 4
    elif tag == 12:  # NameAndType
        offset += 4
    elif tag == 8:
        offset += 2
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
    else:
        print(f'  Unknown tag {tag} at CP#{i}')
        break
    i += 1
print('Done')
