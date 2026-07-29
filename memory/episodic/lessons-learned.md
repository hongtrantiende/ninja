# Lessons Learned

## Compilation Java ME with JDK 21
- **Vấn đề:** Khi biên dịch `Code.java` với `javac` Java 21, `javac` đòi hỏi các class Java ME (`javax.microedition.lcdui.*`, `javax.microedition.midlet.*`). Các stub class này tự động được javac tạo ra trong `build/unpacked/javax`. Nếu đóng gói nguyên thư mục này vào file JAR, máy điện thoại/J2ME Loader sẽ báo lỗi cài đặt (Security Violation / Overriding system package).
- **Giải pháp:** Sau khi chạy `javac`, **BẮT BUỘC** phải chạy `rm -rf build/unpacked/javax` trước khi thực hiện đóng gói `pack_jar.py`.
