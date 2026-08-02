/**
 * MapScanner — Quet level quai tren tat ca map.
 * Lenh: scanmap — Tu dong di chuyen qua tung map, doc level quai, in ket qua.
 *
 * Vi mob data do server gui khi vao map, phai thuc su vao map moi doc duoc.
 * Tool nay tu dong: GoMap(id) → doi load → doc vMob → ghi ket qua → map tiep.
 */
public class MapScanner implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // Danh sach map CAN SCAN (chi map co quai, bo map NTD/truong/dau truong/VDMQ)
    private static final int[] SCAN_MAPS = {
        3, 4, 5, 6, 7, 8, 9,
        13, 14, 15, 16,
        17, 18, 19, 20, 21,
        23, 24, 25, 26,
        29, 30, 31, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47,
        50, 51, 52, 53, 54, 55,
        67, 68, 69, 70, 71
    };

    // Ket qua scan: mapID -> "ten quai LvXX"
    public static String[] results = new String[160];
    public static int resultCount = 0;

    public static void start() {
        if (isRunning) {
            stop();
            return;
        }
        isRunning = true;
        resultCount = 0;
        for (int i = 0; i < results.length; i++) results[i] = null;
        thread = new Thread(new MapScanner());
        thread.start();
        GameScr.gameAC("B\u1eaft \u0111\u1ea7u qu\u00e9t " + SCAN_MAPS.length + " map...");
    }

    public static void stop() {
        isRunning = false;
        thread = null;
    }

    /**
     * Doc quai tren map hien tai. Tra ve "TenQuai Lv15, TenQuai2 Lv20" hoac null.
     */
    private static String readMobsOnCurrentMap() {
        try {
            int size = GameScr.vMob.size();
            if (size == 0) return null;

            // Gom cac loai quai khac nhau (theo templateId)
            String result = "";
            int[] seenIds = new int[20];
            int seenCount = 0;

            for (int i = 0; i < size && seenCount < 5; i++) {
                try {
                    Mob mob = (Mob) GameScr.vMob.elementAt(i);
                    if (mob == null || mob.status == 0) continue;

                    // Kiem tra da thay chua
                    boolean seen = false;
                    for (int j = 0; j < seenCount; j++) {
                        if (seenIds[j] == mob.templateId) { seen = true; break; }
                    }
                    if (seen) continue;
                    seenIds[seenCount++] = mob.templateId;

                    String name = "?";
                    try { name = mob.getTemplate().name; } catch (Exception e) {}

                    if (result.length() > 0) result += ", ";
                    result += name + " Lv" + mob.level;
                } catch (Exception e) {}
            }
            return result.length() > 0 ? result : null;
        } catch (Exception e) { return null; }
    }

    public void run() {
        try { Thread.sleep(2000); } catch (Exception e) {}

        int origMap = TileMap.mapID;
        int scanned = 0;

        for (int idx = 0; idx < SCAN_MAPS.length && isRunning; idx++) {
            int mapID = SCAN_MAPS[idx];

            try {
                // Chuyen map
                TileMap.GoMap(mapID);
                // Doi map load (toi da 8 giay)
                for (int w = 0; w < 16 && isRunning; w++) {
                    try { Thread.sleep(500); } catch (Exception e) {}
                    if (TileMap.mapID == mapID && GameScr.vMob.size() > 0) break;
                }

                // Doc quai
                String mobInfo = readMobsOnCurrentMap();
                if (mobInfo != null) {
                    results[mapID] = mobInfo;
                    resultCount++;
                    GameScr.gameAC("M" + mapID + ": " + mobInfo);
                }
                scanned++;

            } catch (Exception e) {}
        }

        // Quay ve map goc
        try { TileMap.GoMap(origMap); } catch (Exception e) {}

        // In tong ket
        GameScr.gameAC("=== XONG! " + resultCount + "/" + scanned + " map co quai ===");

        // In bang tom tat de copy
        for (int i = 0; i < results.length; i++) {
            if (results[i] != null) {
                GameScr.gameAC("M" + i + ": " + results[i]);
            }
        }

        isRunning = false;
    }
}
