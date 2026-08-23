/**
 * AutoBossNotice — Công cụ hỗ trợ: Lắng nghe thông báo Boss từ Chat Server
 *
 * Chỉ làm 1 việc: Phát hiện boss xuất hiện → Gọi TS Boss ưu tiên đánh.
 * Toàn bộ logic săn boss (lưu vị trí, quét map, về farm) do AutoBossEvent xử lý.
 *
 * Flow: Chat → nhận diện loại boss → AutoBossEvent.triggerImmediate(eventPrio)
 */
public class AutoBossNotice {
    public static boolean isEnabled = true;
    /** Bật debug = true để in ra mọi tin nhắn nhận được */
    public static boolean debugMode = false;
    private static long lastTriggerTime = 0;
    private static String lastNotice = "";

    // Tên hiển thị cho từng loại boss
    private static final String[] TYPE_NAMES = {"V\u0110MQ", "Map Ngo\u00e0i", "L\u00e0ng C\u1ed5", "Th\u1ebf Gi\u1edbi"};

    static {
        loadConfigFromRMS();
    }

    public static void saveConfigToRMS() {
        try {
            RMS.gameAA("boss_notice_cfg", isEnabled ? "1" : "0");
        } catch (Exception e) {}
    }

    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("boss_notice_cfg");
            if (data != null && data.length() > 0) {
                isEnabled = data.equals("1");
            }
        } catch (Exception e) {}
    }

    public static void toggle() {
        isEnabled = !isEnabled;
        saveConfigToRMS();
        GameScr.gameAC("TB Boss (Chat): " + (isEnabled ? "ON" : "OFF"));
    }

    /**
     * Hook gọi mỗi khi nhận tin nhắn từ Server / Kênh Chat.
     */
    public static void onReceiveMessage(String text) {
        if (!isEnabled || text == null || text.length() == 0) return;

        if (debugMode) {
            GameScr.gameAC("[DBG] Chat: " + text);
        }

        String lower = text.toLowerCase();

        // === Bước 1: Kiểm tra đây có phải thông báo boss XUẤT HIỆN không ===
        if (!isBossSpawnMessage(lower)) return;

        // Anti-spam: cùng tin nhắn trong 5s → bỏ qua
        long now = System.currentTimeMillis();
        if (text.equals(lastNotice) && now - lastTriggerTime < 5000L) return;
        lastNotice = text;
        lastTriggerTime = now;

        if (debugMode) {
            GameScr.gameAC("[DBG] MATCH spawn: " + text);
        }

        // === Bước 2: Nhận diện LOẠI BOSS từ từ khóa ===
        int bossType = detectBossType(lower, text);

        if (bossType < 0) {
            if (debugMode) {
                GameScr.gameAC("[DBG] Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c lo\u1ea1i boss");
            }
            return;
        }

        String typeName = (bossType >= 0 && bossType < TYPE_NAMES.length) ? TYPE_NAMES[bossType] : "?";

        if (debugMode) {
            GameScr.gameAC("[DBG] Lo\u1ea1i: " + typeName);
        }

        // === Bước 3: Kiểm tra TS Boss đang săn cùng loại chưa ===
        if (AutoBossEvent.inEvent && AutoSanBoss.isRunning) {
            GameScr.gameAC("\uD83D\uDCE2 Boss " + typeName + " - TS Boss \u0111ang s\u0103n, b\u1ecf qua");
            return;
        }

        // === Bước 4: Gọi TS Boss ưu tiên đánh ===
        GameScr.gameAC("\uD83D\uDCE2 Boss " + typeName + " xu\u1ea5t hi\u1ec7n! G\u1ecdi TS Boss...");
        triggerTsBoss(bossType);
    }

    /**
     * Gọi TS Boss ưu tiên để săn loại boss cụ thể.
     * Nếu TS Boss chưa bật → tự bật tạm.
     */
    private static void triggerTsBoss(int bossType) {
        // Map bossType → eventPrio cho AutoBossEvent
        int eventPrio;
        switch (bossType) {
            case AutoSanBoss.TYPE_THEGIOI:  eventPrio = 3; break;
            case AutoSanBoss.TYPE_LANGCO:   eventPrio = 4; break;
            case AutoSanBoss.TYPE_VDMQ:     eventPrio = 5; break;
            case AutoSanBoss.TYPE_MAPNGOAI: eventPrio = 2; break;
            default: eventPrio = 0; break; // fallback: all
        }

        // Gọi AutoBossEvent xử lý hết
        AutoBossEvent.triggerImmediate(eventPrio);
    }

    // ==================== NHẬN DIỆN ====================

    /**
     * Kiểm tra tin nhắn có phải thông báo boss XUẤT HIỆN không.
     * Loại trừ tin boss CHẾT ("đấm vỡ", "tiêu diệt"...).
     */
    private static boolean isBossSpawnMessage(String lower) {
        // Loại trừ: boss bị giết
        if (lower.indexOf("\u0111\u1ea5m v\u1ee1") >= 0 || lower.indexOf("dam vo") >= 0
            || lower.indexOf("ti\u00eau di\u1ec7t") >= 0 || lower.indexOf("tieu diet") >= 0
            || lower.indexOf("h\u1ea1 g\u1ee5c") >= 0 || lower.indexOf("ha guc") >= 0
            || lower.indexOf("\u0111\u00e3 gi\u1ebft") >= 0 || lower.indexOf("da giet") >= 0) {
            return false;
        }

        // Chỉ match: boss xuất hiện / phục sinh
        return (lower.indexOf("xu\u1ea5t hi\u1ec7n") >= 0 || lower.indexOf("xuat hien") >= 0
             || lower.indexOf("ph\u1ee5c sinh") >= 0 || lower.indexOf("phuc sinh") >= 0);
    }

    /**
     * Nhận diện loại boss từ từ khóa trong tin nhắn.
     * Ưu tiên từ khóa đặc trưng trước, fallback sang tên map.
     */
    private static int detectBossType(String lower, String originalText) {
        // === Từ khóa đặc trưng (chính xác nhất) ===

        // "Boss Vùng đất ma quỷ đã xuất hiện..." → VĐMQ
        if (lower.indexOf("v\u00f9ng \u0111\u1ea5t ma qu\u1ef7") >= 0 || lower.indexOf("vung dat ma quy") >= 0
            || lower.indexOf("vdmq") >= 0 || lower.indexOf("v\u01b0\u01a1ng qu\u1ed1c") >= 0 || lower.indexOf("vuong quoc") >= 0) {
            return AutoSanBoss.TYPE_VDMQ;
        }

        // "Thần thú đã xuất hiện tại..." → Map Ngoài
        if (lower.indexOf("th\u1ea7n th\u00fa") >= 0 || lower.indexOf("than thu") >= 0
            || lower.indexOf("map ngo\u00e0i") >= 0 || lower.indexOf("map ngoai") >= 0) {
            return AutoSanBoss.TYPE_MAPNGOAI;
        }

        // Làng Cổ
        if (lower.indexOf("l\u00e0ng c\u1ed5") >= 0 || lower.indexOf("lang co") >= 0) {
            return AutoSanBoss.TYPE_LANGCO;
        }

        // Thế Giới (Boss Server) — map: Chân thác Kitajima
        if (lower.indexOf("th\u1ebf gi\u1edbi") >= 0 || lower.indexOf("the gioi") >= 0
            || lower.indexOf("kitajima") >= 0) {
            return AutoSanBoss.TYPE_THEGIOI;
        }

        // === Fallback: tìm tên map trong TileMap.mapNames ===
        int mapId = findMapIdFromText(originalText);
        if (mapId >= 0) {
            return getBossTypeFromMap(mapId);
        }

        return -1;
    }

    // ==================== UTILITY ====================

    /**
     * Xác định xem Map ID thuộc nhóm loại Boss nào
     */
    public static int getBossTypeFromMap(int mapId) {
        int[] mn = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPNGOAI);
        for (int i = 0; i < mn.length; i++) {
            if (mn[i] == mapId) return AutoSanBoss.TYPE_MAPNGOAI;
        }
        int[] vm = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_VDMQ);
        for (int i = 0; i < vm.length; i++) {
            if (vm[i] == mapId) return AutoSanBoss.TYPE_VDMQ;
        }
        int[] lc = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGCO);
        for (int i = 0; i < lc.length; i++) {
            if (lc[i] == mapId) return AutoSanBoss.TYPE_LANGCO;
        }
        int[] tg = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_THEGIOI);
        for (int i = 0; i < tg.length; i++) {
            if (tg[i] == mapId) return AutoSanBoss.TYPE_THEGIOI;
        }
        return -1;
    }

    /**
     * Tìm Map ID từ tên Map trong TileMap.mapNames
     */
    public static int findMapIdFromText(String text) {
        if (text == null) return -1;
        String normText = stripAccents(text.toLowerCase());

        int bestMapId = -1;
        int maxLen = 0;

        if (TileMap.mapNames != null) {
            for (int id = 0; id < TileMap.mapNames.length; id++) {
                String name = TileMap.mapNames[id];
                if (name == null || name.trim().length() == 0) continue;

                String normName = stripAccents(name.toLowerCase());
                if (normName.length() < 3) continue;

                if (normText.indexOf(normName) >= 0) {
                    if (normName.length() > maxLen) {
                        maxLen = normName.length();
                        bestMapId = id;
                    }
                }
            }
        }
        return bestMapId;
    }

    /**
     * Chuyển tiếng Việt có dấu thành không dấu
     */
    private static String stripAccents(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\u00e0': case '\u00e1': case '\u1ea1': case '\u1ea3': case '\u00e3':
                case '\u00e2': case '\u1ea7': case '\u1ea5': case '\u1eAD': case '\u1ea9': case '\u1eab':
                case '\u0103': case '\u1eb1': case '\u1eaf': case '\u1eb7': case '\u1eb3': case '\u1eb5':
                    sb.append('a'); break;
                case '\u00e8': case '\u00e9': case '\u1eb9': case '\u1ebb': case '\u1ebd':
                case '\u00ea': case '\u1ec1': case '\u1ebf': case '\u1ec7': case '\u1ec3': case '\u1ec5':
                    sb.append('e'); break;
                case '\u00ec': case '\u00ed': case '\u1ecb': case '\u1ec9': case '\u0129':
                    sb.append('i'); break;
                case '\u00f2': case '\u00f3': case '\u1ecd': case '\u1ecf': case '\u00f5':
                case '\u00f4': case '\u1ed3': case '\u1ed1': case '\u1ed9': case '\u1ed5': case '\u1ed7':
                case '\u01a1': case '\u1edd': case '\u1edb': case '\u1ee3': case '\u1edf': case '\u1ee1':
                    sb.append('o'); break;
                case '\u00f9': case '\u00fa': case '\u1ee5': case '\u1ee7': case '\u0169':
                case '\u01b0': case '\u1eeb': case '\u1ee9': case '\u1ef1': case '\u1eed': case '\u1eef':
                    sb.append('u'); break;
                case '\u1ef3': case '\u00fd': case '\u1ef5': case '\u1ef7': case '\u1ef9':
                    sb.append('y'); break;
                case '\u0111':
                    sb.append('d'); break;
                default:
                    sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
