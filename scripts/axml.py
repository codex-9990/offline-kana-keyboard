"""Small read-only decoder for Android's compiled resource XML chunks.

Only used for artifact verification. Not included in the Android application.
"""
import struct
import xml.etree.ElementTree as ET


def u16(data, offset):
    return struct.unpack_from("<H", data, offset)[0]


def u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def length8(data, offset):
    value = data[offset]
    return (((value & 0x7f) << 8) | data[offset + 1], offset + 2) if value & 0x80 else (value, offset + 1)


def length16(data, offset):
    value = u16(data, offset)
    return (((value & 0x7fff) << 16) | u16(data, offset + 2), offset + 4) if value & 0x8000 else (value, offset + 2)


def decode(data):
    if u16(data, 0) != 3:
        raise ValueError("Not compiled Android XML")
    offset = u16(data, 2)
    strings, stack, root = [], [], None
    while offset < len(data):
        kind, header, size = struct.unpack_from("<HHI", data, offset)
        if size < header or offset + size > len(data):
            raise ValueError("Invalid XML chunk")
        if kind == 1:
            count, flags, start = u32(data, offset + 8), u32(data, offset + 16), u32(data, offset + 20)
            strings = []
            for i in range(count):
                p = offset + start + u32(data, offset + header + 4 * i)
                if flags & 0x100:
                    _, p = length8(data, p)
                    length, p = length8(data, p)
                    strings.append(data[p:p + length].decode("utf-8"))
                else:
                    length, p = length16(data, p)
                    strings.append(data[p:p + length * 2].decode("utf-16le"))
        elif kind == 0x102:
            ns, name = u32(data, offset + 16), u32(data, offset + 20)
            tag = strings[name] if ns == 0xffffffff else "{" + strings[ns] + "}" + strings[name]
            node = ET.Element(tag)
            attr_start, attr_size, attr_count = struct.unpack_from("<HHH", data, offset + 24)
            for i in range(attr_count):
                p = offset + 16 + attr_start + i * attr_size
                ns, name, raw = struct.unpack_from("<III", data, p)
                key = strings[name] if ns == 0xffffffff else "{" + strings[ns] + "}" + strings[name]
                value_type, value = data[p + 15], u32(data, p + 16)
                if raw != 0xffffffff:
                    rendered = strings[raw]
                elif value_type == 3:
                    rendered = strings[value]
                elif value_type == 0x12:
                    rendered = "true" if value else "false"
                elif value_type == 1:
                    rendered = "@0x%08x" % value
                else:
                    rendered = str(value)
                node.set(key, rendered)
            if stack:
                stack[-1].append(node)
            else:
                root = node
            stack.append(node)
        elif kind == 0x103:
            stack.pop()
        offset += size
    if root is None or stack:
        raise ValueError("Incomplete compiled XML")
    ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
    return root
