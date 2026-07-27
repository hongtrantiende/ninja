# Architecture Map — Ninja School Modding

## Custom Mod Classes
- `AutoGaoDa`: Triển khai `Runnable`, quản lý vòng lặp Auto Gạo Đá (Map 23, NPC 62, Map 26, NPC 63).
- `AutoNhanDa`: Triển khai `Runnable`, quản lý Auto Nhận Đá (Map 23, NPC 33).
- `Code`: Class chính chứa handler tin nhắn chat (`gameAF(String)` / `gameAA(String)`). Lệnh `gaoda` gọi `AutoGaoDa.isAuto`.
