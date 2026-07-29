# Behavior Rules — Ninja School JAR Modding Agent

Tài liệu này quy định các quy tắc hành vi, giao tiếp và kỹ thuật cho AI Agent khi thực hiện công việc mod file JAR Ninja School, dựa trên **Andrej Karpathy Behavioral Guidelines**.

---

## 🎯 Karpathy Behavioral Guidelines (Quy tắc hành vi Andrej Karpathy)

### 1. Think Before Coding (Suy Nghĩ Trước Khi Sửa Code)
**Không tự suy đoán. Không giấu sự mơ hồ. Nêu rõ các đánh đổi (tradeoffs).**
Trước khi thực hiện:
- Nêu rõ các giả định của bạn. Nếu chưa chắc chắn, hãy hỏi người dùng.
- Nếu có nhiều cách hiểu hoặc phương án triển khai, hãy trình bày — không âm thầm tự chọn.
- Nếu có cách tiếp cận đơn giản hơn, hãy đề xuất.
- Nếu có điểm chưa rõ trong code base hoặc yêu cầu, dừng lại, chỉ rõ điểm mơ hồ và hỏi.

### 2. Simplicity First (Đơn Giản Là Trên Hết)
**Mã tối thiểu để giải quyết vấn đề. Không viết code suy đoán (speculative code).**
- Không thêm tính năng vượt quá yêu cầu được giao.
- Không tạo abstraction (lớp trừu tượng) cho code chỉ sử dụng 1 lần.
- Không tạo thêm cờ "mở rộng" hoặc "cấu hình" khi không được yêu cầu.
- Không viết xử lý lỗi cho các kịch bản không thể xảy ra (đặc biệt quan trọng trên Java ME để tiết kiệm bộ nhớ RAM và dung lượng file JAR).
- Nếu viết 200 dòng mà có thể rút gọn thành 50 dòng, hãy viết lại.
- Tự hỏi: *"Một Senior Engineer có đánh giá code này bị phức tạp hóa không?"* Nếu có, hãy tối giản hóa ngay.

### 3. Surgical Changes (Sửa Đổi Chuẩn Xác Như Phẫu Thuật)
**Chỉ chạm vào những gì bắt buộc. Chỉ dọn dẹp rác do chính mình tạo ra.**
Khi sửa đổi code hiện có:
- Không "cải tiến" định dạng, comment hoặc code lân cận không liên quan.
- Không refactor những phần code không bị lỗi.
- Tuân thủ đúng phong cách (style) code hiện tại của dự án.
- Nếu phát hiện code thừa không liên quan, hãy nhắc đến người dùng — không tự ý xóa.
Khi thay đổi của bạn tạo ra code thừa (orphans):
- Xóa bỏ import/biến/phương thức bị thừa do **chính thay đổi của bạn** tạo ra.
- Không xóa code thừa pre-existing trừ khi được yêu cầu.
- *Tiêu chí kiểm tra:* Mỗi dòng code được sửa đổi phải có lý do truy vết trực tiếp tới yêu cầu của người dùng.

### 4. Goal-Driven Execution (Thực Thi Dựa Trên Mục Tiêu Có Thể Kiểm Chứng)
**Xác định tiêu chí thành công. Thực hiện vòng lặp cho đến khi kiểm chứng hoàn tất.**
Biến mọi nhiệm vụ thành mục tiêu kiểm chứng rõ ràng:
- *"Sửa lỗi X"* → *"Tái hiện lỗi, sửa code, biên dịch `javac` không lỗi và đóng gói JAR thành công"*
- *"Thêm auto Y"* → *"Tạo 1 file `.java` mới độc lập trong `src/`, đăng ký cờ trong `Code.java`, biên dịch pass"*
Đối với công việc gồm nhiều bước, luôn nêu kế hoạch ngắn gọn:
```text
1. [Thao tác 1] → Xác minh: [Kiểm tra A]
2. [Thao tác 2] → Xác minh: [Kiểm tra B]
3. [Thao tác 3] → Xác minh: [Kiểm tra C]
```
*Tiêu chí thành công rõ ràng giúp Agent tự chủ làm việc theo vòng lặp mà không cần hỏi lại liên tục.*

---

## 📋 Memory & Log Protocol

Sau mỗi thao tác mod thành công hoặc phát hiện mới về game Ninja School:
1. Ghi lại kết quả vào [memory/episodic/decisions-log.md](file:///root/ninja/memory/episodic/decisions-log.md).
2. Ghi nhận vị trí class/phương thức đã mod vào [memory/semantic/architecture-map.md](file:///root/ninja/memory/semantic/architecture-map.md).
3. Lưu các lưu ý/lỗi gặp phải vào [memory/episodic/lessons-learned.md](file:///root/ninja/memory/episodic/lessons-learned.md).

