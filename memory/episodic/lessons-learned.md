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