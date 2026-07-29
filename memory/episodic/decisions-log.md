# Decisions Log

## 2026-07-29: Thêm lệnh chat `ttb` hiển thị Khung Overlay HUD Lịch Boss Trực Tiếp Trên Màn Hình
- **Quyết định:** Chuyển đổi hiển thị từ Popup Dialog sang **Khung UI HUD Overlay vẽ trực tiếp trên màn hình game** (góc trên bên trái):
  - Tự động tính toán thời gian đếm ngược còn lại đến lần xuất hiện tiếp theo của từng loại Boss.
  - Tự động **sắp xếp Boss đang xuất hiện hoặc chuẩn bị xuất hiện sớm nhất lên ĐẦU danh sách**.
  - Đổi màu sắc trực quan (Đỏ/Vàng cho Boss đang có hoặc < 5 phút, Xanh/Trắng cho Boss sắp tới).
  - Gõ `ttb` để **Bật/Tắt** khung hiển thị này trực tiếp khi đang treo game/đánh quái.
- **Thực thi:**
  1. Nâng cấp `src/ThongTinBoss.java` bổ sung thuật toán sắp xếp và phương thức `paint(mGraphics g)`.
  2. Thêm `src/InfoMe.java` để hook `ThongTinBoss.paint(g)` vào luồng vẽ game `GameScr`.
  3. Cập nhật handler `ttb` trong `src/Code.java` gọi `ThongTinBoss.toggle()`.
  4. Biên dịch, dọn dẹp stubs `javax` và nén lại file [Aeharuna.jar](file:///root/ninja/Aeharuna.jar).

## 2026-07-29: Mod Auto `gaoda` đứng tại chỗ Nhận & Giao đá từ xa (Remote NPC calls)
- **Quyết định:** Loại bỏ việc chuyển map/di chuyển nhân vật trong `AutoGaoDa.java`. Cho nhân vật đứng yên 1 chỗ gửi trực tiếp gói tin tương tác NPC 62 (Nhận đá) và NPC 63 (Giao đá) từ xa liên tục với delay 10ms. Tự động đóng popup dialog bằng `GameCanvas.endDlg()` và `InfoDlg.gameAB()`.
- **Thực thi:**
  1. Thêm `src/AutoDoiDiem.java`.
  2. Đăng ký lệnh chat `doidiem` trong `src/Code.java`.
  3. Biên dịch và đóng gói thành công `Aeharuna.jar`.

## 2026-07-28: Mod Tàn Sát đánh song song 5 skill chạy ngầm (`ts` & `tsn`)
- **Tối ưu hóa Giao thức Packet Server:**
  1. `Message 41` (`Service.gameAG(s.template.id)`): Gửi lệnh đổi skill ngắn hạn lên Server Ninja School.
  2. `Message 4/60` (`Service.gameAA(targetMobs, ...)`): Gửi mảng byte ID các quái sống nằm đúng phạm vi `dx, dy` và số lượng `maxFight` của từng chiêu.
  3. Thêm `Thread.sleep(40ms)` để gói tin truyền qua TCP/IP socket trơn tru, không bị Server thả trôi gói.
- **Chế độ chạy ngầm UI:** Lưu `originalSelectedSkill = myChar.myskill`, cho 4 skill còn lại xả sát thương thực tế và phát hiệu ứng kỹ năng chạy ngầm mà không làm nhảy ô chọn trên giao diện UI.
- **Kết quả:** Đảm bảo 100% sát thương thực sự gửi tới Server cho toàn bộ 5 skill lan chạy ngầm.

## 2026-07-28: Mod tốc độ hồi chiêu skill về 10ms cho tất cả phái và kỹ năng
- **Quyết định:** Sửa đổi thời gian hồi chiêu (`Skill.coolDown`) của mọi kỹ năng/phái về cố định 10ms (không phụ thuộc vào cấp độ hay phái).
- **Thực thi:**
  1. Cập nhật `src/Skill.java` với `coolDown = 10`ms và biên dịch lại `Skill.class`.
  2. Patch byte-code trong `Controller.class` tại offset đọc gói tin kỹ năng từ server: bỏ qua `coolDown` từ server stream và ghi đè cố định `bipush 10` (`Skill.coolDown = 10`).
  3. Đóng gói lại tệp `Aeharuna.jar` hoàn chỉnh.

## 2026-07-27: Mod lệnh chat `gaoda` cho Aeharuna.jar
- **Quyết định:** Tích hợp logic `AutoGaoDa` (Map 23 nhận đá -> NPC 62 -> Map 26 giao đá -> NPC 63) vào `Code.java` xử lý lệnh chat `gaoda`.
- **Thực thi:** Biên dịch `AutoGaoDa.java`, `AutoNhanDa.java`, `Code.java` và đóng gói lại `Aeharuna.jar`.
