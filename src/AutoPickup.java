/**
 * AutoPickup v4.0 — Hút VP NHẶT ALL toàn map.
 *
 * Tính năng:
 * - Nhặt ALL toàn bộ vật phẩm trên map (kể cả trang bị, vũ khí, đồ xịn boss rơi, đá, yên, sách, vp sự kiện...)
 * - KHÔNG lọc bỏ trang bị (đảm bảo săn boss rơi đồ xịn là nhặt ngay)
 * - KHÔNG phụ thuộc danh sách lọc game
 * - Hỗ trợ cả khi bật "Ẩn VP rơi" (đọc từ Code.realItemMap)
 * - Dùng Char.gameAC() cho ghost move (client-side, tức thời)
 * - Tự động blast toàn map sau khi boss chết (grabOnce) và chạy nền khi bật Hút VP
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 150;     // 150ms giua moi vong quet
    private static final int BURST_ROUNDS = 3;           // 3 vong burst (grabOnce)
    private static final int GHOST_RANGE = 50;           // Item > 50px thi ghost move
    private static final int GRAB_DELAY_MS = 3;          // 3ms/item trong grabOnce
    private static final int ZONE_CHANGE_WAIT_MS = 1000; // Cho 1s khi chuyen khu

    // Theo doi chuyen khu
    private static int lastZoneID = -1;

    /**
     * Toggle hut VP on/off.
     */
    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("T\u1eaft h\u00FAt VP (All)!");
        } else {
            start();
            GameScr.gameAC("B\u1eadt h\u00FAt VP (All)!");
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
                        if (wasOff) GameScr.gameAC("H\u00FAt VP (All) ON theo TS!");
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
     * Lay danh sach item tren map (uu tien realItemMap neu dang bat An VP roi).
     */
    private static MyVector getItemVector() {
        if (Code.hideItemDrop && Code.realItemMap != null && Code.realItemMap.size() > 0) {
            return Code.realItemMap;
        }
        return GameScr.vItemMap;
    }

    /**
     * Kiem tra item co the nhat khong.
     * Nhat ALL: khong loc, khong bo trang bi, chi bo item null hoac da nhat/bien mat (status == 2).
     */
    private static boolean shouldPickup(ItemMap item) {
        if (item == null) return false;
        if (item.status == 2) return false;
        return true;
    }

    /**
     * Hut toan bo VP 1 lan (dung cho AutoSanBoss sau boss chet).
     * Ghost move NHANH — pham vi toan map, nhat sach tat ca do xin, trang bi, vat pham.
     */
    public static void grabOnce() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            MyVector items = getItemVector();
            if (items == null) return;
            int initSize = items.size();
            if (initSize == 0) return;

            for (int pass = 0; pass < BURST_ROUNDS + 2; pass++) {
                items = getItemVector();
                if (items == null || items.size() == 0) break;
                blastPickupAll(GRAB_DELAY_MS);
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            items = getItemVector();
            int curSize = items != null ? items.size() : 0;
            int picked = initSize - curSize;
            if (picked > 0) {
                GameScr.gameAC("H\u00FAt " + picked + "/" + initSize + " VP (All)!");
            }
        } catch (Exception e) {}
    }

    /**
     * Blast nhat TAT CA item tren map (NHAT ALL, ke ca trang bi, do xin).
     * Ghost move bang Char.gameAC (client-side).
     */
    public static void blastPickupAll(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        MyVector items = getItemVector();
        if (items == null) return;
        int size = items.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) items.elementAt(i);
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
     * Thread chinh — chay nen SONG SONG voi danh quai / san boss.
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

                // Kiem tra hanh trang con cho (chi bo qua khi thuc su khong con slot nao)
                try {
                    if (Char.gameBG() <= 0) {
                        try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
                        continue;
                    }
                } catch (Exception e) {}

                // Hut VP — nhat ALL
                MyVector items = getItemVector();
                if (items != null && items.size() > 0) {
                    blastPickupAll(0);
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
