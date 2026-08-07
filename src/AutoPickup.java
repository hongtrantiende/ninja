/**
 * AutoPickup v4 — Toi uu cho 20 instances treo cung may.
 *
 * Thread nen: moi 300ms quet 1 luot, ghost move + nhat.
 * grabOnce (boss): nhat sach toan map.
 *
 * v4: Giam CPU — 1 pass/scan, tang interval 100->300ms, tang ghost settle.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG (toi uu 20 instances) ===
    private static final int SCAN_INTERVAL_MS = 300;    // 300ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;          // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;          // Item > 50px thi ghost move
    private static final int THREAD_DELAY_MS = 80;      // 80ms/item trong thread nen
    private static final int GRAB_DELAY_MS = 5;         // 5ms/item trong grabOnce
    private static final int GHOST_SETTLE_MS = 50;      // 50ms doi server nhan vi tri

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

    static {
        start();
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
                for (int i = 0; i < 60; i++) { // 60x500ms = 30s
                    if (Code.gameAB instanceof TanSat) {
                        boolean wasOff = !isRunning;
                        start();
                        if (wasOff) GameScr.gameAC("H\u00FAt VP ON theo TS!");
                        return;
                    }
                    try { Thread.sleep(500L); } catch (Exception e) {} // 500ms thay 250ms
                }
            }
        }).start();
    }

    public static void stop() {
        isRunning = false;
        thread = null;
    }

    /**
     * Hut toan bo VP 1 lan (dung cho AutoSanBoss sau boss chet).
     */
    public static void grabOnce() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            int initSize = getItems().size();
            if (initSize == 0) return;

            Code.gameAQ = true;

            for (int pass = 0; pass < BURST_ROUNDS + 2; pass++) {
                if (getItems().size() == 0) break;
                blastPickupSmart(GRAB_DELAY_MS);
                try { Thread.sleep(150); } catch (Exception e) {} // 150ms thay 100ms
            }

            int picked = initSize - getItems().size();
            if (picked > 0) {
                GameScr.gameAC("H\u00FAt " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Ghost move + nhat item toan map.
     * Tele den item → nhat → quay ve vi tri goc.
     * Duyet NGUOC de tranh lech index.
     */
    private static void blastPickupSmart(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        MyVector items = getItems();
        int size = items.size();
        for (int i = size - 1; i >= 0; i--) {
            try {
                if (i >= items.size()) continue;
                ItemMap item = (ItemMap) items.elementAt(i);

                // Bo qua trang bi (type 0-15)
                if (item.template != null && item.template.gameAA()) continue;

                // Ghost move den item
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
     * Thread chinh — 1 pass/scan, delay 300ms.
     */
    public void run() {
        try { Thread.sleep(500); } catch (Exception e) {}

        while (isRunning) {
            try {
                if (getItems().size() > 0) {
                    blastPickupSmart(THREAD_DELAY_MS);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }

    /** Lay vector item thuc te — dung realItemMap khi an VP. */
    private static MyVector getItems() {
        if (Code.hideItemDrop && Code.realItemMap != null) {
            return Code.realItemMap;
        }
        return GameScr.vItemMap;
    }
}
