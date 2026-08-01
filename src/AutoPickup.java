/**
 * AutoPickup v3.1 — Background Vacuum (nhu ts) + Ghost Grab
 *
 * 2 che do:
 * A. Thread nen (song song danh quai): chi blast gameAQ, KHONG di chuyen
 *    → Nhat tat ca item trong tam server (~30-50px), nhan vat dung yen
 *    → Quai van danh muot, khong giat
 *
 * B. grabOnce (sau boss chet): ghost move den item xa roi nhat
 *    → Pham vi toan map, nhat sach 100%
 *
 * Lenh: "nhat" toggle on/off.
 * Cung tu bat khi ts/tsn/ak active.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int PICK_DELAY_MS = 3;        // 3ms giua moi lenh nhat
    private static final int SCAN_INTERVAL_MS = 200;    // 200ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;          // 3 vong burst
    private static final int GHOST_RANGE = 50;          // Item > 50px thi ghost move

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
     * CO ghost move — pham vi toan map, nhat sach.
     * Chi goi khi KHONG dang danh quai.
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
                blastPickupSmart();
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            int picked = initSize - GameScr.vItemMap.size();
            if (picked > 0) {
                GameScr.gameAC("H\u00FAt " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast + Ghost Move nhe — gui packet vi tri den item,
     * nhat, roi KHONG cap nhat cx/cy client.
     * Nhan vat VAN DUNG YEN tren man hinh, server tu accept.
     */
    private static void blastPickupSmart() {
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
                    // Ghost: chi gui packet vi tri, KHONG doi cx/cy
                    Char.gameAC(item.xEnd, item.yEnd);
                }

                Service.gI().gameAQ(item.itemMapID);
            } catch (Exception e) {}
        }

        // Gui packet quay ve vi tri goc
        Char.gameAC(origCx, origCy);
        // Dam bao client giu nguyen vi tri — khong giat
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Thread chinh — chay nen SONG SONG voi danh quai.
     * Dung ghost move nhe de hut VP xa, giu nguyen vi tri client.
     */
    public void run() {
        try { Thread.sleep(300); } catch (Exception e) {}

        while (isRunning) {
            try {
                if (GameScr.vItemMap.size() > 0) {
                    blastPickupSmart();
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
