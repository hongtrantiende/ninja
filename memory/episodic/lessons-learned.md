# Lessons Learned

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

---

## 📋 TODO Phiên Sau
1. Test v9 trên emulator: bật `tspkb` có nhóm → xem members có nhận `pkm` + hiện "Tắt Auto"
2. Chờ boss spawn → xem PkBoss có tự quét khu + tìm boss + đánh không
3. Xem chat nhóm có bị spam pkm/pkk từ PkBoss nội bộ không
4. Test chết + hồi sinh + quay lại boss
5. Nếu PkBoss nội bộ gửi party commands gây rối → cần giải pháp mới
