/**
 * TB2AutoPickup v7 — Ghost move 1 packet + restore k/l ngay.
 * 
 * Giong ban goc AutoPickup: gui 1 packet vi tri, nhat, restore vi tri.
 * KHONG dung Class_dk.e() (loop nhieu buoc), chi dung Class_di.b() (1 packet).
 * Restore k/l NGAY sau moi item de khong thay tele.
 */
public final class TB2AutoPickup implements Runnable {
    public static volatile boolean enabled;
    private static Thread thread;
    private static final int SCAN_INTERVAL_MS = 300;
    private static final int GHOST_RANGE = 50;

    public static void toggle() {
        if (enabled) {
            stop();
            Class_ds.c("T\u1eaft h\u00fat VP!");
        } else {
            start();
            Class_ds.c("B\u1eadt h\u00fat VP!");
        }
    }

    public static void start() {
        if (enabled) return;
        enabled = true;
        thread = new Thread(new TB2AutoPickup());
        thread.start();
    }

    public static void stop() {
        enabled = false;
        thread = null;
    }

    /** Hut toan bo VP 1 lan (boss chet). */
    public static void grabOnce() {
        try {
            if (Class_ds.af == null || Class_ds.af.size() == 0) return;
            for (int pass = 0; pass < 5; pass++) {
                if (Class_ds.af == null || Class_ds.af.size() == 0) break;
                ghostPickup(3);
                try { Thread.sleep(100L); } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    /**
     * Ghost pickup toan map:
     * Moi item: gui packet den -> nhat -> gui packet ve -> restore k/l NGAY.
     * Render loop khong kip thay vi tri thay doi.
     */
    private static void ghostPickup(int delayMs) {
        Class_dk player = Class_dk.g();
        if (player == null || Class_ds.af == null) return;
        int origX = player.k;
        int origY = player.l;

        int size = Class_ds.af.size();
        for (int i = 0; i < size; i++) {
            try {
                Class_bp item = (Class_bp)Class_ds.af.elementAt(i);
                if (item == null || item.h == null || item.h.a()) continue;

                int dx = Math.abs(origX - item.c);
                int dy = Math.abs(origY - item.d);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    // Ghost: gui 1 packet vi tri den item
                    Class_di.a().b(item.c, item.d);
                }

                // Nhat
                Class_di.a().q(item.g);

                if (dx > GHOST_RANGE || dy > GHOST_RANGE) {
                    // Gui packet ve vi tri goc NGAY
                    Class_di.a().b(origX, origY);
                    // Restore rendering position NGAY LAP TUC
                    player.k = origX;
                    player.l = origY;
                }

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); } catch (Exception e2) {}
                }
            } catch (Exception ignored) {}
        }
    }

    public void run() {
        try { Thread.sleep(300L); } catch (Exception ignored) {}
        while (enabled) {
            try {
                if (Class_ds.af != null && Class_ds.af.size() > 0) {
                    ghostPickup(0);
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception ignored) {}
        }
    }
}
