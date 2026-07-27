# Lessons Learned — Ninja School Modding

## 📝 Nhật Ký Bài Học & Bugs

- **[2026-07-26] Khởi tạo dự án Aeharuna.jar:**
  - File JAR gốc `Aeharuna.jar` được thiết lập tại `/root/ninja/` làm target mặc định.
- **[2026-07-26] Khắc phục lỗi `JAR not have META-INF/MANIFEST.MF`:**
  - Khi nén lại file JAR Java ME, bắt buộc dùng `jar cfm` chuẩn JDK (hoặc đảm bảo `MANIFEST.MF` đứng ở đầu tệp zip).
  - Tự động bổ sung `MANIFEST.MF` chuẩn CLDC 1.1 / MIDP 2.1 khi giải nén trong `unpack_jar.sh`.
- **[2026-07-26] Quy trình Biên dịch Bytecode J2ME:**
  - Decompile bằng CFR (`java -jar /tmp/cfr.jar`).
  - Classpath biên dịch cần nạp các tệp API Java ME: `/tmp/midpapi20.jar` và `/tmp/cldcapi11.jar`.
  - Biên dịch bằng `javac --release 8 -cp /tmp/midpapi20.jar:/tmp/cldcapi11.jar:/root/ninja/build/unpacked -d /root/ninja/build/unpacked <file.java>`.

