/**
 * AutoPickup v3.4 — Fix: KHONG ghost move khi dang TS.
 *
 * v3.4: Khi TS bat, chi nhat item gan (blastPickupFast, ko ghost move).
 *       Ghost move chi khi dung lenh "nhat" thu cong hoac grabOnce (boss).
 *       Ly do: ghost move thay doi vi tri server -> attack bi reject -> treo.
 *
 * Thread nen: moi 200ms nhat item.
 * grabOnce (boss): ghost move nhanh (3ms/item), nhat sach toan map.
 * Lenh: "nhat" toggle on/off.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 100;    // 100ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;          // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;          // Item > 50px ngang thi ghost move
    private static final int GHOST_X_LIMIT = 500;       // Cho phep ghost chieu ngang <= 500px
    private static final int GHOST_Y_LIMIT = 200;       // Cho phep ghost chieu cao <= 200px
    private static final int THREAD_DELAY_MS = 50;      // 50ms/item trong thread nen
    private static final int GRAB_DELAY_MS = 3;         // 3ms/item trong grabOnce
    private static final int GHOST_PICK_INTERVAL_MS = 100;  // 100ms giua moi lan ghost pick

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
        // Gui sync vi tri that ve server de account khac thay dung cho
        syncPosition();
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
     * Blast toan map — chi gui gameAQ, KHONG ghost move.
     * Server chap nhan pickup toan map ma khong can ghost move.
     * An toan khi TS bat vi KHONG gui Char.gameAC (khong doi vi tri).
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
     * Ghost move den 1 item xa CUNG TANG, nhat, quay ve ngay.
     * Chi pick item co Y gan (cung tang/platform) de tranh loi TS.
     * Chi 1 item/lan (~140ms lech vi tri).
     */
    private static boolean ghostPickOne() {
        Char myChar = Char.getMyChar();
        if (myChar == null || Code.gameAB == null) return false;
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

                // CHI ghost move item trong tam ngang <= 500px va cao <= 200px
                if (dx > GHOST_RANGE && dx <= GHOST_X_LIMIT && dy <= GHOST_Y_LIMIT) {
                    // Ghost move den item (chi gui server, AN hieu ung)
                    Char.gameAC(item.xEnd, item.yEnd);
                    myChar.cx = origCx; // Giu visual tai cho
                    myChar.cy = origCy;
                    Service.gI().gameAQ(item.itemMapID);
                    try { Thread.sleep(50); } catch (Exception e2) {}
                    // Quay ve (chi gui server, AN hieu ung)
                    Char.gameAC(origCx, origCy);
                    myChar.cx = origCx;
                    myChar.cy = origCy;
                    try { Thread.sleep(50); } catch (Exception e2) {}
                    return true;
                }
            } catch (Exception e) {}
        }
        return false;
    }

    /**
     * Blast + Ghost Move — nhat item toan map.
     * Gui packet vi tri den item, nhat, roi quay ve vi tri goc.
     * v3.4: KHONG duoc goi tu run() khi TS bat (run() dung blastPickupFast thay the).
     *       Chi duoc goi tu: grabOnce(), hoac run() khi TS tat.
     * @param delayMs delay giua moi item (ms) de tranh flood server
     */
    private static void blastPickupSmart(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;
        boolean didGhost = false;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            if (!isRunning) break; // Dung ngay khi stop
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);

                // Bo qua trang bi (type 0-15)
                if (item.template != null && item.template.gameAA()) continue;

                int dx = Math.abs(origCx - item.xEnd);
                int dy = Math.abs(origCy - item.yEnd);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    // Ghost move den item xa — an toan vi run() da chan khi TS bat
                    Char.gameAC(item.xEnd, item.yEnd);
                    didGhost = true;
                }

                Service.gI().gameAQ(item.itemMapID);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay ve vi tri goc — delay de server nhan return packet
        if (didGhost) {
            try { Thread.sleep(10); } catch (Exception e) {}
            Char.gameAC(origCx, origCy);
            try { Thread.sleep(10); } catch (Exception e) {}
        }
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Thread chinh — chay nen SONG SONG voi danh quai.
     * v3.5: TS bat = blastPickupFast (gan, lien tuc) + ghostPickOne (1 item xa, moi 3s).
     */
    public void run() {
        try { Thread.sleep(300); } catch (Exception e) {}

        long lastGhostTime = 0;

        while (isRunning) {
            try {
                if (GameScr.vItemMap.size() > 0) {
                    if (Code.gameAB != null) {
                        // TS bat: nhat gan lien tuc
                        blastPickupFast();

                        // Moi 4s: ghost pick 1 item xa CUNG TANG
                        long now = System.currentTimeMillis();
                        if (now - lastGhostTime >= GHOST_PICK_INTERVAL_MS) {
                            ghostPickOne();
                            lastGhostTime = now;
                        }
                    } else {
                        // Ko TS: ghost move nhat toan map
                        blastPickupSmart(0);
                    }
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }

        // Thread dung: sync vi tri that ve server
        syncPosition();
    }

    /** Gui packet dong bo vi tri hien tai ve server. */
    private static void syncPosition() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar != null) {
                Char.gameAC(myChar.cx, myChar.cy);
            }
        } catch (Exception e) {}
    }
}
