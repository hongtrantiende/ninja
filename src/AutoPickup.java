/**
 * AutoPickup - Nhat do nhanh lien tuc.
 * Khi bat: chay thread rieng, lien tuc quet vItemMap va nhat TAT CA item 30ms/item.
 * Nhanh gap ~5x so voi Code.gameAQ mac dinh (1 item/tick, 50ms).
 * 
 * Su dung: goi tu ChatRouter khi user chat lenh "nhat" (toggle on/off).
 * Cung duoc goi tu ts/tsn/ak khi bat nhung Auto nay.
 */
public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;
    
    // Cau hinh
    private static final int PICK_DELAY_MS = 30;     // Delay giua moi item
    private static final int SCAN_DELAY_MS = 200;     // Delay giua moi vong quet
    private static final int ROUNDS_PER_SCAN = 3;     // So vong nhat moi lan quet
    
    /**
     * Toggle nhat do nhanh on/off.
     * Goi tu ChatRouter khi user chat "nhat".
     */
    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("T\u1eaft nh\u1eb7t nhanh!");
        } else {
            start();
            GameScr.gameAC("B\u1eadt nh\u1eb7t nhanh!");
        }
    }
    
    /**
     * Bat nhat do nhanh (goi tu code, khong hien thong bao).
     */
    public static void start() {
        if (isRunning) return;
        isRunning = true;
        thread = new Thread(new AutoPickup());
        thread.start();
    }
    
    /**
     * Tat nhat do nhanh.
     */
    public static void stop() {
        isRunning = false;
        thread = null;
    }
    
    /**
     * Nhat nhanh 1 lan (khong can bat thread).
     * Goi khi boss chet, mob chet xong, etc.
     */
    public static void grabOnce() {
        try {
            int total = GameScr.vItemMap.size();
            if (total == 0) return;
            for (int round = 0; round < ROUNDS_PER_SCAN && total > 0; round++) {
                int picked = 0;
                for (int i = 0; i < GameScr.vItemMap.size(); i++) {
                    try {
                        ItemMap item = (ItemMap) GameScr.vItemMap.elementAt(i);
                        Service.gI().gameAQ(item.itemMapID);
                        picked++;
                        try { Thread.sleep(PICK_DELAY_MS); } catch (Exception e) {}
                    } catch (Exception e) {}
                }
                if (picked == 0) break;
                try { Thread.sleep(SCAN_DELAY_MS); } catch (Exception e) {}
                total = GameScr.vItemMap.size();
            }
        } catch (Exception e) {}
    }
    
    public void run() {
        // Cho game load xong
        try { Thread.sleep(1000); } catch (Exception e) {}
        
        while (isRunning) {
            try {
                // Chi nhat khi co item tren dat
                if (GameScr.vItemMap.size() > 0) {
                    grabOnce();
                }
            } catch (Exception e) {}
            
            // Cho truoc khi quet lai
            try { Thread.sleep(SCAN_DELAY_MS); } catch (Exception e) {}
        }
    }
}
