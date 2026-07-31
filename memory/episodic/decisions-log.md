# Decisions Log

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


