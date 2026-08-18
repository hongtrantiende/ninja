/**
 * AutoPickup v3.3 — On dinh nhu v3.2 + cai tien thong minh.
 *
 * Cai tien so voi v3.2:
 * - Tam dung khi chuyen khu (detect TileMap.zoneID thay doi)
 * - Loc item thong minh (theo ds nhat, bo item da nhat)
 * - Kiem tra HP va hanh trang truoc khi nhat
 * - Scan nhanh hon (150ms)
 *
 * Van dung Char.gameAC() cho ghost move (client-side, KHONG gui server packet).
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 150;    // 150ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;          // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;          // Item > 50px thi ghost move
    private static final int THREAD_DELAY_MS = 50;      // 50ms/item trong thread nen
    private static final int GRAB_DELAY_MS = 3;         // 3ms/item trong grabOnce
    private static final int ZONE_CHANGE_WAIT_MS = 1000; // Cho 1s khi chuyen khu

    // Theo doi chuyen khu
    private static int lastZoneID = -1;

    /**
     * Toggle hut VP on/off.
     */
    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("T\u1eaft h\u00FAt VP!");
        } else {
            start();
            GameScr.gameAC("B\u1eadt h\u00FAt VP!");
        }
    }

    public static void start() {
        if (isRunning) return;
        isRunning = true;
        lastZoneID = TileMap.zoneID;
        thread = new Thread(new AutoPickup());
        thread.start();
    }

    public static void syncAfterAutoCommand() {
        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 120; i++) {
                    if (Code.gameAB instanceof TanSat) {
                        boolean wasOff = !isRunning;
                        start();
                        if (wasOff) GameScr.gameAC("H\u00FAt VP ON theo TS!");
                        return;
                    }
                    try { Thread.sleep(250L); } catch (Exception e) {}
                }
            }
        }).start();
    }

    public static void stop() {
        isRunning = false;
        thread = null;
    }

    /**
     * Kiem tra item co nen nhat khong.
     * Loc theo ds nhat cua game, bo trang bi, bo item da nhat.
     */
    private static boolean shouldPickup(ItemMap item) {
        if (item == null || item.template == null) return false;
        if (item.gameAK) return false;
        // Bo qua trang bi
        if (item.template.gameAA()) return false;
        try {
            return Code.gameAA(item.template)
                || Char.getMyChar().nClass.classId == 1 && item.template.id == 218;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hut toan bo VP 1 lan (dung cho AutoSanBoss sau boss chet).
     * Ghost move NHANH — pham vi toan map, nhat sach.
     */
    public static void grabOnce() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            int initSize = GameScr.vItemMap.size();
            if (initSize == 0) return;

            for (int pass = 0; pass < BURST_ROUNDS + 2; pass++) {
                if (GameScr.vItemMap.size() == 0) break;
                blastPickupAll(GRAB_DELAY_MS);
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            int picked = initSize - GameScr.vItemMap.size();
            if (picked > 0) {
                GameScr.gameAC("H\u00FAt " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast nhat TAT CA item (tru trang bi) — dung cho grabOnce.
     * Ghost move bang Char.gameAC (client-side).
     */
    private static void blastPickupAll(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);

                // Bo qua trang bi
                if (item.template != null && item.template.gameAA()) continue;

                int dx = Math.abs(origCx - item.xEnd);
                int dy = Math.abs(origCy - item.yEnd);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    Char.gameAC(item.xEnd, item.yEnd);
                }

                Service.gI().gameAQ(item.itemMapID);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay ve vi tri goc
        Char.gameAC(origCx, origCy);
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Blast nhat VP co loc — theo ds nhat, bo item da nhat.
     * Ghost move bang Char.gameAC (client-side).
     */
    private static void blastPickupFiltered(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                if (!shouldPickup(item)) continue;

                int dx = Math.abs(origCx - item.xEnd);
                int dy = Math.abs(origCy - item.yEnd);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    Char.gameAC(item.xEnd, item.yEnd);
                }

                Service.gI().gameAQ(item.itemMapID);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay ve vi tri goc
        Char.gameAC(origCx, origCy);
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Thread chinh — chay nen SONG SONG voi danh quai.
     * Tam dung khi chuyen khu, kiem tra HP + hanh trang.
     */
    public void run() {
        try { Thread.sleep(300); } catch (Exception e) {}

        while (isRunning) {
            try {
                Char myChar = Char.getMyChar();

                // Kiem tra nhan vat con song
                if (myChar == null || myChar.cHP <= 0) {
                    try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
                    continue;
                }

                // Detect chuyen khu — tam dung de map load xong
                if (TileMap.zoneID != lastZoneID) {
                    lastZoneID = TileMap.zoneID;
                    try { Thread.sleep(ZONE_CHANGE_WAIT_MS); } catch (Exception e) {}
                    continue;
                }

                // Kiem tra hanh trang con cho
                try {
                    if (Char.gameBG() <= 2) {
                        try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
                        continue;
                    }
                } catch (Exception e) {}

                // Hut VP
                if (GameScr.vItemMap.size() > 0) {
                    blastPickupFiltered(0);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
