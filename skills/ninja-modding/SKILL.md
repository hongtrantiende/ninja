---
name: ninja-modding
description: Skill chuyên biệt để Mod Game Ninja School (Auto click, Hack speed, Auto buff, Mod Menu, Auto Chat Command, Fix Lag).
---

# Skill: Ninja School Game Modding

Hướng dẫn các kỹ thuật mod đặc thù dành riêng cho dòng game **Ninja School** Java (J2ME).

## 🚀 Các Tính Năng Mod Phổ Biến

1. **Auto Chat Command (Lệnh Chat Auto Như `gaoda`, `nhanda`, `td`):**
   - Tạo class Auto kế thừa/triển khai `Runnable` (ví dụ: `AutoGaoDa.java`).
   - Khai báo cờ `public static boolean isAuto = false`.
   - Viết luồng công việc trong `public void run()` (chuyển map bằng `Code.gameAF("gmXX")`, tương tác NPC bằng `Service.gI().gameAH(npcId)` / `Service.gI().gameAC(...)`).
   - Đăng ký lệnh chat trong `Code.java` (nhánh xử lý chuỗi chat):
     ```java
     if (var31.equals("gaoda")) {
         AutoGaoDa.isAuto = !AutoGaoDa.isAuto;
         if (AutoGaoDa.isAuto) {
             GameScr.gameAC("Bật Auto Gạo Đá!");
             new Thread(new AutoGaoDa()).start();
         } else {
             GameScr.gameAC("Tắt Auto Gạo Đá!");
         }
         return true;
     }
     ```

2. **Hack Speed (Tăng Tốc Độ Game):**
   - Tìm kiếm các hằng số delay trong vòng lặp chính của Canvas (`Thread.sleep(...)`).
   - Sửa tham số delay từ 50ms xuống 10ms hoặc thêm hệ số nhân speed.

3. **Auto Click (Tự Động Bấm Phím):**
   - Inject logic bắt phím gọi Menu Auto Click hoặc giả lập sự kiện `keyPressed(int keyCode)` / `keyReleased(int keyCode)`.

4. **Auto Buff / Auto Đậu / Auto Dược:**
   - Inject gọi hàm dùng HP/MP khi thanh chỉ số xuống dưới ngưỡng %.

5. **Mod Menu Tiện Ích:**
   - Tích hợp thêm các mục chọn vào Menu gốc của game (phím Gọi / Softkey).

