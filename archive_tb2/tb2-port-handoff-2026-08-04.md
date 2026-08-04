# Bàn giao chuyển Nam Mod sang TB2 — 2026-08-04

## Mục tiêu và nguyên tắc làm việc

- Bản đích: `Aeharuna_148TB2.jar` (TB2, tên lớp đã obfuscate).
- Chỉ chuyển từng tính năng sau khi đọc đúng code bản cũ và xác định đúng vị trí/UI/API.
- Không chuyển hàng loạt, không tự đổi hành vi hoặc đặt chức năng sai menu.
- Luôn đóng gói thành đúng một tên: `Aeharuna_148TB2_NamMod.jar`.
- Không đưa `build/compile_stub` hay thư viện Java ME vào JAR; chúng chỉ dùng lúc biên dịch.
- Không tự commit/push GitHub nếu người dùng chưa yêu cầu rõ.

## Trạng thái tính năng đã chuyển

1. Menu `Nam Mod` trong menu ba gạch.
2. Tách đồ lẻ nằm cạnh nút `Tách` khi chọn vật phẩm; dùng đúng chỉ số vật phẩm đang chọn (`Class_ds.ak`), không lấy món đầu giỏ.
3. Gạo đá.
4. Đổi điểm.
5. Hút VP, lệnh `nhat`.
6. Hiện TTB bằng lệnh `ttb` và menu Nam Mod; HUD đã sửa kích thước/nền/sọc đỏ.
7. Săn Boss và Treo Boss, gồm lệnh chat, các loại boss riêng và chế độ tất cả.
8. Giao thức nhóm cho săn/treo boss.
9. Mời nhóm từ danh sách bạn bè thật — **đã test OK, hoàn tất**.

## Mời nhóm — quyết định cuối cùng

### Yêu cầu chính xác

`Mời nhóm` phải mời các nhân vật trong **danh sách bạn bè của game**, tuyệt đối không quét người đang đứng quanh map.

### Ánh xạ đã kiểm chứng

- `GameScr.vFriend` bản cũ = `Class_ds.aa` ở TB2.
- Phần tử `Friend` = `Class_cv`; tên bạn bè = `Class_cv.a`.
- `GameScr.vFriendWait` = `Class_ds.ac`.
- Gửi mời vào nhóm `Service.gameAF(String)` bản cũ = `Class_di.a().f(name)` ở TB2; packet 79.
- Yêu cầu server tải/cập nhật danh sách bạn bè `Service.gameAU()` bản cũ = `Class_di.a().u()` ở TB2; subcommand `-85` không tham số.
- `Class_ds.ae` là danh sách nhân vật quanh map, không phải bạn bè; không được dùng cho Mời nhóm.
- `Class_am.h` là danh sách tên nhóm lưu riêng; bản sửa cuối không dùng, vì yêu cầu hiện tại là đúng danh sách bạn bè.

### Luồng hiện tại

1. Gọi `Class_di.a().u()` xin danh sách bạn bè từ server.
2. Chờ tối đa 30 lần × 100 ms để `Class_ds.aa` có dữ liệu.
3. Duyệt duy nhất `Class_ds.aa`.
4. Bỏ qua tên rỗng, tên chính mình và tên trùng.
5. Gọi `Class_di.a().f(friend.a)`, nghỉ 250 ms giữa mỗi lời mời.
6. Hiện: `TSB: Bạn bè X, đã mời Y!` để biết server trả bao nhiêu bạn và đã gửi bao nhiêu lời mời.
7. Khi bật Săn/Treo Boss, chỉ phát tín hiệu mode nhóm sau khi luồng mời kết thúc.

File thực hiện: `tb2_step1_src/TB2AutoSanBoss.java`.

## Ánh xạ săn/treo boss TB2 đang dùng

- `Class_am` = `Code`; auto hiện tại `Class_am.b`; đặt auto `Class_am.a(Class_af)`; dừng `Class_am.c()`.
- `Class_af` = `Auto`; map `b`, khu `c`; đổi khu thủ công `Class_af.a(zone)`.
- `Class_cc` = `PkBoss`.
- `Class_fq.o` = map ID; `Class_fq.l` = zone ID.
- `Class_ds.ag` = danh sách mob.
- `Class_fk`: HP `c`, trạng thái `h`, cờ boss `y`.
- `Class_dk.g()` = nhân vật của mình; `Class_dk.co` = mob focus; `Class_dk.bc` = tên nhân vật.
- `Class_ds.y` = danh sách party.
- `Class_di.a().k(command)` = gửi chat nhóm packet `-20`.

### Giao thức nhóm

- `pkm -1`: thành viên vào chế độ đánh boss.
- `pkm -2`: thành viên vào chế độ treo/đứng chờ.
- `pkm -3`: dừng và dọn trạng thái săn/treo hoàn toàn.
- `pkm <map>`: di chuyển tới map.
- `pkk <zone>`: chọn khu.
- `pke`: dừng `PkBoss`; ở chế độ treo vẫn giữ trạng thái chờ.

## Dữ liệu boss đang dùng

- Boss Server: giờ `12, 18, 20, 22`; map `63`.
- Boss Thế Giới: giờ `11, 17, 19, 21`; map `65`.
- Boss VDMQ: giờ `6, 13, 19, 23`; map `141, 142, 143`.
- Boss Map Ngoài: giờ `1, 4, 7, 10, 13, 16, 19, 22`; map `14, 15, 16, 44, 67, 70, 21, 41, 45, 18, 46, 54`.
- Đã bỏ hoàn toàn Boss Làng Cổ.

## Cấu trúc build TB2

- Source mod: `tb2_step1_src/`.
- Patcher: `tb2_step1_tools/PatchMenuStep1.java`.
- Lớp gốc: `build/tb2_step1_base/`.
- Stub chỉ để compile: `build/compile_stub/`.
- Lớp mod đã compile: `build/tb2_step1_classes/`.
- Lớp gốc đã patch: `build/tb2_step1_patched/`.
- Script đóng gói: `scripts/pack_tb2_step1.py`.

Do đường dẫn workspace có Unicode (`Máy tính`), `javac` có thể không đọc classpath trực tiếp. Dùng ổ đĩa tạm:

```powershell
subst N: "C:\Users\bac5a\OneDrive\Máy tính\ninja"
Set-Location N:\
javac --release 8 -encoding UTF-8 -cp "build\tb2_step1_base;build\compile_stub" -d build\tb2_step1_classes tb2_step1_src\*.java
& 'C:\Users\bac5a\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' scripts\pack_tb2_step1.py
```

## Quy tắc đóng gói JAR để AngelChip chạy được

- Phải bảo toàn metadata, thứ tự và cấu trúc của JAR TB2 gốc.
- Entry đầu phải là `agent.txt`.
- `META-INF/MANIFEST.MF` phải là entry cuối.
- Hiện tại JAR hợp lệ có 532 entries.
- `ZipFile.testzip()` phải trả `None`.
- Không được có entry `javax/`.
- Java class major version 52 (Java 8).
- Nếu emulator đang giữ file, `os.replace` có thể thất bại và để lại `.tmp`; đóng emulator rồi pack lại, không đổi tên bản JAR.

## Bản build cuối phiên

- File: `Aeharuna_148TB2_NamMod.jar`.
- Kích thước lúc build: `1,088,726` bytes.
- SHA-256: `E8C72A0A977E6EF5144E9F2DE70E81D9967C0E13827884D151AF8F1D9629D60A`.
- Kiểm tra: 532 entries, `agent.txt` đầu, MANIFEST cuối, ZIP không lỗi, không chứa `javax/`.
- Bytecode `TB2AutoSanBoss$1` đã xác nhận gọi `Class_di.u()` và chỉ đọc `Class_ds.aa`; không còn tham chiếu `Class_ds.ae`.

## Bài học cần giữ cho phiên sau

1. Không suy đoán vector obfuscate theo tên; phải đối chiếu bytecode/code bản cũ và call site bản TB2.
2. “Mời nhóm” báo 0 không đồng nghĩa packet mời sai: có thể `vFriend` chưa được tải. Phải gọi API refresh danh sách trước.
3. Không dùng nhân vật quanh map làm phương án dự phòng khi yêu cầu là danh sách bạn bè; điều đó thay đổi chức năng.
4. Khi port UI, phải xác định đúng menu gốc và chỉ số lựa chọn đang active trước khi sửa.
5. Không chèn bytecode thủ công vào method lớn nếu có thể dùng wrapper/methodref replacement; sai branch/StackMap có thể làm game văng im lặng.
6. Luôn kiểm tra JAR sau build, không chỉ dựa vào việc `javac` thành công.

## Việc bắt đầu ở phiên sau

1. Không sửa lại Mời nhóm nếu không có lỗi mới; người dùng đã xác nhận hoạt động tốt.
2. Tiếp tục chuyển tính năng Nam Mod kế tiếp theo yêu cầu, từng tính năng một.
3. Trước mỗi phần phải đọc đúng code bản cũ, xác định vị trí UI/API rồi mới port sang TB2.