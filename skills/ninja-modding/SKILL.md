---
name: ninja-modding
description: Skill chuyên biệt để Mod Game Ninja School (Auto click, Hack speed, Auto buff, Mod Menu, Fix Lag).
---

# Skill: Ninja School Game Modding

Hướng dẫn các kỹ thuật mod đặc thù dành riêng cho dòng game **Ninja School** Java (J2ME).

## 🚀 Các Tính Năng Mod Phổ Biến

1. **Hack Speed (Tăng Tốc Độ Game):**
   - Tìm kiếm các hằng số delay trong vòng lặp chính của Canvas (`Thread.sleep(...)`).
   - Sửa tham số delay từ 50ms xuống 10ms hoặc thêm hệ số nhân speed.

2. **Auto Click (Tự Động Bấm Phím):**
   - Inject logic bắt phím gọi Menu Auto Click hoặc giả lập sự kiện `keyPressed(int keyCode)` / `keyReleased(int keyCode)`.

3. **Auto Buff / Auto Đậu / Auto Dược:**
   - Inject gọi hàm dùng HP/MP khi thanh chỉ số xuống dưới ngưỡng %.

4. **Mod Menu Tiện Ích:**
   - Tích hợp thêm các mục chọn vào Menu gốc của game (phím Gọi / Softkey).
