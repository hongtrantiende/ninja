/**
 * AutoPickup v5 — Hut VP mượt mà, KHÔNG gây giật.
 *
 * Fix từ v4.1:
 * - THAY Char.gameAC(x,y) bằng Service.gameAB(x,y) — chỉ gửi 1 packet vị trí
 *   thay vì pathfinding từng bước 50px (nguyên nhân giật)
 * - KHÔNG update myChar.cx/cy khi ghost move — client giữ nguyên vị trí
 * - Batch pickup: gom TẤT CẢ items → ghost + nhặt 1 lượt → restore 1 lần cuối
 * - Delay 20ms/item trong thread nền
 * - Tắt gameAQ gốc khi AutoPickup chạy — tránh xung đột
 * - grabOnce (boss): nhặt tất cả VP nhanh (3ms/item)
 *
 * Lệnh: "nhat" toggle on/off.
 * Nút menu "Nhặt Xa" cũng toggle AutoPickup.
 * Tự bật khi ts/tsn/ak active.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int SCAN_INTERVAL_MS = 200;    // 200ms giữa mỗi vòng quét
    private static final int BURST_ROUNDS = 3;          // 3 vòng burst (grabOnce)
    private static final int THREAD_DELAY_MS = 20;      // 20ms/item — hút nhanh
    private static final int GRAB_DELAY_MS = 3;         // 3ms/item trong grabOnce

    /**
     * Toggle hút VP on/off.
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
        // Tắt nhặt gốc game để tránh xung đột
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
        // Khôi phục nhặt gốc game
        Code.gameAQ = true;
    }

    /**
     * Kiểm tra item có nên nhặt không — dùng cùng logic với game gốc.
     */
    private static boolean shouldPickup(ItemMap item) {
        if (item == null || item.template == null) return false;
        if (item.gameAK) return false; // Đã nhặt rồi

        try {
            return Code.gameAA(item.template)
                || Char.getMyChar().nClass.classId == 1 && item.template.id == 218;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hút toàn bộ VP 1 lần (dùng cho AutoSanBoss sau boss chết).
     * KHÔNG LỌC — ưu tiên nhanh.
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
     * Blast KHÔNG LỌC — nhặt tất cả, dùng cho grabOnce (boss).
     * Ghost move = Service.gameAB(x,y) — 1 packet, KHÔNG pathfinding.
     */
    private static void blastPickupAll(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null || Service.gI() == null) return;
        int origCx = myChar.cx;
        int origCy = myChar.cy;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                if (item == null || item.template == null) continue;

                // Bỏ qua trang bị
                if (item.template.gameAA()) continue;

                // Ghost move: gửi 1 packet vị trí đến item (KHÔNG pathfinding, KHÔNG update cx/cy)
                Service.gI().gameAB(item.xEnd, item.yEnd);
                Service.gI().gameAQ(item.itemMapID);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay về vị trí gốc — 1 packet
        Service.gI().gameAB(origCx, origCy);
        // GIỮ NGUYÊN cx/cy client → nhân vật không giật
        myChar.cx = origCx;
        myChar.cy = origCy;
    }

    /**
     * Hút VP thông minh — ghost move server-side, nhân vật KHÔNG nhảy/giật.
     *
     * Khác v4: dùng Service.gameAB(x,y) thay Char.gameAC(x,y)
     * - Service.gameAB = gửi 1 packet vị trí (instant)
     * - Char.gameAC = pathfinding từng bước 50px + update cx/cy (gây giật)
     */
    private static void blastPickupFiltered(int delayMs) {
        Char myChar = Char.getMyChar();
        if (myChar == null || Service.gI() == null) return;

        // Check hành trang đầy
        try {
            if (Char.gameBG() <= 2) return;
        } catch (Exception e) {}

        // Lưu vị trí thực — sẽ restore cuối
        int realCx = myChar.cx;
        int realCy = myChar.cy;
        int picked = 0;

        int size = GameScr.vItemMap.size();
        for (int i = 0; i < size; i++) {
            try {
                ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);

                // Lọc theo ds nhặt game gốc
                if (!shouldPickup(item)) continue;

                // Ghost move: 1 packet vị trí → server tưởng ta ở gần item
                // KHÔNG update cx/cy → nhân vật trên client đứng yên
                Service.gI().gameAB(item.xEnd, item.yEnd);
                Service.gI().gameAQ(item.itemMapID);
                picked++;

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception e) {}
        }

        // Quay về vị trí gốc — 1 packet duy nhất
        if (picked > 0) {
            Service.gI().gameAB(realCx, realCy);
        }
        // LUÔN giữ tọa độ client đúng — nhân vật không giật
        myChar.cx = realCx;
        myChar.cy = realCy;
    }

    /**
     * Thread chính — chạy nền SONG SONG với đánh quái.
     * Hút VP: ghost move server-side + lọc ds nhặt + delay 20ms/item.
     * Nhân vật VẪN ĐỨNG YÊN đánh quái, VP tự hút vào.
     */
    public void run() {
        try { Thread.sleep(500); } catch (Exception e) {}

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
