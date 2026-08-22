/**
 * AutoBossNotice — Tự động phát hiện Boss xuất hiện từ kênh chat / thông báo Server.
 * 
 * Lắng nghe các tin nhắn từ Server (InfoMe, Chat), so sánh tên Map trong tin nhắn
 * với bảng tên map TileMap.mapNames để tự động tìm Map ID và Zone, sau đó dịch chuyển đến săn ngay lập tức.
 */
public class AutoBossNotice {
    public static boolean isEnabled = true; // Mặc định BẬT
    private static long lastTriggerTime = 0;
    private static String lastNotice = "";

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
        GameScr.gameAC("Auto S\u0103n Boss qua Chat: " + (isEnabled ? "ON" : "OFF"));
    }

    /**
     * Hook gọi mỗi khi nhận tin nhắn từ Server / Kênh Chat.
     */
    public static void onReceiveMessage(String text) {
        if (!isEnabled || text == null || text.length() == 0) return;

        // Anti-spam / Debounce 3s cho cùng tin nhắn
        long now = System.currentTimeMillis();
        if (text.equals(lastNotice) && now - lastTriggerTime < 3000L) return;

        String lower = text.toLowerCase();

        // Kiểm tra từ khóa thông báo boss xuất hiện
        if (!isBossMessage(lower)) return;

        lastNotice = text;
        lastTriggerTime = now;

        // Tìm Map ID từ tên map trong tin nhắn
        int mapId = findMapIdFromText(text);
        if (mapId < 0) return;

        // Trích xuất khu (zone) nếu có
        int zoneId = extractZoneFromText(lower);

        String mapName = (TileMap.mapNames != null && mapId >= 0 && mapId < TileMap.mapNames.length) 
                ? TileMap.mapNames[mapId] : ("Map " + mapId);

        GameScr.gameAC("\uD83D\uDCE2 [BOSS CHAT] " + mapName + (zoneId >= 0 ? (" Khu " + zoneId) : "") + " -> \u0110ang di chuy\u1ec3n!");

        // Thực hiện dịch chuyển đến săn Boss
        triggerHunt(mapId, zoneId);
    }

    private static boolean isBossMessage(String lower) {
        return (lower.indexOf("xu\u1ea5t hi\u1ec7n") >= 0 || lower.indexOf("xuat hien") >= 0
             || lower.indexOf("boss") >= 0
             || lower.indexOf("th\u1ea7n th\u00fa") >= 0 || lower.indexOf("than thu") >= 0
             || lower.indexOf("ph\u1ee5c sinh") >= 0 || lower.indexOf("phuc sinh") >= 0);
    }

    /**
     * Tìm Map ID bằng cách so sánh tên map trong TileMap.mapNames với nội dung tin nhắn.
     * Hỗ trợ cả tiếng Việt có dấu và không dấu.
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
                // Bỏ qua tên ngắn < 3 ký tự để tránh khớp nhầm
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
     * Trích xuất khu (zone) từ chuỗi ví dụ: "khu 3", "zone 12", "k5"
     */
    private static int extractZoneFromText(String lower) {
        try {
            int idx = lower.indexOf("khu ");
            if (idx < 0) idx = lower.indexOf("zone ");
            if (idx >= 0) {
                int start = lower.indexOf(' ', idx) + 1;
                int end = start;
                while (end < lower.length() && Character.isDigit(lower.charAt(end))) {
                    end++;
                }
                if (end > start) {
                    return Integer.parseInt(lower.substring(start, end));
                }
            }
        } catch (Exception e) {}
        return -1;
    }

    /**
     * Chuyển tiếng Việt có dấu thành không dấu để so sánh chính xác 100%.
     */
    private static String stripAccents(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'à': case 'á': case 'ạ': case 'ả': case 'ã':
                case 'â': case 'ầ': case 'ấ': case 'ậ': case 'ẩ': case 'ẫ':
                case 'ă': case 'ằ': case 'ắ': case 'ặ': case 'ẳ': case 'ẵ':
                    sb.append('a'); break;
                case 'è': case 'é': case 'ẹ': case 'ẻ': case 'ẽ':
                case 'ê': case 'ề': case 'ế': case 'ệ': case 'ể': case 'ễ':
                    sb.append('e'); break;
                case 'ì': case 'í': case 'ị': case 'ỉ': case 'ĩ':
                    sb.append('i'); break;
                case 'ò': case 'ó': case 'ọ': case 'ỏ': case 'õ':
                case 'ô': case 'ồ': case 'ố': case 'ộ': case 'ổ': case 'ỗ':
                case 'ơ': case 'ờ': case 'ớ': case 'ợ': case 'ở': case 'ỡ':
                    sb.append('o'); break;
                case 'ù': case 'ú': case 'ụ': case 'ủ': case 'ũ':
                case 'ư': case 'ừ': case 'ứ': case 'ự': case 'ử': case 'ữ':
                    sb.append('u'); break;
                case 'ỳ': case 'ý': case 'ỵ': case 'ỷ': case 'ỹ':
                    sb.append('y'); break;
                case 'đ':
                    sb.append('d'); break;
                default:
                    sb.append(c); break;
            }
        }
        return sb.toString();
    }

    /**
     * Dịch chuyển tới Map và Zone tìm được
     */
    private static void triggerHunt(final int mapId, final int zoneId) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Chuyển tới map
                    TileMap.GoMap(mapId);
                    if (zoneId >= 0) {
                        Thread.sleep(500);
                        Service.gI().gameAL(zoneId); // Đổi khu
                    }
                } catch (Exception e) {}
            }
        }).start();
    }
}
