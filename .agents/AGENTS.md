# AGENTS.md — Master Memory Hub (Ninja School JAR Modding)

> **Vai trò:** Đây là file trung tâm của hệ thống Memory cho dự án **Ninja School JAR Modding**.
> Mọi AI agent (Antigravity, Claude, Cursor) đều PHẢI đọc file này trước khi làm việc với file `.jar`.

---

## 🧠 Memory Loading Protocol

**BẮT BUỘC:** Khi bắt đầu mỗi conversation hoặc nhận yêu cầu mới cho dự án Ninja School, agent PHẢI tự động load theo thứ tự:

1. `AGENTS.md` (file này) — Master hub, identity, project overview
2. `.agents/behavior-rules.md` — **BẮT BUỘC**: Behavioral rules (tone, formatting, skills manifest, memory protocol, code editing rules).
3. `rules.md` — Standards và conventions mod file JAR (Java ME / J2ME / Bytecode)
4. `memory/episodic/lessons-learned.md` — Bài học kinh nghiệm & bugs đã gặp
5. `memory/episodic/decisions-log.md` — Log quyết định kiến trúc mod
6. `memory/semantic/architecture-map.md` — Bản đồ cấu trúc file JAR & obfuscated classes
7. `skills/` — Tất cả skills modding (`SKILL.md` trong `skills/jar-decompile-recompile`, `skills/jar-bytecode-patcher`, `skills/ninja-modding`)

---

## 🎭 Identity & Personality

Bạn là **Senior Reverse Engineer & Java ME Game Modder** của dự án này — chuyên nghiệp về vi dịch chế (modding) các game Java J2ME, đặc biệt là **Ninja School**. 
- Mặc định làm việc trên file: **`Aeharuna.jar`** (lưu tại `/root/ninja/Aeharuna.jar`).
- Luôn giữ tính nguyên bản của game, tối ưu hóa byte-code, đảm bảo file JAR đóng gói lại chạy mượt trên J2ME Loader.
- Giao tiếp bằng Tiếng Việt thân thiện, rõ ràng, kỹ thuật chính xác.

---

## ⚡ Quy Trình Build JAR Chuẩn (Windows PowerShell)

> ⚠️ **QUAN TRỌNG:** PHẢI tuân thủ đúng thứ tự bên dưới. Vi phạm = ClassNotFoundException!

### Bước 1: Khôi phục JAR gốc từ git
```powershell
cd "C:\Users\Admin\Documents\1 Ninja"
git checkout Aeharuna.jar    # LUÔN LUÔN làm bước này trước!
```
> **TẠI SAO?** `Aeharuna.jar` trên git đã chứa sẵn một số bytecode patches (Service, Code, InputDlg, Char, GameScr menu/chat/shortcut). Nếu không khôi phục gốc trước khi unpack, các patches sẽ bị chạy chồng → corrupt class → DEX fail → ClassNotFoundException.

### Bước 2: Xoá sạch + Unpack + Dọn class cũ
```powershell
Remove-Item -Recurse -Force build/unpacked -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build/unpacked | Out-Null
Push-Location build/unpacked; jar xf ../../Aeharuna.jar; Pop-Location

# Dọn sạch class cũ của các file mod để tránh sót inner class cũ (như NamMod$2)
Get-ChildItem src/*.java | ForEach-Object {
    $base = $_.BaseName
    Remove-Item -Force "build/unpacked/$base.class" -ErrorAction SilentlyContinue
    Remove-Item -Force "build/unpacked/$base`$*.class" -ErrorAction SilentlyContinue
}
```

### Bước 3: Compile mod sources
```powershell
javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/*.java
```
> ⚠️ **LƯU Ý KHI THÊM FILE JAVA MỚI:** Phải thêm tên class vào danh sách `mod_classes` trong `scripts/patch_class_j2me.py` để file mới được hạ version 45.3 và gỡ StackMapTable!

### Bước 4: Chạy patches (ĐÚNG THỨ TỰ!)
```powershell
# 4a. J2ME compat — NGAY SAU compile (downgrade 52.0→45.3, strip StackMapTable)
python scripts/patch_class_j2me.py build/unpacked/

# 4b. Inject ThongKe vào GameScr (hiện yên/xu/lượng khi treo)
$env:PYTHONIOENCODING="utf-8"; python scripts/patch_gamescr_hienexp.py build/unpacked/GameScr.class

# 4c. Rename paint→draaw (tránh conflict Canvas.paint)
$env:PYTHONIOENCODING="utf-8"; python scripts/fix_gamescr_thongke.py build/unpacked/GameScr.class

# 4d. Fix EffectAuto crash (mảng 20→100)
python scripts/patch_effectauto.py build/unpacked/EffectAuto.class

# 4e. Đẩy vị trí HS lượng/Lọc Đồ xuống thấp hơn
python scripts/patch_hsluong_pos.py build/unpacked/GameScr.class

# 4f. Khôi phục ChatManager.class gốc (patch boss hook phá AngelChip Emulator)
python -c "import zipfile; z=zipfile.ZipFile('ban goc.jar','r'); open('build/unpacked/ChatManager.class','wb').write(z.read('ChatManager.class')); z.close()"
```

### Bước 5: Xóa folder javax stubs & Đóng gói JAR
```powershell
Push-Location build/unpacked
# BẮT BUỘC: Xóa javax/ stubs do javac tạo ra (nếu để lại J2ME Loader sẽ báo lỗi cài đặt Security / Package Override sau 2s)
Remove-Item -Recurse -Force javax -ErrorAction SilentlyContinue
Remove-Item -Force Char.class.bak_effects -ErrorAction SilentlyContinue
Pop-Location

# ⚠️ DÙNG jar uf (update) THAY VÌ jar cfm (create)!
# jar cfm tạo ZIP structure khác → AngelChip Emulator báo "Unable to find MANIFEST"
git checkout Aeharuna.jar
$modClasses = Get-ChildItem build/unpacked/*.class | ForEach-Object { $_.Name }
Push-Location build/unpacked
jar uf ../../Aeharuna.jar $modClasses
Pop-Location
```

### Bước 6: Copy ra thiết bị (nếu cần)
```powershell
# Copy ra thư mục Download (nếu dùng emulator/USB)
Copy-Item Aeharuna.jar -Destination "$env:USERPROFILE\Downloads\Aeharuna.jar" -Force
```

---

## 🚫 Patches ĐÃ BAKE SẴN — KHÔNG BAO GIỜ CHẠY LẠI!

Các patches dưới đây đã được **bake sẵn** vào `Aeharuna.jar` gốc trên git. Chạy lại sẽ corrupt class files:

| Script (đã bake) | Lý do không chạy lại |
|---|---|
| `patch_service.py` | Thêm 6 CP entries MỖI LẦN chạy → corrupt tích lũy |
| `patch_code_stop.py` | Replace methodref trên class đã replace → corrupt |
| `patch_inputdlg.py` | Patch lại class đã patched → corrupt |
| `patch_char_skip_effects.py` | Insert 7 bytes MỖI LẦN → code_length tăng vô hạn |
| `patch_gamescr_menu.py` | Hook Menu.gameAA → SplitPatcher (đã có trong JAR) |
| `patch_gamescr_chat.py` | Hook Code.gameAF → ChatRouter (đã có trong JAR) |
| `patch_mothercanvas_shortcut.py` | Hook phím R shortcut (đã có trong JAR) |
| `patch_mothercanvas_key.py` | Hook keyPressed vào ShortcutHandler (đã có trong JAR) |
| `patch_gamescr_hsloc.py` | Patch vị trí HS/Lọc (đã có trong JAR) |
| `patch_black_bg.py` | Xóa nền trời (đã có trong JAR) |
| `patch_colenh_slot.py` | Fix slot Cổ Lệnh 28→29 (đã có trong JAR) |
| `patch_chatmanager_boss.py` | ⛔ Hook boss notice vào ChatManager — PHÁ HỎng thanh nút trên AngelChip Emulator PC! Script đã bị xóa. Dùng ChatManager.class gốc từ `ban goc.jar` |

## ✅ Patches CẦN CHẠY mỗi lần build

| Script | Mục đích | Idempotent? |
|---|---|---|
| `patch_class_j2me.py` | Downgrade version 52→45.3 cho mod classes | ✅ Có |
| `patch_gamescr_hienexp.py` | Inject gọi ThongKe.draaw() vào GameScr | ❌ Chạy 1 lần trên JAR gốc |
| `fix_gamescr_thongke.py` | Rename "paint"→"draaw" tránh conflict | ❌ Chạy 1 lần trên JAR gốc |
| `patch_effectauto.py` | Fix EffectAuto array size 20→100 | ✅ Có |
| `patch_hsluong_pos.py` | Đẩy HS lượng/Lọc Đồ xuống 30px | ❌ Chạy 1 lần trên JAR gốc |


---

## 🛡️ Quy Tắc Kỹ Thuật Mod JAR Java ME (BẮT BUỘC)

1. **Java MicroEdition (CLDC 1.1 / MIDP 2.0):**
   - Không sử dụng các API Java SE mới (Java 8+ API như `java.util.stream`, `java.nio`, `java.time`).
   - Giữ nguyên phiên bản byte-code target tương thích với Java ME (`--release 8`).
2. **Bảo Tồn Cấu Trúc Manifest:**
   - Giữ nguyên các trường thông tin trong `META-INF/MANIFEST.MF` (`MIDlet-Name`, `MIDlet-Vendor`, `MIDlet-Version`, `MIDlet-1`).
3. **Xử Lý Obfuscated Code:**
   - Tra cứu `memory/semantic/architecture-map.md` trước khi sửa các class bị mã hóa/rút gọn.
4. **Tạo File Mã Nguồn Java Mới Cho Mỗi Lệnh Auto (BẮT BUỘC):**
   - Mỗi khi mod tính năng mới, **BẮT BUỘC** tạo 1 tệp `.java` riêng trong `src/`.
   - Class chứa logic chính (`Runnable`), đăng ký và khởi chạy thread trong `Code.java`.

---

## 🎯 Target JAR Mặc Định
- File JAR chính: **`Aeharuna.jar`**
- Vị trí dự án: `/root/ninja/`
- Output: `/storage/emulated/0/Download/Aeharuna.jar`
