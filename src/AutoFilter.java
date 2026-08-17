/**
 * AutoFilter — Tu dong loc trang bi trong hanh trang khi TS.
 * 
 * Logic: Quet hanh trang, voi moi trang bi (type 0-15):
 *   - Tim option "Cong them tiem nang" (optionTemplate.name chua "ti")
 *   - Neu param >= nguong (vd: 2000) → giu lai
 *   - Neu param < nguong hoac khong co option tiem nang → vut ra dat
 *
 * API: Service.gI().gameAR(item.indexUI) = vut item ra dat
 * Hanh trang: Char.getMyChar().arrItemBag[]
 * Item.options = MyVector chua ItemOption
 * ItemOption.optionTemplate.name = ten option
 * ItemOption.param = gia tri (vd: 426 = 426%)
 */
public final class AutoFilter implements Runnable {
    public static volatile boolean enabled;
    private static Thread thread;

    /** Nguong tiem nang toi thieu de giu lai (%) */
    public static int threshold = 2000;

    /** Delay giua moi item vut (ms) */
    private static final int DROP_DELAY_MS = 300;

    /** Delay giua moi vong quet (ms) */
    private static final int SCAN_INTERVAL_MS = 3000;

    public static void toggle() {
        if (enabled) {
            stop();
            GameScr.gameAC("L\u1ecdc \u0111\u1ed3: OFF");
        } else {
            start();
            GameScr.gameAC("L\u1ecdc \u0111\u1ed3: ON (>=" + threshold + "%)");
        }
    }

    public static void start() {
        if (enabled) return;
        enabled = true;
        thread = new Thread(new AutoFilter());
        thread.start();
    }

    public static void stop() {
        enabled = false;
        thread = null;
    }

    /**
     * Quet hanh trang 1 lan, vut item khong dat nguong.
     * Tra ve so item da vut.
     */
    public static int filterOnce() {
        Char myChar = Char.getMyChar();
        if (myChar == null || myChar.arrItemBag == null) return 0;

        int dropped = 0;
        for (int i = 0; i < myChar.arrItemBag.length; i++) {
            Item item = myChar.arrItemBag[i];
            if (item == null) continue;
            if (item.template == null) continue;

            // Chi loc trang bi (type 0-15)
            if (!item.template.gameAA()) continue;

            // Bo qua item bi khoa
            if (item.isLock) continue;

            // Tim option "tiem nang"
            int tiemNang = getTiemNang(item);

            // Khong co option tiem nang hoac duoi nguong → vut
            if (tiemNang < threshold) {
                try {
                    Service.gI().gameAR(item.indexUI);
                    dropped++;
                    try { Thread.sleep(DROP_DELAY_MS); } catch (Exception e) {}
                } catch (Exception e) {}
            }
        }
        return dropped;
    }

    /**
     * Doc gia tri "Cong them tiem nang" tu item options.
     * Tra ve param (vd: 2000 = 2000%), hoac 0 neu khong co.
     */
    public static int getTiemNang(Item item) {
        if (item.options == null || item.options.size() == 0) return 0;

        for (int i = 0; i < item.options.size(); i++) {
            try {
                ItemOption opt = (ItemOption) item.options.elementAt(i);
                if (opt == null || opt.optionTemplate == null) continue;

                // Check ten option chua "ti" (tiem nang)
                // Server tra ve ten nhu "C\u1ed9ng th\u00eam ti\u1ec1m n\u0103ng"
                String name = opt.optionTemplate.name;
                if (name != null && name.indexOf("ti") >= 0) {
                    return opt.param;
                }
            } catch (Exception e) {}
        }
        return 0;
    }

    public void run() {
        try { Thread.sleep(1000L); } catch (Exception e) {}

        while (enabled) {
            try {
                // Chi loc khi dang trong game (khong phai login screen)
                if (GameCanvas.currentScreen == GameScr.instance) {
                    int count = filterOnce();
                    if (count > 0) {
                        GameScr.gameAC("L\u1ecdc: v\u1ee9t " + count + " \u0111\u1ed3 (<" + threshold + "%)");
                    }
                }
            } catch (Exception e) {}

            try { Thread.sleep(SCAN_INTERVAL_MS); } catch (Exception e) {}
        }
    }
}
