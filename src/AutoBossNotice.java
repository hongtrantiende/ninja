/**
 * AutoBossNotice — Tự động đọc vị Boss từ Kênh Chat / Thông báo Server
 * 
 * Khi nhận được tin nhắn thông báo Boss xuất hiện:
 * 1. Đọc vị xem thuộc loại Boss nào (Map Ngoài, VĐMQ, Làng Cổ, Thế Giới) qua tên Map hoặc từ khóa.
 * 2. Tự động kích hoạt chế độ Săn Boss tương ứng (săn toàn bộ các map của loại Boss đó và tự quét tìm khu).
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

        // Anti-spam / Debounce 5s cho cùng tin nhắn
        long now = System.currentTimeMillis();
        if (text.equals(lastNotice) && now - lastTriggerTime < 5000L) return;

        String lower = text.toLowerCase();

        // Kiểm tra từ khóa thông báo boss xuất hiện
        if (!isBossMessage(lower)) return;

        lastNotice = text;
        lastTriggerTime = now;

        // 1. Kiểm tra theo tên Map trong tin nhắn
        int mapId = findMapIdFromText(text);
        int bossType = -1;

        if (mapId >= 0) {
            bossType = getBossTypeFromMap(mapId);
        }

        // 2. Nếu chưa tìm thấy qua mapId, kiểm tra theo từ khóa tên loại Boss trong chat
        if (bossType < 0) {
            if (lower.indexOf("map ngo\u00e0i") >= 0 || lower.indexOf("map ngoai") >= 0) {
                bossType = AutoSanBoss.TYPE_MAPNGOAI;
            } else if (lower.indexOf("vdmq") >= 0 || lower.indexOf("v\u01b0\u01a1ng qu\u1ed1c") >= 0 || lower.indexOf("vuong quoc") >= 0) {
                bossType = AutoSanBoss.TYPE_VDMQ;
            } else if (lower.indexOf("l\u00e0ng c\u1ed5") >= 0 || lower.indexOf("lang co") >= 0) {
                bossType = AutoSanBoss.TYPE_LANGCO;
            } else if (lower.indexOf("th\u1ebf gi\u1edbi") >= 0 || lower.indexOf("the gioi") >= 0) {
                bossType = AutoSanBoss.TYPE_THEGIOI;
            }
        }

        // 3. Thực hiện kích hoạt Săn Boss theo nhóm loại Boss
        if (bossType == AutoSanBoss.TYPE_MAPNGOAI) {
            String detail = (mapId >= 0 && TileMap.mapNames != null && mapId < TileMap.mapNames.length) ? (" t\u1ea1i " + TileMap.mapNames[mapId]) : "";
            GameScr.gameAC("\uD83D\uDCE2 [\u0110\u1ECCC V\u1EAE] BOSS MAP NGO\u00C0I" + detail + " -> S\u0103n to\u00e0n b\u1ed9 Map Ngo\u00e0i!");
            triggerHuntType(AutoSanBoss.TYPE_MAPNGOAI);
        } else if (bossType == AutoSanBoss.TYPE_VDMQ) {
            String detail = (mapId >= 0 && TileMap.mapNames != null && mapId < TileMap.mapNames.length) ? (" t\u1ea1i " + TileMap.mapNames[mapId]) : "";
            GameScr.gameAC("\uD83D\uDCE2 [\u0110\u1ECCC V\u1EAE] BOSS V\u0110MQ" + detail + " -> S\u0103n to\u00e0n b\u1ed9 V\u0110MQ!");
            triggerHuntType(AutoSanBoss.TYPE_VDMQ);
        } else if (bossType == AutoSanBoss.TYPE_LANGCO) {
            GameScr.gameAC("\uD83D\uDCE2 [\u0110\u1ECCC V\u1EAE] BOSS L\u00C0NG C\u1ED0 -> S\u0103n L\u00e0ng C\u1ed5!");
            triggerHuntType(AutoSanBoss.TYPE_LANGCO);
        } else if (bossType == AutoSanBoss.TYPE_THEGIOI) {
            GameScr.gameAC("\uD83D\uDCE2 [\u0110\u1ECCC V\u1EAE] BOSS TH\u1EAE GI\u1EDAI -> S\u0103n Th\u1ebf Gi\u1edbi!");
            triggerHuntType(AutoSanBoss.TYPE_THEGIOI);
        } else if (mapId >= 0) {
            // Map riêng lẻ không thuộc 4 nhóm trên
            String mapName = (TileMap.mapNames != null && mapId < TileMap.mapNames.length) ? TileMap.mapNames[mapId] : ("Map " + mapId);
            GameScr.gameAC("\uD83D\uDCE2 [\u0110\u1ECCC V\u1EAE] BOSS " + mapName + " -> S\u0103n ngay!");
            triggerDirectMap(mapId);
        }
    }

    private static boolean isBossMessage(String lower) {
        return (lower.indexOf("xu\u1ea5t hi\u1ec7n") >= 0 || lower.indexOf("xuat hien") >= 0
             || lower.indexOf("boss") >= 0
             || lower.indexOf("th\u1ea7n th\u00fa") >= 0 || lower.indexOf("than thu") >= 0
             || lower.indexOf("ph\u1ee5c sinh") >= 0 || lower.indexOf("phuc sinh") >= 0);
    }

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
                if (normName.length() < 3) continue; // Bỏ qua từ quá ngắn

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
     * Chuyển tiếng Việt có dấu thành không dấu để so sánh chuẩn
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
     * Kích hoạt tự động săn cả loại Boss tương ứng
     */
    private static void triggerHuntType(final int bossType) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Ưu tiên dùng AutoBossEvent nếu đang dùng TS Ưu Tiên, hoặc dùng AutoSanBoss
                    if (AutoBossEvent.isEnabled) {
                        int priority = 0;
                        if (bossType == AutoSanBoss.TYPE_VDMQ || bossType == AutoSanBoss.TYPE_LANGCO) priority = 1;
                        else if (bossType == AutoSanBoss.TYPE_MAPNGOAI) priority = 2;
                        else if (bossType == AutoSanBoss.TYPE_THEGIOI) priority = 3;
                        AutoBossEvent.togglePriority(priority);
                    } else {
                        if (bossType == AutoSanBoss.TYPE_VDMQ) AutoSanBoss.toggleVM();
                        else if (bossType == AutoSanBoss.TYPE_MAPNGOAI) AutoSanBoss.toggleMN();
                        else if (bossType == AutoSanBoss.TYPE_LANGCO) AutoSanBoss.toggleLangCo();
                        else if (bossType == AutoSanBoss.TYPE_THEGIOI) AutoSanBoss.toggleTheGioi();
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    /**
     * Dịch chuyển tới 1 map riêng lẻ (nếu không nằm trong 4 nhóm trên)
     */
    private static void triggerDirectMap(final int mapId) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    TileMap.GoMap(mapId);
                } catch (Exception e) {}
            }
        }).start();
    }
}
