# Architecture Map — Ninja School Modding

## Core Game Classes (Obfuscated)

### Code (Code.class / Code.java)
- **`gameAB`** (`Auto`): Field chính quyết định auto đang chạy. Nếu != null → menu hiện "Tắt Auto".
- **`gameAA(Auto)`**: Push auto vào stack (`gameAB = auto`).
- **`gameAC()`**: Pop auto stack (`gameAB = gameAB.reAB`).
- **`gameAF(String)`**: Handler lệnh chat từ user (compiled, không có trong source).
- **`gameAH`** (`String`): Tên nhóm trưởng.
- **`gameCC`** (`TanSat`): Instance TanSat singleton.
- Chat handler nằm HOÀN TOÀN trong compiled Code.class. Code.java source chỉ chứa run(), party receiver, field declarations.

### Auto (Auto.class) — Abstract base
- **`gameAK()`**: Abstract method — game loop gọi liên tục. BẮT BUỘC override.
- **`gameAC()`**: Gọi khi auto dừng.
- **`gameAD()`**: Gọi khi auto bắt đầu.
- **`mapID`** (`int`): Map mục tiêu.
- **`zoneID`** (`int`): Khu mục tiêu. `-2` = chế độ quét tất cả khu.
- **`reAB`** (`Auto`): Auto trước đó trong stack (linked list).
- **`gameAK`** (`static boolean`): Cờ static, dùng cho logic khác.

### PkBoss (PkBoss.class)
- Extends `Auto`. Constructor `PkBoss(mapID)` → set `zoneID = -2` (quét mode).
- **`gameAK()` loop tự động:**
  1. Chuyển map đến `mapID`
  2. Quét tất cả khu khi `zoneID == -2`
  3. Khi tìm boss → set `zoneID = TileMap.zoneID`, bắt đầu đánh
  4. Gửi `Service.gameAK("pkm " + mapID + " " + zoneID + " " + templateId)` cho nhóm
  5. Gửi `Service.gameAK("pkk " + zoneID)` cho nhóm
  6. Khi xong gửi `"pke"` → `Code.gameAC()` pop auto stack

### Service (Service.class)
- **`gI()`**: Singleton.
- **`gameAK(String)`**: Gửi party chat message.
- **`gameAF()`**: Gửi lệnh hồi sinh (về nhà).
- **`gameAA(int, int)`**: KHÔNG phải chuyển khu! Đây là method khác.

### TileMap (TileMap.class)
- **`mapID`** (`short`): Map hiện tại.
- **`zoneID`** (`byte`): Khu hiện tại.
- **`gameAF()`**: Refresh/reload map.

### GameScr (GameScr.class)
- **`gameAC(String)`**: Hiển thị chat message trên màn hình.
- **`vParty`** (`MyVector`): Danh sách thành viên nhóm.
- **`vMob`** (`MyVector`): Danh sách mob trên map hiện tại.
- **`gameAB(int, int, int)`**: Mở menu (respawn dialog, etc).

### Char (Char.class)
- **`getMyChar()`**: Singleton nhân vật chính.
- **`statusMe`**: Trạng thái. `14` = Kiệt sức (chết).
- **`cHP`**: HP hiện tại.
- **`clevel`**: Level nhân vật.
- **`cName`**: Tên nhân vật.

### Mob (Mob.class)
- **`isBoss`** (`boolean`): Có phải boss không.
- **`hp`** (`int`): HP hiện tại.
- **`status`** (`int`): Trạng thái. `0` = chết.
- **`templateId`** (`int`): ID template mob.

### GameCanvas (GameCanvas.class)
- **`endDlg()`**: Đóng dialog popup.

## Party Chat Protocol
- **Leader gửi:** `Service.gI().gameAK(command)` 
- **Member nhận:** Handler trong Code.java source dòng 2510+
- **Commands:**
  | Command | Format | Member Action |
  |---------|--------|---------------|
  | `ts` | `ts mapID zoneID templateId` | Bật TanSat nhóm |
  | `pkm` | `pkm mapID` | Bật PkBoss(mapID) |
  | `pkk` | `pkk zoneID` | Chuyển khu (set gameAB.zoneID) |
  | `pke` | `pke` | Tắt PkBoss (Code.gameAC()) |
  | `map` | `map mapID` | Set gameAB.mapID |
  | `khu` | `khu zoneID` | Set gameAB.zoneID |
  | `sts` | `sts` | Stanima command |

## Custom Mod Classes

### AutoSanBoss (src/AutoSanBoss.java)
- Implements `Runnable`, chạy thread riêng.
- **Lệnh:** `tspkb` → `AutoSanBoss.toggle()`
- **Flow:** Quét 4 loại boss → cho mỗi map: `Code.gameAA(new PkBoss(mapID))` → PkBoss tự quét + đánh → chờ xong → map tiếp.
- **Party mode:** Auto-detect nhóm. Gửi `pkm` khi bật (members bật PkBoss). Gửi `pkm + pkk` khi tìm thấy boss. Gửi `pke` khi tắt.
- **Boss data:**
  - Server: M3, giờ 12/18/20/22
  - TheGioi: M23, giờ 12/23
  - VDMQ: M141-143, giờ 9/15/17/21
  - MapNgoai: 12 map {14,15,16,44,67,70,24,41,45,18,36,54}, giờ 6/11/17/22
  - Mỗi boss sống 40 phút (2400s)

### SanBossHolder (src/SanBossHolder.java)
- Extends `Auto`. 3 method rỗng: `gameAC()`, `gameAD()`, `gameAK()`.
- Giữ `Code.gameAB != null` → menu hiện "Tắt Auto" khi PkBoss không active.

### MultiSkillAttack (src/MultiSkillAttack.java)
- AK multi-skill. Đã patch bytecode trong `Auto.class` gọi `attackMultiSkill()`.

### ThongTinBoss (src/ThongTinBoss.java)
- HUD overlay hiển thị lịch boss. Lệnh `ttb`.

### InfoMe (src/InfoMe.java)
- Hook `ThongTinBoss.paint(g)` vào game render loop.

### Code (src/Code.java)
- Source chứa: field declarations, `gameAA(Auto)`, `gameAC()`, party chat receiver (dòng 2510+), run() thread.
- Chat handler lệnh user (`tspkb`, `tsn`, etc.) nằm trong COMPILED class, không trong source.

## Build Pipeline (PowerShell)
```powershell
# 1. Tạo stubs
New-Item -ItemType Directory -Force -Path 'stubs/javax/microedition/lcdui'
# ... tạo Image.java, CommandListener.java, Form.java stubs

# 2. Compile
javac -encoding UTF-8 -source 8 -target 8 -cp "build/unpacked;stubs;src" -d build/unpacked src/File1.java src/File2.java

# 3. Clean stubs
Remove-Item -Recurse -Force "build/unpacked/javax"
Remove-Item -Recurse -Force "stubs"

# 4. Pack JAR (PHẢI dùng cfm, KHÔNG dùng wildcard *)
Set-Location 'build/unpacked'
jar cfm '../../Aeharuna.jar' 'META-INF/MANIFEST.MF' *.class *.txt *.png font map x1
```
