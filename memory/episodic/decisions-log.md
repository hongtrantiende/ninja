# Decisions Log

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
