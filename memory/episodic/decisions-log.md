# Decisions Log

## 2026-08-28 (Session 2): Synchronized Boss Finding with Configurable Zone Delay & Range in BossConfig
- **Bản chính (`Aeharuna.jar`):**
  - Thêm ô nhập **"Tốc độ chuyển khu (ms)"** vào Form **Cài đặt Săn Boss** (`BossConfig`), tùy chỉnh từ `10ms` đến `5000ms` (mặc định `10ms`), lưu RMS `boss_zone_delay`.
  - Thêm ô nhập **"Phạm vi khu"** (VD: `0-29` hoặc `1-29`, `2-20`...), lưu RMS `boss_zone_range` (`scanZoneStart` và `scanZoneEnd`), mặc định `0-29`. Cho phép quét đúng dải khu chỉ định (ví dụ `1-29` thì bắt đầu từ khu 1 đến khu 29). *Lưu ý:* Map **Làng Cổ (M135, M136)** được giữ nguyên cơ chế quét đủ cả 3 khu `K0, K1, K2` và không bị ảnh hưởng bởi cài đặt dải khu này.
  - Nút "Reset giờ": Khôi phục giờ spawn, delay `10ms`, và dải khu `0-29`.
- **Bản chia sẻ (`Aeharuna_share.jar`):**
  - Map VIP 1 (M195) & Map VIP 2 (M196): Tốc độ chuyển khu đặt ở **100ms**.
  - Các map khác (VDMQ, Map Ngoài, Thế Giới, Làng Cổ): Dùng cơ chế `PkBoss` nguyên bản của game.
  - Cài đặt Săn Boss: Form chỉ hiển thị 2 checkbox (Map VIP 1 và Map VIP 2).
  - Tắt săn đón đầu (Pre-spawn = 0s) và ẩn menu CN Test (Exploit).
- **Quy tắc Git:** Không commit / push lên GitHub (chỉ build và lưu cục bộ).


## 2026-08-28: Khôi Phục TsBoost & Tự Bán Phân Thân Lệnh Sau TS Boss / Mất Kết Nối
- **Vấn đề:** Khi bật "Giới hạn Phân Thân Lệnh" trong Cài đặt tàn sát (TsConfig), tính năng tự động bán PTL (`dropExcessPhanThan()`) chạy bình thường khi gõ lệnh `ts`. Tuy nhiên:
  1. Khi kết thúc phiên săn boss ưu tiên (`AutoBossEvent`) và khôi phục lại TS (`returnAndResume()`), TsBoost không được khởi chạy lại -> không bán PTL nữa.
  2. Khi bị mất kết nối và đăng nhập lại, TsBoost thread đã bị terminate do `Code.gameAB == null` tạm thời và không tự sống lại -> mất tác dụng bán PTL.
- **Quyết định & Giải pháp:**
  1. **Nâng cấp `TsBoost.run()`**: Khi `Code.gameAB == null` hoặc đang săn boss (`PkBoss`, `SanBossHolder`, `AutoSanBoss.isRunning`), thread chuyển sang trạng thái tạm nghỉ (`sleep(1000)` + `continue`) thay vì `break` thoát thread vĩnh viễn. Vẫn tiếp tục quét và bán PTL theo chu kỳ `LIMIT_CHECK_INTERVAL` (10s).
  2. **Kích hoạt `TsBoost.checkHang()` trong `Code.run()`**: Kiểm tra nếu `modeEnabled && !isRunning && Code.gameAB != null` (và không phải PkBoss/SanBossHolder) -> tự động gọi `start()`.
  3. **Bổ sung `TsBoost.onTsStarted()` trong `AutoBossEvent.java`**: Tại cả 2 điểm khôi phục `oldAuto` và fallback `Code.gameAA` trong `returnAndResume()`, gọi thêm `TsBoost.onTsStarted()`.
  4. **Bổ sung restart trong `Code.java`**:
     - Thêm `TsBoost.onTsStarted()` vào `Code.gameAA(Auto)` để bắt mọi điểm kích hoạt auto.
     - Thêm `TsBoost.syncAfterTs()` vào `Code.gameAP()` khi reconnect.
- **Files:** `src/TsBoost.java`, `src/Code.java`, `src/AutoBossEvent.java`, `Aeharuna.jar`

## 2026-07-30: AutoSanBoss — Can Thiệp Trực Tiếp Nút "Tách" Mặc Định Trong Game
- **Quyết định:** Tạo `SplitPatcher` và hook trực tiếp vào phương thức `Service.gameAA(short, String)` (bằng `scripts/patch_service.py`).
- **Lý do:** Người dùng muốn ấn chọn vật phẩm -> chọn nút **"Tách" mặc định** của game -> nhập số lượng (ví dụ: `30`) -> tự động chuyển sang cơ chế tách đồ lẻ (tách 30 món lẻ). Giờ đây, khi ấn nút Tách mặc định trong giao diện game và nhập số lượng > 1, game sẽ tự động thực hiện tách đồ lẻ mà không cần nhớ lệnh chat.
- **Files:** `src/SplitPatcher.java`, `scripts/patch_service.py`, `src/AutoSanBoss.java`

## 2026-07-30: AutoSanBoss — Thêm Tính Năng Tách Đồ Lẻ (Tách Từng Món 1)
- **Quyết định:** Thêm phương thức `tachDoLe(count)` trong `AutoSanBoss` và các lệnh chat `tach <số lượng>`, `tl <số lượng>`, `tachle <số lượng>`.
- **Lý do:** Mặc định của game Ninja School khi tách đồ xếp chồng (chồng Đá, Thức ăn, Bù nhìn...) chỉ cho tách đôi (chia 50/50). Tính năng mới giúp tách lẻ vật phẩm thành từng món đơn lẻ (số lượng 1) liên tục đúng số lần người dùng mong muốn (ví dụ: gõ `tach 30` sẽ tách thành 30 món lẻ).
- **Files:** `src/AutoSanBoss.java`, `src/Code.java`, `src/ChatRouter.java`

## 2026-07-30: AutoSanBoss — Tự Động Mời Bạn Bè Vào Nhóm Khi Mất Mạng Đăng Nhập Lại
- **Quyết định:** Thêm phương thức `autoInviteFriends()` trong `AutoSanBoss` và gọi tự động khi bật party mode, khi kết nối lại sau khi mất mạng (`waitForReconnect`), hoặc khi trưởng nhóm chưa có nhóm (`vParty.size() <= 1`). Thêm lệnh chat `moinhom` / `mnb`.
- **Lý do:** Khi trưởng nhóm chạy săn boss bị mất kết nối và đăng nhóm lại, nhóm cũ bị ngắt. Bot sẽ tự động duyệt danh sách bạn bè (`vFriend`) và gửi lời mời nhóm (`Service.gI().gameAH(friendName)`) để khôi phục lại nhóm tự động mà người dùng không cần thao tác tay.
- **Files:** `src/AutoSanBoss.java`, `src/Code.java`, `src/ChatRouter.java`

## 2026-07-30: AutoSanBoss — Thêm Lệnh `tspkball` / `all` Săn Tất Cả Boss 24/24
- **Quyết định:** Thêm lệnh `tspkball` (hoặc phím gõ tắt `all`) qua `ChatRouter`.
- **Lý do:** Kích hoạt chế độ săn ALL boss: quét tuần tự toàn bộ 17 map (Server M3, Thế Giới M23, VDMQ M141-143, MapNgoài 12 maps) trong 1 lượt, không phụ thuộc lịch spawn. Sau mỗi lượt dừng 10 giây và tự động lặp lại liên tục 24/24 cho đến khi tắt.
- **Files:** `src/AutoSanBoss.java`, `src/ChatRouter.java`

## 2026-07-30: AutoSanBoss (tspkb) — Thêm Delay 10 Giây Sau Mỗi Lượt Quét Boss
- **Quyết định:** Thêm delay 10 giây (`sleepSeconds(10)`) sau khi hoàn thành mỗi lượt quét tất cả các map/loại boss đang trong khung giờ spawn (`tspkb`).
- **Lý do:** Khi bật `tspkb`, bot quét liên tục không nghỉ gây hao tài nguyên và spam packet nếu boss chưa hồi sinh. Việc dừng 10s giữa mỗi lượt giúp giảm tải, mô phỏng hành vi tự nhiên hơn mà vẫn đảm bảo quét liên tục trong 40 phút boss sống.
- **Files:** `src/AutoSanBoss.java`

## 2026-07-30: V1 Release — Methodref Replacement Pattern (QUAN TRỌNG)
- **Quyết định:** KHÔNG patch bytecode Code.class khi thêm lệnh chat mới. Thay vào đó, patch **GameScr.class** bằng cách thay đổi methodref index (2 bytes) từ `Code.gameAF(String)Z` → `ChatRouter.checkAll(String)Z`.
- **Lý do:** Code.class có method gameAF(String) dài 9067 bytes với 334 StackMapTable entries. INSERT/APPEND bytecode đều fail vì phải fix quá nhiều thứ (SMT, Exception table, LineNumberTable, branch targets). Thay methodref tại call site chỉ đổi 2 bytes + thêm CP entries → KHÔNG thay đổi bytecode structure.
- **Pattern chuẩn cho tương lai:**
  1. Tìm call site: `GameScr.class` gọi `invokestatic Code.gameAF(String)Z`
  2. Tạo wrapper class: `ChatRouter.checkAll(String)Z` — check lệnh mới TRƯỚC, fallback `Code.gameAF()` SAU
  3. Chạy patcher: `build/patch_gamescr.py` thay methodref index
- **Files:** `build/patch_gamescr.py`, `src/ChatRouter.java`

## 2026-07-30: Force-Boss Commands (tspkbsv/tg/vm/mn)
- **Quyết định:** Thêm 4 lệnh force-boss qua ChatRouter, không cần chờ lịch spawn.
- **Lý do:** Boss có thể được admin cho hồi sớm, cần đi săn ngay mà không cần đợi lịch.
- **Thứ tự check:** Lệnh mở rộng TRƯỚC → `Code.gameAF` gốc SAU (vì gameAF có thể return true cho mọi input).

## 2026-07-30: Auto-Reconnect + Nhặt đồ nhanh
- **Auto-reconnect:** `isDisconnected()` check `Char.getMyChar() == null`. `waitForReconnect(120s)` chờ game tự reconnect, sau đó khôi phục dummyAuto + gửi lại pkm cho nhóm.
- **grabAllItems():** Gửi `Service.gI().gameAQ(itemMapID)` cho TẤT CẢ item 30ms/item, 5 rounds, gọi tự động khi PkBoss kết thúc.

## 2026-07-30: AutoPickup — Nhặt đồ nhanh cho ts/tsn/ak
- **Quyết định:** Tạo class `AutoPickup` (Runnable, thread riêng) nhặt TẤT CẢ item 30ms/item liên tục.
- **Hook qua ChatRouter:** Intercept lệnh `ts`/`tsn`/`ak` → gọi Code.gameAF gốc → check `Code.gameAB != null` → bật/tắt AutoPickup.
- **Lệnh riêng:** `nhat` toggle on/off.
- **An toàn:** `equals("ts")` exact match → lệnh nhóm "ts 1 2 3" không bị intercept.


## 2026-07-29: Redesign AutoSanBoss v9 — Để PkBoss tự quét thay vì scan thủ công
- **Quyết định:** Xóa toàn bộ logic scan khu thủ công (`scanZone`, `travelToMap`, `startPkBossAndWait`, `scanAndFightOnMap`). Thay bằng `pkBossOnMap(mapID)` đơn giản: start `PkBoss(mapID)` → PkBoss tự quét khu, tìm boss, đánh, gửi lệnh nhóm.
- **Lý do:** `Service.gI().gameAA(zone, -1)` KHÔNG phải API chuyển khu → nhân vật đứng yên. PkBoss đã có sẵn logic quét khu trong `gameAK()` loop (bắt đầu với `zoneID = -2`).
- **Files:** `src/AutoSanBoss.java` (rewrite), `src/SanBossHolder.java` (new)

## 2026-07-29: SanBossHolder — Dummy Auto giữ menu "Tắt Auto"
- **Quyết định:** Tạo `SanBossHolder extends Auto` với 3 method rỗng (`gameAC`, `gameAD`, `gameAK`) để giữ `Code.gameAB != null` khi AutoSanBoss chạy nhưng PkBoss không active.
- **Lý do:** `PkBoss(0)` dummy bị game engine xóa ngay trong loop. `SanBossHolder` không làm gì nên không bị pop.

## 2026-07-29: Party Mode tự detect — Không cần lệnh riêng `tsnpkb`
- **Quyết định:** `toggle()` tự detect nhóm: `GameScr.vParty.size() > 1` → party mode ON. Không tạo lệnh `tsnpkb` riêng vì chat handler compiled trong Code.class, không thể thêm lệnh mới dễ dàng.
- **Flow party:**
  1. Leader bật `tspkb` có nhóm → gửi `pkm currentMap` ngay → Members bật PkBoss + hiện "Tắt Auto"
  2. Leader quét solo, tìm boss → gửi `pkm mapID` + `pkk zoneID` → Members đến đánh
  3. Leader tắt → gửi `pke` → Members tắt PkBoss

## 2026-07-29: Boss spawn 40 phút + quét TẤT CẢ loại boss cùng lúc
- **Quyết định:** `BOSS_ALIVE_DURATION = 2400` (40p). `run()` loop quét tuần tự 4 loại boss trong 1 cycle, không chỉ 1 loại. Sau khi xong 1 loại → chuyển ngay sang loại tiếp (0 delay).
- **MapNgoai:** Quét tất cả 12 map `{14,15,16,44,67,70,24,41,45,18,36,54}`, không phân biệt level.

## 2026-07-29: Thêm lệnh `tspkb` — Tự Động Săn Boss 24/7
- **Quyết định:** Tạo class `AutoSanBoss` (Runnable) chạy thread riêng, tự động theo dõi khung giờ spawn 4 loại boss (Server M3, Thế Giới M23, VDMQ M141-143, Map Ngoài), chuyển map, kích hoạt PkBoss đánh, xử lý chết/hồi sinh.

## 2026-07-29: Redesign TTB HUD — Sửa nền đen che game + thiết kế xấu
- **Quyết định:** Thay nền đen fillRect bằng `Paint.gameAA()` native game panel, dời sang phải màn hình, compact 1 dòng/boss, sửa data chính xác 5 loại boss.

## 2026-07-29: Thêm lệnh chat `ttb` hiển thị Khung Overlay HUD Lịch Boss Trực Tiếp Trên Màn Hình
- **Quyết định:** Chuyển đổi hiển thị từ Popup Dialog sang **Khung UI HUD Overlay vẽ trực tiếp trên màn hình game** (góc trên bên trái):
  - Tự động tính toán thời gian đếm ngược còn lại đến lần xuất hiện tiếp theo của từng loại Boss.
  - Tự động **sắp xếp Boss đang xuất hiện hoặc chuẩn bị xuất hiện sớm nhất lên ĐẦU danh sách**.
  - Đổi màu sắc trực quan (Đỏ/Vàng cho Boss đang có hoặc < 5 phút, Xanh/Trắng cho Boss sắp tới).
  - Gõ `ttb` để **Bật/Tắt** khung hiển thị này trực tiếp khi đang treo game/đánh quái.

## 2026-07-29: Mod Auto `gaoda` đứng tại chỗ Nhận & Giao đá từ xa (Remote NPC calls)
- **Quyết định:** Loại bỏ việc chuyển map/di chuyển nhân vật trong `AutoGaoDa.java`. Cho nhân vật đứng yên 1 chỗ gửi trực tiếp gói tin tương tác NPC 62 (Nhận đá) và NPC 63 (Giao đá) từ xa liên tục với delay 10ms.

## 2026-07-28: Mod Tàn Sát đánh song song 5 skill chạy ngầm (`ts` & `tsn`)
- **Tối ưu hóa Giao thức Packet Server:**
  1. `Message 41` (`Service.gameAG(s.template.id)`): Gửi lệnh đổi skill ngắn hạn lên Server Ninja School.
  2. `Message 4/60` (`Service.gameAA(targetMobs, ...)`): Gửi mảng byte ID các quái sống nằm đúng phạm vi `dx, dy` và số lượng `maxFight` của từng chiêu.
  3. Thêm `Thread.sleep(40ms)` để gói tin truyền qua TCP/IP socket trơn tru, không bị Server thả trôi gói.

## 2026-07-28: Mod tốc độ hồi chiêu skill về 10ms cho tất cả phái và kỹ năng

## 2026-07-27: Mod lệnh chat `gaoda` cho Aeharuna.jar
- **[2026-07-31] Thay đổi nút Tách lẻ**:
  - Vấn đề: Mod Tách lẻ cũ can thiệp vào `Service.gameAA` (Packet 92 và 13) làm hiển thị menu hỏi Tách lẻ / Tách đôi sau khi nhập số lượng, gây bất tiện và làm thay đổi flow của game.
  - Quyết định: 
    - Vô hiệu hóa hook cũ trong `SplitPatcher` (return false).
    - Sử dụng `Methodref Replacement` (script `patch_gamescr_menu.py`) để thay thế tất cả lời gọi `Menu.gameAA(MyVector)` trong `GameScr.class` thành `SplitPatcher.hookMenu(Menu, MyVector)`.
    - `hookMenu` quét qua `MyVector`, nếu thấy `cmd.idAction == 110244` (ID của nút Tách gốc) thì chèn thêm nút `Tách lẻ` (idAction 99999) ngay sau đó.
    - Xử lý tách lẻ sử dụng `Service.gI().gameAK(GameScr.gameBM, 1)` (Packet -85) gọi liên tục trong 1 Thread (ngủ 150ms).
  - Ưu điểm: Menu hiện trực quan ngay trong Hành trang. Nút Tách gốc hoạt động bình thường như chưa từng bị mod. Không ảnh hưởng các luồng xử lý khác.

## 2026-07-31: Sửa Lỗi Patch `Service.class` Gây Đơ Game Khi Bấm "Chơi Tiếp" (Login Freeze)
- **Quyết định:** Revert `Service.class` về phiên bản nguyên bản chưa patch.
- **Lý do:** Script `patch_service.py` trước đây chèn 9 bytes bytecode thủ công có offset branch `ifeq` sai 1 byte (+5 thay vì +4) làm hỏng JVM stack balance khi login (Packet 92), gây ra lỗi freeze khi ấn "Chơi tiếp". Hook `SplitPatcher.checkSplit()` vốn trả về `false` nên việc loại bỏ hook này không gây mất bất kỳ tính năng nào.

## 2026-07-31: Phục Hồi Menu "Tắt Auto" Cho Thành Viên Nhóm Trong `AutoSanBoss`
- **Quyết định:** Nâng cấp `restoreDummyAuto()` trong `AutoSanBoss.java`: Tự động khôi phục `SanBossHolder` ngay khi `Code.gameAB` bị `null` HOẶC bị ghi đè bởi auto khác (trừ `PkBoss`), và giảm chu kỳ kiểm tra của thành viên xuống 2 giây.
- **Lý do:** Khi thành viên đi theo trưởng nhóm săn boss, `Code.gameAB` có thể bị reset hoặc ghi đè khiến menu "Tắt Auto" biến mất.

## 2026-07-31: Tự Động Quét & Mời Bạn Bè (`GameScr.vFriend`) Khi Gõ `tspkb`
- **Quyết định:** Thêm `checkHasPartyOrFriends()` kiểm tra cả `vParty`, `Code.gameAI` và `GameScr.vFriend`. Nâng cấp `autoInviteFriends()` quét và gửi lời mời nhóm đến tất cả bạn bè online trong `GameScr.vFriend`.
- **Lý do:** Trưởng nhóm đứng 1 mình khi bật `tspkb` không tự động mời bạn bè nếu `vParty.size() <= 1` và danh sách `Code.gameAI` trống. Nâng cấp giúp tự động lập nhóm với Bạn Bè ngay lập tức.

## 2026-07-31: Cập Nhật Map, Khung Giờ Boss & Xử Lý Member Trong AutoSanBoss
- **Quyết định:** Cập nhật ID map chính xác cho Boss Server (M63), Thế Giới (M65), MapNgoài (M21, M46) và các khung giờ spawn tương ứng. Thêm `startPartyMember()` trong `AutoSanBoss`, hook `stopCurrentAuto()`, `startPartyBoss()`, `stopPartyBoss()` trong `ChatRouter` và cập nhật `ThongTinBoss`.
- **Files:** `src/AutoSanBoss.java`, `src/ChatRouter.java`, `src/ThongTinBoss.java`, `Aeharuna.jar`

## 2026-07-31: Thêm Menu Tiện Ích UI "Nam Mod" Trong Menu 3 Gạch
- **Quyết định:** Thêm `src/NamMod.java` và inject vào Menu 3 gạch qua `SplitPatcher.injectNamMod()`.
- **Lý do:** Giúp người dùng dễ dàng bật/tắt các tính năng Săn Boss, Lịch Boss, Nhặt Nhanh, Mời Nhóm, Tách Lẻ... trực quan bằng giao diện Menu UI in-game mà không cần nhớ các lệnh chat thủ công.
- **Files:** `src/NamMod.java`, `src/SplitPatcher.java`, `Aeharuna.jar`




## 2026-08-03: Thêm Chế Độ Treo Boss (tstreo/treo) & Tối Ưu Săn Boss (tspkb)
- Thêm cờ treoMode trong AutoSanBoss, lệnh chat (tstreo, treo, treosv, treotg, treovm, treomn) và 5 nút menu UI trong NamMod.java.
- Leader trong treoMode chỉ dùng PkBoss để di chuyển tới map, sau đó dừng PkBoss và tự quét khu K0->K29 bằng Auto.gameAA(zone) với delay 100ms/khu. Double check 300ms chống false-positive.
- Leader phát hiện boss sẽ gửi pkm -> pkk -> chờ 3s -> pke. Sửa stopPartyBoss() trong treoMode không kill thread TV mà chỉ pop PkBoss để TV đứng yên tại khu boss.
- Thêm lockBossFocus() liên tục ghim Char.mobFocus = bossMob trong tspkb để không bị nhảy target sang quái thường.
- Tối ưu respawnFast() delay xuống 10-50ms (hồi sinh ~120ms) và restart PkBoss ngay lập tức khi sống lại.
## 2026-08-03 — v20: Tách Tín Hiệu Chế Độ Nhóm Đánh/Treo/Dừng
- **Vấn đề:** Sau khi thành viên từng chạy Treo Boss, `treoMode=true` có thể còn sống trong thread `AutoSanBoss`. Khi trưởng nhóm chuyển sang `tspkb`, thành viên vẫn xử lý `pkk` theo luồng treo nên không đánh boss.
- **Quyết định:** Dùng ba tín hiệu nội bộ riêng trước khi gửi map boss:
  - `pkm -1`: ép thành viên về chế độ **ĐÁNH** (`startPartyMemberNormal()`), xóa `treoMode` cũ.
  - `pkm -2`: ép thành viên vào chế độ **TREO** (`startPartyMemberTreo()`).
  - `pkm -3`: dừng hoàn toàn Auto Săn/Treo Boss của thành viên (`stopPartyMemberFully()`).
- **Luồng `tspkb`:** Leader gửi `pkm -1` → `pkm <map>` → `pkk <zone>`; cả leader và thành viên dùng `PkBoss` để tele tới boss và cùng đánh.
- **Luồng `tstreo/treo`:** Leader gửi `pkm -2` → `pkm <map>` → `pkk <zone>` → `pke`. Thành viên chỉ dùng `PkBoss` để tới map; khi nhận `pkk`, pop `PkBoss`, gọi `Auto.gameAA(zone)` và đứng tại điểm vào khu, không tele tới boss/không đánh.
- **Khi tắt:** Leader gửi `pkm -3` rồi `pke` để thành viên không giữ thread/holder cũ.
- **Files:** `src/AutoSanBoss.java`, `src/ChatRouter.java`, `src/Code.java`, `Aeharuna.jar`

## 2026-08-03: TSBoss - Tam dung TS de san boss va tu khoi phuc rieng tung thanh vien
- Quyet dinh: Them AutoBossEvent va lenh tsboss bat/tat. Moi client tu luu Code.gameAB, map va khu cua chinh minh. Den gio boss, leader moi nhom mot lan, gui pkm -4 de thanh vien luu trang thai nhung van TS; leader tim boss toi da 15 phut. Chi khi thay boss moi goi thanh vien danh. Boss xong hoac het 15 phut, leader gui pkm -5; moi client tu ve map/khu cu va phuc hoi dung auto TS da luu.
- An toan nhom: Khong broadcast mot khu quay ve chung, tranh tat ca thanh vien don vao cung khu.
- Files: src/AutoBossEvent.java, src/AutoSanBoss.java, src/ChatRouter.java, src/NamMod.java, Aeharuna.jar
## 2026-08-03: Chot thu nghiem TSBoss trong 1 ngay
- Co hai lenh rieng:
  - tsboss: che do chinh thuc, cho dung gio boss moi tu khoi dong.
  - tsbosstest: bo qua lich, quet ALL boss ngay lap tuc, toi da 15 phut.
- Thu tu dung tsbosstest bat buoc: tat ca nick bat ts truoc, sau do leader chat tsbosstest. Neu chat ts sau test, TanSat se ghi de boss-auto.
- tsbosstest tu huy co phien inEvent/AutoSanBoss bi ket, cho thread cu thoat 700ms, sau do khoi dong lai phien ALL.
- Test la mot phien; neu tsboss truoc do dang OFF thi ket thuc test se tra ve OFF.
- Khi tim thay boss moi gui pkm -1, pkm map, pkk zone de goi thanh vien danh. Khi boss xong hoac qua 15 phut gui pkm -5.
- Moi client luu rieng Auto cu, map va zone; luc ket thuc tu ve dung map/khu rieng roi gan lai Auto ts cu.
- Jar da xac minh: MANIFEST.MF la entry dau, ZIP flags 0, khong dong goi javax/microedition stub.
## 2026-08-04: Chot TSBoss, softkey R va Hut VP theo TS
- TSBoss chi luu Auto/map/zone tai thoi diem phien boss thuc su khoi dong, khong luu luc vua bat che do cho. Truong nhom van TS va chuyen khu binh thuong truoc gio boss.
- Khi khoi dong boss, leader goi LockGame.gameBK(), cat Code.gameAB tam thoi, cho map/zone on dinh roi moi bat PkBoss. Thanh vien cung huy khoa di chuyen truoc khi nhan PkBoss.
- Mot luot duoc tinh theo day du danh sach map cua tung loai boss: Server, The Gioi, VDMQ 141-143, Map Ngoai 12 map.
- Sau luot dau: nghi 10 giay, quet luot tiep; lap lai trong 10 phut. Het 10 phut khong cat ngang boss/luot dang chay, chi ve TS tai ranh gioi ket thuc luot.
- TSBoss bat trong 40 phut boss con song van khoi dong, ke ca qua phut 15.
- Softkey phai Java ME R dung keycode -7/-22. Khi dang co Auto va o man hinh game, R goi ChatRouter.stopCurrentAuto; khi khong co Auto thi giu hanh vi goc.
- Khong dung chu R va khong co lenh chat r. Hook MotherCanvas dung methodref replacement GameGraphics.gameAA(I)V -> ShortcutHandler.handleKey(GameGraphics,int), script scripts/patch_mothercanvas_shortcut.py.
- Chat ts/tsn/ak dong bo Hut VP. Rieng ts co the tao TanSat tre, AutoPickup.syncAfterAutoCommand cho toi 30 giay va bat Hut VP khi TanSat xuat hien.
- Jar kiem tra: MANIFEST.MF dau tien, ZIP flags 0, khong dong goi javax/microedition stub.

## 2026-08-05: Đóng băng quái (bang / fz) làm mặc định khi vào game
- **Quyết định:** Chuyển hai cờ `Code.gameBE` và `Code.timBG` sang mặc định `true` ngay khi khởi tạo nhân vật / vào game.
- **Tác dụng:** Giống như khi người dùng gõ lệnh `bang` (hoặc `fz`), tính năng đóng băng quái (băng boss & băng skill) được tự động bật sẵn 24/7 từ lúc vừa đăng nhập vào game mà không cần phải gõ lệnh chat thủ công.
- **Files thay đổi:** `src/Code.java`, `Aeharuna.jar`

## 2026-08-05: Mod Xóa Nền Trời Map (Nền Đen 1.4.8 / Fix Lag)
- **Quyết định:** Patch bytecode `GameCanvas.class` tại phương thức `gameAA(mGraphics)`: chèn lệnh `goto 26` ngay tại PC 0.
- **Tác dụng:** Loại bỏ hoàn toàn ảnh nền trời, mây, núi, cảnh nền chuyển động ở phía sau các map, tô đen toàn bộ phông nền canvas (`0x000000`) giống bản 1.4.8. Giữ nguyên bậc đá, mặt đất, cột đá, cây cối địa hình, quái, nhân vật và giao diện UI để di chuyển & chơi bình thường.
- **Files thay đổi:** `scripts/patch_black_bg.py`, `build/unpacked/GameCanvas.class`, `Aeharuna.jar`

## 2026-08-05: Mặc Định Bật Hút VP & SPGame = 20 (Khôi phục lệnh s về mặc định)
- **Quyết định:** Set `Code.gameAQ = true`, `AutoPickup.start()`, và `Char.speedGame = 20` mặc định khi khởi tạo game; khôi phục `Code.gameBG = false`, `Code.gameBH = 5` về mặc định gốc của game.
- **Tác dụng:**
  1. Hút VP (nhặt xa / auto pickup) bật tự động 24/7 từ khi khởi động game.
  2. Tốc độ `SPGame` (tốc độ xử lý game trong menu auto) mặc định bằng `20` (thay vì 30).
  3. Lệnh `s` (fake tốc chạy/tốc giày) được khôi phục về mặc định gốc (`gameBG = false`), không ép bật `s 20`.
- **Files thay đổi:** `src/Code.java`, `src/AutoPickup.java`, `Aeharuna.jar`

## 2026-08-05: Cài Đặt Mặc Định Cho Menu "Tự Động" Theo Ảnh Yêu Cầu
- **Quyết định:** Đã cấu hình lại 20 mục trong menu "Tự động" mặc định chính xác theo 4 ảnh chụp của người dùng:
  - `Dùng HP khi còn dưới: 20%` (Bật)
  - `Dùng MP khi còn dưới: 20%` (Bật)
  - `Dùng thức ăn cấp: 50` (Bật)
  - `Dùng chiêu hỗ trợ` (Bật)
  - `Dùng khiên mana` (Bật)
  - `Dùng đốt quái & ẩn thân` (Bật)
  - `Dùng phân thân` (Bật)
  - `Nhặt yên` (Bật)
  - `Nhặt VP Nhiệm Vụ` (Tắt)
  - `Nhặt VP Sự Kiện` (Bật)
  - `Nhặt All` (Bật)
  - `Nhặt SVC` (Bật)
  - `ReMap` (Bật)
  - `Tàn sát map trống` (Tắt)
  - `Auto Mua Thức Ăn` (Bật)
  - `TS khi hết MP` (Bật)
  - `Auto Reconnect` (Bật)
- **Files thay đổi:** `src/Code.java`, `Aeharuna.jar`

## 2026-08-05: Bật Tự Động Hút VP (NamMod AutoPickup) Khi Bật Tàn Sát Trong Menu Auto
- **Quyết định:** Chèn `AutoPickup.start()` trực tiếp vào phương thức `Code.gameAA(Auto var0)` trong `src/Code.java`.
- **Tác dụng:** Khi người dùng bật tính năng **"Tàn sát"** từ trong Menu Auto (hoặc bất kỳ hình thức kích hoạt Auto nào), tính năng Hút VP của Nam Mod sẽ tự động được khởi chạy ngay lập tức tương tự như khi gõ lệnh chat `ts`.
- **Files thay đổi:** `src/Code.java`, `Aeharuna.jar`

## 2026-08-05: Sửa Triệt Để Lỗi Đứng Hình / Không Đánh Quái Khi Treo Tàn Sát Lâu (`Auto.java`)
- **Quyết định:**
  1. Giảm thời gian chờ đổi mục tiêu quái `gameAR` từ `5000ms` (5s) xuống `1500ms` (1.5s) trong `src/Auto.java`.
  2. Bổ sung kiểm tra quái đã chết (`hp <= 0` hoặc `status == 1`) vào điều kiện đổi mục tiêu quái tức thì.
  3. Reset `var6 = null` khi quái mục tiêu vượt quá tầm đánh (`Res.abs(cx - xFirst) > dx + 30`) để tránh vòng lặp khóa mục tiêu kẹt vị trí.
- **Tác dụng:** Loại bỏ hoàn toàn hiện tượng nhân vật đứng chôn chân ở khu mà không đánh quái khi treo game lâu.
- **Files thay đổi:** `src/Auto.java`, `Aeharuna.jar`

## 2026-08-07: Restore AutoPickup v3.2 từ GitHub, Xóa static block tự start
- **Quyết định:** Khôi phục `AutoPickup.java` về đúng phiên bản v3.2 trên GitHub (commit `df13724`). Xóa `static { start(); }` gây auto-start khi class load.
- **Lý do:** Bản local bị sửa quá nhiều, thêm static block khiến hút VP bật ngay khi vào game dù chưa gõ lệnh. V3.2 GitHub là baseline ổn định.
- **Files:** `src/AutoPickup.java`, `src/NamMod.java`, `src/ChatRouter.java`

## 2026-08-07: Xóa Code.gameAQ=true khỏi AutoLevel + AutoPickup
- **Quyết định:** Xóa mọi dòng `Code.gameAQ = true` trong `AutoLevel.java` và `AutoPickup.java`.
- **Lý do:** `gameAQ = true` (default trong `gameAP()`) = hút VP gần chân, KHÔNG tele. `gameAQ = false` = nhặt xa, CÓ tele. Nếu set `gameAQ = true` trong các class khác → conflict khi game gốc đang ở mode nhặt xa. Để `gameAP()` init duy nhất.
- **Files:** `src/AutoLevel.java`, `src/AutoPickup.java`

## 2026-08-07: VIP Watcher → Thread Riêng, Không Nằm Trong while(gameCA)
- **Quyết định:** Di chuyển VIP map check từ `while(gameCA)` loop → thread riêng `startVipMapWatcher()` trong Code.java.
- **Lý do:** `while(gameCA)` CHỈ chạy khi TS active. Sau disconnect/reconnect, `gameCA = false` → loop dừng → VIP check chết. Thread riêng chạy `while(DungMapVip)` độc lập.
- **Auto-restart:** Thêm restart logic trong `gameAP()` (init method, gọi khi reconnect) để watcher tự sống lại.
- **Files:** `src/Code.java`, `src/NamMod.java`

## 2026-08-08: TsBoost v4 — Anti-Stuck Vị Trí XY (Chính Xác 100%) & Fix Hàm Tự Sát Menu
- **Quyết định:** Nâng cấp hệ thống anti-stuck TsBoost sang v4 dùng vị trí tọa độ `(cx, cy)` làm tín hiệu quyết định CHÍNH. Sửa phương thức `suicideAndReturn()` dùng `Code.gameAN()` (gửi `Service.gI().gameAE()`), chuẩn 100% theo nút "Tự sát" (lệnh chat `die`) trong menu game.
- **Cơ chế Vị trí XY:**
  - Mỗi 30s chụp snapshot tọa độ `(cx, cy)`.
  - 30s tiếp theo nếu vị trí **TRÙNG KHỚP (Khớp!)** -> Xác định KẸT -> Tự sát ngay lập tức (`Code.gameAN()`).
  - Nếu vị trí **THAY ĐỔI (Di)** -> Bình thường -> Xóa tọa độ cũ, lưu tọa độ mới, tiếp tục vòng lặp.
  - HP/MP chỉ hiển thị phụ trên thông báo `safeNotify`, không dùng để quyết định tự sát.
- **Thông báo hiển thị 24/7 mỗi 30s:**
  - OK: `TsPro: 30s OK | (x,y) Di HP:... MP:...`
  - Kẹt: `TsPro: KET! (x,y) Khop! HP:... MP:... => Tu sat!`
  - Chết: `TsPro: 30s | Dang chet, doi hoi sinh`
  - Boss: `TsPro: 30s | Boss mode, bo qua`
- **Tích hợp Menu Tự động:**
  - Bấm nút **"Tàn sát"** trong menu Tự động -> Gọi `TsBoost.onTsStarted()` (tự bật Ts Pro đi kèm như lệnh chat `ts`).
  - Mặc định **"Dùng phân thân"** -> **TẮT** (`Char.DungPhanThan = false`).
  - Mặc định **"Tàn sát map trống"** -> **BẬT** (`Char.TsMapTrong = true`).
- **Files:** `src/TsBoost.java`, `src/Code.java`

## 2026-08-11: Thêm Boss Chúa (Map ID 20, 12h & 21h) vào Săn Boss & Lịch Boss
- **Quyết định:** Thêm loại boss mới `Boss Chúa` (Map ID `20`, khung giờ `12:00` và `21:00`) vào hệ thống tự động săn boss (`AutoSanBoss.java`), lịch boss HUD (`ThongTinBoss.java`), menu UI NamMod (`NamMod.java`), và các lệnh chat mở rộng (`ChatRouter.java`).
- **Chi tiết:**
  - `ThongTinBoss.java`: Thêm `new BossData("Chúa", "M20", new int[] {12, 21})`. Tự động đếm ngược và hiển thị trên HUD overlay.
  - `AutoSanBoss.java`: Thêm `TYPE_CHUA = 4` (`{20}`, `{12, 21}`), hỗ trợ tự động săn/treo theo lịch và các lệnh `tspkbchua`, `chua`, `treochua`.
  - `NamMod.java`: Thêm nút **"Săn Chúa"** và **"Treo Chúa"** vào menu UI NamMod.
  - `ChatRouter.java`: Thêm router nhận dạng câu lệnh `tspkbchua`, `chua`, `treochua`.
- **Files:** `src/ThongTinBoss.java`, `src/AutoSanBoss.java`, `src/NamMod.java`, `src/ChatRouter.java`, `Aeharuna.jar`
## 2026-08-13: Sửa Lỗi Tích Mua Cổ Lệnh Bị Mua Nhầm Vật Phẩm Trong Menu Auto (`TileMap.class`)
- **Nguyên nhân:** Trong `TileMap.class`, khi cờ `Char.MuaCoLenh` được tích chọn và nhân vật không có Cổ Lệnh (ID `490`), game gửi gói tin mua vật phẩm `Service.gI().gameAB(14, 28, 2)` (Cửa hàng Goshu 14, slot 28). Slot 28 trong cửa hàng Goshu (gồm 5 hàng x 6 cột) tương ứng với ô Hàng 5 Cột 5 (ô kế cuối), dẫn đến mua nhầm item đứng trước Cổ Lệnh.
- **Quyết định:** Tạo script [`scripts/patch_colenh_slot.py`](file:///root/ninja/scripts/patch_colenh_slot.py) patch bytecode `TileMap.class` tại vị trí `bipush 28` -> `bipush 29` (Hàng 5, Cột 6 - ô cuối cùng chuẩn 100% của Cổ Lệnh Lượng).
- **Kết quả:** Đã biên dịch, patch và đóng gói lại thành công vào [`Aeharuna.jar`](file:///root/ninja/Aeharuna.jar). Nút tích "Mua Cổ Lệnh" trong Menu Auto hiện đã mua chính xác Cổ Lệnh tại shop Goshu.

## 2026-08-13: Lọc Bỏ 7 Map ID Của Boss Map Ngoài (`AutoSanBoss.java` & `ThongTinBoss.java`)
- **Quyết định:** Theo yêu cầu người dùng, loại bỏ 7 Map ID (`34, 35, 52, 68, 21, 59, 46`) ra khỏi danh sách tự động săn Boss Map Ngoài và giao diện đếm ngược Lịch Boss.
- **Danh sách 12 Map ID còn lại:**
  - Lv45: `14, 15, 16` (đã xóa: `34, 35, 52, 68`)
  - Lv55: `44, 67, 70`
  - Lv65: `24, 41, 45` (đã xóa: `21, 59`)
  - Lv75: `18, 36, 54` (đã xóa: `46`)
- **Kết quả:** Đã cập nhật `AutoSanBoss.java`, `ThongTinBoss.java`, biên dịch pass 100%, patch J2ME compatibility và đẩy bản build mới ra `/sdcard/Download/Aeharuna_v2.jar` (vừa tạo lúc 12:28).

## 2026-08-13: Thêm Tính Năng Tự Động Săn Boss Làng Cổ (`M135-136`) & Xử Lý Thoát Làng Cổ
- **Quy trình Leader & Member:**
  1. Khung giờ spawn: `7:00`, `10:00`, `15:00`, `23:00` (Map ID `135` & `136`, mỗi map giới hạn **chỉ 3 khu: K0, K1, K2**).
  2. Leader & Member tự động bật `Char.MuaCoLenh = true` và `Char.DungCoLenh = true` để mua & dùng Cổ Lệnh di chuyển vào Làng Cổ.
  3. Leader di chuyển tới map 135 -> tìm thấy Boss -> phát tín hiệu `pkm 135` + `pkk <zone>` cho nhóm. Đánh xong 135 -> Leader chạy tiếp sang map 136 -> phát `pkm 136` + `pkk <zone>`.
  4. **Quy trình kết thúc / Thoát Làng Cổ (`finishLangCoAndExit` / `pkm -6`)**:
     - Leader phát tín hiệu `pkm -6` cho nhóm khi kết thúc Làng Cổ.
     - Cả Leader và Member tự động gọi `cleanKhaoDiLenh()`: Quét và bán/vứt sạch vật phẩm ID 35 & 37 **ở cả Hành Trang (`arrItemBag`) VÀ Tủ Đồ/Rương (`arrItemBox`)**.
     - Gọi `Code.gameAN()` (tự sát về làng). Có thêm cơ chế **Double Check**: Nếu sau khi tự sát mà nhân vật vẫn còn ở Làng Cổ (map 135, 136, 138), hệ thống sẽ quét sạch Tủ đồ & Hành trang 1 lần nữa và tự sát lại 100% thoát kẹt.
     - Việc xóa sạch item ID 35 từ cả Tủ đồ lẫn Hành trang giúp nhân vật thoát hoàn toàn khỏi Làng Cổ, sẵn sàng quay về farm hoặc đi săn Boss Map Ngoài tiếp theo.
- **Files:** `src/AutoSanBoss.java`, `src/AutoBossEvent.java`, `src/ChatRouter.java`, `src/ThongTinBoss.java`, `src/NamMod.java`, `Aeharuna.jar`

## 2026-08-13: Fix Lỗi Chủ Nhóm & Thành Viên Ở Sẵn Trong Làng Cổ Khi Chuyển Sang Boss Khác
- **Vấn đề:** Khi chủ nhóm hoặc thành viên ở sẵn trong Làng Cổ (`M135`, `M136`, `M138` hoặc `isLangCo`) mà phiên săn boss chuyển sang các loại boss ngoài Làng Cổ (MapNgoài, Server, VDMQ, Boss Chúa) hoặc chuyển map `mapID != 135/136`:
  - Trước đây, hệ thống chưa tự động gọi `finishLangCoAndExit()` cho Trưởng nhóm trước khi PK/quét map ngoài.
  - Thành viên nhóm khi nhận lệnh `pkm` sang map ngoài cũng chưa tự động phát hiện đang ở Làng Cổ để xóa Cổ Lệnh (ID 35 & 37) và tự sát ra làng.
  - Hậu quả: Cả Trưởng nhóm và Thành viên còn giữ item 35 trong Hành trang / Tủ đồ nên game liên tục giữ lại / vào lại Làng Cổ không thể ra map ngoài được.
- **Giải pháp:**
  1. **Cho Trưởng nhóm (`AutoSanBoss.java`)**: Kiểm tra tại `huntBossType()`, `pkBossOnMap()`, `treoScanMap()`. Nếu `mapID` mục tiêu không phải Làng Cổ (khác 135 & 136) mà nhân vật đang đứng trong Làng Cổ -> Tự động gọi `finishLangCoAndExit()` (gửi `pkm -6` cho nhóm, tắt cờ mua/dùng Cổ Lệnh, xóa sạch item 35/37 ở cả Hành trang & Tủ đồ, rồi `Code.gameAN()` tự sát ra làng).
  2. **Cho Thành viên nhóm (`ChatRouter.java`)**: Trong `startPartyBoss()`, khi nhận tín hiệu `pkm -6`, `pkm -1`, `pkm -2` hoặc `pkm <mapID>` sang map ngoài (khác 135 & 136), nếu Thành viên đang đứng trong Làng Cổ -> Tự động tắt cờ Cổ Lệnh, xóa sạch item 35/37 ở cả Hành trang & Tủ đồ và `Code.gameAN()` tự sát thoát khỏi Làng Cổ ngay lập tức.
- **Files:** `src/AutoSanBoss.java`, `src/ChatRouter.java`, `Aeharuna.jar`

## 2026-08-13: Hoàn Thiện Tự Động Săn Boss Làng Cổ — Cổ Lệnh ID 490/35/37 & Tối Ưu Tốc Độ Chuyển Map
- **Phát hiện & Khắc phục:**
  1. **Item Template ID Cổ Lệnh (`490`) & Khả Dị Lệnh (`35`, `37`)**: Bytecode `TileMap.class` cho thấy vé Cổ Lệnh Lượng có template ID là `490`. Cập nhật `getCoLenhInBag()` kiểm tra và hỗ trợ cả 3 ID (`490`, `35`, `37`).
  2. **Cơ chế Đổi Khu Làng Cổ (`changeZoneLangCo`)**: Làng Cổ không có NPC 13. Đổi khu bắt buộc truyền vị trí vé `item.indexUI` qua gói tin `Service.gI().gameAA(zoneID, item.indexUI)`. Đã ngăn chặn việc gọi `useItem` ngầm khi đã ở trong Làng Cổ (tránh tiêu hao vé và lặp vô tận mua/dùng vé).
  3. **Giữ Vật Phẩm Tái Sử Dụng**: Khôi phục cờ `Char.MuaCoLenh` và giữ nguyên vé Cổ Lệnh/Khả Dị Lệnh khi tự sát/thoát Làng Cổ để tái sử dụng dịch chuyển nhanh ra/vào Làng Cổ.
  4. **Tối Ưu Tốc Độ Chuyển Map Làng Cổ (Fix Đơ 20s)**: Thay thế luồng chờ `PkBoss` ngầm bằng `TileMap.GoMap(mapID)` trực tiếp giữa Map 138 và Map 135/136 với timeout 200ms. Giảm delay quét khu xuống 50ms, giúp chuyển map và quét 3 khu K0, K1, K2 mượt mà tức thì.
- **Files:** `src/AutoSanBoss.java`, `src/Code.java`, `src/ChatRouter.java`, `Aeharuna.jar`


## 2026-08-17: Tái Cấu Trúc Menu NamMod — Gọn Gàng & Sub-Menu
- **Quyết định:** Dọn dẹp menu NamMod: xoá Treo Boss, Radar Boss, Giữ VP, Thông tin. Chuyển Săn Boss và TS Boss thành sub-menu. Tách lẻ ẩn khỏi menu (chỉ dùng ở Hành trang). Lịch Boss giữ ở ngoài.
- **Lý do:** Menu quá chật, nhiều tính năng ít dùng. Sub-menu giúp gom nhóm các tùy chọn liên quan.
- **Files:** `src/NamMod.java`

## 2026-08-17: TS Boss Ưu Tiên — 3 Chế Độ Lọc Boss Theo Sự Kiện
- **Quyết định:** Thêm `eventPriority` (0=Mặc định, 1=Chỉ VDMQ+Làng Cổ, 2=Chỉ Map Ngoài) vào AutoBossEvent. Chọn loại nào bật luôn, ấn lại tắt.
- **Lý do:** User muốn chỉ săn 1 loại boss cụ thể khi đến giờ sự kiện.
- **Files:** `src/AutoBossEvent.java`, `src/AutoSanBoss.java`, `src/NamMod.java`

## 2026-08-17: Patch Vị Trí "HS lượng" / "Lọc Đồ" Xuống Thấp Hơn
- **Quyết định:** Tạo `scripts/patch_hsluong_pos.py` patch bytecode GameScr.class đẩy text HUD xuống 30 pixel.
- **Lý do:** Vị trí gốc quá cao che mất đồ vật/UI khác trên màn hình game.
- **Files:** `scripts/patch_hsluong_pos.py`

## 2026-08-20: Fix Lỗi Hồi Sinh Khi Săn Boss PKB (`tspkb`) — respawnFast() Sai Packet

### Yêu cầu của user
> "Khi vào chế độ săn boss pkb boss thì lúc bị boss đánh chết tôi thấy nó bị lỗi hồi sinh, tôi ấn về nhà thay thì nó bảo đánh boss xong quét lại 1 lượt. Xem chế hồi sinh của lệnh ak, lệnh đó khi chết hồi sinh rồi chạy lại vị trí cũ đánh rất tốt."

### Phân tích
- So sánh lệnh `ak` (game gốc, hồi sinh tốt) vs `tspkb` (mod, hồi sinh lỗi).
- Lệnh `ak` dùng `Auto.gameAA(boolean)` trong `Auto.java` dòng 195-236: gọi `Service.gI().gameAK()` (hồi sinh về làng) hoặc `Service.gI().gameAL()` (hồi sinh lượng tại chỗ) — packet chuẩn game gốc.
- Lệnh `tspkb` dùng `respawnFast()` trong `AutoSanBoss.java`: gọi `GameScr.gameAB(5,0,0)` (mở dialog UI) + `Service.gI().gameAF()` (sai packet) — KHÔNG PHẢI packet hồi sinh.

### Root Cause
1. `GameScr.gameAB(5,0,0)` chỉ MỞ DIALOG hồi sinh (nút Về Nhà / Hồi Sinh), KHÔNG gửi packet hồi sinh tự động.
2. `Service.gI().gameAF()` KHÔNG phải packet hồi sinh chuẩn → hồi sinh không thành công.
3. User phải ấn "Về nhà" thủ công → bị teleport về làng → PkBoss tự thoát (sai map) → while loop `instanceof PkBoss` thoát → hiện "quét lại 1 lượt".

### Quyết định & Fix
- Sửa `respawnFast()` và `respawnIfDead()` đổi sang dùng `Service.gI().gameAK()` / `Service.gI().gameAL()` (packet chuẩn game gốc).
- Ưu tiên `gameAL()` (hồi sinh lượng tại chỗ) khi `Code.HoiSinhLuong && luong > 0` → không cần navigate lại.
- Clear `Auto.gameAN` + `Auto.gameAM` trước hồi sinh (giống game gốc).
- Thêm delay ổn định 500ms sau hồi sinh + 1000ms nếu bị teleport về nhà.
- Reset `sentPartyCmd` khi bị teleport khác map → re-gửi party commands khi quay lại boss.
- Thêm thông báo "TSB: Chet! Hoi sinh..." và "TSB: Quay lai M{mapID}" cho user biết trạng thái.

### Trạng thái: ✅ Code sửa xong, compile PASS — CẦN TEST trên game
- **Files:** `src/AutoSanBoss.java`
- **Chưa build JAR** — cần chạy full build flow theo AGENTS.md trước khi test.

## 2026-08-24: Tối Ưu Quét Boss Làng Cổ — Cơ Chế Quét Cơ Hội (Opportunistic Scan)
- **Quyết định:** Viết lại `pkLangCoMap()` và `treoScanMap()` theo cơ chế quét cơ hội: Khi đi qua cổng ngẫu nhiên từ Map 138, nếu rơi vào map boss nào (Map 135 hoặc Map 136) thì lập tức quét 3 khu của map đó luôn và đánh dấu đã quét, sau đó mới tìm map còn lại.
- **Lý do:** Trước đây bot cố định tìm tuần tự Map 135 trước rồi mới đến Map 136, khiến mỗi lần vào nhầm Map 136 phải quay lại Map 138 retry cổng, gây chậm trễ. Cơ chế cơ hội giúp tiết kiệm tối đa thời gian.
- **Files:** `src/AutoSanBoss.java`

## 2026-08-24: Tính Năng Pre-spawn 30s Trước Giờ Boss (TS Boss Ưu Tiên)
- **Quyết định:** Thêm cơ chế đếm ngược trước khi boss spawn trong `AutoBossEvent.run()`:
  - Khi thời gian còn `<= 30` giây trước giờ boss spawn, tự động lưu vị trí tàn sát, gọi nhóm và di chuyển sẵn ra map boss đứng chờ.
  - Khi boss vừa xuất hiện đúng giờ, bot lập tức quét khu và tấn công ngay, không sợ bị người khác húp trước.
  - Thiết lập `lastWindowKey` tính trước cho khung giờ sắp tới để tránh bị lặp (double-trigger).
- **Files:** `src/AutoBossEvent.java`, `src/AutoSanBoss.java`

## 2026-08-24: Gỡ Bỏ Tính Năng "Auto Boss Qua Chat" & Tạo Stub An Toàn J2ME
- **Quyết định:** Gỡ bỏ hoàn toàn tính năng Auto Boss qua Chat khỏi UI, menu cấu hình `BossConfig`, và `ChatRouter`. Tạo class stub [`src/AutoBossNotice.java`](file:///root/ninja/src/AutoBossNotice.java) rỗng và đăng ký vào `patch_class_j2me.py` để tránh lỗi `ClassNotFoundException` / `UnsupportedClassVersionError` từ `ChatManager.class` gốc.
- **Files:** `src/AutoBossNotice.java`, `src/BossConfig.java`, `src/ChatRouter.java`, `scripts/patch_class_j2me.py`

## 2026-08-26: Refactor Logic Săn Boss — TS Boss Ưu Tiên

### Quy tắc đúng (spec từ user):
1. **Chế độ "Tất cả"** = khi giờ boss **nào** ra thì săn **loại boss đó**, KHÔNG phải săn tất cả loại
2. **Boss trùng giờ** → săn con **ưu tiên cao nhất** trước: VIP2 > VIP > LC > VDMQ > TG > MN
3. **Đang đứng ở map boss** → **quét map đó trước**, không tele đi chỗ khác
4. **Map VIP** chỉ bật 1 map (M195 hoặc M196, user tắt 1 cái ở cài đặt)
5. **Đánh xong boss → next map boss khác ngay** → quét hết tất cả boss đang ra = 1 lượt
6. **Không quét lại khu** sau khi đánh xong — tránh boss bị cướp
7. **Map bị tắt** trong cài đặt → skip cả khi chờ lẫn khi săn
8. **Chế độ Lẻ/VDMQ/VIP/etc.** → chỉ chờ giờ boss loại đó và chỉ săn đúng loại đó

### Bugs đã fix (5 bugs):

#### Bug 1: `exitGatedMapIfNeeded()` tự sát ra VDMQ khi pre-spawn
- **File:** `AutoBossEvent.java` dòng 212
- **Cũ:** `d >= 0 && d < 2400` → lúc 11:59:30, d = -30 → vipActive = false → tự sát
- **Fix:** `d >= -PRE_SPAWN_SECONDS && d < 2400`

#### Bug 2: `eventHuntTypes` race condition
- **File:** `AutoSanBoss.java` `startEventHuntAll()`
- **Cũ:** `stop()` reset `eventHuntTypes = null` → thread mới đọc HUNT_PRIORITY thay vì type cụ thể
- **Fix:** Đã refactor bỏ pre-set eventHuntTypes, không cần nữa

#### Bug 3: `preSpawnType` chọn SAI boss
- **File:** `AutoBossEvent.java` dòng 793-830
- **Cũ:** Dùng `getSecondsTillNextBoss()` (diff > 0) → boss vừa spawn (diff=0) bị skip → chọn boss khác
- **Fix:** Xóa hoàn toàn preSpawnType. Lượt 1 quét TẤT CẢ boss active theo HUNT_PRIORITY

#### Bug 4: Đang ở M195 mà tele sang M196
- **File:** `AutoBossEvent.java` pre-spawn map choice
- **Cũ:** `getFirstMapForPriority()` chọn VIP2 (ưu tiên cao hơn) → rời M195
- **Fix:** Nếu đã ở M195/M196 → ở lại map đó

#### Bug 5: Lượt 1 chỉ săn 1 loại boss
- **File:** `AutoBossEvent.java` beginLeaderEvent
- **Cũ:** `eventHuntTypes = {preSpawnType}` → lượt 1 chỉ 1 type → chậm → boss bị cướp
- **Fix:** Xóa restriction. Lượt 1 quét TẤT CẢ boss active. `ignoreBossHourCheck` không set true (trừ triggerImmediate)

#### Bug 6: standingType quét boss ngoài scope
- **File:** `AutoSanBoss.java` event hunt loop
- **Cũ:** Nếu priority=Lẻ mà đang ở M195, standingType=VIP → quét VIP dù không trong eventHuntTypes
- **Fix:** standingType chỉ áp dụng khi `eventHuntTypes == null` (mode ALL)

### Flow đúng sau fix (ví dụ 12h, priority=ALL, đang ở M195, tắt M196):
```
11:59:30  Pre-spawn: ở lại M195 chờ (không tele đi)
12:00:00  Boss spawn! ignoreBossHourCheck = false
          → startEventHuntAll() → eventHuntTypes = null → HUNT_PRIORITY
          
          Hunt loop (mode ALL, standingType = VIP):
          ① VIP (M195) — đang đứng → quét ngay → đánh → xong
          ② VIP2 (M196) — đã tắt → SKIP
          ③ LC — not active (12h) → SKIP
          ④ VDMQ — not active (12h) → SKIP
          ⑤ TG (M20) — active → tele → đánh → xong
          ⑥ MN — not active → SKIP
          = 1 LƯỢT XONG (2 boss)

          Gửi nhóm về → Lượt 2+ solo quét lại
```

### Giờ spawn tham chiếu:
```
VDMQ:     6h, 13h, 19h, 23h
MapNgoài: 1,3,5,7,9,11,13,15,17,19,21,23h (giờ lẻ)
Làng Cổ:  7h, 10h, 15h, 23h
Thế Giới: 12h, 21h
Map VIP:  6h, 12h, 20h, 23h
Map VIP2: 6h, 12h, 20h, 23h
Boss tồn tại: 40 phút (2400 giây)
```

- **Files sửa:** `AutoBossEvent.java`, `AutoSanBoss.java`

## 2026-08-28: Cấu hình tốc độ quét khu, phạm vi khu và Tự động Hồi sinh / Vào lại map khi chết trong Săn Boss
- **1. Cấu hình tốc độ quét khu & phạm vi khu:**
  - `tfZoneDelay` (10-5000ms, mặc định `10ms`).
  - `tfZoneRange` (ví dụ `0-29`, `1-29`, mặc định `0-29`).
  - Lưu và tải qua RMS: `boss_zone_delay` và `boss_zone_range`.
  - Ngoại lệ Làng Cổ (`M135`, `M136`): Luôn quét cả 3 khu `K0, K1, K2` do đặc thù map.
- **2. Tự động Hồi sinh & Quay lại Map khi chết (Death Check & Recovery):**
  - Bổ sung `isDead()`: kiểm tra `statusMe == 14` hoặc `cHP <= 0`.
  - Viết `navigateToMap(int mapID)` và `enterLangCoSpecificMap(int targetMap)`:
    - Nếu chết trên đường đi: tự động gọi `respawnFast()` và đi tiếp.
    - Nếu chết khi đang quét khu: tự động hồi sinh, quay lại map và tiếp tục quét khu bị gián đoạn.
    - Nếu chết khi đang đánh boss: tự động hồi sinh, vào lại map, chuyển về đúng `bossZone`, khôi phục PkBoss và tiếp tục đánh.
    - Hỗ trợ toàn bộ map: Map VIP 1 (195), Map VIP 2 (196), Làng Cổ (135/136) và Map thường (VDMQ, Map Ngoài, Thế Giới...).
  - Sửa lỗi copy-paste ở Làng Cổ (`scanLangCoZones` trước đó gọi nhầm `enterMapVIP2()`).
- **3. Bản Share (`Aeharuna_share.jar`):**
  - Tự động duy trì qua `build_share.py`: VIP Map delay 100ms, map thường dùng loop PkBoss nguyên bản, menu cài đặt rút gọn (chỉ VIP 1 & 2), không exploit menu, pre-spawn 0s.
- **Files:** `src/AutoSanBoss.java`, `src/BossConfig.java`, `build_share.py`, `Aeharuna.jar`, `Aeharuna_share.jar`

## 2026-08-28: Sửa triệt để lỗi xung đột quét khu giữa AutoSanBoss và PkBoss gốc
- **Nguyên nhân:** Trước đó `navigateToMap` gọi `Code.gameAA(new PkBoss(mapID))`. Khi nhân vật tới map, `PkBoss.gameAK()` ngầm chạy trên game thread và kích hoạt vòng quét khu đảo chiều (29->0) của PkBoss gốc cùng lúc với vòng quét khu đồng bộ (0->29) của AutoSanBoss, dẫn đến tranh chấp đổi khu và gửi lệnh nhóm xung đột.
- **Giải pháp:**
  1. Loại bỏ hoàn toàn việc khởi tạo `PkBoss(mapID)` khi di chuyển map trong `navigateToMap`. Thay bằng `TileMap.GoMap(mapID)` trực tiếp để điều hướng waypoint mà không kích hoạt bot quét khu của PkBoss.
  2. Giữ `Code.gameAB` là `dummyAuto` (`SanBossHolder`) trong suốt quá trình quét để chống auto khác can thiệp.
  3. Chỉ khởi tạo `PkBoss` khi đã tìm thấy boss và luôn gán `pk.zoneID = bossZone` (chỉ kích hoạt chế độ đánh boss `gameAM()`, không chạy quét khu).
## 2026-08-28: Thoát VDMQ tự động về làng (tự sát hồi sinh nhanh) khi chuyển sang Map Ngoài / Boss khác
- **Nguyên nhân:** VDMQ (M139-148) là khu vực kín (gated realm), `TileMap.GoMap` không thể tự tìm đường ra map thường nếu không thoát VDMQ về làng trước.
- **Giải pháp:**
  1. Thêm `isVDMQ(mapID)` và `finishVDMQAndExit()`: Khi quét xong VDMQ (hoặc đang ở trong M139-148) và cần di chuyển sang map không thuộc VDMQ (Map Ngoài, Thế Giới, Làng Cổ, VIP), bot tự động gọi `suicideAndEnsureAlive()` để chết và hồi sinh về làng trong ~0.3s (cách tối ưu & nhanh nhất trong game).
  2. Từ làng, `TileMap.GoMap(mapID)` ngay lập tức pathfind đến đúng map tiếp theo mà không bị kẹt.
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`

## 2026-08-28: Sửa lỗi quét khu map ngoài bị chậm, áp dụng chính xác tốc độ cài đặt (10ms/50ms...)
- **Nguyên nhân:**
  1. `Auto.gameAA(zone)` trong `Auto.java` có điều kiện kiểm tra nếu không có NPC 13 (Cột chuyển khu) và map không nằm trong danh sách hardcode (99, 103, 134-137) thì sẽ `return` ngay mà không gửi gói đổi khu `Service.gameAA(zone, itemIndex)`. Các map ngoài (14, 15, 16...) không có NPC 13 nên bị return vô hiệu.
  2. Vòng lặp quét khu trước đó có đoạn chờ `for (w < 5) sleep(50)` khi `TileMap.zoneID != zone`, dẫn đến bị timeout 250ms trên từng khu (250ms * 30 = 7.5s/map).
- **Giải pháp:**
  1. Viết phương thức `doChangeZone(int zoneID)` độc lập trong `AutoSanBoss`: tự động lấy Cổ Lệnh / Khảo Dị Lệnh trong túi (nếu có), nếu có NPC 13 thì đứng gần, và luôn gửi trực tiếp gói chuyển khu `Service.gI().gameAA(zoneID, itemIndex)` + `TileMap.gameAF()`.
  2. Bỏ toàn bộ vòng chờ `for (w < 5) sleep(50)` gây chậm, chuyển sang đổi khu trực tiếp và sleep đúng `zoneChangeDelayMs` (ví dụ 10ms, 50ms) theo cài đặt của người dùng.
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`

## 2026-08-28: Loại bỏ delay 1s-2s khi gặp boss, nhân vật lao vào tấn công tức thì (0ms)
- **Nguyên nhân:** Khi phát hiện boss trên map, code cũ thực hiện chuỗi lệnh `sleep` đồng bộ: `sleep(50)` double-check + gửi lệnh nhóm + `sleep(300)` + `sleep(1500)` trước khi khởi tạo `PkBoss` và bật tấn công. Tổng thời gian trễ lên tới ~1900ms.
- **Giải pháp:**
  1. Ngay khi `hasBossOnCurrentMap()` phát hiện boss, bật `PkBoss` (`pk.zoneID = bossZone`), gán `Code.gameAA(pk)` và `lockBossFocus()` **ngay lập tức trong 0ms** để nhân vật lao vào đánh boss tức thì.
  2. Tách toàn bộ việc gửi lệnh nhóm (`pkm`, `pkk`, `pke`) vào phương thức chạy nền bất đồng bộ `notifyPartyBossFound(mapID, bossZone)` trên một thread riêng, hoàn toàn không làm chậm tiến trình tấn công của Leader.
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`

## 2026-08-28: Thêm tính năng Ghost Attack Boss (Đánh xa xuyên map + ghim boss + lao vào dần)
- **Mục tiêu:** Khi quét trúng khu có boss, nhân vật lập tức ghim mục tiêu vào boss, xả skill đánh xa xuyên toàn màn hình (Ghost Attack) ngay tại frame 0ms và liên tục bước di chuyển áp sát boss (step move 60px/tick) cho đến khi vào tầm đánh cận chiến.
- **Giải pháp:**
  1. Thêm `doBossGhostAttack()` trong `AutoSanBoss`: tìm boss mob trong `vMob`, ghim `mobFocus`, chọn skill tấn công tốt nhất, gửi gói packet đánh trực tiếp tới boss không phụ thuộc khoảng cách (tầm mặc định 9999px = full map), kết hợp gửi chuỗi Fast Attack (nếu bật). Tự động dịch chuyển step-by-step 60px về phía boss để áp sát dần.
  2. Tích hợp `doBossGhostAttack()` vào điểm kích hoạt đầu tiên khi phát hiện boss (`0ms`) và trong suốt chu kỳ vòng lặp chiến đấu boss (`while (checkStillRunning())`).
  3. Thêm tùy chọn **Ghost Attack Boss (đánh xa)** và **Tầm Ghost Boss (px)** vào form **Cài đặt Săn Boss** (`BossConfig.java`), lưu/tải cấu hình qua RMS (`boss_ghost_atk`, `boss_ghost_range`). Mặc định BẬT (ON).
- **Files:** `src/AutoSanBoss.java`, `src/BossConfig.java`, `Aeharuna.jar`

## 2026-08-28: Khắc phục lỗi Làng Cổ vào map boss nhưng chậm/kẹt quét khu
- **Nguyên nhân:**
  1. `doChangeZone(zoneID)` trước đó truyền `item.indexUI` thay vì slot index trong mảng hành trang `arrItemBag` (`Char.gameAI(490)`). Gói tin đổi khu bị server từ chối khiến bot không chuyển được khu trong Làng Cổ, sau đó tưởng không có boss và quay về M138 rồi nhảy cổng lặp đi lặp lại.
  2. Đoạn code sau khi qua cổng Làng Cổ có vòng chờ `sleep(800)` và `sleep(300)` gây chậm khi chuyển tiếp map.
- **Giải pháp:**
  1. Tạo hàm `getCoLenhBagIndex()` sử dụng `Char.gameAI(490/37/35)` để lấy đúng vị trí slot index `i` (0..29) trong `arrItemBag` truyền vào `Service.gI().gameAA(zoneID, bagIndex)`.
  2. Tối ưu toàn bộ các vòng chờ qua cổng Làng Cổ (`sleep(50)` polling thay vì 100ms/800ms). Ngay khi nhân vật qua cổng vào M135/M136, bot lập tức quét khu với tốc độ cấu hình (10ms).
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`

## 2026-08-28: Khắc phục hiện tượng đứng đơ 3s sau khi vào map Làng Cổ 135/136
- **Nguyên nhân:**
  1. Trong `scanLangCoZones`, vòng lặp quét 3 khu K0..K2 diễn ra quá nhanh (chỉ 30ms nếu delay 10ms) mà không đợi Server phản hồi gói tin chuyển khu, khiến nhân vật chưa kịp nhận mob từ server thì đã quét xong 3 khu và không tìm thấy boss.
  2. Khi không tìm thấy boss, hàm `returnToLangCoHub()` được gọi ngay lập tức, sử dụng `TileMap.gameAJ(0)` khiến nhân vật đi bộ từ vị trí đứng đến cổng dịch chuyển mất ~3 giây. Người dùng nhìn thấy thông báo "Vào M135 quét boss..." rồi nhân vật đứng/đi bộ 3s tưởng bị đơ trước khi quét.
- **Giải pháp:**
  1. Trong `scanLangCoZones` và `scanLangCoZonesForTreo`: Kiểm tra `TileMap.zoneID != zone`, gửi `doChangeZone(zone)` và đợi `TileMap.zoneID == zone` (tối đa 200ms với chu kỳ kiểm tra 20ms) để đảm bảo mob khu đó tải xong trước khi kiểm tra boss.
  2. Trong `returnToLangCoHub()`: Tự động dịch chuyển tức thời (`Char.gameAE(wp.minX, wp.minY)`) đến ngay sát Waypoint 0 trước khi gửi gói qua cổng, loại bỏ hoàn toàn 3 giây đi bộ.
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`

## 2026-08-28: Quy tắc cập nhật GitHub
- **Quy tắc:** Người dùng yêu cầu DỪNG cập nhật/push lên GitHub cho đến khi có lệnh yêu cầu rõ ràng từ người dùng.

## 2026-08-28: Sửa lỗi chết/tự sát khi đang đánh boss làm bot tưởng boss chết và bỏ qua map + Khắc phục kẹt quét khu
- **Vấn đề 1: Đang đánh boss mà chết/tự sát thì bot bỏ qua map**
  - **Nguyên nhân:** Trong vòng lặp đánh boss (`while (checkStillRunning())`), code cũ kiểm tra `if (TileMap.mapID == mapID && !hasBossOnCurrentMap()) break;` TRƯỚC TIÊN. Khi nhân vật chết, game client tự động dọn sạch danh sách quái (`vMob` rỗng), dẫn đến `!hasBossOnCurrentMap()` luôn trả về `true`. Bot lập tức `break` vòng lặp, tuyên bố boss đã chết và bỏ qua map đó mà không hồi sinh quay lại đánh tiếp.
  - **Giải pháp:** Đảo thứ tự ưu tiên: luôn kiểm tra `if (isDead() || TileMap.mapID != mapID)` lên đầu tiên để hồi sinh -> quay lại map -> vào đúng khu của boss -> đợi server nạp mob (1s) -> tiếp tục đánh. Chỉ xác nhận boss chết khi nhân vật còn sống (`!isDead()`) và đang ở đúng map + đúng khu kèm double-check 300ms. Đã áp dụng trên toàn bộ 8 vòng lặp đánh boss (Mode Thường & Treo, Map Thường, VIP 1, VIP 2, Làng Cổ).
- **Vấn đề 2: Kẹt khi quét khu**
  - **Nguyên nhân:** Khi quét khu, nếu có Cổ Lệnh mà code vẫn kiểm tra NPC 13 rồi gọi `Char.gameAC(npc13.cx, npc13.cy)` sẽ khiến nhân vật cố đi bộ tìm cột và bị kẹt địa hình. Ngoài ra, nếu có popup dialog/menu từ server mở lên sẽ chặn việc gửi gói tin đổi khu.
  - **Giải pháp:** Trong `doChangeZone`: tự động đóng mọi dialog/popup (`GameCanvas.endDlg()`), nếu có Cổ Lệnh thì đổi khu trực tiếp từ xa ở bất kỳ đâu trên map mà không bao giờ tìm đường đến NPC 13.
- **Files:** `src/AutoSanBoss.java`, `Aeharuna.jar`










