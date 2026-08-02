/**
 * AutoVT55 — Auto danh chuyen vi tri tren Map 55 (Phong an Ounio).
 * Lenh: avt55 (bat/tat)
 *
 * Logic:
 * 1. Di chuyen den vi tri 1 (241, 1344)
 * 2. Bat tan sat, danh quai tai vi tri do
 * 3. Khi het quai (hoac timeout) → chuyen sang vi tri tiep theo
 * 4. Sau vi tri 11 → quay lai vi tri 1 (loop)
 * 5. Go avt55 lan nua de tat
 */
public class AutoVT55 implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;
    public static int currentPos = 0;

    // 11 vi tri danh tren Map 55, X=241, Y giam dan
    private static final int[][] POSITIONS = {
        {241, 1344},  // VT 1
        {241, 1224},  // VT 2
        {241, 1104},  // VT 3
        {241, 984},   // VT 4
        {241, 864},   // VT 5
        {241, 744},   // VT 6
        {241, 624},   // VT 7
        {241, 504},   // VT 8
        {241, 384},   // VT 9
        {241, 264},   // VT 10
        {241, 144},   // VT 11
    };

    // === CONFIG ===
    private static final int MAP_ID = 55;
    private static final int MOB_CHECK_MS = 2000;     // Check quai moi 2 giay
    private static final int MOVE_DELAY_MS = 1500;     // Doi 1.5 giay sau khi chuyen cho
    private static final int MAX_WAIT_PER_POS = 60;    // Toi da 60 lan check (2 phut) moi vi tri
    private static final int FIGHT_RANGE = 200;        // Pham vi kiem tra quai gan vi tri (px)

    public static void toggle() {
        if (isRunning) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        if (isRunning) stop();

        // Kiem tra dang o Map 55
        if (TileMap.mapID != MAP_ID) {
            GameScr.gameAC("Ph\u1ea3i \u1edf Map 55 m\u1edbi d\u00f9ng \u0111\u01b0\u1ee3c!");
            return;
        }

        isRunning = true;
        currentPos = 0;
        thread = new Thread(new AutoVT55());
        thread.start();
        GameScr.gameAC("AVT55 ON! 11 v\u1ecb tr\u00ed");
    }

    public static void stop() {
        isRunning = false;
        thread = null;
        // Tat ts hien tai
        if (Code.gameAB != null) {
            Code.gameAC();
        }
        GameScr.gameAC("AVT55 OFF!");
    }

    public void run() {
        sleep(500);

        while (isRunning) {
            try {
                // Kiem tra con o Map 55 khong
                if (TileMap.mapID != MAP_ID) {
                    GameScr.gameAC("AVT55: R\u1eddi Map 55, d\u1eebng!");
                    break;
                }

                // Kiem tra disconnect
                try {
                    Char c = Char.getMyChar();
                    if (c == null || c.cName == null) {
                        GameScr.gameAC("AVT55: M\u1ea5t k\u1ebft n\u1ed1i, ch\u1edd...");
                        sleep(5000);
                        continue;
                    }
                } catch (Exception e) {
                    sleep(5000);
                    continue;
                }

                int posIdx = currentPos % POSITIONS.length;
                int targetX = POSITIONS[posIdx][0];
                int targetY = POSITIONS[posIdx][1];

                // === Di chuyen den vi tri ===
                GameScr.gameAC("AVT55: VT" + (posIdx + 1) + " (" + targetX + "," + targetY + ")");
                moveToPosition(targetX, targetY);
                sleep(MOVE_DELAY_MS);

                // === Bat tan sat ===
                if (Code.gameAB == null) {
                    Code.gameAF("ts");
                    sleep(500);
                }

                // === Doi danh het quai tai vi tri nay ===
                int waitCount = 0;
                while (isRunning && waitCount < MAX_WAIT_PER_POS) {
                    sleep(MOB_CHECK_MS);
                    waitCount++;

                    // Dem quai con song gan vi tri
                    int nearbyMobs = countNearbyMobs(targetX, targetY);
                    if (nearbyMobs == 0) {
                        // Het quai → chuyen vi tri
                        break;
                    }
                }

                // === Tat ts truoc khi di chuyen ===
                if (Code.gameAB != null) {
                    Code.gameAC();
                    sleep(300);
                }

                // Chuyen sang vi tri tiep theo
                currentPos++;

            } catch (Exception e) {
                sleep(3000);
            }
        }

        isRunning = false;
    }

    /**
     * Di chuyen nhan vat den toa do (x, y).
     */
    private static void moveToPosition(int x, int y) {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;
            // Gui packet di chuyen
            Char.gameAC(x, y);
            // Cap nhat vi tri client
            myChar.cx = x;
            myChar.cy = y;
        } catch (Exception e) {}
    }

    /**
     * Dem so quai con song trong pham vi FIGHT_RANGE cua vi tri (cx, cy).
     */
    private static int countNearbyMobs(int cx, int cy) {
        int count = 0;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                try {
                    Mob mob = (Mob) GameScr.vMob.elementAt(i);
                    if (mob == null || mob.status == 0) continue;
                    // Kiem tra HP con > 0
                    if (mob.hp <= 0) continue;
                    // Kiem tra khoang cach
                    int dx = Math.abs(cx - mob.x);
                    int dy = Math.abs(cy - mob.y);
                    if (dx <= FIGHT_RANGE && dy <= FIGHT_RANGE) {
                        count++;
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        return count;
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}
