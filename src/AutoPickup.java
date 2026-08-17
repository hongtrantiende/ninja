/**
 * AutoPickup v6 — Hut VP toc do cao, KHONG giat.
 *
 * Toi uu cho TS nhanh (chuyen khu < 1s):
 * - Scan 50ms/vong (gan nhu moi frame)
 * - 0ms delay giua cac item (blast TAT CA cung luc)
 * - Nhat TAT CA item (gan + xa) — khong doi game goc nhat
 * - Ghost move bang Service.gameAB (1 packet, KHONG pathfinding)
 * - Restore cx/cy NGAY SAU moi ghost move -> khong giat
 * - gameAQ=true -> game goc ho tro nhat gan song song
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 100;    // 100ms giua moi vong quet
    private static final int NEAR_RANGE = 40;           // Item <= 40px nhat truc tiep (khong can ghost)
    private static final int ITEM_DELAY_MS = 15;        // 15ms/item — tranh flood server
    private static final int BURST_ROUNDS = 5;          // 5 vong burst (grabOnce)

    /**
     * Toggle hut VP on/off.
     */
    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("T\u1eaft h\u00fat VP!");
        } else {
            start();
            GameScr.gameAC("B\u1eadt h\u00fat VP!");
        }
    }

    public static void start() {
        if (isRunning) return;
        isRunning = true;
        Code.gameAQ = true;
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
                        if (wasOff) GameScr.gameAC("H\u00fat VP ON theo TS!");
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
        Code.gameAQ = true;
    }

    /**
     * Kiem tra item co nen nhat khong.
     */
    private static boolean shouldPickup(ItemMap item) {
        if (item == null || item.template == null) return false;
        if (item.gameAK) return false;
        try {
            return Code.gameAA(item.template)
                || Char.getMyChar().nClass.classId == 1 && item.template.id == 218;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hut toan bo VP 1 lan (boss chet).
     */
    public static void grabOnce() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            int initSize = GameScr.vItemMap.size();
            if (initSize == 0) return;
            for (int pass = 0; pass < BURST_ROUNDS; pass++) {
                if (GameScr.vItemMap.size() == 0) break;
                blastAll(myChar);
                try { Thread.sleep(50); } catch (Exception e) {}
            }
            int picked = initSize - GameScr.vItemMap.size();
            if (picked > 0) {
                GameScr.gameAC("H\u00fat " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast nhat TAT CA item — khong loc, toc do toi da.
     * 0ms delay, ghost move 1 packet, restore cx/cy ngay.
     */
    private static void blastAll(Char myChar) {
        if (myChar == null || Service.gI() == null) return;
        int cx = myChar.cx;
        int cy = myChar.cy;
        boolean ghosted = false;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                if (item == null || item.template == null) continue;
                if (item.template.gameAA()) continue; // Bo qua trang bi

                int dx = Math.abs(cx - item.xEnd);
                int dy = Math.abs(cy - item.yEnd);

                if (dx > NEAR_RANGE || dy > NEAR_RANGE) {
                    Service.gI().gameAB(item.xEnd, item.yEnd);
                    ghosted = true;
                }
                Service.gI().gameAQ(item.itemMapID);
                try { Thread.sleep(ITEM_DELAY_MS); } catch (Exception e2) {}
            } catch (Exception e) {}
        }

        // Quay ve vi tri goc
        if (ghosted) {
            try { Service.gI().gameAB(cx, cy); } catch (Exception e) {}
        }
        myChar.cx = cx;
        myChar.cy = cy;
    }

    /**
     * Hut VP thong minh — loc theo ds nhat, toc do toi da.
     * 0ms delay giua items — blast TAT CA trong 1 vong.
     * Ghost move 1 packet + restore cx/cy ngay -> KHONG GIAT.
     */
    private static void blastFiltered(Char myChar) {
        if (myChar == null || Service.gI() == null) return;

        // Check hanh trang day
        try { if (Char.gameBG() <= 2) return; } catch (Exception e) {}

        int cx = myChar.cx;
        int cy = myChar.cy;
        boolean ghosted = false;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                if (!shouldPickup(item)) continue;

                int dx = Math.abs(cx - item.xEnd);
                int dy = Math.abs(cy - item.yEnd);

                if (dx > NEAR_RANGE || dy > NEAR_RANGE) {
                    // Item xa — ghost move 1 packet
                    Service.gI().gameAB(item.xEnd, item.yEnd);
                    ghosted = true;
                }
                Service.gI().gameAQ(item.itemMapID);
                try { Thread.sleep(ITEM_DELAY_MS); } catch (Exception e2) {}
            } catch (Exception e) {}
        }

        // Ve vi tri goc — 1 packet
        if (ghosted) {
            try { Service.gI().gameAB(cx, cy); } catch (Exception e) {}
        }
        // Giu cx/cy client dung — khong giat
        myChar.cx = cx;
        myChar.cy = cy;
    }

    /**
     * Thread chinh — quet 50ms/vong, blast 0ms delay.
     * Nhanh du de nhat het VP truoc khi chuyen khu (<1s).
     */
    public void run() {
        try { Thread.sleep(200); } catch (Exception e) {}

        while (isRunning) {
            try {
                Char myChar = Char.getMyChar();
                if (myChar != null && myChar.cHP > 0 && GameScr.vItemMap.size() > 0) {
                    blastFiltered(myChar);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
