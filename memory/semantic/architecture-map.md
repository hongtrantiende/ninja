# Architecture Map — Ninja School Modding

## Custom Mod Classes & Patched Bytecode
- `MultiSkillAttack`: Tối ưu hóa chuẩn giao thức gói tin Server Ninja School (`Message 41` chọn skill -> `Message 4/60` gửi mảng byte quái lan trong tầm `dx, dy` -> `sleep 40ms`). Đảm bảo sát thương thực 100% cho 5 skill lan chạy ngầm và giữ tĩnh ô chọn skill UI.
- `Auto`: Đã patch byte-code gọi `MultiSkillAttack.attackMultiSkill` trong luồng đánh quái Tàn Sát (`ts` & `tsn`).
- `Skill`: Đã mod `coolDown = 10`ms và `gameAA()` kiểm tra hồi chiêu 10ms.
- `Controller`: Bytecode patch tại opcode đọc packet skillinfo (`bipush 10` ép thời gian hồi chiêu từ server thành 10ms).
- `AutoGaoDa`: Triển khai `Runnable`, quản lý vòng lặp Auto Gạo Đá (Map 23, NPC 62, Map 26, NPC 63).
- `AutoNhanDa`: Triển khai `Runnable`, quản lý Auto Nhận Đá (Map 23, NPC 33).
- `AutoDoiDiem`: Triển khai `Runnable`, quản lý Auto Đổi Điểm (Tọa độ XY: 3356-240, tương tác NPC 63 tự động ấn đổi quà).
- `ThongTinBoss`: Chứa logic tính toán đếm ngược thời gian xuất hiện của 5 loại Boss (Server, Thế Giới, Làng Cổ, VDMQ, Map Ngoài), vẽ khung UI HUD Overlay bằng Paint.gameAA() native panel, hiển thị bên phải màn hình game.
- `AutoSanBoss`: Triển khai `Runnable`, tự động săn boss 24/7. Theo dõi khung giờ spawn 4 loại boss (Server/TheGioi/VDMQ/MapNgoai), tự chuyển map, quét 30 khu tìm boss, kích hoạt PkBoss đánh, xử lý chết/hồi sinh. Lệnh `tspkb`.
- `InfoMe`: Đã hook `ThongTinBoss.paint(g)` vào phương thức vẽ `gameAA(mGraphics)` để hiển thị bảng HUD liên tục theo từng frame.
- `Code`: Class chính chứa handler tin nhắn chat (`gameAF(String)` / `gameAA(String)`). Lệnh `ttb` Bật/Tắt bảng lịch Boss, lệnh `tspkb` Bật/Tắt tự động săn boss.
