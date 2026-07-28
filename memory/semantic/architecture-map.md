# Architecture Map — Ninja School Modding

## Custom Mod Classes & Patched Bytecode
- `Skill`: Đã mod `coolDown = 10`ms và `gameAA()` kiểm tra hồi chiêu 10ms.
- `Controller`: Bytecode patch tại opcode đọc packet skillinfo (`bipush 10` ép thời gian hồi chiêu từ server thành 10ms).
- `AutoGaoDa`: Triển khai `Runnable`, quản lý vòng lặp Auto Gạo Đá (Map 23, NPC 62, Map 26, NPC 63).
- `AutoNhanDa`: Triển khai `Runnable`, quản lý Auto Nhận Đá (Map 23, NPC 33).
- `Code`: Class chính chứa handler tin nhắn chat (`gameAF(String)` / `gameAA(String)`). Lệnh `gaoda` gọi `AutoGaoDa.isAuto`.

