# Lessons Learned

## Compilation Java ME with JDK 21
- **Vấn đề:** Khi biên dịch `Code.java` với `javac` Java 21, `javac` đòi hỏi các class Java ME (`javax.microedition.lcdui.*`, `javax.microedition.midlet.*`).
- **Giải pháp:** Tạo temporary stubs cho `javax.microedition.lcdui` & `midlet` để biên dịch `Code.java`, sau đó xóa các stubs khỏi `build/unpacked` trước khi đóng gói `Aeharuna.jar`.
