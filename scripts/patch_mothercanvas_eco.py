"""Patch MotherCanvas.class: hook EcoMode.renderPaint into paint, and EcoMode.handlePointer into pointerPressed."""
import struct
import sys

def patch(path):
    data = bytearray(open(path, "rb").read())
    if b"renderPaint" in data:
        print("MotherCanvas EcoMode already patched")
        return

    _, _, _, cp_count = struct.unpack(">IHHH", data[:10])
    pos = 10
    utf8 = {}
    classes = {}
    nats = {}
    methods = []
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            size = struct.unpack(">H", data[pos:pos + 2])[0]
            utf8[i] = data[pos + 2:pos + 2 + size].decode("latin1")
            pos += 2 + size
        elif tag == 7:
            classes[i] = struct.unpack(">H", data[pos:pos + 2])[0]
            pos += 2
        elif tag == 12:
            nats[i] = struct.unpack(">HH", data[pos:pos + 4])
            pos += 4
        elif tag == 10:
            methods.append((i,) + struct.unpack(">HH", data[pos:pos + 4]))
            pos += 4
        elif tag in (3, 4, 9, 11, 18):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag == 8 or tag == 16:
            pos += 2
        elif tag == 15:
            pos += 3
        else:
            raise ValueError("Unsupported constant-pool tag %d" % tag)
        i += 1
    cp_end = pos

    target_paint = -1
    target_pointer = -1
    for index, class_index, nat_index in methods:
        class_name = utf8.get(classes.get(class_index))
        name_index, desc_index = nats.get(nat_index, (0, 0))
        mname = utf8.get(name_index)
        mdesc = utf8.get(desc_index)
        if class_name == "GameGraphics" and mname == "gameAA" and mdesc == "(Ljavax/microedition/lcdui/Graphics;)V":
            target_paint = index
        if class_name == "GameGraphics" and mname == "gameAA" and mdesc == "(II)V":
            target_pointer = index

    if target_paint < 0:
        raise ValueError("GameGraphics.gameAA(Graphics) not found")
    if target_pointer < 0:
        raise ValueError("GameGraphics.gameAA(II)V not found")

    cur = cp_count
    extra = bytearray()
    
    # 1. UTF8 "EcoMode"
    utf_ecomode = cur; cur += 1
    extra += bytes([1]) + struct.pack(">H", len(b"EcoMode")) + b"EcoMode"
    
    # 2. Class EcoMode
    class_ecomode = cur; cur += 1
    extra += bytes([7]) + struct.pack(">H", utf_ecomode)
    
    # 3. UTF8 "renderPaint", "(LGameGraphics;Ljavax/microedition/lcdui/Graphics;)V"
    utf_paint_name = cur; cur += 1
    extra += bytes([1]) + struct.pack(">H", len(b"renderPaint")) + b"renderPaint"
    utf_paint_desc = cur; cur += 1
    paint_desc_bytes = b"(LGameGraphics;Ljavax/microedition/lcdui/Graphics;)V"
    extra += bytes([1]) + struct.pack(">H", len(paint_desc_bytes)) + paint_desc_bytes
    
    # 4. Nat renderPaint
    nat_paint = cur; cur += 1
    extra += bytes([12]) + struct.pack(">HH", utf_paint_name, utf_paint_desc)
    
    # 5. MethodRef EcoMode.renderPaint
    method_paint = cur; cur += 1
    extra += bytes([10]) + struct.pack(">HH", class_ecomode, nat_paint)
    
    # 6. UTF8 "handlePointer", "(LGameGraphics;II)V"
    utf_pointer_name = cur; cur += 1
    extra += bytes([1]) + struct.pack(">H", len(b"handlePointer")) + b"handlePointer"
    utf_pointer_desc = cur; cur += 1
    pointer_desc_bytes = b"(LGameGraphics;II)V"
    extra += bytes([1]) + struct.pack(">H", len(pointer_desc_bytes)) + pointer_desc_bytes
    
    # 7. Nat handlePointer
    nat_pointer = cur; cur += 1
    extra += bytes([12]) + struct.pack(">HH", utf_pointer_name, utf_pointer_desc)
    
    # 8. MethodRef EcoMode.handlePointer
    method_pointer = cur; cur += 1
    extra += bytes([10]) + struct.pack(">HH", class_ecomode, nat_pointer)

    data[cp_end:cp_end] = extra
    data[8:10] = struct.pack(">H", cur)

    start = cp_end + len(extra)
    
    # Replace invokevirtual target_paint (0xB6) -> invokestatic method_paint (0xB8)
    old_paint = bytes([0xB6]) + struct.pack(">H", target_paint)
    new_paint = bytes([0xB8]) + struct.pack(">H", method_paint)
    if data[start:].count(old_paint) != 1:
        raise ValueError("Expected 1 paint call, found %d" % data[start:].count(old_paint))
    data[start:] = data[start:].replace(old_paint, new_paint)
    
    # Replace invokevirtual target_pointer (0xB6) -> invokestatic method_pointer (0xB8)
    old_pointer = bytes([0xB6]) + struct.pack(">H", target_pointer)
    new_pointer = bytes([0xB8]) + struct.pack(">H", method_pointer)
    if data[start:].count(old_pointer) != 1:
        raise ValueError("Expected 1 pointerPressed call, found %d" % data[start:].count(old_pointer))
    data[start:] = data[start:].replace(old_pointer, new_pointer)

    open(path, "wb").write(data)
    print("Patched MotherCanvas paint -> EcoMode.renderPaint and pointerPressed -> EcoMode.handlePointer")

if __name__ == "__main__":
    patch(sys.argv[1] if len(sys.argv) > 1 else "build/unpacked/MotherCanvas.class")
