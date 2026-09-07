# Kiến Trúc Hệ Thống (Architecture Map) — Ninja School JAR Modding

> **Mục đích:** Tài liệu tham khảo kiến trúc toàn diện cho dự án mod `NinjaNamod.jar`.
> Phản ánh chính xác cấu trúc game gốc (obfuscated classes), hệ thống mã nguồn mod trong `src/`, giao thức nhóm, cơ chế di chuyển/rời map và quy trình đóng gói JAR.

---

## 1. Cấu Trúc Các Class Game Gốc (Obfuscated Classes)

### `Code` (`Code.class` / `src/Code.java`)
- **`gameAB`** (`Auto`): Con trỏ auto hiện tại. Nếu `gameAB != null` → menu chính hiện "Tắt Auto".
- **`gameAA(Auto)`**: Đẩy auto vào stack (`gameAB = auto`).
- **`gameAC()`**: Gỡ auto khỏi stack (`gameAB = gameAB.reAB`).
- **`gameAF(String)`**: Xử lý lệnh chat từ người chơi. Đã được hook sang `ChatRouter.checkAll(String)` tại `GameScr.class`.
- **`gameAH`** (`String`): Tên nhóm trưởng (Party Leader).
- **`gameCC`** (`TanSat`): Instance singleton của chức năng Tàn Sát gốc.
- **`gameAN()`**: Lệnh tự sát nhanh (gửi packet tự sát về làng).
- **⚠️ Lưu ý:** KHÔNG bao giờ patch bytecode `Code.class` trực tiếp. Mọi lệnh chat mở rộng đều đi qua `ChatRouter`.

### `Auto` (`Auto.class` / `src/Auto.java`)
- Lớp trừu tượng (abstract base) cho mọi chế độ auto trong game.
- **`gameAK()`**: Abstract method — được game loop gọi liên tục khi auto đang chạy.
- **`gameAC()`**: Gọi khi auto kết thúc.
- **`gameAD()`**: Gọi khi auto bắt đầu.
- **`mapID`** (`int`): Map mục tiêu.
- **`zoneID`** (`int`): Khu mục tiêu (`-2` = chế độ quét khu).
- **`reAB`** (`Auto`): Tham chiếu đến auto trước đó trong stack (danh sách liên kết).

### `PkBoss` (`PkBoss.class`)
- Kế thừa từ `Auto`. Nguyên bản trong game được dùng để tự động dò và đánh boss.
- **⚠️ Lưu ý quan trọng cho Modding:**
  - Thành viên nhóm **KHÔNG** dùng `PkBoss` vì `PkBoss` tự động quét khu, đánh lan man và tự ý gửi packet `pkm`/`pkk`/`pke` gây xung đột.
  - Thành viên nhóm sử dụng vòng lặp đánh tại chỗ riêng (`handleMemberLangCo`, `handleMemberLangTT`, `handleMemberNormalMap`).
  - Quá trình quay về map cũ (`returnAndResume`) dùng trực tiếp `TileMap.GoMap(savedMap)` thay cho `PkBoss`.

### `Service` (`Service.class`)
- Singleton mạng gửi packet lên máy chủ:
  - **`gI()`**: Lấy instance singleton.
  - **`gameAK(String)`**: Gửi tin nhắn chat nhóm (party chat / pkm / pkk).
  - **`gameAF()`**: Gửi lệnh hồi sinh về nhà.
  - **`gameAL()`**: Hồi sinh bằng lượng tại chỗ.
  - **`gameAH(int npcId)`**: Mở menu tương tác với NPC (ví dụ: NPC 7 ở Làng Cổ, NPC 47 ở Map VIP).
  - **`gameAC(int npcId, int menuIdx, int subIdx)`**: Chọn dòng menu NPC.
  - **`gameAQ(int itemMapId)`**: Gửi packet nhặt vật phẩm rơi trên đất.
  - **`gameAA(int zoneId, int itemIndex)`**: Đổi khu vực.

### `TileMap` (`TileMap.class`)
- **`mapID`** (`short`): ID map hiện tại nhân vật đang đứng.
- **`zoneID`** (`byte`): Khu hiện tại.
- **`GoMap(int mapID)`**: Thuật toán tìm đường đi bộ chuyển map gốc của game.
- **`isLangCo(int mapID)`**: Kiểm tra map có thuộc Làng Cổ hay không.
- **`gameAF()`**: Làm mới/tải lại dữ liệu map.
- **`vGo`** (`MyVector`): Danh sách cổng chuyển map (Waypoint).
- **`gameAJ(int index)`**: Bước qua cổng chuyển map số `index`.

### `GameScr` (`GameScr.class`)
- **`gameAC(String)`**: Thông báo nổi chữ vàng trên màn hình.
- **`vParty`** (`MyVector`): Danh sách thành viên trong nhóm.
- **`vMob`** (`MyVector`): Danh sách quái vật trên map.
- **`vItemMap`** (`MyVector`): Danh sách vật phẩm đang rơi trên đất.
- **`gameAI(int npcId)`**: Tìm đối tượng `Npc` theo ID trong map.

### `Char` (`Char.class`)
- **`getMyChar()`**: Singleton nhân vật người chơi hiện tại.
- **`statusMe`**: Trạng thái (`14` = Kiệt sức/chết).
- **`cHP`**: HP hiện tại.
- **`cx`, `cy`**: Tọa độ X, Y hiện tại.
- **`cName`**: Tên nhân vật.
- **`clevel`**: Cấp độ nhân vật.

---

## 2. Bản Đồ Phân Loại Map & Quy Tắc Di Chuyển / Rời Map

Toàn bộ hệ thống di chuyển giữa các boss và quay về map train (`savedMap`) được quản lý tập trung bởi **`AutoSanBoss.exitCurrentMapIfNeeded(targetMap)`**:

```
[Vị Trí Hiện Tại] ─────────── (Kiểm tra Target Map) ───────────► [Hành Động Rời Map]
       │
       ├── Làng Cổ (134-138) ──► Target Ngoài LC ──────► Ra M138 gọi NPC 7 về làng (KHÔNG tự sát)
       │                      └── Target Trong LC ──────► Đi trực tiếp cổng hub M138
       │
       ├── Làng TT (162-165) ──► Target Ngoài LTT ─────► Tự sát về làng
       │                      └── Target Trong LTT ─────► Đi trực tiếp
       │
       ├── VDMQ (139-148)    ──► Target CÙNG VDMQ ─────► KHÔNG tự sát, tự chạy qua (GoMap)
       │                      └── Target NGOÀI VDMQ ────► Tự sát về làng rồi chạy tiếp
       │
       ├── Map Ngoài (Thường) ─► Target bất kỳ ─────────► KHÔNG tự sát, tự chạy thẳng (GoMap)
       │
       └── Map VIP (195,196,192) ► Target khác VIP ─────► Tự sát về làng gọi NPC 47
```

### Chi Tiết Từng Loại Map:

| Loại Map | Dải Map ID | Cách Vào | Cách Rời Đi Chuẩn |
| :--- | :--- | :--- | :--- |
| **Làng Cổ** | `134 - 138` | Dùng Cổ Lệnh (ID 490/35/37) mua từ Shop 14 | Chạy về cổng M138 qua `returnToLangCoHub()`, tương tác **NPC 7 (Kojin)** chọn dòng 4 để về làng (**Tuyệt đối không tự sát**) |
| **Làng Truyền Thuyết (LTT)** | `162 - 165` | Mua VP Làng TT (ID 833) Shop 14 slot 39 | **Tự sát về làng** (`finishLangTTAndExit()`) |
| **Vùng Đất Ma Quỷ (VDMQ)** | `139 - 148` | Đi bộ từ Map 138/Kojin hoặc làng | - Nếu map đích **cũng thuộc VDMQ (139-148)**: **Tự chạy thẳng (không tự sát)**.<br>- Nếu map đích **ở ngoài VDMQ**: **Tự sát về làng** (`finishVDMQAndExit()`) |
| **Map Ngoài / Thế Giới** | 14, 15, 16, 20, 21, 41, 44, 45, 46, 54, 63, 65, 67, 70... | Đi bộ chuyển map (`TileMap.GoMap`) | **Tự chạy thẳng** bằng `TileMap.GoMap`, không cần tự sát |
| **Map VIP / Tu Luyện** | `192, 195, 196` | Qua NPC 47 ở thôn/làng | **Tự sát về làng** (`suicideAndEnsureAlive()`) |

---

## 3. Giao Thức Nhóm (Party Boss Protocol)

Trưởng nhóm và thành viên giao tiếp thông qua tin nhắn Party Chat ẩn:

| Lệnh Chat | Cú Pháp | Người Nhận | Hành Động Thực Hiện |
| :--- | :--- | :--- | :--- |
| `pkm <map> <zone>` | `pkm 140 5` | Thành viên | Nhận map và khu boss cùng lúc. Rời map hiện tại theo quy tắc an toàn, di chuyển đến map và đổi sang khu vực đó, sau đó vào vòng lặp đánh boss tại chỗ |
| `pkm -4` | `pkm -4` | Thành viên | **Lưu trạng thái trước khi săn:** Lưu map train gốc (`savedMap`), khu (`savedZone`), tọa độ (`savedX`, `savedY`) vào RMS |
| `pkm -5` / `pkm -6` | `pkm -5` | Thành viên | **Kết thúc săn boss:** Dừng đánh, kích hoạt cơ chế rời map hiện tại theo đúng quy tắc, quay về `savedMap` tiếp tục auto train |
| `pkm -3` | `pkm -3` | Thành viên | Dừng hoàn toàn auto party của thành viên |
| `pkm -2` | `pkm -2` | Thành viên | Chuyển thành viên sang chế độ treo boss |
| `pkm -1` | `pkm -1` | Thành viên | Chuyển thành viên sang chế độ săn thường |
| `ts` | `ts <map> <zone> <mob>`| Thành viên | Lệnh Tàn Sát nhóm gốc |

---

## 4. Các Module Mod Chính Trong `src/`

### 🎯 `AutoSanBoss.java` — Điều phối Săn Boss
- Quản lý luồng quét map của Trưởng nhóm (`huntBossType`, `pkBossOnMap`, `treoScanMap`).
- Quản lý luồng nhận lệnh của Thành viên: điều phối tập trung qua `handleMemberBoss` (bọc `handleMemberLangCo`, `handleMemberLangTT`, `handleMemberNormalMap`, `handleMemberMapVIP`), tự động hồi sinh và tái nhập map boss qua `navigateToMap` khi bị boss đánh chết, hỗ trợ đầy đủ Làng Cổ, Làng TT, Map VIP, Map Ngoài, VDMQ.
- Hàm rời map an toàn trung tâm: **`exitCurrentMapIfNeeded(int targetMap)`**.
- Cơ chế rời Làng Cổ chuẩn: **`finishLangCoAndExit()`** (qua NPC 7 tại M138).
- Cơ chế rời VDMQ: **`finishVDMQAndExit()`**.
- Tự động mời lại bạn bè khi mất nhóm: **`autoInviteFriends()`**.

### ⏰ `AutoBossEvent.java` — Bộ Hẹn Giờ & Quản Lý Event Săn Boss
- Chạy thread nền theo dõi chu kỳ spawn boss tự động:
  - **Pre-spawn (30s trước giờ boss):** Trưởng nhóm chạy ra map đợi trước.
  - **Lưu trạng thái farm (`saveLocalState` / `saveMemberState`):** Lưu map, khu, tọa độ X/Y của cả nhóm trước khi đi săn.
  - **Quay về farm (`returnMemberState` / `returnAndResume`):** Sau khi quét hết lượt boss, điều phối cả nhóm rời map hiện tại đúng quy tắc và dùng `TileMap.GoMap` quay về map train gốc, đổi đúng khu, đi đến đúng tọa độ và bật lại Tàn Sát.

### 🧭 `ChatRouter.java` — Cổng Điều Hướng Lệnh Chat
- Hook thay thế trực tiếp call site của `Code.gameAF(String)` trong `GameScr.class`.
- Tiếp nhận và điều hướng toàn bộ các lệnh chat mod:
  - `tspkb`, `tspkball`, `tspkbsv`, `tspkbtg`, `tspkbvm`, `tspkbmn`.
  - `pkm`, `pkk`, `pke`.
  - `tach <n>`, `tl <n>` (tách đồ lẻ).
  - `nhat` (hút vật phẩm).
  - `moinhom`, `mnb`.

### 🧹 `AutoPickup.java` — Hút Vật Phẩm Siêu Tốc
- Chế độ vacuum nhặt đồ: gửi packet `Service.gI().gameAQ(itemMapID)` cực nhanh (5ms/item).
- Hỗ trợ teleport nhặt xa và tự động quay về vị trí đứng ban đầu.

### 📊 `ThongKe.java` / `EcoMode.java` / `InfoMe.java`
- Hiển thị HUD thông tin số liệu khi treo máy (Yên, Xu, Lượng, EXP, thời gian săn boss).
- Tích hợp chế độ EcoMode tiết kiệm pin và CPU cho giả lập/điện thoại.

### 🎛️ `NamMod.java` & `SplitPatcher.java`
- Tạo Menu Tiện Ích "Nam Mod" tích hợp ngay vào menu 3 gạch của game.
- Mở nhanh các tính năng: Bật/tắt Săn Boss, Lịch Boss, Auto Bán Đồ, Mời nhóm, Tách đồ lẻ.

---

## 5. Quy Trình Build JAR Chuẩn (`NinjaNamod.jar`)

> ⚠️ **TUÂN THỦ TUYỆT ĐỐI THEO `AGENTS.md`**

1. **Khôi phục JAR gốc từ git:**
   ```powershell
   git checkout NinjaNamod.jar
   ```
2. **Unpack & Dọn sạch class cũ:**
   ```powershell
   Remove-Item -Recurse -Force build/unpacked -ErrorAction SilentlyContinue
   New-Item -ItemType Directory -Force build/unpacked | Out-Null
   Push-Location build/unpacked; jar xf ../../NinjaNamod.jar; Pop-Location
   Get-ChildItem src/*.java | ForEach-Object {
       $base = $_.BaseName
       Remove-Item -Force "build/unpacked/$base.class" -ErrorAction SilentlyContinue
       Remove-Item -Force "build/unpacked/$base`$*.class" -ErrorAction SilentlyContinue
   }
   ```
3. **Biên dịch Java source:**
   ```powershell
   javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/*.java
   ```
4. **Chạy các patches bytecode:**
   - `patch_class_j2me.py` (Hạ bytecode 52.0 → 45.3, gỡ StackMapTable).
   - `patch_gamescr_hienexp.py` & `fix_gamescr_thongke.py` (Hook ThongKe.draaw).
   - `patch_effectauto.py` (Tăng size mảng 20 → 100).
   - `patch_hsluong_pos.py` (Căn chỉnh vị trí menu).
   - Khôi phục `ChatManager.class` gốc từ `ban goc.jar`.
5. **Xóa `javax/` stubs & Đóng gói bằng `jar uf` (Update ZIP):**
   ```powershell
   Push-Location build/unpacked
   Remove-Item -Recurse -Force javax -ErrorAction SilentlyContinue
   Remove-Item -Force Char.class.bak_effects -ErrorAction SilentlyContinue
   Pop-Location
   git checkout NinjaNamod.jar
   $modClasses = Get-ChildItem build/unpacked/*.class | ForEach-Object { $_.Name }
   Push-Location build/unpacked
   jar uf ../../NinjaNamod.jar $modClasses
   Pop-Location
   ```
6. **Copy ra thư mục Downloads:**
   ```powershell
   Copy-Item NinjaNamod.jar -Destination "$env:USERPROFILE\Downloads\NinjaNamod.jar" -Force
   ```
