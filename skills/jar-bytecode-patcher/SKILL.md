---
name: jar-bytecode-patcher
description: Skill sửa trực tiếp Constant Pool, Chuỗi (String Pool) và Hằng Số trong các file .class của JAR mà không làm hỏng cấu trúc.
---

# Skill: JAR Bytecode & String Patcher

Kỹ năng sửa trực tiếp chuỗi văn bản, URL server, hoặc hằng số tốc độ/thời gian trong file `.class` Java ME.

## 🛠️ Quy Trình Thực Hiện

### 1. Tìm Chuỗi Trong Tệp .class
```bash
python3 ./scripts/patch_string.py search "http://" build/unpacked/
```

### 2. Thay Thế Chuỗi An Toàn (Giữ Nguyên Kích Thước Constant Pool)
```bash
python3 ./scripts/patch_string.py replace "ChuỗiCũ" "ChuỗiMới" build/unpacked/target.class
```

### 3. Kiểm Tra Kết Quả Trực Tiếp
```bash
javap -v build/unpacked/target.class | grep "Constant pool" -A 30
```
