import java.util.Calendar;
import java.util.TimeZone;

/**
 * AutoSanBoss - Tu dong san boss theo khung gio
 * Lenh chat: tspkb (bat/tat)
 * 
 * Logic:
 * 1. Kiem tra khung gio spawn cua 4 loai boss (Server, TheGioi, VDMQ, MapNgoai)
 * 2. Khi boss spawn: chuyen map -> quet tat ca khu -> tim boss -> PK boss
 * 3. Danh xong 1 boss: quet lai cac khu khac / map khac
 * 4. Chet: hoi sinh va quay lai map boss
 * 5. Het boss: cho boss loai tiep theo
 */
public class AutoSanBoss implements Runnable {
    public static boolean isRunning = false;
    public static boolean isPartyMode = false;
    public static boolean treoMode = false; // true = tim boss nhung khong danh, chi goi nhom roi dung cho
    public static int forcedBossType = -1; // -1 = auto schedule, 0-3 = force loai boss cu the
    private static Thread thread;
    private static Thread memberMoveThread;
    private static int memberTargetZone = -1;
    private static boolean eventHuntMode;
    private static boolean eventRoundCompleted;
    /** Override thu tu uu tien boss cho TS event (null = dung HUNT_PRIORITY mac dinh) */
    static int[] eventHuntTypes;

    // 6 loai boss (VDMQ, MapNgoai, LangCo, TheGioi, MapVIP, MapVIP2)
    public static final int TYPE_VDMQ = 0;
    public static final int TYPE_MAPNGOAI = 1;
    public static final int TYPE_LANGCO = 2;
    public static final int TYPE_THEGIOI = 3;
    public static final int TYPE_MAPVIP = 4;
    public static final int TYPE_MAPVIP2 = 5;
    public static final int TYPE_ALL = 6;
    public static final int TYPE_TEST_1MAP = 7;
    public static int testMapId = 141;

    private static final String[] BOSS_NAMES = {"VDMQ", "MapNgoai", "L\u00e0ng C\u1ed5", "Th\u1ebf Gi\u1edbi", "Map VIP", "Map VIP2", "T\u1ea5t C\u1ea3", "Test 1 Map"};

    /** Lay ten boss theo type (dung cho thong bao) */
    public static String getBossName(int bossType) {
        if (bossType >= 0 && bossType < BOSS_NAMES.length) return BOSS_NAMES[bossType];
        return "?";
    }

    // === CAI DAT: per-map filtering ===
    /** Danh sach map ID bi tat (khong san). Mac dinh rong = tat ca duoc san. */
    public static java.util.Vector disabledMaps = new java.util.Vector();

    /** Toc do chuyen khu (ms) khi quet tim boss. Mac dinh 10ms. */
    public static int zoneChangeDelayMs = 10;

    /** Khu bat dau khi quet tim boss (0-29). Mac dinh 0. */
    public static int scanZoneStart = 0;
    /** Khu ket thuc khi quet tim boss (0-29). Mac dinh 29. */
    public static int scanZoneEnd = 29;

    /** Ghost Attack Boss: Danh xa ngay khi thay boss va lao vao dan. Mac dinh bat (true). */
    public static boolean isGhostAttack = true;
    /** Tam Ghost Attack Boss (px, 9999=toan map). */
    public static int ghostRange = 9999;

    static {
        loadFromRMS();
        loadBossHoursFromRMS();
    }

    public static String getZoneRangeStr() {
        return scanZoneStart + "-" + scanZoneEnd;
    }

    public static boolean setZoneRangeFromStr(String str) {
        try {
            if (str == null || str.trim().length() == 0) return false;
            str = str.trim();
            int dash = str.indexOf('-');
            if (dash != -1) {
                int z1 = Integer.parseInt(str.substring(0, dash).trim());
                int z2 = Integer.parseInt(str.substring(dash + 1).trim());
                if (z1 >= 0 && z1 <= 29 && z2 >= 0 && z2 <= 29 && z1 <= z2) {
                    scanZoneStart = z1;
                    scanZoneEnd = z2;
                    return true;
                }
            } else {
                int z = Integer.parseInt(str);
                if (z >= 0 && z <= 29) {
                    scanZoneStart = 0;
                    scanZoneEnd = z;
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    public static void loadFromRMS() {
        try {
            String data = RMS.gameAC("dis_boss_maps");
            if (data != null && data.length() > 0) {
                disabledMaps.removeAllElements();
                int start = 0;
                int comma;
                while ((comma = data.indexOf(',', start)) != -1) {
                    String token = data.substring(start, comma).trim();
                    if (token.length() > 0) {
                        disabledMaps.addElement(new Integer(Integer.parseInt(token)));
                    }
                    start = comma + 1;
                }
                if (start < data.length()) {
                    String token = data.substring(start).trim();
                    if (token.length() > 0) {
                        disabledMaps.addElement(new Integer(Integer.parseInt(token)));
                    }
                }
            }
            String delayStr = RMS.gameAC("boss_zone_delay");
            if (delayStr != null && delayStr.trim().length() > 0) {
                int d = Integer.parseInt(delayStr.trim());
                if (d >= 10 && d <= 5000) {
                    zoneChangeDelayMs = d;
                }
            }
            String zoneRangeStr = RMS.gameAC("boss_zone_range");
            if (zoneRangeStr != null && zoneRangeStr.trim().length() > 0) {
                setZoneRangeFromStr(zoneRangeStr.trim());
            }
            String ghostAtkStr = RMS.gameAC("boss_ghost_atk");
            if (ghostAtkStr != null && ghostAtkStr.trim().length() > 0) {
                isGhostAttack = "1".equals(ghostAtkStr.trim()) || "true".equalsIgnoreCase(ghostAtkStr.trim());
            }
            String ghostRangeStr = RMS.gameAC("boss_ghost_range");
            if (ghostRangeStr != null && ghostRangeStr.trim().length() > 0) {
                int r = Integer.parseInt(ghostRangeStr.trim());
                if (r >= 100 && r <= 9999) ghostRange = r;
            }
        } catch (Exception e) {}
    }

    public static void saveToRMS() {
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < disabledMaps.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(disabledMaps.elementAt(i).toString());
            }
            RMS.gameAA("dis_boss_maps", sb.toString());
            RMS.gameAA("boss_zone_delay", String.valueOf(zoneChangeDelayMs));
            RMS.gameAA("boss_zone_range", getZoneRangeStr());
            RMS.gameAA("boss_ghost_atk", isGhostAttack ? "1" : "0");
            RMS.gameAA("boss_ghost_range", String.valueOf(ghostRange));
        } catch (Exception e) {}
    }

    // === Boss Hours RMS ===

    /** Lay chuoi gio spawn cua 1 loai boss, vd "6,13,19,23" */
    public static String getBossHoursStr(int bossType) {
        int[] hours = BOSS_HOURS[bossType];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hours.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(hours[i]);
        }
        return sb.toString();
    }

    /** Set gio spawn tu chuoi, vd "6,13,19,23". Tra ve true neu hop le. */
    public static boolean setBossHoursFromStr(int bossType, String str) {
        try {
            if (str == null || str.trim().length() == 0) return false;
            // Dem so luong phan tu
            int count = 1;
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == ',') count++;
            }
            int[] hours = new int[count];
            int idx = 0;
            int start = 0;
            for (int i = 0; i <= str.length(); i++) {
                if (i == str.length() || str.charAt(i) == ',') {
                    String token = str.substring(start, i).trim();
                    if (token.length() == 0) return false;
                    int h = Integer.parseInt(token);
                    if (h < 0 || h > 23) return false;
                    hours[idx++] = h;
                    start = i + 1;
                }
            }
            BOSS_HOURS[bossType] = hours;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Reset gio spawn ve mac dinh */
    public static void resetBossHours(int bossType) {
        int[] def = DEFAULT_BOSS_HOURS[bossType];
        int[] copy = new int[def.length];
        for (int i = 0; i < def.length; i++) copy[i] = def[i];
        BOSS_HOURS[bossType] = copy;
    }

    /** Luu gio spawn vao RMS. Format: "type0|type1|type2|type3" */
    public static void saveBossHoursToRMS() {
        try {
            StringBuffer sb = new StringBuffer();
            for (int t = 0; t < BOSS_HOURS.length; t++) {
                if (t > 0) sb.append('|');
                sb.append(getBossHoursStr(t));
            }
            RMS.gameAA("boss_hours", sb.toString());
        } catch (Exception e) {}
    }

    /** Load gio spawn tu RMS */
    public static void loadBossHoursFromRMS() {
        try {
            String data = RMS.gameAC("boss_hours");
            if (data != null && data.length() > 0) {
                // Split by '|'
                int typeIdx = 0;
                int start = 0;
                for (int i = 0; i <= data.length() && typeIdx < BOSS_HOURS.length; i++) {
                    if (i == data.length() || data.charAt(i) == '|') {
                        String part = data.substring(start, i);
                        setBossHoursFromStr(typeIdx, part);
                        typeIdx++;
                        start = i + 1;
                    }
                }
            }
        } catch (Exception e) {}
    }

    /** Tat ca maps cho moi loai boss (dung cho menu) */
    public static int[] getAllMapsForType(int bossType) {
        if (bossType == TYPE_MAPNGOAI) {
            return new int[]{14,15,16,44,67,70,24,41,45,18,36,54};
        }
        if (bossType == TYPE_VDMQ) return new int[]{141,142,143};
        if (bossType == TYPE_LANGCO) return new int[]{135,136};
        if (bossType == TYPE_THEGIOI) return new int[]{20};
        if (bossType == TYPE_MAPVIP) return new int[]{195};
        if (bossType == TYPE_MAPVIP2) return new int[]{196};
        return new int[0];
    }

    /** Kiem tra map co phai la map Boss The Gioi khong */
    public static boolean isWorldBossMap(int mapId) {
        if (mapId == 20) return true;
        int[] tg = getAllMapsForType(TYPE_THEGIOI);
        if (tg != null) {
            for (int i = 0; i < tg.length; i++) {
                if (tg[i] == mapId) return true;
            }
        }
        return false;
    }

    /** Kiem tra map co duoc phep san khong */
    public static boolean isMapEnabled(int mapId) {
        return !disabledMaps.contains(new Integer(mapId));
    }

    /** Bat/tat 1 map */
    public static void toggleMap(int mapId) {
        Integer key = new Integer(mapId);
        if (disabledMaps.contains(key)) {
            disabledMaps.removeElement(key);
            GameScr.gameAC("Map " + mapId + ": B\u1eadt");
        } else {
            disabledMaps.addElement(key);
            GameScr.gameAC("Map " + mapId + ": T\u1eaft");
        }
    }

    /** Bat/tat tat ca maps cua 1 loai boss */
    public static void toggleAllMapsOfType(int bossType) {
        int[] maps = getAllMapsForType(bossType);
        // Neu tat ca dang bat -> tat het; nguoc lai -> bat het
        boolean allOn = true;
        for (int i = 0; i < maps.length; i++) {
            if (!isMapEnabled(maps[i])) { allOn = false; break; }
        }
        for (int i = 0; i < maps.length; i++) {
            Integer key = new Integer(maps[i]);
            if (allOn) {
                if (!disabledMaps.contains(key)) disabledMaps.addElement(key);
            } else {
                disabledMaps.removeElement(key);
            }
        }
        GameScr.gameAC(BOSS_NAMES[bossType] + ": " + (!allOn ? "B\u1eadt t\u1ea5t c\u1ea3" : "T\u1eaft t\u1ea5t c\u1ea3"));
    }

    /** Bat tat ca maps (xoa disabledMaps) */
    public static void enableAllMaps() {
        disabledMaps.removeAllElements();
        GameScr.gameAC("S\u0103n Boss: B\u1eadt t\u1ea5t c\u1ea3 maps");
    }

    /** Kiem tra 1 loai boss co con map nao duoc bat khong */
    public static boolean isBossTypeEnabled(int type) {
        int[] maps = getAllMapsForType(type);
        for (int i = 0; i < maps.length; i++) {
            if (isMapEnabled(maps[i])) return true;
        }
        return false;
    }

    /** Dem so map dang bat cho 1 loai boss */
    public static int countEnabledMaps(int bossType) {
        int[] maps = getAllMapsForType(bossType);
        int count = 0;
        for (int i = 0; i < maps.length; i++) {
            if (isMapEnabled(maps[i])) count++;
        }
        return count;
    }

    /** Thu tu uu tien quet boss: MapVIP2 > MapVIP > Lang Co > VDMQ > TheGioi > MapNgoai */
    private static final int[] HUNT_PRIORITY = {TYPE_MAPVIP2, TYPE_MAPVIP, TYPE_LANGCO, TYPE_VDMQ, TYPE_THEGIOI, TYPE_MAPNGOAI};

    /** Expose HUNT_PRIORITY cho AutoBossEvent pre-spawn su dung */
    public static int[] getHuntPriority() { return HUNT_PRIORITY; }

    // Map IDs cho moi loai boss
    private static final int[][] BOSS_MAPS = {
        {141, 142, 143},   // VDMQ
        {},                // MapNgoai
        {135, 136},        // Làng Cổ
        {20},              // Thế Giới
        {195},             // Map VIP
        {196}              // Map VIP2 (VIP 6-7)
    };

    // Map IDs cua MapNgoai theo level (12 maps)
    private static final int[][] MAPNGOAI_BY_LEVEL = {
        {14, 15, 16},                  // Lv45: Xích Phiến Thiên Long (ID 115)
        {44, 67, 70},                  // Lv55: Thần Thố (ID 114)
        {24, 41, 45},                  // Lv65: Samurai Chiến Tướng (ID 116)
        {18, 36, 54}                   // Lv75: Hỏa Ngưu Vương (ID 139)
    };
    private static final int[] MAPNGOAI_LEVELS = {45, 55, 65, 75};

    // Khung gio spawn (gio) — co the chinh sua
    private static final int[][] DEFAULT_BOSS_HOURS = {
        {6, 13, 19, 23},                                       // VDMQ
        {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23},            // MapNgoai (gio le)
        {7, 10, 15, 23},                                       // Làng Cổ
        {12, 21},                                               // Thế Giới
        {6, 12, 20, 23},                                        // Map VIP
        {6, 12, 20, 23}                                         // Map VIP2
    };
    public static int[][] BOSS_HOURS = {
        {6, 13, 19, 23},
        {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23},
        {7, 10, 15, 23},
        {12, 21},
        {6, 12, 20, 23},
        {6, 12, 20, 23}
    };

    // Dummy Auto giu Code.gameAB != null -> menu hien "Tat Auto"
    private static SanBossHolder dummyAuto;
    private static final int BOSS_ALIVE_DURATION = 2400; // Boss ton tai 40 phut
    private static final int MAX_ZONES = 30;
    private static final int RECONNECT_TIMEOUT = 120; // Cho toi da 2 phut de reconnect

    /** Tat co Co Lenh / Khao Di Lenh de khong tu dung lai khi thoat Lang Co. */
    public static void cleanKhaoDiLenh() {
        Char.MuaCoLenh = false;
        Char.DungCoLenh = false;
    }

    /** Finishes Lang Co hunt, sends pkm -6 to party, and uses NPC 7 at M138 to return to village. */
    public static void finishLangCoAndExit() {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                Service.gI().gameAK("pkm -6");
            }
        } catch (Exception e) {}

        if (!TileMap.isLangCo(TileMap.mapID)) return;

        // Xoa sach item KDL
        cleanKhaoDiLenh();
        sleep(200L);

        // Neu dang o M135/136, chuyen ve M138 (zone 0 = cong vao)
        if (TileMap.mapID == 135 || TileMap.mapID == 136) {
            try { Auto.gameAA(0); } catch (Exception e) {}
            for (int w = 0; w < 50 && (TileMap.mapID == 135 || TileMap.mapID == 136); w++) {
                sleep(100L);
            }
        }

        // O M138: dung NPC 7 nut so 4 (index 3) de ve lang
        if (TileMap.mapID == 138) {
            for (int retry = 0; retry < 3; retry++) {
                try {
                    GameScr.gameAB(7, 3, 0);
                } catch (Exception e) {
                    sleep(1000L);
                    continue;
                }
                // Cho roi khoi Lang Co (toi da 10s)
                for (int w = 0; w < 100 && TileMap.isLangCo(TileMap.mapID); w++) {
                    sleep(100L);
                }
                if (!TileMap.isLangCo(TileMap.mapID)) return;
                sleep(500L);
            }
        }

        // Fallback: neu NPC that bai, tu sat ve thon
        if (TileMap.isLangCo(TileMap.mapID)) {
            suicideAndEnsureAlive();
            for (int w = 0; w < 50 && TileMap.isLangCo(TileMap.mapID); w++) {
                sleep(100L);
            }
        }
    }

    /** Kiem tra map co thuoc Vung Dat Ma Quy (M139-148) hay khong */
    public static boolean isVDMQ(int mapID) {
        return mapID >= 139 && mapID <= 148;
    }

    /** Thoat VDMQ ve lang bang cach tu sat hoi sinh (cach nhanh nhat) */
    public static void finishVDMQAndExit() {
        if (isVDMQ(TileMap.mapID)) {
            GameScr.gameAC("TSB: Tho\u00e1t VDMQ v\u1ec1 l\u00e0ng \u0111\u1ec3 sang map kh\u00e1c...");
            suicideAndEnsureAlive();
            for (int w = 0; w < 50 && isVDMQ(TileMap.mapID); w++) {
                sleep(100L);
            }
        }
    }

    /**
     * Tu sat roi hoi sinh ve lang.
     */
    private static void suicideAndEnsureAlive() {
        try {
            if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                try { Code.gameAN(); } catch (Exception e) {}
                sleep(400L);

                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                    try { Service.gI().gameAE(); } catch (Exception e) {}
                }
            }

            for (int w = 0; w < 20 && (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0); w++) {
                sleep(100L);
            }

            respawnIfDead();
        } catch (Exception e) {}
    }

    /** Kiem tra nhan vat da chet chua (HP <= 0 hoac statusMe == 14) */
    public static boolean isDead() {
        try {
            Char me = Char.getMyChar();
            return me == null || me.statusMe == 14 || me.cHP <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Hoi sinh nhanh neu dang chet - dung cho finishLangCoAndExit va suicideAndEnsureAlive. */
    private static void respawnIfDead() {
        try {
            if (isDead()) {
                for (int retry = 0; retry < 10; retry++) {
                    GameCanvas.endDlg();
                    sleep(20L);
                    Auto.gameAN.removeAllElements();
                    Auto.gameAM = false;
                    LockGame.gameAA = true;
                    if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                        Service.gI().gameAL();
                    } else {
                        Service.gI().gameAK();
                        TileMap.gameAF();
                    }
                    LockGame.gameAA = false;
                    sleep(300L);
                    if (!isDead()) return;
                }
            }
        } catch (Exception e) {}
    }

    /** Find Co Lenh / Khao Di Lenh item (ID 490, 35, 37) directly in inventory bag. */
    /** Lay vi tri index (0..29) cua Co Lenh / Khao Di Lenh trong hanh trang de doi khu tu xa */
    public static int getCoLenhBagIndex() {
        try {
            int idx = Char.gameAI(490);
            if (idx >= 0) return idx;
            idx = Char.gameAI(37);
            if (idx >= 0) return idx;
            idx = Char.gameAI(35);
            if (idx >= 0) return idx;
            Char myChar = Char.getMyChar();
            if (myChar != null && myChar.arrItemBag != null) {
                for (int i = 0; i < myChar.arrItemBag.length; i++) {
                    Item item = myChar.arrItemBag[i];
                    if (item != null && item.template != null) {
                        int id = item.template.id;
                        if (id == 490 || id == 35 || id == 37) {
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return -1;
    }

    /** Find Co Lenh / Khao Di Lenh item (ID 490, 35, 37) directly in inventory bag. */
    public static Item getCoLenhInBag() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar != null && myChar.arrItemBag != null) {
                for (int i = 0; i < myChar.arrItemBag.length; i++) {
                    Item item = myChar.arrItemBag[i];
                    if (item != null && item.template != null) {
                        int id = item.template.id;
                        if (id == 490 || id == 35 || id == 37) {
                            return item;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    /** 
     * Doi khu chuan cho moi loai map (Map Ngoai, Lang Co, VDMQ, VIP...):
     * - Neu co Co Lenh / Khao Di Lenh trong hanh trang: truyen bagIndex de doi khu khong can dung gan cot.
     * - Neu co cot chuyen khu (NPC 13): dung gan NPC 13 roi doi khu.
     * - Luon gui Service.gameAA(zoneID, itemIndex) va TileMap.gameAF().
     */
    public static void doChangeZone(int zoneID) {
        try {
            // Dong dialog / popup lam nghen lenh
            try { GameCanvas.endDlg(); } catch (Exception e) {}

            int itemIndex = getCoLenhBagIndex();

            // Chi khi KHONG CO Co Lenh thi moi can dung gan NPC 13
            if (itemIndex < 0) {
                Npc npc13 = GameScr.gameAI(13);
                if (npc13 != null && npc13.statusMe != 15) {
                    if (Math.abs(npc13.cx - Char.getMyChar().cx) > 22 || Math.abs(npc13.cy - Char.getMyChar().cy) > 22) {
                        Char.gameAC(npc13.cx, npc13.cy);
                    }
                }
            }
            Service.gI().gameAA(zoneID, itemIndex);
            TileMap.gameAF();
        } catch (Exception e) {}
    }

    /** Doi khu trong Lang Co bang cach truyen Co Lenh / Khao Di Lenh qua Service.gameAA(zoneID, itemIndex) */
    public static void changeZoneLangCo(int zoneID) {
        doChangeZone(zoneID);
    }

    /** Ensures character is inside Lang Co. If currently outside, buys Co Lenh / Khao Di Lenh (ID 490/35/37) and uses it to enter Lang Co. */
    public static boolean ensureInLangCo() {
        Char.MuaCoLenh = true;
        Char.DungCoLenh = true;

        // Dam bao graph map Lang Co duoc thiet lap
        restoreLangCoGraph();

        // Neu da o trong Lang Co -> KHONG mua / KHONG useItem lai!
        if (TileMap.isLangCo(TileMap.mapID)) return true;

        // 1. Tim Co Lenh / Khao Di Lenh (ID 490, 35, 37) bang ca 2 cach
        Item item = findCoLenhItem();

        // 2. Neu chua co -> Mua tu Shop Goshu (14, 29, 2)
        if (item == null) {
            GameScr.gameAC("LC: Mua C\u1ed5 L\u1ec7nh...");
            try {
                GameScr.gameAB(4, 0, 0);
                Service.gI().gameAB(14, 29, 2);
                LockGame.gameAG();
            } catch (Exception e) {}

            // Cho server update inventory (retry tim item toi da 5 lan, 500ms/lan)
            for (int retry = 0; retry < 5; retry++) {
                sleep(500L);
                item = findCoLenhItem();
                if (item != null) break;
            }

            if (item == null) {
                // Thu mua tu Tabemono NPC (9, 6, 1) neu Goshu ko co
                try {
                    GameScr.gameAB(4, 0, 0);
                    Service.gI().gameAB(9, 6, 1);
                    LockGame.gameAG();
                } catch (Exception e) {}
                sleep(1000L);
                item = findCoLenhItem();
            }
        }

        if (item == null) {
            GameScr.gameAC("LC: Kh\u00f4ng t\u00ecm th\u1ea5y C\u1ed5 L\u1ec7nh!");
            return false;
        }

        // 3. Dung Co Lenh de vao Lang Co
        GameScr.gameAC("LC: D\u00f9ng C\u1ed5 L\u1ec7nh v\u00e0o L\u00e0ng C\u1ed5...");
        for (int attempt = 0; attempt < 3; attempt++) {
            if (TileMap.isLangCo(TileMap.mapID)) return true;

            // Dong moi dialog truoc khi dung item
            try { GameCanvas.endDlg(); } catch (Exception e) {}
            sleep(200L);

            // Tim lai item (co the indexUI thay doi sau mua)
            item = findCoLenhItem();
            if (item == null) break;

            try {
                Service.gI().useItem(item.indexUI);
                TileMap.gameAF();
            } catch (Exception e) {}

            // Cho vao Lang Co (toi da 8s moi lan thu)
            for (int i = 0; i < 80; i++) {
                sleep(100L);
                if (TileMap.isLangCo(TileMap.mapID)) {
                    GameScr.gameAC("LC: \u0110\u00e3 v\u00e0o L\u00e0ng C\u1ed5! (M" + TileMap.mapID + ")");
                    return true;
                }
            }
            GameScr.gameAC("LC: Ch\u01b0a v\u00e0o \u0111\u01b0\u1ee3c, th\u1eed l\u1ea1i l\u1ea7n " + (attempt + 2) + "...");
        }

        return TileMap.isLangCo(TileMap.mapID);
    }

    /** Tim Co Lenh / Khao Di Lenh bang ca Char.gameAF (giong game goc) va scan arrItemBag. */
    private static Item findCoLenhItem() {
        Item item = null;
        // Cach 1: Dung Char.gameAF (game engine) - chinh xac nhat
        try {
            item = Char.gameAF(490);
            if (item == null) item = Char.gameAF(37);
            if (item == null) item = Char.gameAF(35);
        } catch (Exception e) {}
        // Cach 2: Fallback scan truc tiep arrItemBag
        if (item == null) {
            item = getCoLenhInBag();
        }
        return item;
    }

    // Trang thai hien tai
    private int currentBossType = -1;
    private int currentMapIndex = 0;
    private int lastDeathMapID = -1;
    private int lastDeathZoneID = -1;
    // Track map Lang Co nao da quet trong 1 phien huntBossType
    private boolean langCoScanned135 = false;
    private boolean langCoScanned136 = false;

    private static boolean checkHasPartyOrFriends() {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) return true;
            if (Code.gameAI != null && Code.gameAI.size() > 0) return true;
            if (GameScr.vFriend != null && GameScr.vFriend.size() > 0) return true;
        } catch (Exception e) {}
        return false;
    }

    /**
     * tspkb - Toggle san boss tu dong theo lich
     */
    public static void toggle() {
        toggleInternal(checkHasPartyOrFriends(), -1);
    }

    /**
     * tspkbsv - San boss Server (M3) ngay lap tuc
     */
    /**
     * tspkbvm - San boss VDMQ (M141-143) ngay lap tuc
     */
    public static void toggleVM() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_VDMQ);
    }

    /**
     * tspkbmn - San boss MapNgoai (12 maps) ngay lap tuc
     */
    public static void toggleMN() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_MAPNGOAI);
    }

    /**
     * tspkblangco / langco - San boss LangCo (M135-136) ngay lap tuc
     */
    public static void toggleLangCo() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_LANGCO);
    }

    /**
     * tspkbtg - San boss TheGioi (M20) ngay lap tuc
     */
    public static void toggleTheGioi() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_THEGIOI);
    }

    /**
     * tspkbmv - San boss Map VIP (M195) ngay lap tuc
     */
    public static void toggleMapVIP() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_MAPVIP);
    }

    public static void toggleMapVIP2() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_MAPVIP2);
    }

    /**
     * tspkball - San TAT CA boss 24/24 nguyen ngay
     */
    public static void toggleALL() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_ALL);
    }

    /**
     * tstreo / treo - Tim ALL boss nhung KHONG danh, chi goi nhom roi dung cho.
     */
    public static void toggleTreo() {
        toggleTreoInternal(TYPE_ALL);
    }

    /** treovm - Treo boss VDMQ */
    public static void toggleTreoVM() {
        toggleTreoInternal(TYPE_VDMQ);
    }

    /** treomn - Treo boss MapNgoai */
    public static void toggleTreoMN() {
        toggleTreoInternal(TYPE_MAPNGOAI);
    }

    /** treolangco - Treo boss LangCo */
    public static void toggleTreoLangCo() {
        toggleTreoInternal(TYPE_LANGCO);
    }

    /** treotg - Treo boss TheGioi */
    public static void toggleTreoTheGioi() {
        toggleTreoInternal(TYPE_THEGIOI);
    }

    /** treomv - Treo boss MapVIP */
    public static void toggleTreoMapVIP() {
        toggleTreoInternal(TYPE_MAPVIP);
    }

    public static void toggleTreoMapVIP2() {
        toggleTreoInternal(TYPE_MAPVIP2);
    }

    private static void toggleTreoInternal(int bossType) {
        if (isRunning) {
            toggleInternal(true, -1); // tat
        } else {
            treoMode = true;
            toggleInternal(true, bossType);
        }
    }

    public static void toggleParty() {
        toggleInternal(true, -1);
    }

    public static void startEventHunt() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        toggleInternal(true, -1);
    }

    public static void startEventHuntAll() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        toggleInternal(true, TYPE_ALL);
    }

    public static void stopEventHunt() {
        eventHuntMode = false;
        stop();
    }

    /** TS Boss chi san VDMQ + Lang Co */
    public static void startEventHuntVdmqLc() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        // Override HUNT_PRIORITY TRUOC khi start thread (tranh race condition)
        eventHuntTypes = new int[]{TYPE_LANGCO, TYPE_VDMQ};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san MapNgoai */
    public static void startEventHuntMN() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_MAPNGOAI};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san TheGioi */
    public static void startEventHuntTG() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_THEGIOI};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san Lang Co */
    public static void startEventHuntLC() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_LANGCO};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san VDMQ */
    public static void startEventHuntVDMQ() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_VDMQ};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san MapVIP */
    public static void startEventHuntMapVIP() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_MAPVIP};
        toggleInternal(true, TYPE_ALL);
    }

    public static void startEventHuntMapVIP2() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = new int[]{TYPE_MAPVIP2};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss Test: quet dung 1 map test duy nhat */
    public static void startEventHuntTest1Map(int mapId) {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        testMapId = mapId;
        toggleInternal(true, TYPE_TEST_1MAP);
    }

    public static boolean consumeEventRoundCompleted() {
        if (!eventRoundCompleted) return false;
        eventRoundCompleted = false;
        return true;
    }

    /** Nhan pke tu truong nhom; pop PkBoss roi tat holder/thread thanh vien. */
    public static void stopPartyBoss() {
        Code.gameAC();
        if (AutoSanBoss.isRunning && AutoSanBoss.treoMode) {
            // Treo mode: chi pop PkBoss, giu thread song de dung cho
            AutoSanBoss.restoreDummyAuto();
        } else {
            AutoSanBoss.stop();
        }
    }

    /** Bat che do thanh vien thuong khi nhan pkm -1. */
    public static void startPartyMember() {
        if (!isRunning) {
            treoMode = false;
            toggleInternal(true, -1);
        }
    }

    /** Ep thanh vien ve che do DANH khi nhan pkm -1. */
    public static void startPartyMemberNormal() {
        treoMode = false;
        if (!isRunning) {
            toggleInternal(true, -1);
        }
    }

    /** Dung han AutoSanBoss thanh vien khi nhan pkm -3. */
    public static void stopPartyMemberFully() {
        if (Code.gameAB instanceof PkBoss) {
            try { Code.gameAC(); } catch (Exception e) {}
        }
        stop();
    }

    /** Bat che do thanh vien TREO khi nhan pkm -2. */
    public static void startPartyMemberTreo() {
        if (!isRunning) {
            treoMode = true;
            toggleInternal(true, -1);
        } else {
            treoMode = true;
        }
    }

    /**
     * Thanh vien treo: PkBoss chi dua toi map. Khi toi map thi pop PkBoss,
     * sau do tu doi khu bang Auto.gameAA de khong tele/khong danh boss.
     */
    public static void setPartyBossZone(final int zone) {
        if (!isRunning || !treoMode) {
            if (Code.gameAB != null) Code.gameAB.zoneID = zone;
            return;
        }

        memberTargetZone = zone;
        final Auto travelAuto = Code.gameAB;
        if (!(travelAuto instanceof PkBoss)) {
            doChangeZone(zone);
            return;
        }
        final int targetMap = travelAuto.mapID;

        memberMoveThread = new Thread(new Runnable() {
            public void run() {
                // PkBoss chi dua nhan vat toi map; poll nhanh de pop truoc khi danh.
                for (int i = 0; i < 3000 && isRunning && treoMode; i++) {
                    if (TileMap.mapID == targetMap) break;
                    sleep(10);
                }

                if (!isRunning || !treoMode || TileMap.mapID != targetMap) return;
                if (Code.gameAB == travelAuto) {
                    try { Code.gameAC(); } catch (Exception e) {}
                }
                restoreDummyAuto();

                int targetZone = memberTargetZone;
                doChangeZone(targetZone);
                for (int i = 0; i < 300 && isRunning && treoMode && TileMap.zoneID != targetZone; i++) {
                    sleep(10);
                }
                GameScr.gameAC("TREO: Da toi M" + targetMap + " K" + targetZone + ", dung cho!");
            }
        });
        memberMoveThread.start();
    }

    /**
     * @param forcedType -1 = auto schedule, 0-3 = force boss type
     */
    private static void toggleInternal(boolean partyMode, int forcedType) {
        if (isRunning) {
            isRunning = false;
            isPartyMode = false;
            treoMode = false;
            forcedBossType = -1;
            if (Code.gameAB == dummyAuto) {
                Code.gameAB = null;
            }
            dummyAuto = null;
            // Gui lenh tat cho nhom
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                try {
                    Service.gI().gameAK("pkm -3");
                    Service.gI().gameAK("pke");
                } catch (Exception e) {}
            }
            GameScr.gameAC("T\u1eaft T\u1ef1 S\u0103n Boss!");
        } else {
            isRunning = true;
            isPartyMode = partyMode;
            forcedBossType = forcedType;
            dummyAuto = new SanBossHolder();
            Code.gameAB = dummyAuto;
            AutoSanBoss instance = new AutoSanBoss();
            thread = new Thread(instance);
            thread.start();
            // Thong bao
            if (treoMode) {
                GameScr.gameAC("B\u1eadt TREO Boss! T\u00ecm boss → g\u1ecdi nh\u00f3m → \u0111\u1ee9ng ch\u1edd!");
            } else {
                String modeName = partyMode ? " NH\u00d3M" : "";
                if (forcedType >= 0) {
                    GameScr.gameAC("S\u0103n " + BOSS_NAMES[forcedType] + modeName + " ngay!");
                } else if (partyMode) {
                    GameScr.gameAC("B\u1eadt S\u0103n Boss NH\u00d3M!");
                } else {
                    GameScr.gameAC("B\u1eadt T\u1ef1 S\u0103n Boss!");
                }
            }
            // Gui pkm va moi ban be vao nhom
            if (partyMode) {
                boolean isLeader = false;
                try {
                    Char myChar = Char.getMyChar();
                    if (GameScr.vParty != null && GameScr.vParty.size() > 0) {
                        Party first = (Party) GameScr.vParty.firstElement();
                        if (first != null && myChar != null && first.charId == myChar.charID) {
                            isLeader = true;
                        }
                    } else {
                        isLeader = true; // Chua co nhom -> Tu moi (se thanh nhom truong)
                    }
                } catch (Exception e) {}
                
                if (isLeader && !eventHuntMode) {
                    // Moi ban be / thanh vien chua co trong nhom (KHONG roi nhom hien tai)
                    autoInviteFriends();
                    
                    // Gui pkm de khoi dong member (du da co nhom hay chua)
                    try { Service.gI().gameAK("pkm " + (treoMode ? -2 : -1)); } catch (Exception e) {}
                }
            }
        }
    }

    public static void stop() {
        if (isRunning) {
            isRunning = false;
            isPartyMode = false;
            treoMode = false;
            forcedBossType = -1;
            eventHuntTypes = null;
            if (Code.gameAB == dummyAuto) {
                Code.gameAB = null;
            }
            dummyAuto = null;
            // Khong can gui pke vi thao tac nay co the la tat ca nhan
        }
    }

    /**
     * Gui lenh party chat neu dang o che do nhom
     */
    private void sendPartyCommand(String cmd) {
        if (!isPartyMode) return;
        if (GameScr.vParty.size() <= 1) return;
        try {
            Service.gI().gameAK(cmd);
        } catch (Exception e) {}
    }

    /**
     * Kiem tra AutoSanBoss con dang chay khong.
     * Detect ca truong hop user nhan "Tat Auto" tu menu (Code.gameAF() set gameAB = null).
     */
    private boolean checkStillRunning() {
        if (!isRunning) return false;
        return true;
    }

    /**
     * Dam bao SanBossHolder luon ton tai de giu menu "Tat Auto".
     * Phuc hoi khi gameAB bi null HOAC bi ghi de boi auto khac (khong phai PkBoss).
     * PkBoss duoc giu nguyen vi no la phan cua flow san boss nhom.
     */
    public static void restoreDummyAuto() {
        if (!isRunning || dummyAuto == null) return;
        Auto current = Code.gameAB;
        if (current == null) {
            // gameAB bi null (bi pe, hoac auto khac tat)
            Code.gameAB = dummyAuto;
        } else if (current != dummyAuto && !(current instanceof PkBoss)) {
            // gameAB bi ghi de boi auto khac (khong phai PkBoss)
            // Giu lai reAB chain: dummyAuto.reAB = current (de pop dung)
            dummyAuto.reAB = current;
            Code.gameAB = dummyAuto;
        }
    }

    /**
     * Kiem tra mat ket noi: Char chua load hoac session chua san sang
     */
    private boolean isDisconnected() {
        try {
            Char c = Char.getMyChar();
            if (c == null || c.cName == null) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Cho game tu reconnect (Char.ReConnect xu ly).
     * Tra ve true neu reconnect thanh cong, false neu timeout.
     */
    private boolean waitForReconnect(int maxWaitSec) {
        GameScr.gameAC("TSB: M\u1ea5t k\u1ebft n\u1ed1i! Ch\u1edd \u0111\u0103ng nh\u1eadp l\u1ea1i...");
        for (int i = 0; i < maxWaitSec && isRunning; i++) {
            sleep(1000);
            if (!isDisconnected()) {
                // Reconnect thanh cong, doi game load xong
                sleep(5000);
                // Khoi phuc dummyAuto de menu hien "Tat Auto"
                if (dummyAuto == null) {
                    dummyAuto = new SanBossHolder();
                }
                Code.gameAB = dummyAuto;
                GameScr.gameAC("TSB: \u0110\u00e3 k\u1ebft n\u1ed1i l\u1ea1i! Ti\u1ebfp t\u1ee5c s\u0103n boss...");
                // Gui lai pkm cho nhom neu dang party mode
                if (isPartyMode) {
                    try {
                        // Doi vParty sync tu server (toi da 3s) truoc khi quyet dinh moi lai
                        for (int ps = 0; ps < 30 && GameScr.vParty.size() <= 1; ps++) {
                            sleep(100);
                        }
                        if (GameScr.vParty.size() > 1) {
                            // Nhom van con -> chi gui pkm de sync member
                            Service.gI().gameAK("pkm " + TileMap.mapID);
                        } else {
                            // Party that su bi giai tan do disconnect -> moi lai
                            autoInviteFriends();
                        }
                    } catch (Exception e) {}
                }
                return true;
            }
        }
        GameScr.gameAC("TSB: Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i l\u1ea1i. D\u1eebng.");
        return false;
    }

    /**
     * Tu dong moi danh sach thanh vien (Code.gameAI) va ban be (GameScr.vFriend) vao nhom.
     * Su dung Service.gI().gameAF(name) (Packet 79 - Party Invite chuan Ninja School).
     */
    private static Thread inviteThread = null;

    public static void autoInviteFriends() {
        // Cancel thread invite cu (neu dang chay) de tranh leak thread
        if (inviteThread != null && inviteThread.isAlive()) {
            inviteThread.interrupt();
            inviteThread = null;
        }
        inviteThread = new Thread(new Runnable() {
            public void run() {
                try {
                    // === Dung DUNG co che cua lenh "pt" trong Code.java ===

                    // Check nhom truong: Code.gameAH = ten nhom truong da luu bang "sn"
                    String myName = Char.getMyChar() != null ? Char.getMyChar().cName : "";
                    if (Code.gameAH != null && Code.gameAH.length() > 0
                            && !myName.equals(Code.gameAH)) {
                        GameScr.gameAC("B\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                        return;
                    }

                    // Check co danh sach thanh vien da luu khong
                    if (Code.gameAI == null || Code.gameAI.size() == 0) {
                        GameScr.gameAC("Ch\u01b0a l\u01b0u nh\u00f3m! D\u00f9ng 'addn' th\u00eam t\u1eebng ng\u01b0\u1eddi ho\u1eb7c 'sn' l\u01b0u nh\u00f3m hi\u1ec7n t\u1ea1i.");
                        return;
                    }

                    int invitedCount = 0;
                    // Duyet gameAI, check Code.gameAD(name) = da trong party chua
                    for (int i = 0; i < Code.gameAI.size(); i++) {
                        String name = (String) Code.gameAI.elementAt(i);
                        if (name == null || name.length() == 0) continue;
                        // Code.gameAD(name) = true neu name DA trong party -> skip
                        if (!Code.gameAD(name)) {
                            Service.gI().gameAF(name);
                            invitedCount++;
                            sleep(300);
                        }
                    }

                    if (invitedCount > 0) {
                        GameScr.gameAC("\u0110\u00e3 m\u1eddi " + invitedCount + " ng\u01b0\u1eddi v\u00e0o nh\u00f3m!");
                    } else {
                        GameScr.gameAC("Nh\u00f3m \u0111\u00e3 \u0111\u1ee7 ng\u01b0\u1eddi!");
                    }
                } catch (Exception e) {}
            }
        });
        inviteThread.start();
    }


    private static boolean isAlreadyInParty(String name) {
        try {
            if (GameScr.vParty != null) {
                for (int p = 0; p < GameScr.vParty.size(); p++) {
                    Party partyMember = (Party) GameScr.vParty.elementAt(p);
                    if (partyMember != null && partyMember.name != null && partyMember.name.equals(name)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Tach do le tu Tu do (arrItemBox) hoac Hanh trang (arrItemBag).
     * Tach thanh tung mon le (so luong 1) cho den 'count' lan.
     */
    public static void tachDoLeById(final int itemId, final int count) {
        if (count <= 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar == null) return;

                    Item item = null;
                    int tUI = 3;
                    int idxUI = -1;

                    // 1. Tim trong Hanh trang
                    if (myChar.arrItemBag != null) {
                        for (int i = 0; i < myChar.arrItemBag.length; i++) {
                            if (myChar.arrItemBag[i] != null && myChar.arrItemBag[i].template != null && myChar.arrItemBag[i].template.id == itemId) {
                                item = myChar.arrItemBag[i];
                                tUI = 3;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    // 2. Tim trong Tu do
                    if (item == null && myChar.arrItemBox != null) {
                        for (int i = 0; i < myChar.arrItemBox.length; i++) {
                            if (myChar.arrItemBox[i] != null && myChar.arrItemBox[i].template != null && myChar.arrItemBox[i].template.id == itemId) {
                                item = myChar.arrItemBox[i];
                                tUI = 4;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    if (item == null || idxUI < 0) {
                        GameScr.gameAC("TSB: Kh\u00f4ng t\u00ecm th\u1ea5y v\u1eadt ph\u1ea9m c\u00f3 ID " + itemId + "!");
                        return;
                    }

                    final int typeUI = tUI;
                    final int indexUI = idxUI;

                    String itemName = item.template.name;
                    GameScr.gameAC("TSB: \u0110ang t\u00e1ch l\u1ebb " + count + " m\u00f3n " + itemName + "...");

                    for (int i = 0; i < count; i++) {
                        Service.gI().gameAA(typeUI, indexUI, 1);
                        Thread.sleep(150);
                    }

                    GameScr.gameAC("TSB: \u0110\u00e3 t\u00e1ch xong " + count + " m\u00f3n l\u1ebb " + itemName + "!");
                } catch (Exception e) {
                    GameScr.gameAC("TSB: L\u1ed7i khi t\u00e1ch \u0111\u1ed3!");
                }
            }
        }).start();
    }

    public static void tachDoLe(final int count) {
        if (count <= 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar == null) return;

                    Item item = null;
                    int tUI = 3;
                    int idxUI = -1;

                    // 1. Uu tien chon tu Hanh trang (Bag = 3)
                    if (myChar.arrItemBag != null && GameScr.gameBM >= 0 && GameScr.gameBM < myChar.arrItemBag.length && myChar.arrItemBag[GameScr.gameBM] != null) {
                        item = myChar.arrItemBag[GameScr.gameBM];
                        tUI = 3;
                        idxUI = item.indexUI;
                    } 
                    // 2. Kiem tra trong Tu do (Box = 4) neu dang chon
                    else if (myChar.arrItemBox != null && GameScr.gameBM >= 0 && GameScr.gameBM < myChar.arrItemBox.length && myChar.arrItemBox[GameScr.gameBM] != null) {
                        item = myChar.arrItemBox[GameScr.gameBM];
                        tUI = 4;
                        idxUI = item.indexUI;
                    }

                    // 3. Fallback: tim item dau tien trong Hanh trang
                    if (item == null && myChar.arrItemBag != null) {
                        for (int i = 0; i < myChar.arrItemBag.length; i++) {
                            if (myChar.arrItemBag[i] != null) {
                                item = myChar.arrItemBag[i];
                                tUI = 3;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    // 4. Fallback: tim item dau tien trong Tu do
                    if (item == null && myChar.arrItemBox != null) {
                        for (int i = 0; i < myChar.arrItemBox.length; i++) {
                            if (myChar.arrItemBox[i] != null) {
                                item = myChar.arrItemBox[i];
                                tUI = 4;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    if (idxUI < 0) {
                        GameScr.gameAC("TSB: H\u00e3y ch\u1ecdn v\u1eadt ph\u1ea9m trong T\u1ee7 \u0111\u1ed3 ho\u1eb7c H\u00e0nh trang \u0111\u1ec3 t\u00e1ch!");
                        return;
                    }

                    final int typeUI = tUI;
                    final int indexUI = idxUI;

                    String itemName = item.template != null ? item.template.name : "v\u1eadt ph\u1ea9m";
                    GameScr.gameAC("TSB: \u0110ang t\u00e1ch l\u1ebb " + count + " m\u00f3n " + itemName + "...");

                    for (int i = 0; i < count; i++) {
                        Service.gI().gameAA(typeUI, indexUI, 1);
                        sleep(150); // Delay 150ms giua cac lan tach 1 mon
                    }

                    GameScr.gameAC("TSB: \u0110\u00e3 t\u00e1ch xong " + count + " m\u00f3n l\u1ebb " + itemName + "!");
                } catch (Exception e) {
                    GameScr.gameAC("TSB: L\u1ed7i khi t\u00e1ch \u0111\u1ed3!");
                }
            }
        }).start();
    }

    /**
     * Hoi sinh nhanh: dong dialog, gui packet hoi sinh chuan game goc
     * (copy logic tu Auto.gameAA(boolean) trong Auto.java)
     * - gameAL() = hoi sinh luong (tai cho) — UU TIEN khi san boss
     * - gameAK() = hoi sinh ve lang (fallback)
     */
    private void respawnFast() {
        for (int retry = 0; retry < 10 && isRunning; retry++) {
            if (isDisconnected()) return;
            try {
                GameCanvas.endDlg();
                sleep(20);
                // Clear state giong Auto.gameAA(boolean)
                Auto.gameAN.removeAllElements();
                Auto.gameAM = false;
                LockGame.gameAA = true;
                // Gui packet hoi sinh chuan game goc
                if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                    Service.gI().gameAL();  // Hoi sinh luong (tai cho)
                } else {
                    Service.gI().gameAK();  // Hoi sinh ve lang
                    TileMap.gameAF();       // Refresh map
                }
                LockGame.gameAA = false;
                sleep(300);
                if (!isDead()) {
                    return;
                }
            } catch (Exception e) {
                return;
            }
            sleep(200);
        }
    }

    /**
     * Di chuyen vao map cu the trong Lang Co (M135 hoac M136).
     */
    private boolean enterLangCoSpecificMap(int targetMap) {
        if (!checkStillRunning()) return false;
        if (TileMap.mapID == targetMap && !isDead()) return true;

        if (isDead()) {
            respawnFast();
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }
        }

        if (!ensureInLangCo()) return false;

        for (int retry = 0; retry < 25 && checkStillRunning(); retry++) {
            if (TileMap.mapID == targetMap && !isDead()) return true;
            if (isDead()) {
                respawnFast();
                if (!ensureInLangCo()) return false;
            }
            if (TileMap.mapID != 138 && TileMap.mapID != targetMap) {
                returnToLangCoHub();
            }
            if (TileMap.mapID == 138) {
                try {
                    TileMap.gameAJ(0);
                    TileMap.gameAF();
                } catch (Exception e) {}
                for (int w = 0; w < 30 && checkStillRunning() && TileMap.mapID == 138; w++) {
                    sleep(50);
                    if (isDead()) {
                        respawnFast();
                        break;
                    }
                }
                sleep(50);
            }
            if (TileMap.mapID == targetMap && !isDead()) return true;
        }
        return TileMap.mapID == targetMap && !isDead();
    }

    /**
     * Dieu huong den map chi dinh (ho tro Map VIP 1, Map VIP 2, Lang Co, va Map thuong).
     * Tu dong hoi sinh va di tiep neu bi quai / nguoi danh chet tren duong.
     */
    private boolean navigateToMap(int mapID) {
        if (!checkStillRunning()) return false;

        // 1. Thoat cac map gated/dac thu neu map dich khong nam cung khu vuc
        if (TileMap.isLangCo(TileMap.mapID) && !TileMap.isLangCo(mapID)) {
            finishLangCoAndExit();
        }
        if ((TileMap.mapID == 195 || TileMap.mapID == 196) && mapID != 195 && mapID != 196) {
            suicideAndEnsureAlive();
        }
        if (isVDMQ(TileMap.mapID) && !isVDMQ(mapID)) {
            finishVDMQAndExit();
        }

        if (mapID == 195) {
            if (TileMap.mapID == 195 && !isDead()) return true;
            if (isDead()) respawnFast();
            return enterMapVIP();
        } else if (mapID == 196) {
            if (TileMap.mapID == 196 && !isDead()) return true;
            if (isDead()) respawnFast();
            return enterMapVIP2();
        } else if (mapID == 135 || mapID == 136) {
            if (TileMap.mapID == mapID && !isDead()) return true;
            if (isDead()) respawnFast();
            return enterLangCoSpecificMap(mapID);
        } else {
            // Map thuong (VDMQ, Map Ngoai, The Gioi...)
            if (TileMap.mapID == mapID && !isDead()) return true;
            if (isDead()) respawnFast();

            // Dam bao PkBoss cu khong con chay ngam gay xung dot quet khu
            if (Code.gameAB instanceof PkBoss) {
                Code.gameAC();
            }
            restoreDummyAuto();

            for (int attempt = 0; attempt < 30 && checkStillRunning() && TileMap.mapID != mapID; attempt++) {
                if (isDead()) {
                    GameScr.gameAC("TSB: Ch\u1ebft khi \u0111i \u0111\u01b0\u1eddng \u0111\u1ebfn M" + mapID + "! H\u1ed3i sinh...");
                    respawnFast();
                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }
                }

                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                }

                if (TileMap.mapID == mapID && !isDead()) break;

                // Dung TileMap.GoMap(mapID) truc tiep — KHONG set PkBoss vao Code.gameAB
                try {
                    TileMap.GoMap(mapID);
                } catch (Exception e) {}

                // Cho chuyen map (toi da 3s moi buoc)
                for (int w = 0; w < 30 && checkStillRunning() && TileMap.mapID != mapID; w++) {
                    sleep(100);
                    if (isDead() || isDisconnected()) break;
                }
            }

            if (Code.gameAB instanceof PkBoss) {
                Code.gameAC();
            }
            restoreDummyAuto();
            return TileMap.mapID == mapID && !isDead();
        }
    }


    /**
     * Lay danh sach map ID cho boss MapNgoai dua tren level nhan vat
     */
    public static int[] getMapNgoaiMaps() {
        // Boss MapNgoai spawn tren 12 map (da loc)
        return new int[] {
            14, 15, 16,
            44, 67, 70,
            24, 41, 45,
            18, 36, 54
        };
    }

    public static boolean ignoreBossHourCheck = false;

    /**
     * Kiem tra khung gio spawn cho 1 loai boss.
     */
    public static boolean isBossActive(int bossType) {
        if (ignoreBossHourCheck) return true; // Khi duoc kich hoat qua Chat Notice / Force: Bo qua check gio!
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        int currentSec = h * 3600 + m * 60 + s;

        int[] hours = BOSS_HOURS[bossType];
        for (int i = 0; i < hours.length; i++) {
            int spawnSec = hours[i] * 3600;
            int diff = currentSec - spawnSec;
            if (diff >= 0 && diff < BOSS_ALIVE_DURATION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tra ve so giay con lai den gio boss tiep theo cua loai boss nay.
     * Tra ve Integer.MAX_VALUE neu khong co boss nao trong ngay.
     * Chi tinh boss CHUA spawn (spawnSec > currentSec).
     */
    public static int getSecondsTillNextBoss(int bossType) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        int currentSec = h * 3600 + m * 60 + s;

        int[] hours = BOSS_HOURS[bossType];
        int minDiff = Integer.MAX_VALUE;
        for (int i = 0; i < hours.length; i++) {
            int spawnSec = hours[i] * 3600;
            int diff = spawnSec - currentSec;
            // Chi tinh boss CHUA spawn (tuong lai)
            if (diff > 0 && diff < minDiff) {
                minDiff = diff;
            }
        }
        return minDiff;
    }

    /**
     * Tim boss type nao dang active, uu tien theo thu tu
     * Tra ve -1 neu khong co boss nao
     */
    private int findActiveBoss() {
        for (int i = 0; i < HUNT_PRIORITY.length; i++) {
            int t = HUNT_PRIORITY[i];
            if (isBossTypeEnabled(t) && isBossActive(t)) {
                return t;
            }
        }
        return -1;
    }

    /**
     * Lay map IDs cho 1 loai boss
     */
    private int[] getMapsForBoss(int bossType) {
        if (bossType == TYPE_MAPNGOAI) {
            return getMapNgoaiMaps();
        }
        if (bossType >= 0 && bossType < BOSS_MAPS.length) {
            return BOSS_MAPS[bossType];
        }
        return new int[0];
    }

    /**
     * Kiem tra tren map hien tai co boss khong
     */
    public static boolean hasBossOnCurrentMap() {
        try {
            if (GameScr.vMob == null) return false;
            for (int i = 0; i < GameScr.vMob.size(); i++) {
                Mob mob = (Mob)GameScr.vMob.elementAt(i);
                if (mob != null && mob.isBoss && mob.hp > 0 && mob.status != 0) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Ghim boss: set Char.mobFocus = boss mob de nhan vat luon danh boss.
     * Goi lien tuc moi 200ms trong luc PkBoss dang danh.
     */
    private void lockBossFocus() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            Mob boss = findBossMob();
            if (boss != null) {
                myChar.mobFocus = boss;
            }
        } catch (Exception e) {}
    }

    /** Tim boss mob trong vMob hien tai */
    public static Mob findBossMob() {
        try {
            if (GameScr.vMob == null) return null;
            for (int i = 0; i < GameScr.vMob.size(); i++) {
                Mob mob = (Mob)GameScr.vMob.elementAt(i);
                if (mob != null && mob.isBoss && mob.hp > 0 && mob.status != 0) {
                    return mob;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    /** Tim skill attack tot nhat cua nhan vat */
    private static int findBestAttackSkill(Char myChar) {
        try {
            if (myChar == null || myChar.vSkillFight == null) return -1;
            int bestId = -1;
            for (int i = 0; i < myChar.vSkillFight.size(); i++) {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                if (s.template.type == 1 || s.template.type == 3) {
                    bestId = s.template.id;
                    if (s.template.type == 3) return bestId;
                }
            }
            return bestId;
        } catch (Exception e) { return -1; }
    }

    /**
     * Ghost Attack: Danh boss tu xa xuyen toan map va lao vao dan.
     */
    public static void doBossGhostAttack() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null || myChar.statusMe == 14 || myChar.cHP <= 0) return;

            Mob boss = findBossMob();
            if (boss == null || boss.hp <= 0 || boss.status == 0) return;

            int dx = boss.x - myChar.cx;
            int dy = boss.y - myChar.cy;
            int dist = Math.abs(dx) + Math.abs(dy);
            if (dist > ghostRange) return;

            // 1. Ghim target
            myChar.mobFocus = boss;

            // 2. Chon skill danh
            int bestSkillId = findBestAttackSkill(myChar);
            if (bestSkillId >= 0) {
                try { Service.gI().gameAG(bestSkillId); } catch (Exception e) {}
            }

            // 3. Gui attack truc tiep toi boss (danh xa xuyen map)
            MyVector mobs = new MyVector();
            mobs.addElement(boss);
            MyVector chars = new MyVector();
            int atkType = bestSkillId >= 0 ? 2 : 1;
            Service.gI().gameAA(mobs, chars, atkType);

            if (ExploitConfig.isFastAttack && ExploitConfig.isActive()) {
                for (int fa = 0; fa < ExploitConfig.FAST_ATTACK_COUNT; fa++) {
                    Service.gI().gameAA(mobs, chars, atkType);
                }
            }

            // 4. Lao vao dan ve phia boss neu con o xa (buoc nhay 60px)
            if (Math.abs(dx) > 40 || Math.abs(dy) > 40) {
                int step = 60;
                int nextX = myChar.cx + (dx > 0 ? Math.min(step, dx) : Math.max(-step, dx));
                int nextY = myChar.cy + (dy > 0 ? Math.min(step, dy) : Math.max(-step, dy));
                Char.gameAC(nextX, nextY);
            }
        } catch (Exception e) {}
    }

    /**
     * Gui lenh party thong bao tim thay boss chay ngam trong background de KHONG lam cham Leader tan cong.
     */
    private void notifyPartyBossFound(final int mapID, final int bossZone) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    sendPartyCommand("pkm -1");
                    Thread.sleep(30L);
                    sendPartyCommand("pkm " + mapID);
                    Thread.sleep(50L);
                    sendPartyCommand("pkk " + bossZone);
                    Thread.sleep(500L);
                    sendPartyCommand("pke");
                } catch (Exception e) {}
            }
        }).start();
    }

    /**
     * Nhat nhanh tat ca item tren dat sau khi boss chet.
     * Cho 1.5s de do roi het, roi blast pickup 1 lan.
     */
    private void grabAllItems() {
        sleep(1500);
        AutoPickup.grabOnce();
    }

    /**
     * Doi cho den khi chuyen map xong
     */
    private boolean waitForMap(int targetMapID, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (isRunning && TileMap.mapID != targetMapID) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            sleep(200);
        }
        return isRunning && TileMap.mapID == targetMapID;
    }

    /**
     * Doi cho den khi chuyen khu xong
     */
    private boolean waitForZone(int targetZone, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (isRunning && TileMap.zoneID != targetZone) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            sleep(50);
        }
        // Doi mob load nhanh
        sleep(100);
        return isRunning;
    }

    /**
     * Navigate den dung map boss Lang Co (135/136).
     * Tu M138, chay ra cong se random vao 134/135/136/137.
     *
     * Dung TileMap.gameAJ(0) thay GoMap — di den waypoint exit 1 lan,
     * KHONG pathfind, KHONG chay nguoc. GoMap chay thread BFS rieng nen
     * khi vao sai map no tu dong neck quay lai (bug 2 lan neck).
     * gameAJ chi di den cong + Service.gameAC() roi dung.
     */
    private boolean navigateToLangCoMap(int targetMap) {
        for (int retry = 0; retry < 10 && checkStillRunning(); retry++) {
            if (TileMap.mapID == targetMap) return true;

            // Ensure in Lang Co
            if (!TileMap.isLangCo(TileMap.mapID)) {
                ensureInLangCo();
                if (!TileMap.isLangCo(TileMap.mapID)) return false;
                if (TileMap.mapID == targetMap) return true;
            }

            // Phai o M138 truoc
            if (TileMap.mapID != 138) {
                // Tu map sai -> di ra cong de ve M138 (1 neck)
                try {
                    TileMap.gameAJ(0);
                    TileMap.gameAF();
                } catch (Exception e) {}
                for (int w = 0; w < 100 && checkStillRunning() && TileMap.mapID != 138; w++) {
                    sleep(100);
                }
                if (TileMap.mapID != 138) continue;
                sleep(500);
            }

            // Tu M138: di den cong exit (gameAJ = 1 neck duy nhat)
            GameScr.gameAC("TSB: Neck M" + targetMap + " (l\u1ea7n " + (retry + 1) + ")");
            try {
                TileMap.gameAJ(0);
                TileMap.gameAF();
            } catch (Exception e) {}

            // Cho vao phong random (toi da 10s)
            for (int w = 0; w < 100 && checkStillRunning() && TileMap.mapID == 138; w++) {
                sleep(100);
            }

            // Cho map load 1s
            sleep(1000);

            // Check ngay trong phong
            if (TileMap.mapID == targetMap) {
                GameScr.gameAC("TSB: \u0110\u00e3 v\u00e0o M" + targetMap + "!");
                return true;
            }

            if (TileMap.mapID == 138) {
                GameScr.gameAC("TSB: Ch\u01b0a ra \u0111\u01b0\u1ee3c c\u1ed5ng, th\u1eed l\u1ea1i...");
            } else {
                GameScr.gameAC("TSB: V\u00e0o nh\u1ea7m M" + TileMap.mapID + ", quay v\u1ec1...");
            }
        }
        return TileMap.mapID == targetMap;
    }


    /** Restore duong nguoc 134..137 -> 138 de GoMap(138) va zone change hoat dong. */
    public static void restoreLangCoGraph() {
        try {
            if (TileMap.gameBZ != null) {
                // M138 exit ra 4 map
                if (TileMap.gameBZ.length > 138) {
                    TileMap.gameBZ[138] = new short[] {134, 135, 136, 137};
                }
                // 4 map quay ve M138
                for (int m = 134; m <= 137 && m < TileMap.gameBZ.length; m++) {
                    TileMap.gameBZ[m] = new short[] {138};
                }
            }
        } catch (Exception e) {}
    }

    private boolean returnToLangCoHub() {
        if (TileMap.mapID == 138) return true;
        if (!TileMap.isLangCo(TileMap.mapID)) return false;
        try {
            if (TileMap.vGo != null && TileMap.vGo.size() > 0) {
                Waypoint wp = (Waypoint) TileMap.vGo.elementAt(0);
                if (wp != null) {
                    Char.gameAE(wp.minX, wp.minY);
                    Char.getMyChar().cx = wp.minX;
                    Char.getMyChar().cy = wp.minY;
                    Service.gI().gameAC(wp.minX, wp.minY);
                }
            }
            TileMap.gameAJ(0);
            TileMap.gameAF();
        } catch (Exception e) {}
        for (int w = 0; w < 30 && checkStillRunning() && TileMap.mapID != 138; w++) {
            sleep(50);
        }
        return TileMap.mapID == 138;
    }

    /**
     * Scan 1 map o che do TREO BOSS (PkBoss tu dong danh, leader khong party).
     */
    private boolean treoScanMap(int mapID) {
        if (!checkStillRunning()) return false;

        // === XU LY MAP VIP (M195) — vao qua NPC ===
        if (mapID == 195) {
            return treoScanMapVIP();
        }
        // === XU LY MAP VIP2 (M196) — vao qua NPC ===
        if (mapID == 196) {
            return treoScanMapVIP2();
        }

        // === XU LY LANG CO RIENG — quet co hoi (giong pkLangCoMap) ===
        if (mapID == 135 || mapID == 136) {
            if (langCoScanned135 && langCoScanned136) return false;
            ensureInLangCo();

            // Ket hop voi isMapEnabled de biet map nao can quet
            if (!isMapEnabled(135)) langCoScanned135 = true;
            if (!isMapEnabled(136)) langCoScanned136 = true;
            if (langCoScanned135 && langCoScanned136) return false;

            GameScr.gameAC("TREO: Qu\u00e9t L\u00e0ng C\u1ed5 (c\u01a1 h\u1ed9i)...");

            if (Code.gameAB instanceof PkBoss) {
                Code.gameAC();
            }
            restoreDummyAuto();

            for (int retry = 0; retry < 25 && checkStillRunning(); retry++) {
                if (langCoScanned135 && langCoScanned136) break;

                // 1. Phai ve M138 truoc khi qua cong
                if (TileMap.mapID != 138) {
                    if (TileMap.mapID == 135 && !langCoScanned135) {
                        // Dang o 135 chua quet -> quet luon ben duoi
                    } else if (TileMap.mapID == 136 && !langCoScanned136) {
                        // Dang o 136 chua quet -> quet luon ben duoi
                    } else {
                        returnToLangCoHub();
                    }
                }

                // 2. Tu M138: qua cong random
                if (TileMap.mapID == 138) {
                    GameScr.gameAC("TREO: Qua c\u1ed5ng LC (l\u1ea7n " + (retry + 1) + ")");
                    try { TileMap.gameAJ(0); TileMap.gameAF(); } catch (Exception e) {}
                    for (int w = 0; w < 40 && checkStillRunning() && TileMap.mapID == 138; w++) sleep(50);
                    sleep(50);
                }

                int curMap = TileMap.mapID;

                if (curMap == 135 && !langCoScanned135) {
                    GameScr.gameAC("TREO: V\u00e0o M135, qu\u00e9t boss...");
                    boolean found = scanLangCoZonesForTreo(135);
                    langCoScanned135 = true;
                    if (found) return true;
                    returnToLangCoHub();
                } else if (curMap == 136 && !langCoScanned136) {
                    GameScr.gameAC("TREO: V\u00e0o M136, qu\u00e9t boss...");
                    boolean found = scanLangCoZonesForTreo(136);
                    langCoScanned136 = true;
                    if (found) return true;
                    returnToLangCoHub();
                } else if (curMap == 134 || curMap == 137 || (curMap == 135 && langCoScanned135) || (curMap == 136 && langCoScanned136)) {
                    GameScr.gameAC("TREO: V\u00e0o M" + curMap + " (kh\u00f4ng c\u1ea7n), v\u1ec1 M138...");
                    returnToLangCoHub();
                }
            }

            GameScr.gameAC("TREO: L\u00e0ng C\u1ed5 xong");
            return false;
        }

        // === MAP THUONG (khong phai Lang Co) ===
        GameScr.gameAC("TREO: Qu\u00e9t M" + mapID + "...");

        // 1. Di chuyen den map
        if (!navigateToMap(mapID)) {
            GameScr.gameAC("TREO: Kh\u00f4ng \u0111\u1ebfn \u0111\u01b0\u1ee3c M" + mapID);
            return false;
        }

        // 2. Quet khu theo pham vi scanZoneStart -> scanZoneEnd
        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TREO: Ch\u1ebft/l\u1ea1c map khi t\u00ecm boss! H\u1ed3i sinh...");
                if (isDead()) respawnFast();
                if (!navigateToMap(mapID)) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TREO: Ch\u1ebft khi qu\u00e9t K" + zone + "! H\u1ed3i sinh...");
                if (isDead()) respawnFast();
                if (!navigateToMap(mapID)) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TREO: Boss t\u1ea1i M" + mapID + " K" + bossZone + "!");
                notifyPartyBossFound(mapID, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map
                    if (isDead() || TileMap.mapID != mapID) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TREO: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M" + mapID + ", d\u1eebng!");
                            break;
                        }
                        GameScr.gameAC("TREO: Ch\u1ebft khi theo d\u00f5i boss M" + mapID + "! H\u1ed3i sinh + quay l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        if (!navigateToMap(mapID)) return false;
                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            GameScr.gameAC("TREO: Boss M" + mapID + " K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                            grabAllItems();
                            return true;
                        }
                    }

                    restoreDummyAuto();
                    sleep(200);
                }
            }
            restoreDummyAuto();
        }

        GameScr.gameAC("TREO: Kh\u00f4ng th\u1ea5y boss M" + mapID);
        return false;
    }

    /**
     * Quet 3 khu (K0-K2) tren 1 map Lang Co cho TREO mode.
     * Khong dung PkBoss danh — chi cho boss chet (ae danh).
     * Luon quet du ca 3 khu K0-K2 (khong ap dung pham vi khu).
     * @return true neu boss da chet
     */
    private boolean scanLangCoZonesForTreo(int mapID) {
        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        for (int zone = 0; zone < 3 && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TREO: Ch\u1ebft/tho\u00e1t LC M" + mapID + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterLangCoSpecificMap(mapID)) return false;
            }

            if (TileMap.zoneID != zone) {
                doChangeZone(zone);
                for (int w = 0; w < 10 && checkStillRunning() && TileMap.zoneID != zone; w++) {
                    sleep(20);
                }
            }
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TREO: Ch\u1ebft khi qu\u00e9t LC K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterLangCoSpecificMap(mapID)) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TREO: Boss LC M" + mapID + " K" + bossZone + "!");
                notifyPartyBossFound(mapID, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map
                    if (isDead() || TileMap.mapID != mapID) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TREO: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i LC M" + mapID + ", d\u1eebng!");
                            break;
                        }
                        GameScr.gameAC("TREO: Ch\u1ebft khi theo d\u00f5i boss LC M" + mapID + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        if (!enterLangCoSpecificMap(mapID)) return false;
                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            GameScr.gameAC("TREO: Boss LC M" + mapID + " K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                            grabAllItems();
                            return true;
                        }
                    }

                    restoreDummyAuto();
                    sleep(200);
                }
            }
            restoreDummyAuto();
        }
        return false;
    }

    /**
     * Chuyen biet cho L\u00e0ng C\u1ed5: Qu\u00e9t C\u00c1 2 map boss 135 + 136 theo ki\u1ec3u c\u01a1 h\u1ed9i.
     * Qua c\u1ed5ng random \u2014 v\u00e0o \u0111\u01b0\u1ee3c map n\u00e0o (135 ho\u1eb7c 136) th\u00ec qu\u00e9t boss lu\u00f4n,
     * kh\u00f4ng m\u1ea5t th\u1eddi gian retry \u0111\u1ec3 v\u00e0o \u0111\u00fang map c\u1ee5 th\u1ec3.
     *
     * @param requestedMap map \u0111\u01b0\u1ee3c g\u1ecdi t\u1eeb huntBossType (135 ho\u1eb7c 136)
     * @return true n\u1ebfu t\u00ecm th\u1ea5y v\u00e0 \u0111\u00e1nh boss xong
     */

    // ======================== MAP VIP (M195) ========================

    /**
     * Vao Map VIP (M195) qua NPC VIP (type 47, option 4).
     * Neu dang o map gated (192, Lang Co, Tu Luyen) -> tu sat ve thon truoc.
     * @return true neu da vao M195 thanh cong
     */
    private boolean enterMapVIP() {
        if (TileMap.mapID == 195) return true;

        int curMap = TileMap.mapID;

        // Neu da o thon (M22) thi khong can tu sat, goi NPC luon
        if (curMap != 22) {
            // Thoat Lang Co truoc (neu dang o)
            if (TileMap.isLangCo(curMap)) {
                cleanKhaoDiLenh();
                sleep(300);
            }

            // Check lai — neu cleanKhaoDiLenh da dua ve thon thi skip suicide
            if (TileMap.mapID != 22) {
                // Dung moi auto dang chay
                LockGame.gameBK();
                if (Code.gameAB != null && !(Code.gameAB instanceof SanBossHolder)) {
                    Code.gameAB = null;
                }

                GameScr.gameAC("TSB: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o Map VIP...");
                try { Code.gameAN(); } catch (Exception e) {}
                // Doi nhan vat chet that su (toi da 3s)
                for (int d = 0; d < 30 && checkStillRunning(); d++) {
                    sleep(100);
                    try {
                        Char me2 = Char.getMyChar();
                        if (me2 != null && (me2.statusMe == 14 || me2.cHP <= 0)) break;
                    } catch (Exception e) {}
                }

                // Hoi sinh ve thon (Map VIP KHONG cho hoi sinh luong)
                for (int r = 0; r < 15 && checkStillRunning(); r++) {
                    try {
                        Char me = Char.getMyChar();
                        if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                        GameCanvas.endDlg();
                        sleep(20);
                        Auto.gameAN.removeAllElements();
                        Auto.gameAM = false;
                        LockGame.gameAA = true;
                        Service.gI().gameAK();  // LUON ve lang (Map VIP ko cho HSL)
                        TileMap.gameAF();
                        LockGame.gameAA = false;
                        sleep(300);
                    } catch (Exception e) { break; }
                }
                sleep(200);
            }
        }

        if (TileMap.mapID == 195) return true;

        // Goi NPC VIP (type 47, option 4) de vao M195
        GameScr.gameAC("TSB: G\u1ecdi NPC VIP v\u00e0o M195...");
        for (int retry = 0; retry < 3 && checkStillRunning(); retry++) {
            if (TileMap.mapID == 195) return true;

            try {
                GameScr.gameAB(AutoVipMap.npcType, AutoVipMap.menuOption, 0);
            } catch (Exception e) {
                GameScr.gameAC("TSB: Kh\u00f4ng g\u1ecdi \u0111\u01b0\u1ee3c NPC VIP!");
                sleep(1000);
                continue;
            }

            // Cho vao map (toi da 15s)
            for (int w = 0; w < 150 && checkStillRunning(); w++) {
                sleep(100);
                if (TileMap.mapID == 195) {
                    GameScr.gameAC("TSB: \u0110\u00e3 v\u00e0o M195!");
                    sleep(100);
                    return true;
                }
            }

            GameScr.gameAC("TSB: Th\u1eed l\u1ea1i v\u00e0o M195 (l\u1ea7n " + (retry + 2) + ")...");
            sleep(500);
        }

        if (TileMap.mapID != 195) {
            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M195!");
        }
        return TileMap.mapID == 195;
    }

    /**
     * Thoat M195 ve thon (tu sat + hoi sinh) de co the di san boss map khac.
     */
    private void exitMapVIP() {
        if (TileMap.mapID != 195) return;
        GameScr.gameAC("TSB: Tho\u00e1t M195...");
        try { Code.gameAN(); } catch (Exception e) {}
        sleep(1000);
        for (int r = 0; r < 10 && checkStillRunning(); r++) {
            try {
                Char me = Char.getMyChar();
                if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                GameCanvas.endDlg();
                sleep(20);
                LockGame.gameAA = true;
                if (Code.HoiSinhLuong && me != null && me.luong > 0) {
                    Service.gI().gameAL();
                } else {
                    Service.gI().gameAK();
                    TileMap.gameAF();
                }
                LockGame.gameAA = false;
                sleep(500);
            } catch (Exception e) { break; }
        }
        sleep(1000);
    }

    /**
     * San boss Map VIP (M195): vao qua NPC, quet khu, PkBoss danh boss.
     */
    private boolean pkBossMapVIP() {
        if (!checkStillRunning()) return false;

        // Thoat Lang Co neu dang o
        if (TileMap.isLangCo(TileMap.mapID)) {
            finishLangCoAndExit();
        }

        // 1. Vao M195 qua NPC
        if (!enterMapVIP()) return false;
        if (!checkStillRunning()) return false;

        restoreDummyAuto();

        GameScr.gameAC("TSB: Qu\u00e9t boss M195...");

        // 2. Quet khu theo pham vi scanZoneStart -> scanZoneEnd
        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != 195) {
                GameScr.gameAC("TSB: Ch\u1ebft/tho\u00e1t M195! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP()) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != 195) {
                GameScr.gameAC("TSB: Ch\u1ebft khi qu\u00e9t VIP K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP()) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = zone;
                GameScr.gameAC("TSB: Boss M195 K" + bossZone + "!");

                // 1. Ghost attack tu xa ngay lap tuc
                if (isGhostAttack) {
                    doBossGhostAttack();
                }

                // 2. Lao vao tan cong NGAY LAP TUC (0ms delay)
                try {
                    PkBoss pk = new PkBoss(195);
                    pk.zoneID = bossZone;
                    Code.gameAA(pk);
                } catch (Exception e) {}
                lockBossFocus();

                // 3. Thong bao party bat dong bo
                notifyPartyBossFound(195, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map TRUOC TIEN
                    if (isDead() || TileMap.mapID != 195) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TSB: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M195, b\u1ecf qua!");
                            break;
                        }
                        GameScr.gameAC("TSB: Ch\u1ebft khi \u0111\u00e1nh boss M195! H\u1ed3i sinh + quay l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (Code.gameAB instanceof PkBoss) {
                            Code.gameAC();
                        }
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }

                        if (!enterMapVIP()) {
                            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o l\u1ea1i \u0111\u01b0\u1ee3c M195!");
                            return false;
                        }

                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }

                        // Cho server nap mob (toi da 1s)
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }

                        restoreDummyAuto();
                        try {
                            PkBoss pk2 = new PkBoss(195);
                            pk2.zoneID = bossZone;
                            Code.gameAA(pk2);
                        } catch (Exception e2) {}
                        lockBossFocus();
                        if (isGhostAttack) {
                            doBossGhostAttack();
                        }
                        sleep(100);
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            break;
                        }
                    }

                    lockBossFocus();
                    if (isGhostAttack) {
                        doBossGhostAttack();
                    }
                    sleep(100);
                }

                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    restoreDummyAuto();

                    GameScr.gameAC("TSB: Boss M195 K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();
                    sendPartyCommand("pkm -6");
                    return true;
                }
            restoreDummyAuto();
        }

        GameScr.gameAC("TSB: Kh\u00f4ng th\u1ea5y boss M195");
        return false;
    }

    /**
     * Treo boss Map VIP (M195): vao qua NPC, quet khu, KHONG danh - chi doi boss chet.
     */
    private boolean treoScanMapVIP() {
        if (!checkStillRunning()) return false;

        // Thoat Lang Co neu dang o
        if (TileMap.isLangCo(TileMap.mapID)) {
            finishLangCoAndExit();
        }

        // 1. Vao M195 qua NPC
        if (!enterMapVIP()) return false;
        if (!checkStillRunning()) return false;

        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        GameScr.gameAC("TREO: Qu\u00e9t M195...");

        // 2. Quet khu theo pham vi scanZoneStart -> scanZoneEnd
        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != 195) {
                GameScr.gameAC("TREO: Ch\u1ebft/tho\u00e1t M195! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP()) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != 195) {
                GameScr.gameAC("TREO: Ch\u1ebft khi qu\u00e9t VIP K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP()) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TREO: Boss M195 K" + bossZone + "!");
                notifyPartyBossFound(195, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map
                    if (isDead() || TileMap.mapID != 195) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TREO: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M195, d\u1eebng!");
                            break;
                        }
                        GameScr.gameAC("TREO: Ch\u1ebft khi theo d\u00f5i boss M195! H\u1ed3i sinh + v\u00e0o l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        if (!enterMapVIP()) return false;
                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            GameScr.gameAC("TREO: Boss M195 K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                            grabAllItems();
                            return true;
                        }
                    }

                    restoreDummyAuto();
                    sleep(200);
                }
            }
            restoreDummyAuto();
        }

        GameScr.gameAC("TREO: Kh\u00f4ng th\u1ea5y boss M195");
        return false;
    }

    // ======================== MAP VIP 2 (M196 - VIP 6-7) ========================

    /**
     * Vao Map VIP 2 (M196): tu sat ve thon → goi NPC VIP (type 47, opt 5).
     */
    private boolean enterMapVIP2() {
        if (TileMap.mapID == 196) return true;

        int curMap = TileMap.mapID;

        // Neu da o thon (M22) thi goi NPC luon
        if (curMap != 22) {
            if (TileMap.isLangCo(curMap)) {
                cleanKhaoDiLenh();
                sleep(300);
            }
            if (TileMap.mapID != 22) {
                LockGame.gameBK();
                if (Code.gameAB != null && !(Code.gameAB instanceof SanBossHolder)) {
                    Code.gameAB = null;
                }
                GameScr.gameAC("TSB: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o Map VIP2...");
                try { Code.gameAN(); } catch (Exception e) {}
                // Doi nhan vat chet that su (toi da 3s)
                for (int d = 0; d < 30 && checkStillRunning(); d++) {
                    sleep(100);
                    try {
                        Char me2 = Char.getMyChar();
                        if (me2 != null && (me2.statusMe == 14 || me2.cHP <= 0)) break;
                    } catch (Exception e) {}
                }
                // Hoi sinh ve thon (Map VIP2 KHONG cho hoi sinh luong)
                for (int r = 0; r < 15 && checkStillRunning(); r++) {
                    try {
                        Char me = Char.getMyChar();
                        if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                        GameCanvas.endDlg();
                        sleep(20);
                        Auto.gameAN.removeAllElements();
                        Auto.gameAM = false;
                        LockGame.gameAA = true;
                        Service.gI().gameAK();  // LUON ve lang (Map VIP ko cho HSL)
                        TileMap.gameAF();
                        LockGame.gameAA = false;
                        sleep(300);
                    } catch (Exception e) { break; }
                }
                sleep(200);
            }
        }

        if (TileMap.mapID == 196) return true;

        // Goi NPC VIP (type 47, option 5) de vao M196
        GameScr.gameAC("TSB: G\u1ecdi NPC VIP v\u00e0o M196...");
        for (int retry = 0; retry < 3 && checkStillRunning(); retry++) {
            if (TileMap.mapID == 196) return true;

            try {
                GameScr.gameAB(AutoVipMap.npcType, AutoVipMap.menuOption + 1, 0);
            } catch (Exception e) {
                GameScr.gameAC("TSB: Kh\u00f4ng g\u1ecdi \u0111\u01b0\u1ee3c NPC VIP2!");
                sleep(1000);
                continue;
            }

            for (int w = 0; w < 150 && checkStillRunning(); w++) {
                sleep(100);
                if (TileMap.mapID == 196) {
                    GameScr.gameAC("TSB: \u0110\u00e3 v\u00e0o M196!");
                    sleep(100);
                    return true;
                }
            }

            GameScr.gameAC("TSB: Th\u1eed l\u1ea1i v\u00e0o M196 (l\u1ea7n " + (retry + 2) + ")...");
            sleep(500);
        }

        if (TileMap.mapID != 196) {
            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M196!");
        }
        return TileMap.mapID == 196;
    }

    /**
     * San boss Map VIP 2 (M196): vao qua NPC, quet khu, PkBoss danh boss.
     */
    private boolean pkBossMapVIP2() {
        if (!checkStillRunning()) return false;

        if (TileMap.isLangCo(TileMap.mapID)) {
            finishLangCoAndExit();
        }

        if (!enterMapVIP2()) return false;
        if (!checkStillRunning()) return false;

        restoreDummyAuto();

        GameScr.gameAC("TSB: Qu\u00e9t boss M196...");

        // 2. Quet khu theo pham vi scanZoneStart -> scanZoneEnd
        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != 196) {
                GameScr.gameAC("TSB: Ch\u1ebft/tho\u00e1t M196! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP2()) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != 196) {
                GameScr.gameAC("TSB: Ch\u1ebft khi qu\u00e9t VIP2 K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP2()) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = zone;
                GameScr.gameAC("TSB: Boss M196 K" + bossZone + "!");

                // 1. Ghost attack tu xa ngay lap tuc
                if (isGhostAttack) {
                    doBossGhostAttack();
                }

                // 2. Lao vao tan cong NGAY LAP TUC (0ms delay)
                try {
                    PkBoss pk = new PkBoss(196);
                    pk.zoneID = bossZone;
                    Code.gameAA(pk);
                } catch (Exception e) {}
                lockBossFocus();

                // 3. Thong bao party bat dong bo
                notifyPartyBossFound(196, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map TRUOC TIEN
                    if (isDead() || TileMap.mapID != 196) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TSB: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M196, b\u1ecf qua!");
                            break;
                        }
                        GameScr.gameAC("TSB: Ch\u1ebft khi \u0111\u00e1nh boss M196! H\u1ed3i sinh + quay l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (Code.gameAB instanceof PkBoss) {
                            Code.gameAC();
                        }
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }

                        if (!enterMapVIP2()) {
                            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o l\u1ea1i \u0111\u01b0\u1ee3c M196!");
                            return false;
                        }

                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }

                        // Cho server nap mob (toi da 1s)
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }

                        restoreDummyAuto();
                        try {
                            PkBoss pk2 = new PkBoss(196);
                            pk2.zoneID = bossZone;
                            Code.gameAA(pk2);
                        } catch (Exception e2) {}
                        lockBossFocus();
                        if (isGhostAttack) {
                            doBossGhostAttack();
                        }
                        sleep(100);
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            break;
                        }
                    }

                    lockBossFocus();
                    if (isGhostAttack) {
                        doBossGhostAttack();
                    }
                    sleep(100);
                }

                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    restoreDummyAuto();

                    GameScr.gameAC("TSB: Boss M196 K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();
                    sendPartyCommand("pkm -6");
                    return true;
                }
            restoreDummyAuto();
        }

        GameScr.gameAC("TSB: Kh\u00f4ng th\u1ea5y boss M196");
        return false;
    }

    /**
     * Treo boss Map VIP 2 (M196): vao qua NPC, quet khu, KHONG danh - chi doi boss chet.
     */
    private boolean treoScanMapVIP2() {
        if (!checkStillRunning()) return false;

        if (TileMap.isLangCo(TileMap.mapID)) {
            finishLangCoAndExit();
        }

        if (!enterMapVIP2()) return false;
        if (!checkStillRunning()) return false;

        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        GameScr.gameAC("TREO: Qu\u00e9t M196...");

        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != 196) {
                GameScr.gameAC("TREO: Ch\u1ebft/tho\u00e1t M196! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP2()) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != 196) {
                GameScr.gameAC("TREO: Ch\u1ebft khi qu\u00e9t VIP2 K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterMapVIP2()) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TREO: Boss M196 K" + bossZone + "!");
                notifyPartyBossFound(196, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map
                    if (isDead() || TileMap.mapID != 196) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TREO: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M196, d\u1eebng!");
                            break;
                        }
                        GameScr.gameAC("TREO: Ch\u1ebft khi theo d\u00f5i boss M196! H\u1ed3i sinh + v\u00e0o l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        if (!enterMapVIP2()) return false;
                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            GameScr.gameAC("TREO: Boss M196 K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                            grabAllItems();
                            return true;
                        }
                    }

                    restoreDummyAuto();
                    sleep(200);
                }
            }
            restoreDummyAuto();
        }

        GameScr.gameAC("TREO: Kh\u00f4ng th\u1ea5y boss M196");
        return false;
    }

    // ======================== LANG CO (M135-136) ========================
    private boolean pkLangCoMap(int requestedMap) {
        if (!checkStillRunning()) return false;
        // Da quet ca 2 map trong lan goi truoc (huntBossType goi 2 lan)
        if (langCoScanned135 && langCoScanned136) return false;
        ensureInLangCo();

        // Ket hop voi isMapEnabled
        if (!isMapEnabled(135)) langCoScanned135 = true;
        if (!isMapEnabled(136)) langCoScanned136 = true;

        if (langCoScanned135 && langCoScanned136) return false;

        GameScr.gameAC("TSB: Qu\u00e9t L\u00e0ng C\u1ed5 (c\u01a1 h\u1ed9i)...");

        // Tat PkBoss ng\u1ea7m tr\u01b0\u1edbc khi di chuy\u1ec3n
        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        // T\u1ed1i \u0111a 25 l\u1ea7n qua c\u1ed5ng (\u0111\u1ee7 \u0111\u1ec3 cover c\u1ea3 2 map)
        for (int retry = 0; retry < 25 && checkStillRunning(); retry++) {
            if (langCoScanned135 && langCoScanned136) break;

            // 1. Phai ve M138 truoc khi qua cong
            if (TileMap.mapID != 138) {
                if (TileMap.mapID == 135 && !langCoScanned135) {
                    // Dang o 135 chua quet -> quet luon ben duoi
                } else if (TileMap.mapID == 136 && !langCoScanned136) {
                    // Dang o 136 chua quet -> quet luon ben duoi
                } else {
                    returnToLangCoHub();
                }
            }

            // 2. Tu M138: qua cong random
            if (TileMap.mapID == 138) {
                GameScr.gameAC("TSB: Qua c\u1ed5ng LC (l\u1ea7n " + (retry + 1) + ")");
                try {
                    TileMap.gameAJ(0);
                    TileMap.gameAF();
                } catch (Exception e) {}

                for (int w = 0; w < 40 && checkStillRunning() && TileMap.mapID == 138; w++) {
                    sleep(50);
                }
                sleep(50);
            }

            int curMap = TileMap.mapID;

            // 3. Ki\u1ec3m tra map v\u1eeba v\u00e0o
            if (curMap == 135 && !langCoScanned135) {
                GameScr.gameAC("TSB: V\u00e0o M135, qu\u00e9t boss...");
                boolean found = scanLangCoZones(135);
                langCoScanned135 = true;
                if (found) return true;
                returnToLangCoHub();
            } else if (curMap == 136 && !langCoScanned136) {
                GameScr.gameAC("TSB: V\u00e0o M136, qu\u00e9t boss...");
                boolean found = scanLangCoZones(136);
                langCoScanned136 = true;
                if (found) return true;
                returnToLangCoHub();
            } else if (curMap == 134 || curMap == 137 || (curMap == 135 && langCoScanned135) || (curMap == 136 && langCoScanned136)) {
                GameScr.gameAC("TSB: V\u00e0o M" + curMap + " (kh\u00f4ng c\u1ea7n), v\u1ec1 M138...");
                returnToLangCoHub();
            }
        }

        if (!langCoScanned135 || !langCoScanned136) {
            GameScr.gameAC("TSB: L\u00e0ng C\u1ed5 h\u1ebft l\u01b0\u1ee3t th\u1eed c\u1ed5ng");
        } else {
            GameScr.gameAC("TSB: L\u00e0ng C\u1ed5 h\u1ebft boss");
        }
        return false;
    }

    /**
     * Quet 3 khu (K0-K2) tren 1 map Lang Co.
     * Neu tim thay boss -> danh, goi nhom, doi xong.
     * Luon quet du ca 3 khu K0-K2 (khong ap dung pham vi khu).
     * @return true neu boss da chet
     */
    private boolean scanLangCoZones(int mapID) {
        for (int zone = 0; zone < 3 && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TSB: Ch\u1ebft/tho\u00e1t LC M" + mapID + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterLangCoSpecificMap(mapID)) return false;
            }

            if (TileMap.zoneID != zone) {
                doChangeZone(zone);
                for (int w = 0; w < 10 && checkStillRunning() && TileMap.zoneID != zone; w++) {
                    sleep(20);
                }
            }
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TSB: Ch\u1ebft khi qu\u00e9t LC K" + zone + "! H\u1ed3i sinh + v\u00e0o l\u1ea1i...");
                if (isDead()) respawnFast();
                if (!enterLangCoSpecificMap(mapID)) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TSB: Boss LC M" + mapID + " K" + bossZone + "!");

                // 1. Ghost attack tu xa ngay lap tuc
                if (isGhostAttack) {
                    doBossGhostAttack();
                }

                // 2. Lao vao tan cong NGAY LAP TUC (0ms delay)
                try {
                    PkBoss pk = new PkBoss(mapID);
                    pk.zoneID = bossZone;
                    Code.gameAA(pk);
                } catch (Exception e) {}
                lockBossFocus();

                // 3. Thong bao party bat dong bo
                notifyPartyBossFound(mapID, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map TRUOC TIEN
                    if (isDead() || TileMap.mapID != mapID) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TSB: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i LC M" + mapID + ", b\u1ecf qua!");
                            break;
                        }
                        GameScr.gameAC("TSB: Ch\u1ebft khi \u0111\u00e1nh boss LC M" + mapID + "! H\u1ed3i sinh + quay l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (Code.gameAB instanceof PkBoss) {
                            Code.gameAC();
                        }
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }

                        if (!enterLangCoSpecificMap(mapID)) {
                            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o l\u1ea1i \u0111\u01b0\u1ee3c LC M" + mapID + "!");
                            return false;
                        }

                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 15 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }

                        // Cho server nap mob (toi da 1s)
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }

                        restoreDummyAuto();
                        try {
                            PkBoss pk2 = new PkBoss(mapID);
                            pk2.zoneID = bossZone;
                            Code.gameAA(pk2);
                        } catch (Exception e2) {}
                        lockBossFocus();
                        if (isGhostAttack) {
                            doBossGhostAttack();
                        }
                        sleep(100);
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            break;
                        }
                    }

                    lockBossFocus();
                    if (isGhostAttack) {
                        doBossGhostAttack();
                    }
                    sleep(100);
                }

                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    restoreDummyAuto();

                    GameScr.gameAC("TSB: Boss M" + mapID + " K" + bossZone + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();
                    sendPartyCommand("pkm -6");
                    return true;
                }
            restoreDummyAuto();
        }
        return false;
    }

    /**
     * Bat PkBoss tren 1 map va doi cho den khi xong.
     * PkBoss TU DONG: quet khu, tim boss, danh boss, gui lenh nhom.
     * Return true neu PkBoss da chay du lau (co danh boss).
     */
    private boolean pkBossOnMap(int mapID) {
        if (!checkStillRunning()) return false;
        if (mapID == 135 || mapID == 136) {
            return pkLangCoMap(mapID);
        } else if (mapID == 195) {
            return pkBossMapVIP();
        } else if (mapID == 196) {
            return pkBossMapVIP2();
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
            // Thoat Map VIP neu dang o (M195/M196 la gated map, PkBoss khong the thoat)
            if (TileMap.mapID == 195 || TileMap.mapID == 196) {
                GameScr.gameAC("TSB: Tho\u00e1t Map VIP \u0111\u1ec3 s\u0103n map kh\u00e1c...");
                suicideAndEnsureAlive();
            }
        }
        GameScr.gameAC("TSB: PK M" + mapID);

        // 1. Di chuyen den map
        if (!navigateToMap(mapID)) {
            GameScr.gameAC("TSB: Kh\u00f4ng \u0111\u1ebfn \u0111\u01b0\u1ee3c M" + mapID);
            return false;
        }

        // 2. Quet khu theo pham vi scanZoneStart -> scanZoneEnd voi delay zoneChangeDelayMs
        for (int zone = scanZoneStart; zone <= scanZoneEnd && checkStillRunning(); zone++) {
            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TSB: Ch\u1ebft/l\u1ea1c map khi t\u00ecm boss! H\u1ed3i sinh...");
                if (isDead()) respawnFast();
                if (!navigateToMap(mapID)) return false;
            }

            doChangeZone(zone);
            sleep(zoneChangeDelayMs);

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            if (isDead() || TileMap.mapID != mapID) {
                GameScr.gameAC("TSB: Ch\u1ebft khi qu\u00e9t K" + zone + "! H\u1ed3i sinh...");
                if (isDead()) respawnFast();
                if (!navigateToMap(mapID)) return false;
                zone--;
                continue;
            }

            if (hasBossOnCurrentMap()) {
                int bossZone = TileMap.zoneID;
                GameScr.gameAC("TSB: Boss M" + mapID + " K" + bossZone + "!");

                // 1. Ghost attack danh xa ngay lap tuc + lock focus
                if (isGhostAttack) {
                    doBossGhostAttack();
                }

                // 2. Lao vao tan cong NGAY LAP TUC (0ms delay)
                try {
                    PkBoss pk = new PkBoss(mapID);
                    pk.zoneID = bossZone;
                    Code.gameAA(pk);
                } catch (Exception e) {}
                lockBossFocus();

                // 3. Thong bao party bat dong bo
                notifyPartyBossFound(mapID, bossZone);

                int deathCount = 0;
                while (checkStillRunning()) {
                    // 1. Uu tien xu ly chet / mat ket noi / lac map TRUOC TIEN
                    if (isDead() || TileMap.mapID != mapID) {
                        deathCount++;
                        if (deathCount > 10) {
                            GameScr.gameAC("TSB: Ch\u1ebft qu\u00e1 10 l\u1ea7n t\u1ea1i M" + mapID + ", b\u1ecf qua!");
                            break;
                        }
                        GameScr.gameAC("TSB: Ch\u1ebft khi \u0111\u00e1nh boss M" + mapID + "! H\u1ed3i sinh + quay l\u1ea1i (l\u1ea7n " + deathCount + ")...");
                        if (Code.gameAB instanceof PkBoss) {
                            Code.gameAC();
                        }
                        if (isDead()) respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }

                        if (!navigateToMap(mapID)) return false;

                        doChangeZone(bossZone);
                        sleep(100);
                        for (int w2 = 0; w2 < 20 && checkStillRunning() && TileMap.zoneID != bossZone; w2++) {
                            sleep(100);
                        }

                        // Cho server nap mob (toi da 1s)
                        for (int wm = 0; wm < 10 && checkStillRunning() && !hasBossOnCurrentMap(); wm++) {
                            sleep(100);
                        }

                        restoreDummyAuto();
                        try {
                            PkBoss pk2 = new PkBoss(mapID);
                            pk2.zoneID = bossZone;
                            Code.gameAA(pk2);
                        } catch (Exception e2) {}
                        lockBossFocus();
                        if (isGhostAttack) {
                            doBossGhostAttack();
                        }
                        sleep(100);
                        continue;
                    }

                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }

                    // 2. Chi xac nhan boss chet khi con song va dang o dung map + dung khu
                    if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                        sleep(300);
                        if (!hasBossOnCurrentMap() && TileMap.zoneID == bossZone && !isDead()) {
                            break;
                        }
                    }

                    lockBossFocus();
                    if (isGhostAttack) {
                        doBossGhostAttack();
                    }
                    sleep(100);
                }

                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    restoreDummyAuto();
                    GameScr.gameAC("TSB: Xong M" + mapID);
                    grabAllItems();
                    return true;
                }
            restoreDummyAuto();
        }
        return false;
    }

    private void sleepSeconds(int seconds) {
        for (int w = 0; w < seconds && checkStillRunning(); w++) {
            sleep(1000);
            restoreDummyAuto();

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) {
                    isRunning = false;
                }
                break;
            }
        }
    }

    public void run() {
        sleep(2000);

        while (checkStillRunning()) {
            try {
                // Kiem tra disconnect dau moi vong lap
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                }

                restoreDummyAuto();

                // Kiem tra xem co phai la thanh vien nhom (khong phai truong nhom) hay khong
                boolean isMember = false;
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar != null && GameScr.vParty.size() > 1) {
                        Party first = (Party) GameScr.vParty.firstElement();
                        if (first != null && first.charId != myChar.charID) {
                            isMember = true;
                        }
                    }
                } catch (Exception e) {}

                if (isMember) {
                    // === TREO MODE cho thanh vien ===
                    // Leader gui pkm -> pkk -> doi 3s -> pke
                    // stopPartyBoss() se pop PkBoss nhung giu treo thread song
                    // -> TV tu dong dung tai map/zone boss
                    if (treoMode && Code.gameAB instanceof PkBoss) {
                        GameScr.gameAC("TREO: \u0110ang di t\u1edbi map/khu boss...");
                    }
                    // Thanh vien khong tu quet map, chi giu menu doi lenh
                    sleepSeconds(2);
                    continue;
                }

                if (forcedBossType == TYPE_ALL) {
                    // === CHE DO FORCE ALL: San theo danh sach uu tien ===
                    int[] types = (eventHuntTypes != null) ? eventHuntTypes : HUNT_PRIORITY;
                    boolean huntedAnyAll = false;

                    if (eventHuntMode) {
                        // Event mode (TS Boss Uu Tien): CHI quet loai boss DANG DEN GIO spawn
                        // Thu tu uu tien: VIP2 > VIP > LC > VDMQ > TG > MN (HUNT_PRIORITY)

                        // Neu dang dung o VIP map VA hunt ALL (khong restrict loai boss)
                        // → uu tien boss map do truoc (tranh tele vo ich)
                        if (eventHuntTypes == null) {
                            int curMapId = TileMap.mapID;
                            int standingType = -1;
                            if (curMapId == 195) standingType = TYPE_MAPVIP;
                            else if (curMapId == 196) standingType = TYPE_MAPVIP2;
                            // Quet boss map dang dung truoc (neu active)
                            if (standingType >= 0 && isBossTypeEnabled(standingType) && isBossActive(standingType)) {
                                huntedAnyAll = true;
                                huntBossType(standingType);
                            }
                            // Quet cac boss con lai theo thu tu uu tien
                            for (int i = 0; i < types.length && checkStillRunning(); i++) {
                                if (types[i] == standingType) continue; // Da quet roi
                                if (isBossTypeEnabled(types[i]) && isBossActive(types[i])) {
                                    huntedAnyAll = true;
                                    huntBossType(types[i]);
                                }
                            }
                        } else {
                            // eventHuntTypes da restrict (Le, VDMQ, VIP, etc.) → chi quet dung loai do
                            for (int i = 0; i < types.length && checkStillRunning(); i++) {
                                if (isBossTypeEnabled(types[i]) && isBossActive(types[i])) {
                                    huntedAnyAll = true;
                                    huntBossType(types[i]);
                                }
                            }
                        }
                    } else {
                        // Khong phai event mode (lenh tspkball) -> quet tat ca khong check gio
                        for (int i = 0; i < types.length && checkStillRunning(); i++) {
                            if (isBossTypeEnabled(types[i])) {
                                huntBossType(types[i]);
                            }
                        }
                        huntedAnyAll = true;
                    }

                    if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;

                    if (huntedAnyAll && checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong 1 l\u01b0\u1ee3t, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    } else if (!huntedAnyAll && checkStillRunning()) {
                        // Chua den gio boss nao trong danh sach uu tien -> doi 30s
                        GameScr.gameAC("TSB: Ch\u01b0a \u0111\u1ebfn gi\u1edd boss, \u0111\u1ee3i 30s...");
                        sleepSeconds(30);
                    }
                } else if (forcedBossType == TYPE_TEST_1MAP) {
                    // === CHE DO TEST 1 MAP: Quet duy nhat 1 map roi bao hoan thanh ===
                    GameScr.gameAC("TSB Test: Qu\u00e9t map test M" + testMapId + "...");
                    pkBossOnMap(testMapId);
                    if (checkStillRunning()) eventRoundCompleted = true;
                    if (checkStillRunning()) {
                        GameScr.gameAC("TSB Test: \u0110\u00e3 xong map test M" + testMapId + "!");
                        sleepSeconds(2);
                    }
                } else if (forcedBossType >= 0) {
                    // === CHE DO FORCE 1 LOAI ===
                    if (eventHuntMode && !isBossActive(forcedBossType)) {
                        // Event mode: boss chua den gio -> doi 30s
                        GameScr.gameAC("TSB: " + BOSS_NAMES[forcedBossType] + " ch\u01b0a \u0111\u1ebfn gi\u1edd, \u0111\u1ee3i 30s...");
                        sleepSeconds(30);
                    } else {
                        huntBossType(forcedBossType);
                        // Sau khi quet xong 1 round, doi 10s roi quet lai
                        if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;
                        if (checkStillRunning()) {
                            GameScr.gameAC("TSB: Xong " + BOSS_NAMES[forcedBossType] + ", qu\u00e9t l\u1ea1i sau 10s...");
                            sleepSeconds(10);
                        }
                    }
                } else {
                    // === CHE DO TU DONG: Quet theo lich spawn ===
                    boolean huntedAny = false;

                    for (int i = 0; i < HUNT_PRIORITY.length && checkStillRunning(); i++) {
                        int bossType = HUNT_PRIORITY[i];
                        if (!isBossTypeEnabled(bossType) || !isBossActive(bossType)) continue;

                        huntedAny = true;
                        huntBossType(bossType);
                    }

                    if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;

                    if (huntedAny && checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong 1 l\u01b0\u1ee3t, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    } else if (!huntedAny && checkStillRunning()) {
                        // Khong co boss nao -> doi 30s
                        sleepSeconds(30);
                    }
                }

            } catch (Exception e) {
                // Exception co the do disconnect
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                } else {
                    sleep(5000);
                }
            }
        }

        // Cleanup
        if (dummyAuto != null && Code.gameAB == dummyAuto) {
            Code.gameAB = null;
        }
        dummyAuto = null;
        forcedBossType = -1;
        eventHuntTypes = null;
        isRunning = false;
        GameScr.gameAC("TSB: Da dung.");
    }

    /**
     * San 1 loai boss cu the: quet tung map trong danh sach
     */
    private void huntBossType(int bossType) {
        currentBossType = bossType;
        if (bossType == TYPE_LANGCO) {
            langCoScanned135 = false;
            langCoScanned136 = false;
            Char.MuaCoLenh = true;
            Char.DungCoLenh = true;
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
        }
        int[] maps = getMapsForBoss(bossType);
        int enabledCount = countEnabledMaps(bossType);
        String prefix = treoMode ? "TREO" : "TSB";
        GameScr.gameAC(prefix + ": San " + BOSS_NAMES[bossType] + " (" + enabledCount + "/" + maps.length + " maps)");

        // Ưu tiên map đang đứng: nếu đang ở 1 trong các map boss → quét map đó trước
        // Copy mảng để tránh thay đổi BOSS_MAPS gốc
        int curMap = TileMap.mapID;
        boolean needReorder = false;
        for (int i = 1; i < maps.length; i++) {
            if (maps[i] == curMap && isMapEnabled(curMap)) {
                needReorder = true;
                break;
            }
        }
        if (needReorder) {
            int[] reordered = new int[maps.length];
            System.arraycopy(maps, 0, reordered, 0, maps.length);
            for (int i = 1; i < reordered.length; i++) {
                if (reordered[i] == curMap) {
                    int tmp = reordered[0];
                    reordered[0] = reordered[i];
                    reordered[i] = tmp;
                    break;
                }
            }
            maps = reordered;
        }

        for (int mi = 0; mi < maps.length && checkStillRunning(); mi++) {
            // Skip map bi tat trong cai dat
            if (!isMapEnabled(maps[mi])) continue;
            // Neu mode tu dong (forcedBossType == -1), check gio
            if (!eventHuntMode && forcedBossType < 0 && !isBossActive(bossType)) break;
            if (treoMode) {
                treoScanMap(maps[mi]);
            } else {
                pkBossOnMap(maps[mi]);
            }
        }

        if (bossType == TYPE_LANGCO) {
            finishLangCoAndExit();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
