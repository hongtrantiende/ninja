---
name: jar-decompile-recompile
description: Skill giải nén, decompile, biên dịch lại và đóng gói file JAR Java ME cho Ninja School Modding.
---

# Skill: JAR Decompile & Recompile

Hướng dẫn quy trình chuẩn để giải nén file `.jar`, phân tích mã nguồn `.class`, chỉnh sửa và đóng gói lại.

## 🛠️ Quy Trình Thực Hiện

### Bước 1: Giải Nén File JAR
```bash
cd /root/ninja
./scripts/unpack_jar.sh Aeharuna.jar
```
Thư mục giải nén sẽ được lưu tại `build/unpacked/`.

### Bước 2: Xem Cấu Trúc Class & Bytecode
```bash
# Xem các phương thức và hằng số của một class
javap -c -p build/unpacked/a.class
```

### Bước 3: Đóng Gói Lại File JAR
```bash
./scripts/pack_jar.sh output_modded.jar
```

### Bước 4: Kiểm Tra Độ Toàn Vẹn
```bash
unzip -t output_modded.jar
```
