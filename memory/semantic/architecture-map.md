# Architecture Map — Ninja School Modding

## Custom Mod Classes & Patched Bytecode
- `MultiSkillAttack`: Tối ưu hóa chuẩn giao thức gói tin Server Ninja School (`Message 41` chọn skill -> `Message 4/60` gửi mảng byte quái lan trong tầm `dx, dy` -> `sleep 40ms`). Đảm bảo sát thương thực 100% cho 5 skill lan chạy ngầm và giữ tĩnh ô chọn skill UI.
- `Auto`: Đã patch byte-code gọi `MultiSkillAttack.attackMultiSkill` trong luồng đánh quái Tàn Sát (`ts` & `tsn`).
- `Skill`: Đã mod `coolDown = 10`ms và `gameAA()` kiểm tra hồi chiêu 10ms.
- `Controller`: Bytecode patch tại opcode đọc packet skillinfo (`bipush 10` ép thời gian hồi chiêu từ server thành 10ms).
- `AutoGaoDa`: Triển khai `Runnable`, quản lý vòng lặp Auto Gạo Đá (Map 23, NPC 62, Map 26, NPC 63).
- `AutoNhanDa`: Triển khai `Runnable`, quản lý Auto Nhận Đá (Map 23, NPC 33).
- `Code`: Class chính chứa handler tin nhắn chat (`gameAF(String)` / `gameAA(String)`). Lệnh `gaoda` gọi `AutoGaoDa.isAuto`.
