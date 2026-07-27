# Decisions Log — Ninja School Modding

## 📌 Nhật Ký Quyết Định Kiến Trúc

- **[2026-07-26] Chọn Aeharuna.jar làm bản Mod mặc định:**
  - Quyết định lấy `Aeharuna.jar` làm bản JAR base tiêu chuẩn cho mọi thao tác nâng cấp.
- **[2026-07-26] Khắc phục lỗi MANIFEST & đồng bộ tự động:**
  - Chuyển `pack_jar.sh` sang dùng `jar cfm` chuẩn JDK để nén file JAR. Tự động đồng bộ tới `/storage/emulated/0/Download/Extransion-TTC/`.
- **[2026-07-26] Tích hợp lệnh chat `nhanda`:**
  - Di chuyển tới Map 23 -> Tọa độ (481, 168) -> Tương tác NPC nhận đá level 62 -> Giữ trong Hành trang (không cất kho).
- **[2026-07-26] Tích hợp lệnh chat `gaoda`:**
  - Thực hiện vòng lặp gạo đá: `gm23` -> `npc62` (nhận đá) -> `gm26` -> `npc63` (giao đá). Tất cả thao tác thực hiện trực tiếp trong Hành trang.

