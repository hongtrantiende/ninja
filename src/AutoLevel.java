/**
 * AutoLevel — Tu dong treo level tu Lv10 den 99.
 * Lenh: tslv50, tslv80, tslv99... (tslv + level muc tieu). Tat: tslv
 *
 * Logic:
 * 1. Kiem tra level hien tai cua nhan vat
 * 2. Tim map phu hop (quai chenh ~5-10 lv)
 * 3. Bat TanSat (ts) tren map do
 * 4. Moi 15 giay check level:
 *    - Len level + vuot nguong → tat ts cu, chuyen map moi, bat ts moi
 *    - Chet → doi hoi sinh (game tu xu ly) → tiep tuc
 *    - Mat mang → doi reconnect
 * 5. Dat level muc tieu → tat auto + thong bao
 *
 * Bang map co the chinh sua trong LEVEL_MAP.
 */
public class AutoLevel implements Runnable {
    public static boolean isRunning = false;
    public static int targetLevel = 90;
    private static Thread thread;
    private static int lastKnownLevel = 0;

    // === BANG MAP THEO LEVEL ===
    // Moi dong: {minLevel, mapID}
    // Nhan vat tu minLevel tro len se farm tai mapID nay
    // Cho den khi dat minLevel cua dong tiep theo thi chuyen map
    // ** CHINH SUA O DAY NEU SERVER KHAC **
    private static final int[][] LEVEL_MAP = {
        {10, 40},   // Lv10+: Map 40 (Canh dong Hiya)
        {20, 12},   // Lv20+: Map 12 (Mieu Oboko)
        {32, 14},   // Lv32+: Map 14 (Rung Aokigahara)
        {42, 41},   // Lv42+: Map 41 (Khu da do Akai)
        {47, 62},   // Lv47+: Map 62 (Hang Chi)
        {49, 44},   // Lv49+: Map 44 (Dinh Okama)
        {51, 18},   // Lv51+: Map 18 (San den Orochi)
        {54, 59},   // Lv54+: Map 59 (Mui Hone)
        {55, 24},   // Lv55+: Map 24 (Dinh Ichidai)
        {58, 53},   // Lv58+: Map 53 (Dong Tamatamo)
        {60, 139},  // Lv60+: Map 139 (Quy Son)
        {67, 140},  // Lv67+: Map 140 (Son Hai Vuc)
        {70, 54},   // Lv70+: Map 54 
        {82, 55},   // Lv82+: Map 55 
    };

    // === CONFIG ===
    private static final int CHECK_INTERVAL_MS = 15000;  // Check moi 15 giay
    private static final int MAP_CHANGE_DELAY_MS = 3000;  // Cho 3 giay khi chuyen map
    private static final int RECONNECT_TIMEOUT = 120;     // Cho toi da 2 phut reconnect

    /**
     * Bat auto level. Goi tu ChatRouter khi user chat "ts50", "ts99"...
     * @param lvTarget Level muc tieu (vd: 50, 80, 99)
     */
    public static void start(int lvTarget) {
        if (isRunning) {
            stop();
        }
        targetLevel = lvTarget;
        isRunning = true;
        lastKnownLevel = 0;
        thread = new Thread(new AutoLevel());
        thread.start();
    }

    /**
     * Tat auto level.
     */
    public static void stop() {
        isRunning = false;
        thread = null;
        // Tat ts hien tai neu dang chay
        if (Code.gameAB != null) {
            Code.gameAC();
        }
        GameScr.gameAC("T\u1eaft Auto Level!");
    }

    /**
     * Toggle tu menu.
     */
    public static void toggle() {
        if (isRunning) {
            stop();
        } else {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            start(99);
        }
    }

    /**
     * Tim map phu hop voi level hien tai.
     * Tra ve mapID cua dong cuoi cung co minLevel <= charLevel.
     */
    private static int findMapForLevel(int charLevel) {
        int bestMap = LEVEL_MAP[0][1]; // Default: map dau tien
        for (int i = 0; i < LEVEL_MAP.length; i++) {
            if (charLevel >= LEVEL_MAP[i][0]) {
                bestMap = LEVEL_MAP[i][1];
            } else {
                break;
            }
        }
        return bestMap;
    }

    /**
     * Lay ten map (de hien thong bao).
     */
    private static String getMapName(int mapID) {
        switch (mapID) {
            case 12: return "Mi\u1ebfu Oboko";
            case 14: return "R\u1eebng Aokigahara";
            case 18: return "S\u00e2n \u0111\u1ec1n Orochi";
            case 24: return "\u0110\u1ec9nh Ichidai";
            case 40: return "C\u00e1nh \u0111\u1ed3ng Hiya";
            case 44: return "\u0110\u1ec9nh Okama";
            case 52: return "R\u1eebng Kappa";
            case 53: return "\u0110\u1ed9ng Tamatamo";
            case 59: return "M\u0169i Hone";
            case 62: return "Hang Chi";
            case 139: return "Qu\u1ef7 S\u01a1n";
            case 140: return "S\u01a1n H\u1ea3i V\u1ef1c";
            default: return "Map" + mapID;
        }
    }

    /**
     * Kiem tra mat mang.
     */
    private static boolean isDisconnected() {
        try {
            Char c = Char.getMyChar();
            if (c == null || c.cName == null) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Cho reconnect.
     */
    private void waitForReconnect() {
        GameScr.gameAC("ALv: M\u1ea5t m\u1ea1ng, ch\u1edd...");
        for (int i = 0; i < RECONNECT_TIMEOUT && isRunning; i++) {
            if (!isDisconnected()) {
                GameScr.gameAC("ALv: \u0110\u00e3 k\u1ebft n\u1ed1i l\u1ea1i!");
                sleep(3000);
                return;
            }
            sleep(1000);
        }
    }

    /**
     * Bat ts (TanSat) tren map chi dinh.
     * Game TanSat se TU DONG di chuyen nhan vat den map nay
     * khi mapID != TileMap.mapID (Auto.gameAA navigation).
     */
    private void startTsOnMap(int mapID) {
        try {
            // Tat auto cu neu co
            if (Code.gameAB != null) {
                Code.gameAC();
                sleep(500);
            }

            // Bat TanSat: truyen mapID muc tieu, game se tu di chuyen
            Code.gameAA(-1, mapID);


            // Khong bat nhat do game goc (tranh tele)
        } catch (Exception e) {}
    }

    /**
     * Sleep an toan.
     */
    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    /**
     * Thread chinh.
     */
    public void run() {
        sleep(1000); // Cho game load

        Char myChar = Char.getMyChar();
        if (myChar == null) {
            GameScr.gameAC("ALv: L\u1ed7i - kh\u00f4ng t\u00ecm th\u1ea5y nh\u00e2n v\u1eadt!");
            isRunning = false;
            return;
        }

        int charLevel = myChar.clevel;
        lastKnownLevel = charLevel;
        GameScr.gameAC("ALv: B\u1eaft \u0111\u1ea7u Lv" + charLevel + " \u2192 Lv" + targetLevel);

        // Tim map dau tien va bat ts
        int currentTargetMap = findMapForLevel(charLevel);
        GameScr.gameAC("ALv: Farm t\u1ea1i " + getMapName(currentTargetMap));
        startTsOnMap(currentTargetMap);

        // Bat tu dong cong ky nang
        Char.CongKN = true;
        GameScr.gameAC("ALv: Auto c\u1ed9ng KN ON!");

        // === MAIN LOOP ===
        while (isRunning) {
            sleep(CHECK_INTERVAL_MS);

            if (!isRunning) break;

            // Kiem tra mat mang
            if (isDisconnected()) {
                waitForReconnect();
                if (!isRunning) break;
                // Sau reconnect: restart ts
                currentTargetMap = findMapForLevel(myChar.clevel);
                startTsOnMap(currentTargetMap);
                continue;
            }

            // Lay level hien tai
            myChar = Char.getMyChar();
            if (myChar == null) continue;
            charLevel = myChar.clevel;

            // Dat level muc tieu?
            if (charLevel >= targetLevel) {
                GameScr.gameAC("ALv: \u0110\u1ea1t Lv" + charLevel + "! XONG!");
                stop();
                return;
            }

            // Len level?
            if (charLevel != lastKnownLevel) {
                GameScr.gameAC("ALv: Lv" + lastKnownLevel + " \u2192 Lv" + charLevel + "!");
                lastKnownLevel = charLevel;

                // Kiem tra co can chuyen map khong
                int newMap = findMapForLevel(charLevel);
                if (newMap != currentTargetMap) {
                    GameScr.gameAC("ALv: Chuy\u1ec3n \u2192 " + getMapName(newMap) + " (Lv" + charLevel + ")");
                    currentTargetMap = newMap;

                    // Tat ts cu, cho, bat ts tren map moi
                    sleep(MAP_CHANGE_DELAY_MS);
                    startTsOnMap(currentTargetMap);
                }
            }

            // Kiem tra chet (statusMe == 14)
            if (myChar.statusMe == 14) {
                GameScr.gameAC("ALv: Ch\u1ebft, ch\u1edd h\u1ed3i sinh...");
                // Gui lenh hoi sinh
                try { Service.gI().gameAF(); } catch (Exception e) {}
                sleep(5000);
                // Restart ts sau khi hoi sinh
                if (isRunning && myChar.statusMe != 14) {
                    startTsOnMap(currentTargetMap);
                }
            }

            // Kiem tra ts con chay khong
            // Neu user tat ts tu menu, Code.gameAB = null
            // Cho 2 giay roi check lai — neu van null thi user chu y tat → dung AutoLevel
            if (Code.gameAB == null && isRunning) {
                sleep(2000);
                if (Code.gameAB == null && isRunning) {
                    GameScr.gameAC("ALv: Ts b\u1ecb t\u1eaft, d\u1eebng Auto Level!");
                    isRunning = false;
                    return;
                }
            }
        }
    }
}
