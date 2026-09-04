# Rules & Technical Standards — JAR Modding (Ninja School)

Tài liệu quy định chi tiết tiêu chuẩn kỹ thuật khi làm việc với file `.jar` và game Ninja School Java ME.

---

## ⚙️ 1. Cấu Trúc File JAR Java ME

File `.jar` bản chất là một tệp ZIP chuẩn chứa:
- `META-INF/MANIFEST.MF`: File khai báo thông tin MIDlet (nhà phát hành, lớp chính `MIDlet-1`).
- `*.class`: Các file bytecode chứa logic game.
- `*.png`, `*.mid`, `*.png.bin` / data files: Tài nguyên đồ họa, âm thanh, dữ liệu bản đồ.

---

## 🛡️ 2. Tiêu Chuẩn Mod Bytecode & Class

1. **Khôi Phục / Sửa Đổi Class:**
   - Khi sửa chuỗi tiếng Việt hoặc thông số game, sử dụng UTF-8 mã hóa chuẩn hoặc Unicode escape (`\uXXXX`) để tránh lỗi font trên game Java.
2. **Quản Lý Bộ Nhớ (Heap Memory trên J2ME):**
   - Các thiết bị Java cũ có RAM rất hạn chế (vài MB). Tránh tạo đối tượng trùng lặp trong vòng lặp game (`paint` / `update`).
3. **Nén File JAR:**
   - Khi nén lại `.jar`, bắt buộc file `META-INF/MANIFEST.MF` phải nằm đúng vị trí đầu tiên hoặc không bị nén hỏng cấu trúc.
4. **Tạo File Mã Nguồn Java Mới Cho Mỗi Tính Năng Auto/Lệnh Chat (BẮT BUỘC):**
   - Mỗi khi mod một lệnh chat mới hoặc tính năng auto mới (ví dụ: `gaoda`, `nhanda`), **BẮT BUỘC** phải tạo 1 tệp mã nguồn `.java` riêng biệt trong `src/` (ví dụ: `src/AutoGaoDa.java`).
   - Class mới triển khai `Runnable`, chứa toàn bộ luồng xử lý chính, và chỉ đăng ký cờ/khởi chạy thread trong `Code.java` để giữ mã nguồn mô-đun hóa, sạch sẽ và dễ nâng cấp.


---

## 🔧 3. Bộ Công Cụ Hỗ Trợ Dự Án Ninja

- **Default JAR Target:** `NinjaNamod.jar`
- **Unpack script:** `./scripts/unpack_jar.sh`
- **Repack script:** `./scripts/pack_jar.sh`
- **String patcher:** `./scripts/patch_string.py`
