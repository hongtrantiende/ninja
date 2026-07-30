# Decisions Log

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
