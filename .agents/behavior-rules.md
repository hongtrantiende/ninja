# Behavior Rules — Ninja School JAR Modding Agent

Tài liệu này quy định các quy tắc hành vi, giao tiếp và kỹ thuật cho AI Agent khi thực hiện công việc mod file JAR Ninja School.

---

## 🎯 Karpathy Behavioral Guidelines

1. **Think Before Modding (Nghĩ Trước Khi Sửa):**
   - Phân tích kỹ byte-code / class trước khi can thiệp.
   - Luôn sao lưu file `.jar` gốc trước khi thực hiện thay đổi.

2. **Simplicity First (Đơn Giản Là Trên Hết):**
   - Thay đổi tối thiểu byte-code để đạt được mục tiêu mod.
   - Tránh thêm thư viện ngoài không cần thiết vào file JAR Java ME để giữ file gọn nhẹ.

3. **Surgical Changes (Sửa Đổi Tỉ Mỉ & Chính Xác):**
   - Chỉ sửa đổi các class/phương thức liên quan trực tiếp đến tính năng mod (Auto, Speed, Menu, UI).
   - Bảo toàn các giá trị checksum và cấu trúc file ZIP/JAR.

4. **Goal-Driven Verification (Xác Thực Có Mục Tiêu):**
   - Sử dụng `unzip -t` để kiểm tra độ toàn vẹn sau khi nén.
   - Kiểm tra `META-INF/MANIFEST.MF` để đảm bảo game khởi chạy thành công trên Java ME emulator.

---

## 📋 Memory & Log Protocol

Sau mỗi thao tác mod thành công hoặc phát hiện mới về game Ninja School:
1. Ghi lại kết quả vào `memory/episodic/decisions-log.md`.
2. Ghi nhận vị trí class/phương thức đã mod vào `memory/semantic/architecture-map.md`.
3. Lưu các lưu ý/lỗi gặp phải vào `memory/episodic/lessons-learned.md`.
