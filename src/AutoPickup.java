/**
 * AutoPickup v3.2 — Ghost Move voi delay de hut xa ma khong flood server.
 *
 * Thread nen: moi 200ms, ghost move den tung item + nhat, delay 50ms/item.
 * grabOnce (boss): ghost move nhanh (3ms/item), nhat sach toan map.
 *
 * Lenh: "nhat" toggle on/off.
 * Cung tu bat khi ts/tsn/ak active.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 200;    // 200ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;          // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;          // Item > 50px thi ghost move
    private static final int THREAD_DELAY_MS = 50;      // 50ms/item trong thread nen
    private static final int GRAB_DELAY_MS = 3;         // 3ms/item trong grabOnce

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
        Code.gameAQ = true;
        thread = new Thread(new AutoPickup());
        thread.start();
    }

    public static void stop() {
        isRunning = false;
        thread = null;
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

            Code.gameAQ = true;

            for (int pass = 0; pass < BURST_ROUNDS + 2; pass++) {
                if (GameScr.vItemMap.size() == 0) break;
                blastPickupSmart(GRAB_DELAY_MS);
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            int picked = initSize - GameScr.vItemMap.size();
            if (picked > 0) {
                GameScr.gameAC("H\u00FAt " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast NHANH — chi gui gameAQ, KHONG ghost move.
     * Nhat item trong tam server (~30-50px).
     */
    private static void blastPickupFast() {
        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                Service.gI().gameAQ(item.itemMapID);
            } catch (Exception e) {}
        }
    }

    /**
     * Blast + Ghost Move — nhat item toan map.
     * Gui packet vi tri den item, nhat, roi quay ve vi tri goc.
     * @param delayMs delay giua moi item (ms) de tranh flood server
     */
    private static void blastPickupSmart(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                int dx = Math.abs(origCx - item.xEnd);
                int dy = Math.abs(origCy - item.yEnd);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    // Ghost: gui packet vi tri den item
                    Char.gameAC(item.xEnd, item.yEnd);
                }

                Service.gI().gameAQ(item.itemMapID);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay ve vi tri goc — nhan vat khong giat
        Char.gameAC(origCx, origCy);
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Thread chinh — chay nen SONG SONG voi danh quai.
     * Dung ghost move voi delay 50ms/item de hut xa ma khong flood.
     */
    public void run() {
        try { Thread.sleep(300); } catch (Exception e) {}

        while (isRunning) {
            try {
                if (GameScr.vItemMap.size() > 0) {
                    blastPickupSmart(0);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
