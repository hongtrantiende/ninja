# Lessons Learned

## 2026-09-01: Phân Biệt InfoDlg.gameAB() (Hiển thị) vs InfoDlg.gameAD() (Đóng dialog) & Bộ Lọc Từ Cấm Tạo Nhân Vật
- **Nguyên nhân bug kẹt xoay xoay:** Trong mã nguồn obfuscated Ninja School:
  - `InfoDlg.gameAB()` thực chất là hàm **HIỂN THỊ** bảng "Đang tải dữ liệu..." (với `gameAF = true` vẽ biểu tượng xoay xoay ở góc trên trong 5000 frame ~ 3 phút và khóa di chuyển).
  - Hàm **ĐÓNG/HỦY DIALOG** thực sự là `InfoDlg.gameAD()`.
  - Nếu gọi `InfoDlg.gameAB()` với ý định đóng dialog thì sẽ phản tác dụng, khiến màn hình bị kẹt xoay xoay và nhân vật không di chuyển được.
- **Bộ lọc từ cấm khi tạo nhân vật:** Server chặn các chuỗi chứa `cac` (như trong `cacao`), `lon`, `vai` (như trong `vaicalon`). Cần dùng helper `getCleanCharName()` chuyển đổi sang `kacao`, `vcalon` để tạo nhân vật thành công 100%.
- **Quy tắc vàng:**
  1. Khi cần tắt bảng loading/xoay xoay, LUÔN gọi `InfoDlg.gameAD()` kết hợp `GameCanvas.isLoading = false`, `GameCanvas.endDlg()`, `LockGame.gameBK()`.
  2. Mở khóa di chuyển: `Char.getMyChar().isLockMove = false`.


## 2026-08-29: Map VIP (195/196) & Tu Luyện (192) trong TS Boss Ưu Tiên
- **Nguyên nhân bug:** Khi tối ưu TS Boss lưu vị trí farm vào RMS, `saveLocalState()` đã lọc bỏ Map VIP (195, 196) vì nhầm lẫn với map boss tạm thời (như Làng Cổ 135/136). Khi người dùng đang cắm farm tàn sát tại Map VIP hoặc Tu Luyện, `savedMap` bị gán `-1` dẫn tới `returnAndResume()` thoát sớm (`if (map < 0) return;`) và không tự động quay lại map.
- **Quy tắc vàng:**
  1. Map VIP 1 (195), Map VIP 2 (196) và Map Tu Luyện (192) là các map cắm farm lâu dài của người chơi. BẮT BUỘC phải lưu đầy đủ `savedMap`, `savedZone`, `savedX`, `savedY` và ghi vào RMS khi bắt đầu phiên săn boss.
  2. Khi quay lại Map VIP/Tu Luyện, KHÔNG dùng `PkBoss` (vì map không có cổng dịch chuyển thông thường) mà PHẢI: tự sát về làng -> hồi sinh `Service.gI().gameAK()` -> gọi NPC 47 với menu option tương ứng (195: option 4, 196: option 5, 192: option 3) -> chuyển đúng khu -> di chuyển đúng tọa độ -> tiếp tục tàn sát.
  3. Khi thành viên nhóm nhận lệnh `pkm` sang map boss ngoài, phải kiểm tra `curMap != auto.mapID` để tự sát thoát ra khỏi Map VIP/Tu Luyện đi theo nhóm.


## 2026-08-28: TsBoost Thread Lifecycle & Limiter Recovery
- **Nguyên nhân bug:** `TsBoost.run()` có đoạn `if (Code.gameAB == null) { sleep(1500); if (Code.gameAB == null) break; }`. Khi có sự kiện ngắt quãng như TS Boss chuyển map/săn boss, hoặc mất kết nối, `Code.gameAB` bị `null` hoặc đổi sang `PkBoss` tạm thời khiến luồng `TsBoost` bị `break` và chết hẳn (`isRunning = false`). Khi TS được khôi phục hoặc kết nối lại, TsBoost không được gọi lại -> các tính năng như tự bán Phân Thân Lệnh, AoE boost bị mất.
- **Quy tắc vàng:**
  1. Không bao giờ `break` hủy thread TsBoost khi `Code.gameAB` tạm thời null/đang săn boss. Dùng `sleep(1000)` + `continue` để giữ thread sống, đồng thời duy trì chu kỳ quét bán PTL nếu limiter bật.
  2. Luôn hook `TsBoost.onTsStarted()` / `TsBoost.checkHang()` tại:
     - `Code.run()` (`checkHang()` kiểm tra và auto-start mỗi tick)
     - `Code.gameAA(Auto)` (mọi lời gọi push auto)
     - `Code.gameAP()` (`syncAfterTs()` khi reconnect)
     - `AutoBossEvent.returnAndResume()` (cả 2 nhánh khôi phục TS)


## Registration Lệnh Chat Mới — Phải Đăng Ký Trực Tiếp Trong Code.java
- **Nguyên nhân:** Các lệnh chat mới trước đây chỉ khai báo trong `ChatRouter.java`, nhưng call site chưa được trigger đúng khiến chat lệnh (như `tspkball`, `all`, `sv`, `tg`, `vm`, `mn`, `nhat`) không phản hồi.
- **Giải pháp:** Đăng ký trực tiếp tất cả câu lệnh chat mới vào phương thức `gameAF(String)` trong [`src/Code.java`](file:///root/ninja/src/Code.java) và biên dịch lại `Code.java` cùng toàn bộ tệp nguồn `src/*.java`. Khi đó game engine nhận diện chính xác 100% khi người dùng gõ lệnh.

## Compilation Java ME with JDK 21
- **Vấn đề:** Khi biên dịch `Code.java` với `javac` Java 21, `javac` đòi hỏi các class Java ME (`javax.microedition.lcdui.*`, `javax.microedition.midlet.*`). Các stub class này tự động được javac tạo ra trong `build/unpacked/javax`. Nếu đóng gói nguyên thư mục này vào file JAR, máy điện thoại/J2ME Loader sẽ báo lỗi cài đặt (Security Violation / Overriding system package).
- **Giải pháp:** Sau khi chạy `javac`, **BẮT BUỘC** phải chạy `Remove-Item -Recurse -Force "build/unpacked/javax"` trước khi đóng gói.

## JAR Packing — Đúng lệnh đóng gói
- **Vấn đề:** `jar cf * ` corrupt ZIP vì include `META-INF/` 2 lần khi dùng wildcard.
- **Giải pháp đúng:** `jar cfm '../../Aeharuna.jar' 'META-INF/MANIFEST.MF' *.class *.txt *.png font map x1` — liệt kê explicit, KHÔNG dùng wildcard `*`.
- **File bị lock:** Nếu file JAR đang mở trong emulator, dùng tên khác (v2, v3...) hoặc đóng emulator trước.

## Code.gameAB — Gate cho menu "Tắt Auto"
- **Phát hiện:** `Code.gameAB` là field quyết định menu hiển thị nút "Tắt Auto" trong `GameScr.gameCD()`. Nếu `gameAB == null` → menu KHÔNG hiện "Tắt Auto".
- **Vấn đề:** Dùng `PkBoss(0)` dummy bị game loop xóa ngay (`Code.gameAC()` pop auto stack).
- **Giải pháp:** Tạo `SanBossHolder` extends `Auto` — class rỗng, override `gameAC()`, `gameAD()`, `gameAK()` không làm gì → giữ `Code.gameAB` luôn != null.
- **Quan trọng:** `Auto` có abstract method `gameAK()` — BẮT BUỘC override, nếu không `javac` báo lỗi.

## PkBoss — Tự động quét khu + đánh boss + gửi lệnh nhóm
- **QUAN TRỌNG:** PkBoss khởi tạo `zoneID = -2` (chế độ quét). Trong `gameAK()` loop, PkBoss TỰ ĐỘNG:
  1. Chuyển map đến `mapID`
  2. Quét tất cả khu (zone) tìm boss
  3. Khi tìm thấy boss → đánh
  4. Gửi `pkm`/`pkk` qua `Service.gameAK()` cho nhóm
  5. Khi xong gửi `pke` (pk end)
- **SAI:** `Service.gI().gameAA(zone, -1)` KHÔNG PHẢI API chuyển khu! Đây là method khác (TanSat setup). Kết quả: nhân vật đứng yên, không chuyển khu.
- **ĐÚNG:** Để PkBoss tự quét — `Code.gameAA(new PkBoss(mapID))` → chờ `Code.gameAB instanceof PkBoss` hết → PkBoss đã xong.

## Party Commands — Flow Leader ↔ Member
- **Leader gửi:** `Service.gI().gameAK(String)` → gửi party chat
- **Member nhận:** Handler trong `Code.java` source (dòng 2510+):
  - `"ts" mapID zoneID templateId` → Member bật TanSat (`Code.gameAA(gameCC)`)
  - `"pkm" mapID` → Member bật PkBoss (`Code.gameAA(new PkBoss(mapID))`) → `gameAB = PkBoss` → menu hiện "Tắt Auto"
  - `"pkk" zoneID` → Member chuyển khu (`gameAB.zoneID = zone`)
  - `"pke"` → Member tắt PkBoss (`Code.gameAC()`)
- **Quan trọng:** Member chỉ hiện "Tắt Auto" khi `Code.gameAB != null`. Phải gửi `pkm` ngay khi leader bật party mode để member bật PkBoss chờ sẵn.
- **PkBoss gửi pkm/pkk nội bộ:** PkBoss tự gửi party commands trong `gameAK()` loop khi tìm thấy boss. KHÔNG cần gửi thủ công khi PkBoss đang chạy.

## Boss Spawn Data
- **Thời gian sống:** 40 phút (2400 giây), KHÔNG phải 15 phút.
- **MapNgoai:** Boss spawn trên TẤT CẢ 12 map `{14,15,16,44,67,70,24,41,45,18,36,54}`, không phân biệt level nhân vật.
- **Cùng giờ:** Server 22h + MapNgoai 22h → spawn đồng thời → phải quét tuần tự TẤT CẢ loại boss, không chỉ 1.

## sendPartyCommand Timing
- **SAI:** Gửi `pkm` cho nhóm ở MỌI map khi quét → nhóm bị kéo qua tất cả map vô ích.
- **ĐÚNG:** Leader quét solo, CHỈ khi `hasBossOnCurrentMap() == true` mới gửi `pkm + pkk` cho nhóm → nhóm chỉ di chuyển khi có boss thật.

---

## 🔧 TSPKB — Lịch Sử Fix Bug & Tiến Độ (v1 → v9)

### v1-v3: Sai JAR packing
- **Bug:** JAR corrupt, emulator không nhận.
- **Fix:** Đổi `jar cf *` → `jar cfm ... *.class *.txt *.png font map x1`. Xóa `build/unpacked/javax` trước khi pack.
- **Status:** ✅ FIXED

### v4: Menu không hiện "Tắt Auto"
- **Bug:** Khi bật `tspkb`, mở menu mod không thấy nút "Tắt Auto".
- **Root cause:** `Code.gameAB = null` khi AutoSanBoss thread chạy nhưng PkBoss chưa active.
- **Fix:** Tạo `SanBossHolder` extends `Auto` (class rỗng) → `Code.gameAB = dummyAuto` → menu luôn hiện "Tắt Auto".
- **Status:** ✅ FIXED

### v5: Không tách lệnh `tsnpkb` riêng
- **Bug:** Muốn lệnh `tsnpkb` cho party mode.
- **Root cause:** Chat handler nằm trong compiled Code.class, không thể thêm lệnh mới vào source dễ dàng.
- **Fix:** `toggle()` tự detect nhóm (`GameScr.vParty.size() > 1`) → party mode ON tự động, không cần lệnh riêng.
- **Status:** ✅ FIXED (workaround)

### v6: Không chuyển khu — nhân vật đứng yên
- **Bug:** Hiện "TSB: M23 K0" nhưng không quét sang khu 1, 2, 3...
- **Root cause:** `Service.gI().gameAA(zone, -1)` KHÔNG phải API chuyển khu! Đây là method khác.
- **Fix v6:** Giảm timeout, thêm consecutiveFails → vẫn không chuyển khu.
- **Status:** ❌ NOT FIXED (v6 vẫn lỗi)

### v7: Rewrite — Để PkBoss tự quét
- **Fix:** Xóa toàn bộ scan thủ công (`scanZone`, `travelToMap`, `scanAndFightOnMap`, `startPkBossAndWait`). Thay bằng `pkBossOnMap()`: start `PkBoss(mapID)` → PkBoss tự quét khu, tìm boss, đánh.
- **Status:** ✅ FIXED chuyển khu. Nhưng phát sinh 2 bug mới:
  - Party members bị kéo qua mọi map (PkBoss gửi pkm nội bộ khi quét)
  - Members không thấy "Tắt Auto" trong menu

### v8: Chỉ gửi lệnh nhóm khi tìm thấy boss
- **Bug:** Party members bị kéo qua tất cả map dù không có boss.
- **Fix:** Thêm `sentPartyCmd` flag + `hasBossOnCurrentMap()` check → chỉ gửi `pkm + pkk` khi boss thật sự có.
- **Status:** ✅ FIXED

### v9: Members hiện "Tắt Auto" + tắt nhóm
- **Bug:** Members không thấy "Tắt Auto" vì chưa nhận `pkm` khi leader bật.
- **Fix:** 
  - Khi bật party mode → gửi `pkm currentMap` ngay → members bật PkBoss → hiện "Tắt Auto".
  - Khi tắt → gửi `pke` → members tắt PkBoss.
- **Status:** ✅ FIXED (chưa test thực tế)

### v11: Bytecode Patching — INSERT/APPEND đều FAIL, chỉ REPLACEMENT an toàn
- **Bug:** JAR không khởi chạy được sau khi patch `Code.class` thêm 4 lệnh chat mới.
- **3 lần thất bại:**
  1. **INSERT giữa method (patch_boss_commands.py v1):** Chèn 64 bytes bytecode giữa gameAF. Fix được ifeq branch offset nhưng QUÊN fix StackMapTable + Exception table → JVM verifier crash.
  2. **INSERT giữa method (patch_boss_commands.py v2):** Fix cả StackMapTable + Exception table + branch offsets. Nhưng QUÊN fix LineNumberTable + các goto/branch instructions KHÁC trong method trỏ đến PC sau insert point → vẫn crash.
  3. **APPEND cuối method:** Thay `iconst_1; ireturn` ở cuối gameAF bằng call ChatRouter. Nhưng PC 9065 LÀ branch target (có StackMapTable entry) → thay đổi code tại đó vẫn crash.
- **Giải pháp THÀNH CÔNG — Methodref Replacement:**
  - **KHÔNG patch Code.class** (quá phức tạp, 9067 bytes bytecode, 334 StackMapTable entries)
  - Patch **GameScr.class** thay thế: tìm `invokestatic Code.gameAF(String)Z` → đổi sang `invokestatic ChatRouter.checkAll(String)Z`
  - Chỉ thay **2 bytes** (methodref index) + thêm CP entries
  - `ChatRouter.checkAll()` gọi `Code.gameAF()` gốc + check 4 lệnh mở rộng
  - **KHÔNG thay đổi bytecode structure** → không StackMapTable/Exception/LineNumber issues
- **Quy tắc VÀNG khi thêm lệnh chat mới:**
  1. **KHÔNG BAO GIỜ** insert/append bytecode vào method lớn (>1000 bytes code_length)
  2. **Luôn dùng Methodref Replacement:** Tìm call site → thay methodref → wrapper class gọi method gốc + logic mới
  3. File patcher: `build/patch_gamescr.py` (mẫu chuẩn)
  4. Wrapper class: `src/ChatRouter.java` (gọi Code.gameAF gốc + check lệnh mới)
- **Status:** ✅ THÀNH CÔNG — Game chạy, 4 lệnh mới hoạt động

### v11b: Nhặt đồ nhanh khi boss chết (grabAllItems)
- Thêm method `grabAllItems()` vào AutoSanBoss
- Gửi `Service.gI().gameAQ(itemMapID)` cho TẤT CẢ item trên đất chỉ 30ms/item
- Nhanh gấp ~5x so với auto pickup mặc định (1 item/tick, 50ms)
- Gọi tự động khi PkBoss kết thúc (boss chết)
- **Status:** ✅ Implemented (chưa test thực tế)

### v12: AutoPickup — Nhặt đồ nhanh cho ts/tsn/ak
- **Tạo class:** `src/AutoPickup.java` — thread riêng nhặt TẤT CẢ item 30ms/item liên tục
- **Lệnh mới:** `nhat` → toggle AutoPickup on/off (qua ChatRouter)
- **Tự động hook:** Khi user gõ `ts`/`tsn`/`ak`:
  - ChatRouter intercept → gọi `Code.gameAF(text)` gốc trước (xử lý bật/tắt auto bình thường)
  - Sau đó check `Code.gameAB != null` → bật AutoPickup / `null` → tắt AutoPickup
- **An toàn:** `text.equals("ts")` chỉ match exact "ts", không match "ts 1 2 3" (lệnh nhóm có tham số)
- **Game gốc:** `Code.gameAQ` nhặt 1 item/tick (chọn gần nhất, 50ms delay). AutoPickup nhặt TẤT CẢ item song song.
- **Status:** ✅ Implemented (chưa test thực tế)

### v13: patch_service.py — ifeq offset sai gây game đơ khi login
- **Bug:** Ấn "Chơi tiếp" game bị đơ (freeze) hoàn toàn, không vào được.
- **Root cause:** `scripts/patch_service.py` chèn 9 bytes bytecode vào đầu `Service.gameAA(short, String)` với `ifeq` offset +5 (sai, đúng phải là +4). Khi `SplitPatcher.checkSplit()` trả `false` (luôn luôn), `ifeq` nhảy tới PC 10 (`astore_3`) thay vì PC 9 (`aconst_null`) → bỏ qua `aconst_null` → stack trống khi `astore_3` cần pop → **VerifyError / crash**.
- **Tại sao nghiêm trọng:** `Service.gameAA(short, String)` = Packet 92, được gọi ngay trong quá trình login → game đơ ngay khi ấn "Chơi tiếp".
- **Fix:** Thay `Service.class` bị patch bằng bản gốc (từ JAR cũ). `SplitPatcher.checkSplit` vốn đã `return false` nên hook này vô dụng.
- **Quy tắc VÀNG bổ sung:** **KHÔNG BAO GIỜ INSERT bytecode vào method bất kỳ** — kể cả chèn đầu method. Dù fix Exception table đúng, offset `ifeq`/`goto` chỉ sai 1 byte cũng crash. Luôn dùng **Methodref Replacement** (thay 2 bytes index trong constant pool).
- **Status:** ✅ FIXED

### v14: AutoSanBoss — Phục hồi menu "Tắt Auto" cho thành viên nhóm khi theo Trưởng nhóm
- **Bug:** Khi thành viên ở trong nhóm và theo trưởng nhóm săn boss, menu "Tắt Auto" bị mất (do `Code.gameAB` bị set thành `null` hoặc bị ghi đè bởi auto khác).
- **Root cause:** 
  1. `restoreDummyAuto()` trước đây chỉ khôi phục khi `Code.gameAB == null`, nên nếu `gameAB` bị ghi đè bởi object auto tạm khác (không phải `PkBoss`) thì không tự restore lại được.
  2. Chu kỳ chờ của thành viên nhóm (`sleepSeconds(5)`) quá dài làm chậm việc phát hiện và phục hồi menu.
- **Fix:** 
  1. Cập nhật `restoreDummyAuto()` trong `AutoSanBoss.java`: Tự động khôi phục `SanBossHolder` ngay khi `Code.gameAB` bị `null` HOẶC bị ghi đè bởi auto khác (chỉ chừa lại `PkBoss` vốn thuộc luồng săn boss).
  2. Giảm thời gian chờ của thành viên từ `5s` xuống `2s` để quét khôi phục menu liên tục.
- **Status:** ✅ FIXED

### v15: AutoSanBoss — Sửa logic tự động mời Nhóm & Bạn bè khi Trưởng nhóm gõ tspkb
- **Bug:** Khi Trưởng nhóm chưa có nhóm (`vParty.size() <= 1`) và gõ `tspkb`, game coi như chạy Solo (`hasParty = false`) nên KHÔNG mời nhóm. Ngoài ra `autoInviteFriends()` trước đây chỉ quét danh sách nhóm `Code.gameAI`, chưa quét danh sách Bạn Bè `GameScr.vFriend`.
- **Fix:**
  1. Thêm `checkHasPartyOrFriends()`: Nếu đã ở trong nhóm HOẶC có tên nhóm đã lưu (`Code.gameAI`) HOẶC có Bạn Bè (`GameScr.vFriend`) -> Bật chế độ săn boss nhóm (`isPartyMode = true`).
  2. Nâng cấp `autoInviteFriends()`: Quét cả danh sách nhóm `Code.gameAI` VÀ các bạn bè trong `GameScr.vFriend` để gửi lời mời vào nhóm.
- **Status:** ✅ FIXED

---

## 🚧 CÁC VẤN ĐỀ CÒN TỒN TẠI (Cần test phiên sau)

### 1. PkBoss quét khu có thật sự hoạt động?
- **Mô tả:** PkBoss khởi tạo `zoneID = -2` → lý thuyết quét tất cả khu. Nhưng chưa xác nhận thực tế: có chuyển khu liên tục không? Có dừng khi hết khu không?
- **Cách test:** Bật `tspkb` khi có boss spawn, xem log message "TSB: PK M23" → PkBoss có tự chuyển khu không. Xem PkBoss chạy bao lâu trước khi kết thúc.
- **Priority:** HIGH

### 2. PkBoss tự gửi pkm/pkk khi quét → members bị kéo theo?
- **Mô tả:** PkBoss nội bộ có thể gửi `pkm`/`pkk` cho nhóm trong `gameAK()` loop. Nếu vậy, dù ta không gửi manual, PkBoss vẫn kéo members đi mọi map.
- **Cách kiểm tra:** Xem chat nhóm khi leader quét solo — có thấy pkm/pkk từ PkBoss tự gửi không?
- **Giải pháp nếu lỗi:** Có thể cần tạm rời nhóm khi quét, hoặc override PkBoss behavior.
- **Priority:** MEDIUM

### 3. Member nhận pkm → PkBoss member hoạt động đúng?
- **Mô tả:** Khi member nhận `pkm mapID`, member tạo `PkBoss(mapID)` → member có tự di chuyển đến map boss không? Có đánh boss cùng leader không?
- **Cách test:** Chạy 2 account cùng nhóm, bật `tspkb` trên leader, xem member có tự di chuyển + đánh boss.
- **Priority:** HIGH

### 4. Respawn sau khi chết — PkBoss restart có ổn?
- **Mô tả:** Khi leader chết, `respawnFast()` gọi `Service.gI().gameAF()` + `GameScr.gameAB(5,0,0)`. Sau đó restart PkBoss. Nhưng nhân vật có thể bị teleport về nhà → PkBoss phải tự travel lại map boss.
- **Cách test:** Để nhân vật yếu vào map boss, chết, xem tự hồi sinh + quay lại đánh tiếp không.
- **Priority:** MEDIUM

### 5. Chuyển map liên tục 12 map MapNgoai — có quá chậm?
- **Mô tả:** MapNgoai có 12 map. Mỗi map PkBoss quét → nếu không boss thì PkBoss kết thúc nhanh (<5s). Nhưng 12 map × 5s = 60s chỉ cho 1 round.
- **Cách test:** Đo thời gian quét 1 round đầy đủ 12 map. Nếu quá chậm, có thể cần ưu tiên map nào quét trước.
- **Priority:** LOW

### 6. `toggle()` auto-detect nhóm — edge case
- **Mô tả:** Nếu player bật `tspkb` rồi mới vào nhóm → `GameScr.vParty.size()` lúc bật = 0 → solo mode → không gửi lệnh nhóm. Phải tắt + bật lại `tspkb`.
- **Giải pháp tiềm năng:** Check `vParty.size()` liên tục trong run() loop, không chỉ lúc toggle.
- **Priority:** LOW

### v16: AutoPickup v2 — Vacuum Mode (Hút VP tốc độ cao)
- **Bug v1:** Nhặt từng cái (30ms/item), chỉ 3 vòng, walk chậm (`Char.gameAC` = đi bộ), items boss rơi xa ~200px range chỉ nhặt ~10 cái ở chân, không có thông báo.
- **Root cause:**
  1. Server validate khoảng cách pickup (~100px). Items xa bị reject im lặng.
  2. `Char.gameAC(x,y)` = đi bộ (slow walk), 50ms chờ không đủ thời gian đến nơi.
  3. Game gốc có "nhặt xa" (`Code.gameAQ` / lệnh `cnhat`) nhặt 1 item/tick trong 100px nhưng AutoPickup v1 không bật flag này.
- **Fix v2 — Vacuum Mode:**
  1. **Blast**: Gửi `Service.gI().gameAQ(itemMapID)` cho TẤT CẢ item cùng lúc (5ms/item thay 30ms).
  2. **Auto-tele**: Nếu sau blast vẫn còn item (server reject do xa) → teleport đến item gần nhất (`myChar.cx/cy` + `Char.gameAC` cho server sync) → blast lại.
  3. **Song song**: Bật `Code.gameAQ = true` để game gốc nhặt xa hỗ trợ đồng thời.
  4. **15 vòng** thay 3, scan delay 50ms thay 200ms.
  5. **Feedback**: Hiện "Hút X/Y VP!" sau khi xong.
  6. **Return**: Về vị trí ban đầu sau khi hút xong.
- **Tốc độ ước tính**: 200 items × 5ms = 1s blast + tele, tổng ~3-5s thay vì 30s+ cũ.
- **Status:** ✅ Built (chưa test thực tế)

### v17: MultiSkillAttack v2 — Auto Buff + Cooldown + Elemental Intelligence
- **Phân tích:** Reverse engineer bytecode `Auto.class` (dứa mod) vs `MultiSkillAttack.java` (nam mod).
- **Phát hiện 4 thiếu sót nam mod:**
  1. Không check cooldown → gửi skill chưa hết CD → server reject
  2. Không auto buff (bỏ skill type 2) → không tự bật khiên/đốt quái
  3. Không check nguyên tố → đốt mob đang cháy = lãng phí mana
  4. Buff dùng sai packet (`gameAA` thay `gameAR`)
- **Fix v2:**
  - **P1 (Cooldown):** `isSkillReady(s)` check `elapsed >= coolDown - 300ms`. Set `paintCanNotUseSkill=true` khi chưa hết CD.
  - **P2 (Auto Buff):** `autoBuff()` duyệt `vSkillFight`, check `type==2`, check Effect đã active (`isBuffActive`), gọi `gameAR()` thay `gameAA()`.
  - **P3 (Elemental):** `checkElementalConditions()`: skip fire skill (id 7,16) nếu `mob.isFire`, skip ice (25,34) nếu `!mob.isIce`, skip wind (43) nếu `!mob.isWind`. Type 3 chỉ dùng khi boss hoặc mob HP > 50%.
  - **P4 (Smart Buff):** `checkBuffConditions()`: id 31 chỉ khi `!KhienMana`, id 15 chỉ khi `DotQuai && HP < aHpValue%`, id 6 chỉ khi `isHuman`.
- **API quan trọng:** `Service.gameAR()` = buff self packet, `Service.gameAG(int)` = select skill, `Service.gameAA(MyVector,MyVector,int)` = attack packet.
- **Status:** ✅ Built (chưa test thực tế)

---

## 📋 TODO Phiên Sau
1. Test v9 trên emulator: bật `tspkb` có nhóm → xem members có nhận `pkm` + hiện "Tắt Auto"
2. Chờ boss spawn → xem PkBoss có tự quét khu + tìm boss + đánh không
3. Xem chat nhóm có bị spam pkm/pkk từ PkBoss nội bộ không
4. Test chết + hồi sinh + quay lại boss
5. Nếu PkBoss nội bộ gửi party commands gây rối → cần giải pháp mới
- **[2026-07-31] Chèn Nút Vào Menu (Hook Menu.gameAA)**: 
  Không nên vá `GameScr.class` bằng cách `insert/append bytecode` vì class này quá lớn (sẽ làm hỏng ExceptionTable, StackMapTable, LineNumberTable nếu không fix offset toàn diện). 
  Thay vào đó, dùng `Methodref Replacement`: Đổi lệnh `invokevirtual Menu.gameAA` thành `invokestatic SplitPatcher.hookMenu(Menu, MyVector)`. Trong hàm `hookMenu`, có thể dễ dàng sửa đổi `MyVector` (như thêm bớt `Command`) trước khi gọi tiếp `Menu.gameAA(MyVector)`. Phương pháp này vô cùng an toàn và dễ triển khai trên Java ME.
- **[2026-08-01] BẮT BUỘC: Chạy `patch_gamescr_menu.py` sau mỗi lần unpack JAR**:
  Khi unpack `Aeharuna.jar` vào `build/unpacked/`, GameScr.class CHƯA CÓ hook `SplitPatcher.hookMenu`. **PHẢI** chạy:
  ```powershell
  $env:PYTHONIOENCODING="utf-8"; python scripts/patch_gamescr_menu.py build/unpacked/GameScr.class
  ```
  TRƯỚC KHI đóng gói JAR. Nếu quên → NamMod sẽ KHÔNG hiện trong menu 3 gạch.
  Script thay 82 lời gọi `invokevirtual Menu.gameAA(MyVector)` → `invokestatic SplitPatcher.hookMenu(Menu, MyVector)`.
  Script tự detect nếu đã patch (check `hookMenu` in data) → chạy lại an toàn.
- **[2026-08-01] AutoSanBoss.toggleInternal() NPE khi tắt**:
  `GameScr.vParty.size()` gây NullPointerException khi không có nhóm → không tắt được boss từ menu.
  **Fix:** Thêm `GameScr.vParty != null &&` trước `.size()`.
- **[2026-08-01] Code.java: Hook pkm/pke → ChatRouter cho party member**:
  Code gốc dứa mod khi nhận `pkm` tạo `PkBoss` trực tiếp (`Code.gameAA(new PkBoss(...))`) mà KHÔNG gọi `AutoSanBoss.startPartyMember()`.
  → Thành viên nhóm không có icon "PK Boss" nhấp nháy.
  **Fix:** Sửa `Code.java`:
  - `pkm`: `ChatRouter.startPartyBoss(pBoss)` — wrapper gọi cả `AutoSanBoss.startPartyMember()` + `Code.gameAA(auto)`
  - `pke`: `ChatRouter.stopPartyBoss()` — wrapper gọi cả `Code.gameAC()` + `AutoSanBoss.stop()`
  **LƯU Ý:** Code.java là file decompiled lớn (2722 dòng), compile bằng `javac -source 8 -target 8`.

### v18: AutoPickup v3.1 — Ghost Move Nhặt Xa (Kỹ Thuật Quan Trọng)
- **Vấn đề:** Server check khoảng cách khi nhặt item (`gameAQ`). Nếu nhân vật xa item > ~30-50px → server reject "Khoảng cách quá xa".
- **Kỹ thuật Ghost Move:**
  ```java
  // 1. Lưu vị trí gốc
  int origCx = myChar.cx, origCy = myChar.cy;
  
  // 2. Với MỖI item xa:
  Char.gameAC(item.xEnd, item.yEnd);  // Gửi PACKET vị trí đến item (server nghĩ ta ở đó)
  Service.gI().gameAQ(item.itemMapID); // Gửi nhặt (server accept vì "gần")
  
  // 3. Cuối vòng: quay về
  Char.gameAC(origCx, origCy);  // Server sync lại vị trí thật
  myChar.cx = origCx;            // GIỮ NGUYÊN cx/cy client → không giật màn hình
  myChar.cy = origCy;
  ```
- **Tại sao không giật khi đánh quái:**
  - `Char.gameAC(x,y)` chỉ gửi **packet mạng** (movement request), KHÔNG thay đổi vị trí vẽ trên client.
  - `myChar.cx/cy` quyết định nơi nhân vật hiển thị → giữ nguyên = nhân vật đứng yên.
  - Thread AutoPickup chạy **riêng biệt** với game loop → không block render/attack.
  - Cuối vòng gửi `gameAC(origCx, origCy)` → server biết vị trí thật.
- **Phạm vi:** TOÀN MAP — không giới hạn khoảng cách.
- **GHOST_RANGE:** 50px — item gần hơn nhặt trực tiếp, xa hơn mới ghost move.
- **API:** `Char.gameAC(int x, int y)` = static method gửi walk/move packet.
- **Status:** ✅ Built + tested (server accept nhặt xa)

### Git / GitHub Rules
- **Tuy?t d?i KH�NG t? � git commit ho?c git push** l�n GitHub n?u ngu?i d�ng chua ra l?nh r� r�ng.

### v19: Phân biệt Luồng PkBoss (Đánh) vs Treo Boss (Đứng chờ)
- **Vấn đề:** PkBoss mặc định có logic tự quét khu, tự tele tới boss và tự tấn công. Không thể dùng PkBoss làm scanner cho chế độ treo vì nó sẽ đánh boss trước khi can thiệp.
- **Giải pháp Scanner thủ công:** Dùng PkBoss để di chuyển tới mapID target. Ngay khi TileMap.mapID == targetMap, dừng PkBoss bằng Code.gameAC(). Sau đó chạy vòng lặp zone 0..29 gọi Auto.gameAA(zone) với sleep(100ms) để đổi khu thủ công.
- **Tránh False-Positive Mob Data:** Khi đổi khu nhanh (100ms), GameScr.vMob có thể chứa dữ liệu mob cũ từ khu trước. Bắt buộc phải sleep 300ms và double-check hasBossOnCurrentMap() trước khi xác nhận có boss.
- **Giao thức Party pke không ngắt Thread:** Đội trưởng gửi pke để ngắt PkBoss của thành viên khi tới nơi. Bình thường pke gọi stopPartyBoss() -> AutoSanBoss.stop() làm tắt toàn bộ auto. Trong treoMode, stopPartyBoss() chỉ thực hiện Code.gameAC() + restoreDummyAuto() để pop PkBoss mà vẫn giữ thread thành viên sống tiếp tục đứng treo tại khu.
### v20: Không Dùng Chung Một Tín Hiệu Cho Đánh Boss Và Treo Boss
- **Bài học:** Một thread thành viên có thể tiếp tục sống sau `pke` trong Treo Boss. Vì vậy không được suy ra chế độ hiện tại chỉ từ `isRunning`; phải truyền mode rõ ràng từ leader.
- **Quy tắc giao thức:**
  - `pkm -1` = normal combat mode.
  - `pkm -2` = treo/wait mode.
  - `pkm -3` = full stop/cleanup.
- **Quy tắc `pkk`:**
  - Trong mode đánh: đặt `PkBoss.zoneID`, cho `PkBoss` tiếp tục tele/đánh.
  - Trong mode treo: không giao zone cho `PkBoss`. Chờ tới đúng map, pop `PkBoss`, rồi gọi `Auto.gameAA(zone)` để đứng tại điểm vào khu.
- **Chống giữ trạng thái cũ:** Trước mỗi lần gọi nhóm tới boss, leader phải gửi lại tín hiệu mode (`-1` hoặc `-2`), không chỉ gửi một lần lúc bật.
- **Cleanup:** Khi tắt từ leader phải gửi `pkm -3` trước `pke`; chỉ gửi `pke` là chưa đủ vì trong Treo Boss `pke` cố ý giữ thread thành viên sống.
- **Kết quả đã xác nhận:** `tspkb` cho cả leader và thành viên đánh; `tstreo/treo` cho cả nhóm đứng chờ, không tele vào boss.

## 2026-08-03: Khoi phuc TS sau boss phai luu cuc bo tren tung client
- Khong the dung map/khu cua leader lam diem quay ve cho ca nhom, vi thanh vien dang treo su kien o cac khu khac nhau.
- Phai giu nguyen tham chieu Auto cu cua tung may, dung PkBoss chi de van chuyen ve map, doi khu bang Auto.gameAA(zone), sau do gan lai auto cu.
- Tin hieu bat dau (pkm -4) chi luu trang thai; thanh vien van TS den khi leader tim thay boss. Tin hieu ket thuc (pkm -5) moi kich hoat quay ve va resume.
## 2026-08-03: Bai hoc tu test tsbosstest
- Lenh test chay ngay khac voi tsboss cho lich: phai co Auto ts san truoc khi chup savedAuto.
- Neu nguoi dung go tsbosstest roi moi go ts, lenh ts se ghi de flow PkBoss va co the lam phien khong di map.
- Khong nen tu choi test chi vi inEvent=true. Che do test can co kha nang stop phien cu, ha co inEvent, cho thread cu thoat, roi force TYPE_ALL lai.
- Khi thay thong bao 'Dang trong phien san boss' ma nhan vat khong di, can xem day la stale state/race thread, khong coi la phien dang hoat dong hop le.
## 2026-08-04: Bai hoc ve chuyen khu TS, softkey va auto khoi tao tre
- Gan Code.gameAB=null khong huy het chuyen khu; LockGame co the van giu luong/giai phong packet. Can LockGame.gameBK truoc khi PkBoss chiem quyen dieu khien.
- Khong dung timer cung de dung san boss giua tran. Timer chi cho phep dung tai diem ket thuc mot luot day du.
- Softkey R Java ME khong phai ASCII R. Ma dung thuong la -7 hoac -22; chi intercept khi dang co auto de khong pha nut Back/phai cua game.
- Shortcut phai chan truoc GameGraphics.gameAA; neu xu ly sau, game co the mo chat/menu truoc khi mod nhan phim.
- Code.gameAF("ts") co the tra ve truoc khi TanSat duoc gan vao Code.gameAB. Kiem tra ngay sau lenh se bo sot Hut VP; can watcher ngan han doi TanSat.

## 2026-08-05: GhostBoss — Exploit Bug Server Đánh Boss Vô Hình
- **Phát hiện:** Server chỉ validate `Mob.mobId` (1 byte, short) trong attack packet. KHÔNG validate client có load mob đó trong `GameScr.vMob` hay không.
- **Packet Attack:** `Service.gameAA(MyVector mobs, MyVector chars, int skillType)` gửi Message type 4 (skillType=1) hoặc 73 (skillType=2). Nội dung: `writeByte(count)` + `writeByte(Mob.mobId)` cho mỗi mob.
- **Exploit Flow:**
  1. Vào map boss, quét zone tìm boss → ghi nhớ `Mob.mobId` (short)
  2. Tạo fake `Mob` object với đúng `mobId` → add vào `MyVector` targets
  3. Gọi `Service.gI().gameAA(targets, emptyChars, 1)` liên tục
  4. Server nhận `mobId`, tìm boss trên server, xử lý damage → trả kết quả
  5. Nhân vật đứng im, không cần thấy boss
- **Mob Constructor:** `Mob(short mobId, boolean isDisable, boolean isDontMove, boolean isFire, boolean isIce, boolean isWind, int templateId, int sys, int hp, int maxHp, int level, short x, short y, byte status, byte levelBoss, boolean isBoss, boolean removeWhenDie)`
- **Lệnh:** `gb` / `ghostboss` (auto detect) hoặc `gb65` (chỉ map 65)
- **Status:** ✅ Built (chưa test thực tế — cần xác nhận server có reject hay không)

## 2026-08-06: J2ME Loader DEX Converter — Class Version & StackMapTable

### Vấn đề
Khi compile bằng `javac -source 8 -target 8` (JDK 21), class files có version **52.0** (Java 8). Trên PC (MicroEmulator) chạy JVM đầy đủ nên OK. Nhưng **J2ME Loader** (Android) chuyển `.class` → `.dex` (Dalvik) và DEX converter **KHÔNG xử lý được**:
1. **Class version 52.0** — game gốc dùng version 45.3 (Java 1.1)
2. **StackMapTable attribute** — Java 7+ javac tự thêm attribute này vào MỌI method. Khi class version bị patch thành 45.3 nhưng vẫn còn StackMapTable → DEX converter lỗi phân tích → method không được add vào DEX → `NoSuchMethodError`

### Triệu chứng
```
java.lang.NoSuchMethodError: No static method PAINT(LmGraphics;)V 
in class Lpackage1/ThongKe; or its super classes
```
- Lỗi chỉ trên J2ME Loader (Android), PC MicroEmulator chạy bình thường
- Error message hiển thị method name VIẾT HOA (VD: `PAINT` thay vì `paint`) — đây là format hiển thị của J2ME Loader, KHÔNG phải case mismatch

### Giải pháp — Build Workflow BẮT BUỘC
```powershell
# 1. Compile bằng javac (vẫn tạo version 52.0)
javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/*.java

# 2. Patch J2ME compatibility: downgrade version 52→45.3 + strip StackMapTable
python scripts/patch_class_j2me.py build/unpacked

# 3. Đóng gói JAR
Push-Location "build/unpacked"; jar cfm "../../Aeharuna.jar" "META-INF/MANIFEST.MF" .; Pop-Location
```

### Script: `scripts/patch_class_j2me.py`
- Tự động quét tất cả mod class files (ThongKe, TsBoost, Auto, Code, etc.)
- Downgrade class version header: 52.0 → 45.3
- **Strip StackMapTable** attribute từ MỌI method (bao gồm cả sub-attributes trong Code attribute)
- Xử lý inner classes (VD: `AutoSanBoss$1.class`)
- An toàn: skip class đã là version 45.x

### Quy tắc VÀNG
1. **LUÔN chạy `patch_class_j2me.py` sau mỗi lần compile** — dù chỉ compile 1 file
2. **Chỉ patch header (version bytes) là KHÔNG ĐỦ** — phải strip StackMapTable
3. **Test trên J2ME Loader mỗi khi thêm class MỚI** — class mới dễ bị lỗi nhất vì DEX converter phải tạo method table từ đầu

## 2026-08-06: Method Name `paint` Bị Conflict Trên J2ME Loader

### Vấn đề
Class `ThongKe` có method `public static void paint(mGraphics g)`. Dù class version + StackMapTable đã fix, J2ME Loader vẫn báo `NoSuchMethodError` cho `paint`.

### Nguyên nhân
`paint` là **reserved method name** trong J2ME framework (`Canvas.paint(Graphics)`). J2ME Loader DEX converter có thể xử lý đặc biệt các method tên `paint` → gây conflict khi class không extends `Canvas`.

### Giải pháp
Đổi tên method từ `paint` → `draaw` (hoặc tên bất kỳ KHÔNG trùng J2ME API):
1. **ThongKe.java**: `public static void draaw(mGraphics g)`
2. **GameScr.class**: Patch constant pool UTF-8 entry — thay `paint` → `draaw` (cùng 5 bytes)
3. Script: `scripts/fix_gamescr_thongke.py`

### Quy tắc
- **KHÔNG đặt tên method trùng J2ME API** trong class mới: `paint`, `run`, `keyPressed`, `pointerPressed`, etc.
- Nếu class extends `Runnable` thì `run()` OK vì là override. Nhưng class độc lập KHÔNG nên dùng tên reserved.

## 2026-08-06: Anti-Stuck vs Boss Hunting Conflict

### Vấn đề
TsBoost có 2 cơ chế anti-stuck:
- **10s không giết quái** → reload zone (`reloadZone()`)
- **30s không giết quái** → tự sát về làng (`suicideAndReturn()`)

Khi đang đánh boss hoặc săn boss, các cơ chế này gây:
- Tự reload zone giữa trận boss
- Tự sát khi boss chưa chết (boss HP cao, đánh lâu)

### Giải pháp
Thêm **2 lớp bảo vệ** trong TsBoost main loop:
1. `isBossHunting = AutoSanBoss.isRunning` → **TẮT TOÀN BỘ** (anti-stuck, chuyển khu, ghost move, idle nudge)
2. `hasBoss = hasBossOnMap()` → **TẮT chỉ anti-stuck** 10s/30s (vẫn giữ attack)

### `hasBossOnMap()` check
```java
// Quét GameScr.vMob tìm mob có isBoss=true VÀ hp > 0
for (Mob mob : GameScr.vMob) {
    if (mob.isBoss && mob.hp > 0 && mob.status != 0) return true;
}
```

## 2026-08-06: Auto.gameAM() — Ngưỡng Chuyển Khu

### Vấn đề
`Auto.gameAM()` là method gốc game gọi khi cần đổi khu. Có **3 call sites** trong `Auto.java`:
1. Dòng 627: `countAliveMobs() < N` (smart zone check)
2. Dòng 705: `Char.ChuyenMapHetQuai` (hết mob gần)
3. Dòng 781: `Char.ChuyenMapHetQuai` (tránh chướng ngại)

### Giải pháp
Thêm `countAliveMobs() < 3` vào **TẤT CẢ 3** call sites. Nếu chỉ sửa 1 chỗ, 2 chỗ còn lại vẫn trigger chuyển khu sớm.

## 2026-08-07: Auto Map VIP — Mua + Dùng Thẻ Map VIP Khi TS

### Vấn đề
Khi TS ở map VIP (cần thẻ vào), nếu chết/mất kết nối → respawn ở ngoài → không tự vào lại map VIP.

### Phát hiện quan trọng: TanSat.mapid ≠ TileMap.mapID
- **`TanSat.mapid`** = ID nội bộ của TanSat (VD: **134** cho map VIP)
- **`TileMap.mapID`** = ID map thực tế game (VD: **195** cho map VIP)
- **KHÔNG THỂ** so sánh trực tiếp hai ID này — luôn khác nhau kể cả khi đúng map

### NPC Shop Goshu — Slot Calculation
- **Grid:** 6 cột × 8 hàng = 48 slot (index 0-47)
- **Công thức:** `slot = (row - 1) × 6 + (col - 1)` (1-indexed)
- **Thẻ Map VIP (ID 906):** Cột 5, Hàng 6 → **slot = 34**
- **Cổ lệnh (ID 490):** slot = 28
- **API:** `Service.gI().gameAB(14, slot, 1)` — menuId=14 (Goshu)
- **Giá:** 100 lượng/thẻ

### VIP Watcher — Kiến Trúc Thread Độc Lập (BẮT BUỘC)
- **SAI:** Đặt VIP check trong `while (gameCA)` loop — loop này CHỈ chạy khi TS active. Sau disconnect, `gameCA = false` → loop dừng → VIP check chết.
- **ĐÚNG:** Thread RIÊNG `startVipMapWatcher()` chạy `while (DungMapVip)`.
- **Khởi tạo:** NamMod toggle ON → `Code.startVipMapWatcher()` + auto-restart trong `gameAP()` (init method gọi khi reconnect).
- **gameAP()** là init method, gọi mỗi khi game khởi tạo/reconnect → restart watcher nếu `DungMapVip` còn ON.
- **Logic:** Track `prevMap`. Map KHÔNG đổi (stuck/failed) → mua+dùng thẻ. Map đổi → reset `justBought`.
- **Sau mua+dùng:** `Thread.sleep(10000)` chờ game xử lý, update `prevMap`.
- **Kill old thread:** Trong gameAP(), `vipWatcherThread.interrupt()` + set null trước khi start mới.

### Files
- **Code.java:** `startVipMapWatcher()` + restart trong `gameAP()` + fields: `vipWatcherThread`, `DungMapVip`, `MuaMapVip`, `lastVipMapId`
- **NamMod.java:** Toggle "Dùng thẻ map vip" → `startVipMapWatcher()`, Toggle "Mua thẻ map vip"

---

## 2026-08-07: Code.gameAQ — Cơ Chế Nhặt VP Game Gốc (QUAN TRỌNG)

### Hai chế độ nhặt VP (lệnh `cnhat` toggle)
| `gameAQ` | Tên | Hành vi | Tele? |
|----------|-----|---------|-------|
| `true` | **Hút VP** | Nhặt item gần chân (≤100px), 1 item/tick | **KHÔNG** |
| `false` | **Nhặt xa** | `Auto.gameAH/gameAC()` TELE đến item → nhặt → quay về | **CÓ** |

### Default
- `gameAQ = true` (init tại `gameAP()` line 175) → **hút VP** mode, KHÔNG tele
- Lệnh `cnhat` toggle giữa 2 mode

### Nơi set `gameAQ = true` cần CHÚ Ý
- `AutoPickup.java` — đã XÓA (tránh conflict)
- `AutoLevel.java` — đã XÓA (tránh tele)
- `Code.java` init — giữ `true`

---

## 2026-08-07: AutoPickup v3.2 — Bản Ổn Định (GitHub baseline)

### Architecture
- Thread liên tục (`Runnable`), scan 200ms/vòng
- `blastPickupSmart(0)` = ghost move + `Service.gI().gameAQ()` cho mỗi item
- Lưu `origCx/origCy` → ghost move → nhặt → quay về vị trí gốc
- `toggle()` = bật/tắt từ NamMod menu + lệnh `nhat`
- `start()/stop()` = hook từ ChatRouter khi TS bật/tắt

### Quy tắc KHÔNG được phá
- **KHÔNG có `static { start(); }`** — tránh auto-start khi class load
- **KHÔNG set `Code.gameAQ = true`** — tránh conflict với game gốc
- **KHÔNG refactor toggle logic** — `toggle()` + `start()/stop()` đã hoạt động

---

## 2026-08-07: while(gameCA) — Scope Giới Hạn (BẮT BUỘC NHỚ)

### `gameCA` lifecycle
- `gameCA = true` khi `gameAA()` gọi (bắt đầu auto/TS)
- `gameCA = false` khi `gameAB()` gọi HOẶC disconnect/reconnect
- `while (gameCA)` loop trong `Code.run()` → **CHỈ chạy khi TS/auto active**

### Quy tắc
- **KHÔNG đặt logic cần chạy 24/7** trong `while (gameCA)` (VD: VIP watcher, reconnect handler)
- Dùng **thread riêng** cho logic cần chạy liên tục bất kể TS
- `gameAP()` = init method, chạy khi game khởi tạo → nơi restart các watcher thread

## 2026-08-08: EffectAuto ArrayIndexOutOfBoundsException — Fix Crash Liên Tục

### Vấn đề
Log game spam liên tục:
```
Err update effauto: java.lang.ArrayIndexOutOfBoundsException: length=20; index=40
```
Lỗi gây lag nặng, có thể góp phần khiến game bị treo/stuck khi TS Pro.

### Nguyên nhân
`EffectAuto.class` (game gốc) khai báo `arrEffAtutoTemplate = new EffAtutoTemp[20]`.
Server gửi effect ID lớn hơn 19 (VD: 40). Khi `this.id = 40`:
- `gameAD()` trả `arrEffAtutoTemplate[40]` → **ArrayIndexOutOfBoundsException**
- Method update `gameAA()` có try/catch nên không crash game, nhưng **spam error liên tục**
- Method paint `gameAA(mGraphics)` KHÔNG có try/catch → có thể crash render

### Giải pháp — Bytecode Patch
- Script: `scripts/patch_effectauto.py`
- Tìm `bipush 20` + `anewarray EffAtutoTemp` trong static initializer `<clinit>`
- Thay `bipush 20` (0x10 0x14) → `bipush 100` (0x10 0x64)
- Kết quả: `arrEffAtutoTemplate[100]` — đủ chứa effect ID lên đến 99
- **Offset**: 6510 (0x196E) trong EffectAuto.class gốc

### Quy tắc
- **Luôn chạy `patch_effectauto.py` sau mỗi lần unpack JAR** — thêm vào build flow
- Nếu server gửi ID > 99 → cần tăng size tiếp

## 2026-08-08: TsBoost Anti-Stuck v2 — Fix Treo Tại Chuyển Khu

### Vấn đề
TsBoost bị stuck khi chuyển khu (GoMap):
1. **GoMap loop vô hạn**: GoMap thất bại (dialog block, lock game) → lặp mỗi 8s mãi mãi
2. **Watchdog 30s không phát hiện stuck**: HP/MP thay đổi do regen/buff → watchdog coi là OK dù nhân vật đứng im sai map
3. **Nhân vật đứng im 1 chỗ**: Stuck ở điểm chuyển khu, đánh quái nếu có nhưng không thoát

### Fix
1. **GoMap retry limit** (`MAX_GOMAP_RETRIES = 5`): Đếm số lần GoMap thất bại liên tiếp. Sau 5 lần → tự sát về nhà thay vì loop tiếp.
2. **Wrong map suicide** (`WRONG_MAP_SUICIDE_MS = 60000`): Watchdog thread kiểm tra: nếu sai map liên tục > 60s → tự sát ngay, BẤT KỂ HP/MP có thay đổi.
3. **Vị trí tracking**: Watchdog thêm check `cx/cy` — nếu vị trí không đổi + sai map → cảnh báo stuck dù HP/MP thay đổi.
4. **`wrongMapStartTime`**: Timestamp bắt đầu sai map, dùng cho cả main loop lẫn watchdog thread. Reset khi về đúng map.

### State fields mới
- `wrongMapRetries`: Đếm GoMap thất bại liên tiếp
- `wrongMapStartTime`: Thời điểm bắt đầu sai map (cho watchdog 60s)

## 2026-08-08: Build Corruption — ClassNotFoundException & NoClassDefFoundError

### Triệu chứng
```
java.lang.ClassNotFoundException: Didn't find class "GameMidlet" on path:
DexPathList[,nativeLibraryDirectories=[/system/lib64, /system_ext/lib64]]
```
Game không khởi động được. DEX conversion fail hoàn toàn.

### Nguyên nhân 1: Patch chồng patch (Double-patching)
JAR trên git (`Aeharuna.jar`) đã **bake sẵn** các bytecode patches từ lần build trước (vì `pack_jar.py` ghi đè JAR gốc). Khi unpack và patch lại:
- `patch_service.py`: Thêm 6 CP entries MỖI LẦN chạy → CP tăng: 351→357→363→369→375... → corrupt
- `patch_code_stop.py`: Replace methodref trên class đã bị replace → corrupt  
- `patch_inputdlg.py`: Patch lại class đã patched → corrupt
- `patch_char_skip_effects.py`: Insert 7 bytes MỖI LẦN → code_length 76→83→90→97→104... → corrupt

### Nguyên nhân 2: JAR bị ghi đè
`pack_jar.py` output ghi đè `Aeharuna.jar` (cùng tên với input). Lần build sau unpack từ bản đã patch → patch lại → tích lũy corruption.

### Nguyên nhân 3: Thiếu patch_gamescr_hienexp
`patch_gamescr_hienexp.py` inject gọi `ThongKe.draaw()` vào GameScr để hiện yên/xu/lượng khi treo.
Nếu không chạy patch này → menu Dưa Mod không hiện thống kê.
**LƯU Ý:** Patch này PHẢI chạy TRƯỚC `fix_gamescr_thongke.py` (vì fix_thongke đổi "paint"→"draaw").

### Giải pháp
1. **LUÔN `git checkout Aeharuna.jar`** trước khi build → đảm bảo JAR gốc sạch
2. **XOÁ 4 scripts lỗi** (đã bake sẵn trong JAR):
   - `patch_service.py` ❌
   - `patch_code_stop.py` ❌ 
   - `patch_inputdlg.py` ❌
   - `patch_char_skip_effects.py` ❌
3. **Dùng `jar cfm` để pack** thay vì Python zipfile (đúng format, đúng size)
4. **Output JAR tên khác** hoặc ra thư mục khác, không ghi đè JAR gốc

### Quy tắc VÀNG cho build
```
1. git checkout Aeharuna.jar          # Khôi phục JAR gốc
2. rm -rf build/unpacked              # Xoá sạch
3. jar xf Aeharuna.jar                # Unpack từ gốc
4. javac src/*.java → build/unpacked  # Compile mod sources
5. patch_class_j2me.py                # J2ME compat (NGAY SAU compile)
6. patch_gamescr_hienexp.py           # Inject ThongKe vào GameScr
7. fix_gamescr_thongke.py             # Rename paint→draaw
8. patch_effectauto.py                # Fix array size 20→100
9. jar cfm output.jar                 # Pack bằng jar command
```

### Patches AN TOÀN (có idempotency check, skip nếu đã patch):
- `patch_gamescr_menu.py` ✅ (nhưng JAR gốc đã có)
- `patch_gamescr_chat.py` ✅ (nhưng JAR gốc đã có)
- `patch_mothercanvas_shortcut.py` ✅ (nhưng JAR gốc đã có)

### Patches CẦN CHẠY (chưa có trong JAR gốc, hoặc cần re-apply):
- `patch_class_j2me.py` ✅ (cần cho mod classes mới compile)
- `patch_gamescr_hienexp.py` ✅ (inject ThongKe call)
- `fix_gamescr_thongke.py` ✅ (rename paint→draaw)
- `patch_effectauto.py` ✅ (fix array crash)

---

## 2026-08-08: TsBoost v4 — Anti-Stuck Vị Trí XY & Hàm Tự Sát Chuẩn Menu (`Code.gameAN`)

### 1. Vị Trí XY Là Tín Hiệu Anti-Stuck Chuẩn Nhất
- **HP/MP không phản ánh chính xác trạng thái kẹt:** Nhân vật regen HP/MP hoặc buff skill làm HP/MP biến động dù đang đứng im kẹt địa hình / dialog block.
- **Tọa độ `(cx, cy)` chính xác 100%:** Khi đánh quái, nhân vật liên tục di chuyển tiếp cận mob. Chỉ khi kẹt thật sự (bị cản, vướng địa hình, lỗi GoMap) mới đứng yên trùng vị trí 30s.
- **Quy trình:**
  - Chụp `(prevCx, prevCy)`.
  - Sau 30s: `posChanged = (cx != prevCx || cy != prevCy)`.
  - Nếu `!posChanged` (trùng khớp vị trí) -> Tự sát ngay lập tức.
  - Nếu `posChanged` -> Cập nhật vị trí mới, tiếp tục loop.

### 2. Hàm Tự Sát Đúng Chuẩn Game (`Code.gameAN`)
- **Lỗi cũ:** `suicideAndReturn()` gọi `GameScr.gameAB(5,0,0)` + `Service.gI().gameAF()` (packet hồi sinh khi đã chết) -> không phải packet tự sát khi đang sống.
- **Chuẩn game gốc:** Nút "Tự sát" trong menu (lệnh chat `die`) gọi `Code.gameAN()` -> gửi `Service.gI().gameAE()` (Packet -27 / Tự sát). Gọi `Code.gameAN()` đảm bảo tự sát tức thì và an toàn.

## 2026-08-14: Làng Cổ Boss — Root Causes & Fixes

### 1. Game Auto-Exit Từ Map Làng Cổ (ROOT CAUSE chính!)
- **Vấn đề:** Nhân vật vào map Làng Cổ (134-137) rồi bị đá ra ngay. Code.java line 719 tự động exit khi không có item 35/37 (Khao Di Lệnh).
- **Fix:** Thêm `!Char.DungCoLenh` vào điều kiện. Khi `DungCoLenh = true` (đang săn boss), game KHÔNG auto-exit.

### 2. GoMap vs TileMap.gameAJ(0)
- **GoMap:** BFS thread riêng, vào map random rồi pathfind quay lại → "neck 2 lần".
- **gameAJ(0):** Đi đến waypoint exit → chuyển map 1 lần → DỪNG. Dùng cho Làng Cổ.

### 3. Party Member Không Mua Được Cổ Lệnh Từ Chat Handler
- **Vấn đề:** `ensureInLangCo()` mở shop NPC nhưng ae đang ở map khác → không có shop → fail.
- **Fix:** Chỉ set flags `MuaCoLenh/DungCoLenh = true`. Game auto loop (Code.java line 749) tự mua + dùng.

### 4. pkm -2 Xóa DungCoLenh Quá Sớm
- **Fix:** `pkm -2` chỉ startPartyMemberTreo(), KHÔNG clear flags. `pkm 135` set flags, `pkm <map khác>` clear.

### 5. AutoBossEvent — 2 Lượt Thay 10 Phút
- Lượt 1 + gọi ae → ae về → lượt 2 solo → VỀ. Không chờ 10 phút.

### 6. HUNT_PRIORITY: Làng Cổ > MapNgoài > VDMQ
- Mảng `{TYPE_LANGCO, TYPE_MAPNGOAI, TYPE_VDMQ}` cho cả Force ALL và Auto.

### 7. Delay Tối Ưu: chờ ae 3s→1.5s, poll boss chết 2s→0.5s, double check 300ms→150ms.

## NPC Interaction — Cách Mở NPC Dialog & Chọn Menu Option

### 1. GameScr.gameAB(npcType, param1, param2) — Talk NPC
- **Chức năng:** Mở NPC dialog. Bên trong gọi:
  1. `gameAI(npcType)` → tìm NPC trên map hiện tại (trả về `Npc` object)
  2. `Char.gameAC(npc.cx, npc.cy)` → di chuyển nhân vật đến NPC
  3. `Service.gI().gameAC(npcType, param1, param2)` → gửi packet interact NPC đến server
- **param1** (parameter thứ 2) = **menu option index (0-based)**
  - `GameScr.gameAB(47, 0, 0)` → NPC VIP, chọn ô 1 (Nhận quà VIP)
  - `GameScr.gameAB(47, 4, 0)` → NPC VIP, chọn ô 5 (Map Up Lượng)
  - `GameScr.gameAB(4, 0, 0)` → NPC Shop
  - `GameScr.gameAB(6, 1, 1)` → NPC VDMQ
- **SAI:** Dùng `GameScr.gameAB(47, 0, 0)` rồi gọi `Service.gI().gameAI(4)` riêng → `gameAI(int)` là packet **-103** (request NPC shop data), KHÔNG phải chọn menu option!

### 2. Service Packet Types cho NPC
| Method | Packet | Mô tả |
|---|---|---|
| `Service.gameAC(int,int,int)` | interact NPC | Gửi khi click NPC, param = npcType + option |
| `Service.gameAI(int)` | **-103** | Request NPC shop/inventory data, **KHÔNG phải** menu select |
| `Service.gameAK(int)` | **-104** | Gửi số xu cho NPC (buy xu) |
| `Service.gameAK(String)` | **-104** | Gửi lệnh chat/command đến server |
| `Service.gameAK(int,int)` | **-85** | NPC action với byte+int params |

### 3. GameScr.gI().gameAD(int) — Mở NPC Dialog UI
- `gameAD(npcTypeId)` = switch case (2..50) → set flag + khởi tạo UI
- Case 47: set `gameMP = true`, `upitem = new Item[18]` → VIP upgrade dialog
- **Khác với** `gameAB(npcType, param1, param2)` — `gameAD` chỉ set UI flag, `gameAB` gửi packet thật

### 4. TileMap Map Type Checks
| Method | Maps | Mô tả |
|---|---|---|
| `TileMap.gameAD(mapID)` | 10,17,22,32,38,43,48,138 | VDMQ maps |
| `TileMap.gameAF(mapID)` | 1,27,72 | VIP maps (Cổ Lệnh) |
| `TileMap.isLangCo(mapID)` | 134-138 | Làng Cổ maps |

## Auto VIP Map (Map Up Lượng M196)

### 1. Flow hoạt động
- NPC VIP [47] nằm **ngay map thôn** (chỗ hồi sinh) → KHÔNG cần GoMap
- Chết ở M196 → hồi sinh ở thôn → `AutoVipMap.checkAndReturn()` phát hiện mapID ≠ 196
- Gọi `GameScr.gameAB(47, 4, 0)` → NPC VIP chọn ô 5 "Map Up Lượng"
- Chờ server chuyển map → vào lại M196

### 2. Lỗi đã gặp
- **Lần 1:** GoMap(48) → SAI vì NPC VIP ở thôn, không phải M48
- **Lần 2:** `GameScr.gameAB(47, 0, 0)` + `Service.gI().gameAI(4)` → SAI vì `gameAI` không phải menu select, và param1=0 = ô 1
- **Fix:** `GameScr.gameAB(47, 4, 0)` — truyền thẳng option index vào param1

## 2026-08-17: TS Boss Ưu Tiên — UI Đơn Giản Hóa

### Vấn đề
Menu TS Boss ban đầu có nút "Bật/Tắt" riêng + 3 nút chọn chế độ ưu tiên → lằng nhằng, user phải ấn 2 lần.

### Giải pháp
- Gộp bật/tắt vào mỗi nút chế độ: ấn chọn loại nào → bật luôn loại đó
- Ấn lại loại đang bật (có dấu ✔) → tắt
- Đổi loại khác khi đang bật → tự chuyển sang loại mới
- Method `togglePriority(int p)` thay cho `setPriority(int p)` + `toggle()` riêng

### Quy tắc UX
- Menu toggle nên gộp "chọn" và "bật" vào 1 hành động. Không tách riêng nút bật/tắt khi đã có nhiều lựa chọn.

## 2026-08-17: eventHuntTypes Race Condition & State Leakage

### Bug 1: Race condition
- `startEventHuntVdmqLc()` gọi `toggleInternal()` (start thread) TRƯỚC khi gán `eventHuntTypes`
- Thread mới có thể đọc `eventHuntTypes = null` và dùng HUNT_PRIORITY mặc định
- **Fix:** Gán `eventHuntTypes` TRƯỚC `toggleInternal()`

### Bug 2: State leakage
- `eventHuntTypes` chỉ reset trong `stop()` khi `isRunning == true`
- Thread kết thúc tự nhiên set `isRunning = false` mà không clear `eventHuntTypes`
- **Fix:** Reset `eventHuntTypes = null` ở: `startEventHunt()`, `startEventHuntAll()`, `startEventHuntMN()`, thread cleanup

### Quy tắc
- Shared state dùng bởi worker thread phải được set TRƯỚC khi start thread
- Cleanup shared state ở MỌI exit path (stop, natural completion, error)

## 2026-08-17: Patch Vị Trí HUD "HS lượng" / "Lọc Đồ" Trong GameScr.class

### Vấn đề
Text vàng "HS lượng" và "Lọc Đồ" trong GameScr (code gốc obfuscated) hiển thị quá cao, che đồ.

### Phân tích bytecode
- Text vẽ bằng `mFont.tahoma_7_yellow.gameAA(g, text, iload_3, iload_2, 0, mFont.tahoma_7)`
- `iload_2` = biến local #2 = tọa độ y, tăng dần bằng `iinc 2, 12` sau mỗi dòng
- Trước đoạn code vẽ "HS lượng" có ~30 byte NOP (từ patch trước đã NOP-out code cũ)

### Giải pháp
- Script `scripts/patch_hsluong_pos.py`: tìm 3 NOP liền trước `getstatic mFont.tahoma_7_yellow`
- Thay 3 NOP bằng `iinc 2, 30` (opcode: `84 02 1E`) → đẩy y xuống 30 pixel
- KHÔNG thay đổi code structure, KHÔNG ảnh hưởng StackMapTable

### Phân biệt ThongKe vs HS lượng/Lọc Đồ
- **ThongKe.java** (mod): hiển thị thống kê yên/xu/exp khi treo, vị trí y=155, SỬA ĐƯỢC trong source
- **HS lượng/Lọc Đồ** (game gốc GameScr.class): bytecode obfuscated, chỉ patch được bằng script

## 2026-08-17: TS Boss Ưu Tiên Mặc Định — Quét Sai Loại Boss Không Đúng Giờ

### Vấn đề
Khi `eventPriority = 0` (Mặc định), đến giờ boss Map Ngoài thì hệ thống quét luôn cả Làng Cổ + VDMQ dù chưa tới giờ spawn của 2 loại đó.

### Root cause
Trong `AutoSanBoss.run()`, nhánh `forcedBossType == TYPE_ALL`:
- Khi `eventHuntTypes != null` (VD: VDMQ+LangCo) → check `isBossActive()` trước khi quét ✅
- Khi `eventHuntTypes == null` (Mặc định) → quét TẤT CẢ boss types KHÔNG check giờ ❌

### Fix
Đổi điều kiện từ `if (eventHuntTypes != null)` sang `if (eventHuntMode)`:
- `eventHuntMode = true` (TS Boss Ưu Tiên) → luôn check `isBossActive()` cho TỪNG loại boss
- `eventHuntMode = false` (lệnh `tspkball`) → quét tất cả không check giờ (giữ hành vi cũ)
- Thứ tự ưu tiên khi trùng giờ: `HUNT_PRIORITY = {TYPE_LANGCO, TYPE_VDMQ, TYPE_MAPNGOAI}`

### Quy tắc
- Logic check giờ spawn phải gắn với **chế độ hoạt động** (`eventHuntMode`) chứ không phải **có override hay không** (`eventHuntTypes != null`).

## 2026-08-17: AutoPickup v4.1 — Hút VP Thông Minh + Sync gameAQ

### 3 Cơ Chế Nhặt Chồng Nhau (Phát Hiện)
| # | Cơ chế | Flag | Nhặt gần/xa | Ngừng đánh? |
|---|--------|------|-------------|-------------|
| 1 | Nhặt gốc game | `Code.gameAQ=true` | Gần (~50px) | Không |
| 2 | Nhặt xa TS | `Code.gameAQ=false` | Xa (di chuyển) | **CÓ** |
| 3 | AutoPickup mod | `AutoPickup.isRunning` | Toàn map (ghost) | Không |

- Cơ chế 1 và 2 **loại trừ nhau** (toggle bởi `gameAQ`)
- Cơ chế 3 chạy **song song** bất kể `gameAQ` → xung đột

### Fix: Sync gameAQ với AutoPickup
- `AutoPickup.start()` → `Code.gameAQ = false` (tắt nhặt gốc)
- `AutoPickup.stop()` → `Code.gameAQ = true` (khôi phục nhặt gốc)
- `Code.gameAF()` (tắt auto) → `AutoPickup.stop()` (tắt hút VP)
- Lệnh `cnhat` → `AutoPickup.toggle()` (thay vì flip gameAQ trực tiếp)

### Fix: Hook Nút Menu Gốc 1100080
- Nút "Nhặt Xa" gốc (command 1100080) ở bytecode `GameScr.class` chỉ flip `Code.gameAQ`
- **KHÔNG THỂ sửa bytecode** → hook qua `SplitPatcher.hookMenu()`:
  - Detect `cmd.idAction == 1100080` → thay bằng Command mới gọi `AutoPickup.toggle()`
  - Label: "Hút VP: ON" / "Hút VP: OFF" thay "Nhặt Xa"

### AutoPickup v4.1 Cải Tiến
| Thông số | v3.2 | v4.1 |
|----------|------|------|
| Delay/item | 50ms (thread), 0ms (scan) | **20ms** |
| Scan interval | 200ms | **150ms** |
| Lọc ds nhặt | ❌ Nhặt tất cả | ✅ `Code.gameAA(ItemTemplate)` |
| Check bag đầy | ❌ | ✅ `Char.gameBG() <= 2` → skip |
| Ghost move | Có check GHOST_RANGE | ✅ **Luôn ghost move** (server-side) |
| Restore vị trí | Chỉ khi picked > 0 | ✅ **Luôn restore** `myChar.cx/cy` |

### Hàm Lọc Item: `Code.gameAA(ItemTemplate)`
- Lọc theo danh sách nhặt `Code.nhat[]`, `Char.NhatYen`, `Char.NhatDa`, `Char.NhatTrangBi`, `Char.NhatAll`
- Lọc HP/MP theo `Char.NhatHpMp` + `Char.CapHpMp`
- Check trang bị: `itemTemplate.gameAA()` = true nếu là equipment
- Check NV: `itemTemplate.gameAB()` = true nếu là vật phẩm NV

### Bài Học: Khi Có Nhiều Hệ Thống Cùng Chức Năng
1. **Xác định TẤT CẢ các cơ chế** trước khi sửa — có thể có 3+ hệ thống cùng làm 1 việc
2. **Sync state giữa các hệ thống** — tắt cái gốc khi bật mod, khôi phục khi tắt
3. **Hook menu bytecode** qua SplitPatcher — thay Command object trong MyVector
4. **KHÔNG BAO GIỜ** để 2 hệ thống nhặt chạy song song — gây flood server

## 2026-08-17: Kiểm Tra Toàn Bộ Boss System — Kết Quả Audit

### Tất Cả Modules Đã Verify ✅
- **AutoSanBoss**: Toggle ON/OFF, force type (VDMQ/MN/LC/ALL), auto schedule, reconnect, Lang Cổ exit — logic đúng
- **AutoBossEvent**: TS ưu tiên boss, 3 priority levels (Mặc định/VDMQ+LC/MN), save/restore state — đúng
- **NamMod menu**: Sub-menus boss, labels, checkmarks, handlers — mapping đúng
- **bossStatus()**: Hiện "ON"/"OFF"/"Auto" đúng theo `forcedBossType`

### Quy Tắc Audit
- Trace flow TỪ ĐẦU ĐẾN CUỐI: Menu → handler → toggle → thread → run() → huntBossType()
- Verify cả chiều BẬT lẫn TẮT
- Check edge cases: disconnect, chết, Lang Cổ random map

## 2026-08-17: Char.gameAC(x,y) vs Service.gameAB(x,y) — QUAN TRỌNG

### Char.gameAC(int, int) — PATHFINDING (GÂY GIẬT!)
- **Di chuyển từng bước 50px** dọc đường đến đích
- Mỗi 50 bước gọi `Thread.sleep(200ms)` → chậm
- Gọi `Service.gameAB(x,y)` cho MỖI bước
- **SET `myChar.cx = x, myChar.cy = y`** ở cuối → nhân vật nhảy vị trí
- **SET `cxSend, cySend`** → server biết vị trí mới
- **KHÔNG BAO GIỜ dùng cho ghost move** — gây giật 100%

### Service.gameAB(int, int) — 1 PACKET (MƯỢT!)
- Gửi **1 packet vị trí** trực tiếp tới server
- **KHÔNG update cx/cy** → nhân vật đứng yên trên client
- **KHÔNG pathfinding** → tức thì, không delay
- **Dùng cho ghost move**: gửi vị trí giả → server accept nhặt → restore vị trí

### Quy tắc Ghost Move
```java
// ĐÚNG — 1 packet, không giật
Service.gI().gameAB(item.xEnd, item.yEnd);
Service.gI().gameAQ(item.itemMapID);
myChar.cx = realCx;  // Giữ nguyên vị trí client
myChar.cy = realCy;

// SAI — pathfinding, giật nặng
Char.gameAC(item.xEnd, item.yEnd);  // KHÔNG DÙNG!
```

## 2026-08-17: gameAQ=true vs false — Nguyên Nhân Nhảy VP

### gameAQ = true (Mặc định)
- Game gốc nhặt item **GẦN** (~50px) **KHÔNG di chuyển**
- An toàn, không giật

### gameAQ = false
- Game engine **TỰ CHẠY** nhân vật tới VP để nhặt (nhặt xa gốc)
- `Auto.gameAH/gameAC()` TELE đến item → nhặt → quay về
- **GÂY GIẬT** — nhân vật nhảy qua VP rồi quay lại

### Sai lầm v4: set gameAQ=false khi bật AutoPickup
- Ý định: "tắt nhặt gốc để tránh xung đột"
- Thực tế: **BẬT nhặt xa gốc** → game engine chạy nhân vật tới VP → GIẬT
- Fix v6: **giữ gameAQ=true** → game gốc chỉ nhặt gần (không giật)
  + mod ghost move nhặt xa bằng Service.gameAB (không nhìn thấy)

### AutoPickup v6 — Config Tối Ưu
| Thông số | Giá trị | Lý do |
|----------|---------|-------|
| SCAN_INTERVAL | 100ms | TS nhanh, chuyển khu <1s |
| ITEM_DELAY | 15ms | Tránh flood server |
| NEAR_RANGE | 40px | Game gốc nhặt gần, mod nhặt xa |
| gameAQ | true | Nhặt gần không giật |
| Ghost move | Service.gameAB | 1 packet, không pathfinding |

## 2026-08-20: respawnFast() Dùng Sai Packet Hồi Sinh — Root Cause Lỗi Hồi Sinh Khi Săn Boss

### Vấn đề
Khi đánh boss PKB (`tspkb`), nhân vật chết → hồi sinh bị lỗi → ấn "Về nhà thay" → hiện "quét lại 1 lượt" thay vì quay lại đánh boss.

### Nguyên nhân
`respawnFast()` trong `AutoSanBoss.java` dùng SAI packet:
- `GameScr.gameAB(5, 0, 0)` = **mở dialog UI hồi sinh** (nút Về Nhà / Hồi Sinh), KHÔNG gửi packet
- `Service.gI().gameAF()` = **KHÔNG phải packet hồi sinh chuẩn**

Game gốc (`Auto.gameAA(boolean)` dòng 195-236 trong `Auto.java`) dùng:
- `Service.gI().gameAK()` = packet hồi sinh về làng (chuẩn)
- `Service.gI().gameAL()` = packet hồi sinh lượng (tại chỗ)
- `TileMap.gameAF()` = refresh map
- `Auto.gameAN.removeAllElements()` + `Auto.gameAM = false` = clear state

### Hậu quả
1. Dialog hồi sinh hiện nhưng không tự gửi packet → user phải ấn thủ công "Về nhà"
2. Khi ấn "Về nhà" → bị teleport về làng → PkBoss tự thoát (sai map) → while loop `instanceof PkBoss` thoát
3. `pkBossOnMap()` return false → hiện "quét lại 1 lượt"

### Fix
- `respawnFast()` + `respawnIfDead()` đổi sang dùng `Service.gI().gameAK()` / `Service.gI().gameAL()` giống game gốc
- Ưu tiên `gameAL()` (hồi sinh lượng tại chỗ) khi săn boss vì không cần navigate lại
- Thêm delay 500ms sau hồi sinh để nhân vật ổn định
- Reset `sentPartyCmd` khi bị teleport về map khác

### Quy tắc
- **KHÔNG DÙNG `GameScr.gameAB(5,0,0)` + `Service.gI().gameAF()` để hồi sinh** — đây chỉ mở dialog UI
- **LUÔN dùng `Service.gI().gameAK()` hoặc `Service.gI().gameAL()`** — đây là packet hồi sinh chuẩn
- Nhớ clear `Auto.gameAN` + `Auto.gameAM` trước khi hồi sinh (giống game gốc)

## 2026-08-21: Làng Cổ Tự Sát Desync — HP Còn Nhưng Hiện "Kiệt Sức"

### Vấn đề
Khi tsboss ưu tiên săn boss Làng Cổ xong, gọi `finishLangCoAndExit()` tự sát về làng, lâu lâu bị desync:
- HP còn đầy nhưng vẫn hiện nút "Kiệt sức"
- Hoặc HP = 0 nhưng hiện "Kiệt sức" mà không tự hồi sinh
- Phải tự sát lần nữa thủ công mới hết lỗi

### Root Cause 1: `Code.gameAN()` KHÔNG gửi packet tự sát khi có item 35/37
```java
// Code.gameAN():
if (!Char.gameAJ(37) && !Char.gameAJ(35)) {
    Service.gI().gameAE(); // CHI gui packet tu sat KHI KHONG co item 35/37
} else {
    Char.gameAC(var0.cx, TileMap.pxh); // CHI DI CHUYEN, KHONG TU SAT!
}
```
`finishLangCoAndExit()` gọi `Code.gameAN()` mà **CHƯA gọi `cleanKhaoDiLenh()`** trước.
Nếu item 35/37 vẫn còn trong bag → `Char.gameAJ(35)` trả `true` → packet tự sát KHÔNG được gửi.

### Root Cause 2: `respawnIfDead()` thiếu `GameScr.gameAB(5,0,0)`
Khi tự sát thành công, `respawnIfDead()` gọi `Service.gI().gameAK()` KHÔNG có
`GameScr.gameAB(5,0,0)` trước → client chưa mở dialog hồi sinh → server có thể
reject packet → desync trạng thái chết/sống.

### Fix (3 files)
1. **`AutoSanBoss.finishLangCoAndExit()`**: Gọi `cleanKhaoDiLenh()` + `sleep(300)` TRƯỚC `Code.gameAN()`
2. **`AutoSanBoss.suicideAndEnsureAlive()`** (method mới): Gọi `gameAN()`, nếu vẫn sống → gửi trực tiếp `Service.gI().gameAE()` (fallback)
3. **`AutoSanBoss.respawnIfDead()`**: Thêm `GameScr.gameAB(5,0,0)` trước respawn packet
4. **`ChatRouter.respawnQuick()`**: Cùng fix — thêm `GameScr.gameAB(5,0,0)` + dùng `gameAK()/gameAL()` thay `gameAF()`
5. **`ChatRouter.startPartyBoss()`**: Thêm sleep + fallback `gameAE()` cho member exit Làng Cổ
6. **`AutoBossEvent.returnMemberState()`**: Thêm sleep + fallback `gameAE()` cho member return

### Quy tắc VÀNG bổ sung
- **LUÔN gọi `cleanKhaoDiLenh()` + `sleep(300)` TRƯỚC `Code.gameAN()`** khi ở Làng Cổ
- **LUÔN có fallback `Service.gI().gameAE()`** sau `Code.gameAN()` nếu statusMe != 14
- **LUÔN gọi `GameScr.gameAB(5,0,0)` TRƯỚC packet hồi sinh** (`gameAK()/gameAL()`) để sync client

## 2026-08-21: Boss Chúa M20 — Chết Rồi Bỏ Boss Đi Săn Boss Khác

### Vấn đề
Boss Chúa M20 mạnh, nhân vật chết giữa trận → hồi sinh về nhà → bot bỏ qua boss đó đi săn boss khác.
Dù user thủ công về nhà để bot chạy lại M20, bot vẫn chuyển sang boss type khác. Rất ức chế vì boss chưa chết.

### Root Cause
`pkBossOnMap()` dùng `while (Code.gameAB instanceof PkBoss)` làm điều kiện loop.
Khi chết → PkBoss bị pop (`Code.gameAC()` tự động) → `Code.gameAB` không còn là PkBoss → while loop THOÁT.
Dù dòng 1507 restart PkBoss, nếu hồi sinh về nhà thì PkBoss mới chưa kịp load map → loop thoát ngay → `return false` → `huntBossType` chuyển map tiếp.

### Fix
- Thay `while (Code.gameAB instanceof PkBoss)` → `while (keepFighting)` + `deathCount`
- Khi PkBoss bị pop mà boss đã được tìm thấy (`sentPartyCmd == true`):
  → Hồi sinh → Restart PkBoss → Quay lại map boss → Tiếp tục đánh
- Giới hạn: `MAX_DEATH_RETRIES = 10`, `MAX_FIGHT_TIME_MS = 10 phút`
- Nếu quá 10 lần chết hoặc 10 phút → bỏ qua map, hiện thông báo
- Thêm `deathCount` hiển thị: "TSB: Chet lan X! Hoi sinh..."

### Quy tắc
- **KHÔNG dùng `Code.gameAB instanceof PkBoss` làm điều kiện duy nhất** cho while loop chiến đấu boss
- **Boss chưa chết thì PHẢI retry** — dùng flag + death counter
- **Timeout 10 phút/map** tránh loop vô hạn

## 2026-08-22: J2ME Loader Văng Sau 2s Khi Cài Đặt — Root Causes & Quy Trình Chuẩn

### 1. Root Cause 1: Package `javax/` bị đóng gói vào JAR
- **Triệu chứng:** Cài đặt JAR trên J2ME Loader chạy thanh tiến trình chỉ được 2 giây rồi báo lỗi cài đặt / DEX conversion fail.
- **Nguyên nhân:** Khi `javac` biên dịch với `stubs/javax/microedition/...`, nó sinh ra các file `.class` trong `build/unpacked/javax/`. Nếu không xóa thư mục `javax/` này trước khi pack `jar cfm`, hệ điều hành Android / Dalvik VM sẽ phát hiện package bị cấm `javax.*` (Security Exception: Overriding system package) và từ chối nạp APK/DEX ngay lập tức.
- **Giải pháp:** Sau khi chạy `javac` và trước khi đóng gói `jar cfm`, **BẮT BUỘC** chạy:
  `Remove-Item -Recurse -Force "build/unpacked/javax"`

### 2. Root Cause 2: Ép kiểu sai `Displayable` trong LCDUI Form
- **Triệu chứng:** `ClassCastException` hoặc `VerifyError` khi đổi màn hình từ Form LCDUI về Canvas game.
- **Nguyên nhân:** GameCanvas không kế thừa `javax.microedition.lcdui.Displayable` hay `Canvas`. `MotherCanvas` mới là lớp kế thừa `Canvas` (`Displayable`).
- **Sai:** `Display.getDisplay(GameMidlet.instance).setCurrent((Displayable)(Object)GameCanvas.instance);`
- **Đúng:** `Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());`

### 3. Root Cause 3: Inner Class rác còn sót lại (`NamMod$2.class`)
- **Nguyên nhân:** Khi unpack JAR cũ, các inner class cũ (như `NamMod$2.class`) vẫn nằm trong `build/unpacked/`. Khi code mới không còn sinh `NamMod$2`, class cũ bị bỏ rơi mang tham chiếu đến method cũ không tồn tại và chưa được strip `StackMapTable`.
- **Giải pháp:** Trước khi `javac`, luôn xóa sạch các file `.class` và `$*.class` của các file mã nguồn mod trong `src/`.

### 4. Root Cause 4: Quên thêm class mới vào whitelist `scripts/patch_class_j2me.py`
- **Nguyên nhân:** Khi tạo file Java mới (như `BossConfig.java`), nếu quên thêm vào `mod_classes` trong script patch, class mới sẽ giữ nguyên version 52.0 và StackMapTable → J2ME Loader không đọc được.
- **Giải pháp:** Luôn cập nhật danh sách `mod_classes` trong `scripts/patch_class_j2me.py` khi thêm file Java mới.

## 2026-08-22: Thoát Map Đóng Kín (M196 Up Lượng, M192 Tu Luyện, M135/136 Làng Cổ) Khi Bắt Đầu Săn Boss

### 1. Vấn đề
- Khi nhân vật đang cắm Tàn Sát ở các map phòng kín vào qua NPC như **Map Up Lượng (M196)**, **Map Tu Luyện (M192)** hoặc **Làng Cổ (M135/136)**:
- Đến giờ săn boss (hoặc khi gõ lệnh test), hệ thống lưu lại `savedMap` nhưng không tự sát thoát map.
- Các map phòng kín không có cổng waypoint thông thường ra bản đồ thế giới, khiến `PkBoss` bị kẹt lại bên trong map và không thể chạy ra map boss.

### 2. Giải pháp: `exitGatedMapIfNeeded()`
- Trước khi khởi chạy `PkBoss` đi săn boss, hệ thống kiểm tra: nếu nhân vật đang ở `mapID == 196 || mapID == 192 || AutoVipMap.isEnabled || AutoTuLuyen.isEnabled || TileMap.isLangCo(mapID)`:
  1. `saveLocalState()`: Chụp lưu lại Map/Khu/Auto trước.
  2. `cleanKhaoDiLenh()` (nếu ở Làng Cổ).
  3. `Code.gameAN()` (tự sát).
  4. `Service.gI().gameAK()` (Bấm "Về làng", hồi sinh tại Thôn/Trường có NPC).
  5. Khi đã ở Thôn/Trường an toàn, `PkBoss` bắt đầu di chuyển đường tắt sang map boss.
- Áp dụng cho cả **Trưởng nhóm (`beginLeaderEvent`, `testNow`)** và **Thành viên nhóm (`ChatRouter.startPartyBoss`)**.

## 2026-08-23: Mob Cooldown — Chống Đánh Trượt (TsBoost)

### Vấn đề
TsBoost thỉnh thoảng đánh "không khí" — mob đã chết trên server nhưng client chưa nhận packet xóa. Đây là server-side desync, game gốc cũng bị.

### Giải pháp: Circular Buffer Cooldown
- Thêm `isCooldownEnabled` + `COOLDOWN_MS` (default 1500ms, OFF) vào TsBoost config
- Circular buffer 16 slot lưu `(x, y, timestamp)` của mob vừa đánh
- `collectMobsInRange()`: skip mob có tọa độ trùng với buffer chưa hết cooldown
- `fireAttack()`: sau khi đánh, đánh dấu tọa độ mob vào buffer
- RMS format mở rộng 5→7 fields, backward compat: `idx >= 5` vẫn load bình thường

### Kỹ thuật
- **Tracking by position** (không phải mobId) vì mobId bị reuse khi mob respawn
- **Circular buffer** tránh GC — `cdIdx = (cdIdx + 1) % CD_SIZE`
- Cài đặt trong **TsConfig** form (checkbox + textfield)

## 2026-08-23: Auto Suicide & Auto Jump

### Auto Suicide (AutoSuicide.java)
- Monitor thread kiểm tra tọa độ nhân vật mỗi `CHECK_INTERVAL_MS` (default 5s)
- Nếu tọa độ không đổi quá `IDLE_TIMEOUT_MS` (default 30s) → gọi `Code.gameAN()` tự sát
- Chỉ kích hoạt khi auto đang chạy (`Code.gameAB != null`)
- Mặc định TẮT

### Auto Jump
- Thread gửi `Char.gameAC(cx, cy - 50)` mỗi `JUMP_INTERVAL_MS` (default 30s) để reset tọa độ server
- Phải gửi move THẬT (Char.gameAC) — move ảo không reset server-side coords
- Mặc định TẮT

### Persistence
- Cả 2 lưu chung RMS key `auto_suicide_cfg` format: `enabled;timeout;interval;jumpEnabled;jumpInterval`
- Toggle từ NamMod trigger `AutoSuicide.saveConfigToRMS()` ngay lập tức
- Cài đặt chi tiết trong TsConfig form

## 2026-08-23: ExploitConfig — Centralized Exploit Settings

### Architecture
- **ExploitConfig.java**: Form cài đặt tất cả exploit, tách khỏi NamMod
- NamMod chỉ có nút "Cài đặt CN Test ▸" → mở ExploitConfig form
- Mọi exploit mặc định **TẮT**, user tự bật từng cái test
- RMS key `exploit_cfg`, format 15 fields backward compat

### 6 Exploit Features
| # | Tên | Trạng thái | Config |
|---|-----|-----------|--------|
| 1 | Fast Attack | ✅ Implemented | Số lần gửi thêm (1-20, default 3) |
| 2 | Dupe Pickup | ✅ Implemented | Số lần nhặt thêm (1-20, default 3) |
| 3 | Item Use Race | ☐ Config only | Số lần dùng thêm (chưa hook) |
| 4 | NPC Repeat | ✅ Implemented + Test button | NPC Type/Opt1/Opt2/Count/Delay |
| 5 | Multi-hit | ☐ Config only | Số cặp skill+thường (chưa hook) |
| 6 | Ghost NPC | ☐ Config only | Checkbox (chưa hook) |

### NPC Repeat — Test NPC VIP 7
- Nút "Test NPC Spam" trong form → spawn thread
- Thread gửi `Service.gI().gameAC(npcT, opt1, 0)` + `Service.gI().gameAC(npcT, opt2, 0)` × N lần
- Default: NPC 47 (VIP), Opt1=0 (Nhận quà), Opt2=6 (VIP 7)
- Gửi packet trực tiếp — **KHÔNG cần NPC trên map, KHÔNG check coin client-side**
- Server tự check coin/VIP level → nếu server yếu (không lock giữa checks) có thể nhận quà nhiều lần

### Migration từ NamMod
- `NamMod.isDupePickup` → `ExploitConfig.isDupePickup`
- `NamMod.isFastAttack` → `ExploitConfig.isFastAttack`
- References cập nhật: Code.java, TsBoost.java, GhostBoss.java (2 chỗ)
- Dupe Pickup: hard-coded x3 → configurable loop `ExploitConfig.DUPE_PICKUP_COUNT`
- Fast Attack: hard-coded x2 → configurable loop `ExploitConfig.FAST_ATTACK_COUNT`

## 2026-08-23: Server Exploit Analysis — Packet Structure

### Attack Packet (Message 4/73/60/61)
- Client chỉ gửi `mobId` (byte) — server tự tính damage, tự quyết kill
- **KHÔNG THỂ fake mob kill** — kill logic 100% server-side
- `mobId` là ID tạm (0-255) do server gán khi mob spawn

### NPC Interact Packet (Message 29)
- `Service.gI().gameAC(npcType, option, param)` → 3 bytes
- **Client KHÔNG check coin** khi interact NPC — gửi packet trực tiếp OK
- `GameScr.gameAB(47, 0, 0)` check NPC tồn tại trên map (`gameAI()`)
- Bypass: gọi `Service.gI().gameAC()` trực tiếp bỏ qua NPC check

### Pickup Packet (gameAQ)
- Gửi `itemMapID` (int) — server check khoảng cách
- Ghost Move đã bypass check khoảng cách (đã chứng minh hoạt động)
- Dupe: gửi cùng `itemMapID` nhiều lần nhanh — server có race condition

### Server Validation Summary
| Gì | Mức | Bypass |
|----|-----|--------|
| Mob exists khi attack | Chặt | Không |
| Damage server-side | Chặt | Không |
| Khoảng cách nhặt item | Yếu | Ghost Move ✅ |
| Khoảng cách NPC | Yếu | Direct packet ✅ |
| Vị trí nhân vật | Yếu | Char.gameAC() spoof ✅ |
| Rate limit attack | Chưa rõ | Fast Attack x3+ chưa bị kick |
| Item pickup race | Yếu | Dupe x3+ hoạt động ✅ |

### patch_class_j2me.py
- Thêm `ExploitConfig`, `TsConfig`, `AutoSuicide`, `AutoBossNotice` vào `mod_classes` set
- Tổng 56 class files patched

## Sửa Các Lỗi Săn Boss & Pre-spawn
1. **Pre-spawn 30s phải đứng chờ đúng giờ:**
   - **Hiện tượng:** Khi còn 30s, bot chạy ra map và lập tức quét khu tìm boss luôn khi boss chưa spawn, quét hết khu không có boss rồi kết thúc lượt lãng phí.
   - **Khắc phục:** Di chuyển ra map boss đầu tiên, vào vòng lặp `while` đếm ngược hiển thị `TSBoss: Chờ tại M... (X s)...` và đứng yên đợi cho đến khi `secLeft <= 0` (đúng 00:00:00). Khi đúng giờ mới bắt đầu quét khu và tấn công.

2. **Làng Cổ quét sót map thứ 2:**
   - **Hiện tượng:** Khi quét map 135 không có boss, bot đứng yên trong map 135 rồi lặp 15 lần hết vòng lặp và tự sát thoát ra mà không tìm map 136.
   - **Khắc phục:** Thêm `returnToLangCoHub()` (`TileMap.gameAJ(0)` về M138). Khi quét xong 135 (hoặc 136) không thấy boss, hoặc vào nhầm map 134/137, bot lập tức quay về M138 để đi qua cổng tiếp cho đến khi quét đủ cả 2 map 135 & 136.

3. **Mở Rương / Cất đồ vào NPC sau khi tự sát về làng:**
   - **Nguyên nhân gốc rễ:** Trong luồng game loop của [`src/Code.java`](file:///root/ninja/src/Code.java) (dòng 753-820) chứa toàn bộ đoạn code vi dịch cũ (Auto Luyện Đá / Tự Mua Cổ Lệnh / Mở Rương Đồ NPC 4). Khi nhân vật xuất hiện ở làng/trường học, đoạn code này tự động gọi `Service.gI().gameAI(4)` (tương tác NPC rương) và `GameScr.gameAB(5,0,0)` (mở menu rương).
   - **Khắc phục triệt để:** **XÓA SẠCH HOÀN TOÀN** toàn bộ khối code tự động tương tác NPC rương/luyện đá này khỏi [`src/Code.java`](file:///root/ninja/src/Code.java). Trong toàn bộ dự án hiện tại không còn bất kỳ lệnh nào gọi `Service.gI().gameAI(4)` tự động nữa.

6. **Sửa Triệt Để Lỗi Tự Sát 2 Lần & Vào NPC Rương Đồ Sau Khi Về Làng:**
- **Nguyên nhân gốc rễ 1 (NPC Rương Đồ):** Trong tất cả các hàm hồi sinh (`respawnIfDead`, `respawnFast`, `respawnQuick`, `ensureAlive`), trước đây đều chứa dòng `GameScr.gameAB(5, 0, 0)`. Trong game engine J2ME gốc, `gameAB(npcId, option, 0)` là hàm **di chuyển đến NPC và mở menu đối thoại** (NPC 5 là Okane / Rương Đồ). Vì vậy mỗi khi chết/hồi sinh, nhân vật tự động chạy đến NPC Rương Đồ.
   - **Nguyên nhân gốc rễ 2 (Tự sát lặp 2 lần):** Trong `finishLangCoAndExit()`, sau khi tự sát lần 1, nhân vật đang chuyển map về làng nhưng `TileMap.mapID` chưa kịp đồng bộ (vẫn mang ID map Làng Cổ cũ trong vài trăm ms đầu). Đoạn code cũ có điều kiện `if (TileMap.isLangCo(TileMap.mapID))` lần 2 lập tức kích hoạt và gửi tiếp 1 packet tự sát nữa khi nhân vật vừa xuất hiện ở làng.
   - **Khắc phục triệt để:**
     1. **Xóa sạch toàn bộ lệnh `GameScr.gameAB(5, 0, 0)`** khỏi tất cả các file (`AutoSanBoss`, `AutoBossEvent`, `ChatRouter`, `AutoLevel`, `GhostBoss`). Khi hồi sinh chỉ gửi packet chuẩn `Service.gI().gameAK()` (về làng) hoặc `gameAL()` (tại chỗ) kèm `LockGame.gameAA = true`.
     2. Viết lại `finishLangCoAndExit()` chỉ tự sát 1 lần duy nhất, sau đó có vòng lặp chờ cho đến khi nhân vật thực sự rời khỏi Làng Cổ (`!TileMap.isLangCo(TileMap.mapID)`) mới kết thúc.

## 2026-08-25: TSBoss Ưu Tiên (AutoBossEvent) — 4 Bug Fix

### Bug 1: Vòng lặp chờ pre-spawn mắc kẹt 1 tiếng (s > 3600 boundary)
- **Hiện tượng:** Bot ra map boss chờ đếm ngược 30s, boss spawn xong nhưng KHÔNG đánh — tiếp tục hiện "Chờ tại M141 (1000s)..." hàng giờ.
- **Root cause:** Vòng lặp `while` chờ pre-spawn dùng `if (s <= 0 || s > 3600) break`.
  - Khi boss VDMQ 19h spawn, `getSecondsTillNextForPriority()` bỏ qua 19h (diff=0) → next là MapVIP 20h (diff=3600).
  - `3600 > 3600` = **FALSE** → vòng lặp KHÔNG break → chờ cả tiếng!
- **Fix:** Đổi `s > 3600` → `s > PRE_SPAWN_SECONDS` (30). Chờ 30s cuối xong, s nhảy lên hàng nghìn → `s > 30` → break ngay → đánh luôn.
- **File:** `AutoBossEvent.java` dòng 759
- **Quy tắc:** Vòng lặp chờ pre-spawn chỉ dùng cho 30s cuối. Threshold phải bằng `PRE_SPAWN_SECONDS`, không dùng magic number 3600.

### Bug 2: triggerImmediate (Chat Notice) vẫn vào vòng lặp chờ
- **Hiện tượng:** Khi nhận thông báo boss từ server (Chat Notice), `triggerImmediate()` set `ignoreBossHourCheck=true` nhưng vẫn vào vòng lặp pre-spawn wait.
- **Fix:** Thêm `!AutoSanBoss.ignoreBossHourCheck` vào điều kiện vào block pre-spawn.
- **File:** `AutoBossEvent.java` dòng 734

### Bug 3: Đang ở Map VIP + priority "Tất cả" → tự sát thừa khi boss VIP đang ra
- **Hiện tượng:** Priority "Tất cả", đang ở M195, boss VIP đang spawn → bot tự sát về thôn rồi lại vào M195 qua NPC → lãng phí thời gian.
- **Fix ban đầu (quá rộng):** Thêm `(eventPriority == 0 && (curMap == 195 || curMap == 196))` → luôn giữ ở VIP. **SAI** vì khi boss VIP chưa ra mà boss VDMQ ra thì bot mắc kẹt ở M195 không thoát!
- **Fix đúng:** Kiểm tra boss VIP **thực sự active** bằng cách check giờ spawn trực tiếp (không dùng `ignoreBossHourCheck`):
  ```java
  int[] hrs = AutoSanBoss.BOSS_HOURS[vipType];
  for (int i = 0; i < hrs.length; i++) {
      int d = nowSec - hrs[i] * 3600;
      if (d >= 0 && d < 2400) { vipActive = true; break; }
  }
  ```
  - VIP boss active → ở lại M195 săn luôn
  - VIP boss CHƯA active → fall through → tự sát thoát → đi săn boss khác
- **File:** `AutoBossEvent.java` dòng 198-222
- **Quy tắc:** Khi check "có nên ở lại gated map không", PHẢI check giờ spawn **thực tế**, KHÔNG dùng `isBossActive()` vì nó bị ảnh hưởng bởi `ignoreBossHourCheck`.

### Bug 4: Không tự sát thoát Map VIP khi cần đi săn map khác
- **Hiện tượng:** Săn xong boss ở M195, cần đi M14/M141 nhưng PkBoss không thoát được M195 (gated map) → bot kẹt.
- **Fix:** Thêm check `TileMap.mapID == 195 || TileMap.mapID == 196` trong `pkBossOnMap()` và `treoScanMap()` — khi target là map thường mà đang ở VIP → `suicideAndEnsureAlive()` trước.
- **File:** `AutoSanBoss.java` — `pkBossOnMap()` dòng 2690+ và `treoScanMap()` dòng 1617+
- **Quy tắc:** Mọi gated map (M195, M196, Làng Cổ) cần logic thoát riêng trước khi PkBoss di chuyển đến map thường. Pattern: check + suicide giống Làng Cổ đã có sẵn.

### Kiến trúc AutoBossEvent ↔ AutoSanBoss (tóm tắt)
- **AutoBossEvent** (`run()` loop mỗi 1s):
  - Monitor giờ boss → `anyBossActiveForPriority()` / `getSecondsTillNextForPriority()`
  - Pre-spawn 30s trước: `beginLeaderEvent()` → lưu vị trí TS → dừng auto → `exitGatedMapIfNeeded()` → travel map boss → chờ đếm ngược → start AutoSanBoss
  - Chờ `consumeEventRoundCompleted()` → xong → `finishEvent()` → quay về TS
- **AutoSanBoss** (`run()` loop):
  - `eventHuntMode=true`: quét boss types theo `eventHuntTypes` hoặc `HUNT_PRIORITY`
  - `isBossActive(type)`: check giờ spawn ± 40 phút (2400s)
  - Xong 1 round: set `eventRoundCompleted=true`
- **Gated maps cần thoát riêng:** M195, M196 (VIP), M135/136/LangCo, M192 (Tự luyện)
- **`ignoreBossHourCheck`:** Set true bởi `triggerImmediate()`, reset false bởi `beginLeaderEvent()` và `finishEvent()`

### Cần test phiên sau (2026-08-25)
1. **Pre-spawn 30s:** Treo ở M195/M192, chờ boss VDMQ/MapNgoài ra → bot có tự sát thoát và ra đúng map boss chờ không?
2. **Boss spawn → đánh ngay:** Sau 30s đếm ngược, boss ra → bot có quét và đánh ngay không? (không chờ thêm 1000s)
3. **VIP boss + priority All:** Ở M195 lúc boss VIP đang ra → bot có ở lại săn VIP trước không?
4. **Chuyển map sau VIP:** Săn xong VIP → bot có tự sát thoát M195 để đi map khác không?

## Bug: Đang chờ map VIP mà tự tele ra VDMQ (2026-08-26)
- **Triệu chứng:** Bot đang chờ ở M195/M196 (map VIP), chờ đến 0s boss ra, chưa kịp tìm boss thì tự sát ra map VDMQ (M141) để săn.
- **Nguyên nhân 1 — `exitGatedMapIfNeeded()` pre-spawn race (AutoBossEvent.java:212):**
  - Khi `eventPriority == 0` (Tất cả), hàm check `vipActive` bằng `d = nowSec - hrs[i]*3600`. Điều kiện `d >= 0 && d < 2400` yêu cầu boss ĐÃ spawn.
  - Nhưng pre-spawn trigger 30s trước giờ spawn → `d = -30` → `vipActive = false` → bot tự sát ra VDMQ.
  - **Fix:** Đổi thành `d >= -PRE_SPAWN_SECONDS && d < 2400` — cho phép giữ lại VIP map khi boss sắp spawn trong 30s.
- **Nguyên nhân 2 — `eventHuntTypes` bị reset race condition:**
  - `beginLeaderEvent()` gọi `startEventHuntAll()` → bên trong `stop()` reset `eventHuntTypes = null` → thread mới đọc `HUNT_PRIORITY` (bao gồm VDMQ) thay vì chỉ pre-spawn type.
  - Dòng 852 gán lại `eventHuntTypes` NHƯNG thread đã đọc null trước đó.
  - **Fix:** (1) Set `eventHuntTypes` TRƯỚC khi gọi `startEventHuntAll()`, (2) Trong `startEventHuntAll()` lưu/khôi phục `eventHuntTypes` thay vì xóa trắng, (3) Set lại SAU startEventHunt để đảm bảo.
- **Nguyên nhân 3 — `preSpawnType` dùng `getSecondsTillNextBoss` (chỉ tìm boss TƯƠNG LAI):**
  - Tại 12:00:00, boss VIP vừa spawn → `diff = 0` → `getSecondsTillNextBoss` yêu cầu `diff > 0` → **skip boss vừa spawn!**
  - Boss tiếp theo gần nhất là MapNgoai 13h (diff=3600) → `preSpawnType = TYPE_MAPNGOAI` → **SAI!**
  - **Fix:** Dùng `isBossActive()` + `HUNT_PRIORITY` để tìm boss ưu tiên cao nhất đang active, fallback sang closest nếu chưa active.
- **Nguyên nhân 4 — Đang ở M195 mà chuyển sang M196:**
  - `getFirstMapForPriority()` default case chọn VIP2 (ưu tiên cao hơn VIP) → bot tele khỏi M195 (đang đứng) sang M196 → lãng phí thời gian.
  - **Fix:** Nếu đã đứng ở M195/M196 và `firstMap == -1` (VIP), ở lại map hiện tại. Đồng thời `preSpawnType` ưu tiên boss map đang đứng.
- **Files sửa:** `AutoBossEvent.java`, `AutoSanBoss.java`

