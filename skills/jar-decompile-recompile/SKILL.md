---
name: jar-decompile-recompile
description: Skill giải nén, decompile, biên dịch lại (.java -> .class với J2ME stubs) và đóng gói file JAR Java ME cho Ninja School Modding.
---

# Skill: JAR Decompile & Recompile

Hướng dẫn quy trình chuẩn để giải nén file `.jar`, biên dịch mã nguồn Java, và đóng gói lại trên Windows PowerShell.

## 🛠️ Quy Trình Thực Hiện

### Bước 1: Giải Nén File JAR
```powershell
powershell -Command "New-Item -ItemType Directory -Force -Path 'build/unpacked'; Set-Location 'build/unpacked'; jar xf '../../Aeharuna.jar'"
```
Các file `.class` và tài nguyên sẽ được trích xuất vào `build/unpacked/`.

### Bước 2: Xem Bytecode / Constant Pool (nếu cần)
```powershell
javap -v build/unpacked/Code.class | Select-String "gaoda"
```

### Bước 3: Biên Dịch Code Java Nguồn (.java -> .class)
Nếu biên dịch file `.java` mới (ví dụ: `AutoGaoDa.java`, `Code.java`), tạo temporary stubs cho `javax.microedition` để tránh lỗi thiếu class J2ME:

1. **Tạo stubs tạm thời:**
   - `stubs/javax/microedition/lcdui/`: `Image.java`, `CommandListener.java`, `Form.java`, `Displayable.java`, `Command.java`, `Graphics.java`, `Canvas.java`
   - `stubs/javax/microedition/midlet/`: `MIDlet.java`

2. **Chạy javac biên dịch:**
   ```powershell
   javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/AutoGaoDa.java src/Code.java
   ```

3. **Dọn dẹp stubs:**
   ```powershell
   powershell -Command "if (Test-Path 'build/unpacked/javax') { Remove-Item -Recurse -Force 'build/unpacked/javax' }; if (Test-Path 'stubs') { Remove-Item -Recurse -Force 'stubs' }"
   ```

### Bước 4: Đóng Gói Lại File JAR
```powershell
powershell -Command "Set-Location 'build/unpacked'; jar cfm '../../Aeharuna.jar' 'META-INF/MANIFEST.MF' *"
```

### Bước 5: Kiểm Tra Độ Toàn Vẹn & Lớp Mới
```powershell
jar tf Aeharuna.jar | Select-String "AutoGaoDa"
```

