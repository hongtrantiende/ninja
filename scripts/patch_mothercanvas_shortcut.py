"""Replace MotherCanvas' GameGraphics.gameAA(int) call with ShortcutHandler.handleKey."""
import struct
import sys


def patch(path):
    data = bytearray(open(path, "rb").read())
    if b"ShortcutHandler" in data:
        print("MotherCanvas shortcut already patched")
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

    target = -1
    for index, class_index, nat_index in methods:
        class_name = utf8.get(classes.get(class_index))
        name_index, desc_index = nats.get(nat_index, (0, 0))
        if class_name == "GameGraphics" and utf8.get(name_index) == "gameAA" and utf8.get(desc_index) == "(I)V":
            target = index
            break
    if target < 0:
        raise ValueError("GameGraphics.gameAA(I)V not found")

    class_utf = cp_count
    class_ref = cp_count + 1
    name_utf = cp_count + 2
    desc_utf = cp_count + 3
    nat_ref = cp_count + 4
    method_ref = cp_count + 5
    extra = bytearray()
    for value in (b"ShortcutHandler",):
        extra += bytes([1]) + struct.pack(">H", len(value)) + value
    extra += bytes([7]) + struct.pack(">H", class_utf)
    for value in (b"handleKey", b"(LGameGraphics;I)V"):
        extra += bytes([1]) + struct.pack(">H", len(value)) + value
    extra += bytes([12]) + struct.pack(">HH", name_utf, desc_utf)
    extra += bytes([10]) + struct.pack(">HH", class_ref, nat_ref)
    data[cp_end:cp_end] = extra
    data[8:10] = struct.pack(">H", cp_count + 6)

    old = bytes([0xB6]) + struct.pack(">H", target)
    new = bytes([0xB8]) + struct.pack(">H", method_ref)
    start = cp_end + len(extra)
    count = data[start:].count(old)
    if count != 1:
        raise ValueError("Expected one key call, found %d" % count)
    tail = data[start:].replace(old, new)
    data[start:] = tail
    open(path, "wb").write(data)
    print("Patched MotherCanvas keyPressed -> ShortcutHandler.handleKey")


if __name__ == "__main__":
    patch(sys.argv[1] if len(sys.argv) > 1 else "build/unpacked/MotherCanvas.class")
