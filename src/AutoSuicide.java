/**
 * AutoSuicide — Tu dong tu sat khi dung im qua lau.
 * AutoJump — Tu dong nhay (Char.gameAC lên 50px) dinh ky de reset toa do.
 *
 * Chi hoat dong khi dang treo auto (Code.gameAB != null).
 * Mac dinh TAT — user bat qua TsConfig.
 */
public final class AutoSuicide implements Runnable {
    // === CONFIG Tu Sat ===
    private static final int DEF_IDLE_TIMEOUT_MS = 30000;   // 30 giay dung im -> tu sat
    private static final int DEF_CHECK_INTERVAL_MS = 5000;   // Kiem tra moi 5 giay

    public static int IDLE_TIMEOUT_MS = DEF_IDLE_TIMEOUT_MS;
    public static int CHECK_INTERVAL_MS = DEF_CHECK_INTERVAL_MS;

    public static boolean isEnabled = false;
    private static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG Auto Jump ===
    private static final int DEF_JUMP_INTERVAL_MS = 20000;  // Nhay moi 20 giay
    private static final int JUMP_HEIGHT = 50;               // Nhay len 50px

    public static int JUMP_INTERVAL_MS = DEF_JUMP_INTERVAL_MS;
    public static boolean isJumpEnabled = false;
    private static boolean isJumpRunning = false;
    private static Thread jumpThread;

    // Toa do cuoi cung thay doi
    private static int lastX = -1;
    private static int lastY = -1;
    private static long lastMoveTime = 0;

    static {
        loadConfigFromRMS();
    }

    private AutoSuicide() {}

    // ===================== TU SAT =====================

    /** Bat/tat auto suicide */
    public static void toggle() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            GameScr.gameAC("Auto T\u1ef1 S\u00e1t: ON (" + (IDLE_TIMEOUT_MS / 1000) + "s)");
            start();
        } else {
            GameScr.gameAC("Auto T\u1ef1 S\u00e1t: OFF");
            stop();
        }
        saveConfigToRMS();
    }

    public static void start() {
        if (isRunning || !isEnabled) return;
        isRunning = true;
        lastX = -1;
        lastY = -1;
        lastMoveTime = System.currentTimeMillis();
        thread = new Thread(new AutoSuicide());
        thread.start();
    }

    public static void stop() {
        isRunning = false;
        thread = null;
    }

    public void run() {
        try { Thread.sleep(3000); } catch (Exception e) {}

        while (isRunning && isEnabled) {
            try {
                Thread.sleep(CHECK_INTERVAL_MS);

                if (Code.gameAB == null) {
                    lastMoveTime = System.currentTimeMillis();
                    continue;
                }

                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    lastMoveTime = System.currentTimeMillis();
                    continue;
                }

                if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                    lastMoveTime = System.currentTimeMillis();
                    lastX = -1;
                    lastY = -1;
                    continue;
                }

                int cx = myChar.cx;
                int cy = myChar.cy;

                if (lastX == -1 || cx != lastX || cy != lastY) {
                    lastX = cx;
                    lastY = cy;
                    lastMoveTime = System.currentTimeMillis();
                    continue;
                }

                long idleTime = System.currentTimeMillis() - lastMoveTime;
                if (idleTime >= IDLE_TIMEOUT_MS) {
                    GameScr.gameAC("Auto Die: \u0110\u1ee9ng im " + (idleTime / 1000) + "s -> T\u1ef1 s\u00e1t!");
                    Code.gameAN();
                    lastMoveTime = System.currentTimeMillis();
                    lastX = -1;
                    lastY = -1;
                    Thread.sleep(5000);
                }

            } catch (Exception e) {
                try { Thread.sleep(2000); } catch (Exception ex) {}
            }
        }
        isRunning = false;
    }

    // ===================== AUTO JUMP =====================

    /** Bat/tat auto jump */
    public static void toggleJump() {
        isJumpEnabled = !isJumpEnabled;
        if (isJumpEnabled) {
            GameScr.gameAC("Auto Nh\u1ea3y: ON (" + (JUMP_INTERVAL_MS / 1000) + "s)");
            startJump();
        } else {
            GameScr.gameAC("Auto Nh\u1ea3y: OFF");
            stopJump();
        }
        saveConfigToRMS();
    }

    public static void startJump() {
        if (isJumpRunning || !isJumpEnabled) return;
        isJumpRunning = true;
        jumpThread = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(3000); } catch (Exception e) {}

                while (isJumpRunning && isJumpEnabled) {
                    try {
                        Thread.sleep(JUMP_INTERVAL_MS);

                        // Chi nhay khi dang treo auto
                        if (Code.gameAB == null) continue;

                        Char myChar = Char.getMyChar();
                        if (myChar == null || myChar.cName == null) continue;
                        if (myChar.statusMe == 14 || myChar.cHP <= 0) continue;

                        // Nhay that: di chuyen len JUMP_HEIGHT px
                        int cx = myChar.cx;
                        int cy = myChar.cy;
                        Char.gameAC(cx, cy - JUMP_HEIGHT);

                    } catch (Exception e) {
                        try { Thread.sleep(2000); } catch (Exception ex) {}
                    }
                }
                isJumpRunning = false;
            }
        });
        jumpThread.start();
    }

    public static void stopJump() {
        isJumpRunning = false;
        jumpThread = null;
    }

    // ===================== RMS =====================

    /** Luu config vao RMS. Format: "timeout;interval;enabled;jumpInterval;jumpEnabled" */
    public static void saveConfigToRMS() {
        try {
            String data = IDLE_TIMEOUT_MS + ";" + CHECK_INTERVAL_MS + ";"
                + (isEnabled ? 1 : 0) + ";" + JUMP_INTERVAL_MS + ";" + (isJumpEnabled ? 1 : 0);
            RMS.gameAA("auto_suicide_cfg", data);
        } catch (Exception e) {}
    }

    /** Load config tu RMS */
    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("auto_suicide_cfg");
            if (data != null && data.length() > 0) {
                int[] vals = new int[5];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 5; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        vals[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 2) {
                    IDLE_TIMEOUT_MS = vals[0];
                    CHECK_INTERVAL_MS = vals[1];
                }
                if (idx >= 3) {
                    isEnabled = vals[2] == 1;
                }
                if (idx >= 4) {
                    JUMP_INTERVAL_MS = vals[3];
                }
                if (idx >= 5) {
                    isJumpEnabled = vals[4] == 1;
                }
            }
        } catch (Exception e) {}
    }

    /** Reset config ve mac dinh */
    public static void resetConfig() {
        IDLE_TIMEOUT_MS = DEF_IDLE_TIMEOUT_MS;
        CHECK_INTERVAL_MS = DEF_CHECK_INTERVAL_MS;
        JUMP_INTERVAL_MS = DEF_JUMP_INTERVAL_MS;
    }
}
