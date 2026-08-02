/**
 * AutoTsXa — Bat ts goc + ghost move toan map khi het quai gan.
 * Lenh: tsxa (bat/tat)
 *
 * Co che:
 * - Bat ts GOC (Code.gameAA) — game xu ly: skill, dame, chet hoi sinh, quay lai
 * - Thread nen: khi het quai gan, ghost move den cum quai xa
 * - Ts goc tiep tuc danh tai vi tri moi
 * - Het quai cum do → ghost move den cum tiep
 * - Giong ts binh thuong nhung CO THE DANH MOI QUAI TREN MAP
 */
public class AutoTsXa implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // Vi tri ban dau (de quay ve khi het quai)
    private static int homeX = 0;
    private static int homeY = 0;

    // === CONFIG ===
    private static final int CHECK_MS = 1500;       // 1.5 giay check 1 lan
    private static final int NEARBY_RANGE = 150;     // Quai <150px = gan

    public static void toggle() {
        if (isRunning) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        if (isRunning) return;

        Char myChar = Char.getMyChar();
        if (myChar != null) {
            homeX = myChar.cx;
            homeY = myChar.cy;
        }

        isRunning = true;

        // Bat ts goc — game xu ly moi thu (skill, dame, chet, hoi sinh)
        if (Code.gameAB == null) {
            Code.gameAA(-1, TileMap.mapID);
        }

        thread = new Thread(new AutoTsXa());
        thread.start();
        GameScr.gameAC("B\u1eadt Ts Xa! Ts g\u1ed1c + auto move to\u00e0n map!");
    }

    public static void stop() {
        isRunning = false;
        thread = null;
        // Tat ts goc
        if (Code.gameAB != null) {
            Code.gameAC();
        }
        GameScr.gameAC("T\u1eaft Ts Xa!");
    }

    /**
     * Tim con quai song xa nhat (de ghost move den cum quai xa).
     */
    private static Mob findFarMob(int cx, int cy) {
        Mob best = null;
        int bestDist = 0;
        int size = GameScr.vMob.size();
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0) continue;
                int dist = Math.abs(cx - mob.x) + Math.abs(cy - mob.y);
                if (dist > NEARBY_RANGE && dist > bestDist) {
                    best = mob;
                    bestDist = dist;
                }
            } catch (Exception e) {}
        }
        return best;
    }

    /**
     * Dem quai song gan vi tri.
     */
    private static int countNearbyMobs(int cx, int cy) {
        int count = 0;
        int size = GameScr.vMob.size();
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0) continue;
                if (Math.abs(cx - mob.x) + Math.abs(cy - mob.y) <= NEARBY_RANGE) {
                    count++;
                }
            } catch (Exception e) {}
        }
        return count;
    }

    public void run() {
        try { Thread.sleep(1000); } catch (Exception e) {}

        while (isRunning) {
            try {
                Char myChar = Char.getMyChar();
                if (myChar == null) break;

                // Kiem tra ts goc con chay khong — restart neu can
                if (Code.gameAB == null && isRunning) {
                    try { Thread.sleep(2000); } catch (Exception e) {}
                    if (Code.gameAB == null && isRunning) {
                        Code.gameAA(-1, TileMap.mapID);
                    }
                }

                int cx = myChar.cx;
                int cy = myChar.cy;

                // Het quai gan → ghost move den cum quai xa
                int nearby = countNearbyMobs(cx, cy);
                if (nearby == 0) {
                    Mob far = findFarMob(cx, cy);
                    if (far != null) {
                        // Ghost move den cum quai xa
                        Char.gameAC(far.x, far.y);
                        myChar.cx = far.x;
                        myChar.cy = far.y;
                    } else {
                        // Het quai tren map → quay ve home doi respawn
                        if (Math.abs(cx - homeX) + Math.abs(cy - homeY) > NEARBY_RANGE) {
                            Char.gameAC(homeX, homeY);
                            myChar.cx = homeX;
                            myChar.cy = homeY;
                        }
                    }
                }

            } catch (Exception e) {}

            try { Thread.sleep(CHECK_MS); } catch (Exception e) {}
        }
    }
}
