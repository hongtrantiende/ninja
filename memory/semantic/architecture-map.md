# Architecture Map — Aeharuna.jar (Ninja School Mod)

## 🗺️ Bản Đồ Kiến Trúc & Cấu Trúc File JAR

- **File JAR Target:** `Aeharuna.jar` / `Aeharuna_nhanda.jar`
- **Kích thước:** ~1.2 MB
- **Loại:** Java MicroEdition (MIDlet 2.0 / CLDC 1.1)

### 📂 Các thành phần chính & Lệnh Chat:
- `Code.class`: Xử lý phân tích cú pháp các lệnh chat (`glv`, `locdo`, `atpk`, `acpk`, `adpk`, `nhanda`, `gaoda`, `speed`, `td`, `hp`, `mp`, `food`, `abuff`).
- `AutoNhanDa.class`: Xử lý tự động di chuyển tới Map 23 (481, 168), đối thoại NPC 33 nhận đá 62 giữ trong Hành trang.
- `AutoGaoDa.class`: Xử lý vòng lặp gạo đá (Map 23 -> NPC 62 -> nhận đá -> Map 26 -> NPC 63 -> giao đá).
- `AutoNpc.class`: Xử lý tự động chuyển map, di chuyển tọa độ X-Y và tương tác menu NPC.
- `META-INF/MANIFEST.MF`: Khai báo MIDlet chính (Đã được tự động hóa nén chuẩn bằng `jar cfm`).

---

### ⚙️ Danh Sách Lệnh Chat Đã Tích Hợp:
- **`speed <10-50>`**: Hack tốc độ game (x2, x3, x5).
- **`td <0-100>`**: Hack tốc độ ra chiêu / delay skill đòn đánh.
- **`hp <1-99>`**: Cài đặt % máu tự động bơm HP.
- **`mp <1-99>`**: Cài đặt % mana tự động bơm MP.
- **`food`**: Bật/tắt tự động ăn thức ăn tăng dẻo dai/chỉ số.
- **`abuff`**: Bật/tắt tự động buff chiêu hỗ trợ.
- **`nhanda`**: Tự động di chuyển tới Map 23 (481, 168) -> Tương tác NPC 33 nhận đá 62 giữ trong Hành trang.
- **`gaoda`**: Vòng lặp gạo đá tự động: `gm23` -> `npc62` (nhận đá) -> `gm26` -> `npc63` (giao đá) -> lặp lại liên tục từ Hành trang.


