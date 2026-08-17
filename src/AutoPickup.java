/**
 * AutoPickup v4 — Hut VP thong minh: loc ds nhat, check bag, delay chong spam.
 *
 * Cai tien tu v3.2:
 * - Loc item theo Code.gameAA(ItemTemplate) — cung ds nhat voi game goc
 * - Check hanh trang day (Char.gameBG() <= 2) — khong nhat khi day
 * - Delay 30ms/item trong thread nen — tranh flood server
 * - Tat gameAQ goc khi AutoPickup chay — tranh xung dot 2 he thong
 * - grabOnce (boss): van nhat tat ca VP (khong loc, uu tien nhanh)
 *
 * Lenh: "nhat" toggle on/off.
 * Nut menu "Nhat Xa" cung toggle AutoPickup.
 * Tu bat khi ts/tsn/ak active.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 150;    // 150ms giua moi vong quet — nhanh hon
    private static final int BURST_ROUNDS = 3;          // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;          // Item > 50px thi ghost move
    private static final int THREAD_DELAY_MS = 20;      // 20ms/item — hut nhanh
    private static final int GRAB_DELAY_MS = 3;         // 3ms/item trong grabOnce

    /**
     * Toggle hut VP on/off.
     * Dong bo voi nut menu "Nhat Xa": tat gameAQ goc khi AutoPickup chay.
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
        // Tat nhat goc game de tranh xung dot 2 he thong nhat cung luc
        Code.gameAQ = false;
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
        // Khoi phuc nhat goc game
        Code.gameAQ = true;
    }

    /**
     * Kiem tra item co nen nhat khong — dung cung logic voi game goc.
     * Gom: ds nhat (Code.nhat[]), NhatYen, NhatDa, NhatTrangBi, NhatAll...
     */
    private static boolean shouldPickup(ItemMap item) {
        if (item == null || item.template == null) return false;
        if (item.gameAK) return false; // Da nhat roi

        // Dung ham loc goc cua game — cung ds nhat voi menu "Item Nhat"
        try {
            return Code.gameAA(item.template)
                || Char.getMyChar().nClass.classId == 1 && item.template.id == 218;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hut toan bo VP 1 lan (dung cho AutoSanBoss sau boss chet).
     * Ghost move NHANH — pham vi toan map, nhat sach, KHONG LOC (uu tien nhanh).
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
                GameScr.gameAC("H\u00fat " + picked + "/" + initSize + " VP!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast KHONG LOC — nhat tat ca, dung cho grabOnce (boss).
     * Bo qua trang bi (item.template.gameAA()).
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

                // Bo qua trang bi (type 0-15)
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

        Char.gameAC(origCx, origCy);
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Hut VP thong minh — ghost move server-side, nhan vat KHONG nhay.
     * Chi gui packet vi tri + pickup, nhan vat van dung yen danh quai.
     * Loc theo ds nhat game goc.
     * @param delayMs delay giua moi item (ms)
     */
    private static void blastPickupFiltered(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null || Service.gI() == null) return;

        // Check hanh trang day — khong nhat khi con <= 2 o trong
        try {
            if (Char.gameBG() <= 2) return;
        } catch (Exception e) {}

        // Luu vi tri thuc cua nhan vat — se restore cuoi cung
        int realCx = myChar.cx;
        int realCy = myChar.cy;
        int picked = 0;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);

                // Loc theo ds nhat game goc
                if (!shouldPickup(item)) continue;

                // Ghost move server-side: gui packet vi tri den item
                // Server tuong nhan vat o gan item -> cho phep nhat
                // Nhan vat tren man hinh KHONG nhay — chi packet di
                Char.gameAC(item.xEnd, item.yEnd);
                Service.gI().gameAQ(item.itemMapID);
                picked++;

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Khoi phuc vi tri thuc — server biet nhan vat dang o dau
        if (picked > 0) {
            Char.gameAC(realCx, realCy);
        }
        // Luon giu toa do client dung — nhan vat khong giat
        myChar.cx = realCx;
        myChar.cy = realCy;
    }

    /**
     * Thread chinh — chay nen SONG SONG voi danh quai.
     * Hut VP: ghost move server-side + loc ds nhat + delay 20ms/item.
     * Nhan vat van dung yen danh quai, VP tu hut vao.
     */
    public void run() {
        try { Thread.sleep(300); } catch (Exception e) {}

        while (isRunning) {
            try {
                if (GameScr.vItemMap.size() > 0 && Char.getMyChar() != null
                    && Char.getMyChar().cHP > 0) {
                    blastPickupFiltered(THREAD_DELAY_MS);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}

