/**
 * AutoTsXa — Tự động chọn quái xa và ghost move/tele toàn map khi hết quái gần.
 * Tích hợp tự động chạy song song cùng Tàn Sát (ts).
 * Lệnh chat: tsxa (bật/tắt riêng)
 */
public class AutoTsXa implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;

    // Vị trí ban đầu (để quay về khi hết sạch quái trên map chờ hồi sinh)
    public static int homeX = 0;
    public static int homeY = 0;

    // === CONFIG ===
    private static final int CHECK_MS = 800;        // 800ms kiểm tra 1 lần
    private static final int NEARBY_RANGE = 120;    // Quái <= 120px = quái gần

    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("T\u1eaft T\u00e0n S\u00e1t Xa!");
        } else {
            start();
            GameScr.gameAC("B\u1eadt T\u00e0n S\u00e1t Xa! T\u1ef1 tele qu\u00e1i xa to\u00e0n map.");
        }
    }

    public static void start() {
        if (isRunning) return;

        Char myChar = Char.getMyChar();
        if (myChar != null) {
            homeX = myChar.cx;
            homeY = myChar.cy;
        }

        if (Code.gameAB == null) {
            Code.gameAA(-1, TileMap.mapID);
        }

        isRunning = true;
        thread = new Thread(new AutoTsXa());
        thread.start();
    }

    public static void stop() {
        isRunning = false;
        thread = null;
    }

    /**
     * Tìm con quái sống gần nhất nhưng nằm ngoài NEARBY_RANGE (để tele đến đánh cụm quái tiếp theo).
     */
    private static Mob findFarMob(int cx, int cy) {
        Mob best = null;
        int bestDist = Integer.MAX_VALUE;
        int size = GameScr.vMob.size();
        int targetTemplate = -1;
        if (Code.gameAB instanceof TanSat) {
            targetTemplate = ((TanSat) Code.gameAB).templateId;
        }
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0 || mob.status == 0 || mob.status == 1 || mob.isBoss) continue;
                if (targetTemplate != -1 && mob.templateId != targetTemplate) continue;
                int dist = Math.abs(cx - mob.x) + Math.abs(cy - mob.y);
                if (dist > NEARBY_RANGE && dist < bestDist) {
                    best = mob;
                    bestDist = dist;
                }
            } catch (Exception e) {}
        }
        return best;
    }

    /**
     * Đếm số quái sống gần vị trí hiện tại.
     */
    private static int countNearbyMobs(int cx, int cy) {
        int count = 0;
        int size = GameScr.vMob.size();
        int targetTemplate = -1;
        if (Code.gameAB instanceof TanSat) {
            targetTemplate = ((TanSat) Code.gameAB).templateId;
        }
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0 || mob.status == 0 || mob.status == 1 || mob.isBoss) continue;
                if (targetTemplate != -1 && mob.templateId != targetTemplate) continue;
                if (Math.abs(cx - mob.x) + Math.abs(cy - mob.y) <= NEARBY_RANGE) {
                    count++;
                }
            } catch (Exception e) {}
        }
        return count;
    }

    public void run() {
        try { Thread.sleep(500); } catch (Exception e) {}

        while (isRunning) {
            try {
                // Chỉ chạy khi đang bật Tàn Sát hoặc Auto và không trong chế độ săn boss
                if (Code.gameAB == null || AutoSanBoss.isRunning || AutoBossEvent.inEvent) {
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    continue;
                }

                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.statusMe == 14 || myChar.cHP <= 0) {
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    continue;
                }

                int cx = myChar.cx;
                int cy = myChar.cy;

                // Khi hết quái sống ở gần -> tele đến cụm quái sống xa gần nhất
                int nearby = countNearbyMobs(cx, cy);
                if (nearby == 0) {
                    Mob far = findFarMob(cx, cy);
                    if (far != null) {
                        int groundY = TileMap.gameAD(far.x, far.y);
                        int targetY = (groundY > 0 && Math.abs(groundY - far.y) <= 150) ? groundY : far.y;
                        Char.gameAC(far.x, targetY);
                        myChar.cx = far.x;
                        myChar.cy = targetY;
                        Service.gI().gameAC(far.x, targetY);
                        myChar.mobFocus = far;
                    } else {
                        // Hết sạch quái trên map -> quay về vị trí ban đầu chờ hồi sinh
                        if (homeX > 0 && (Math.abs(cx - homeX) + Math.abs(cy - homeY) > NEARBY_RANGE)) {
                            Char.gameAC(homeX, homeY);
                            myChar.cx = homeX;
                            myChar.cy = homeY;
                            Service.gI().gameAC(homeX, homeY);
                        }
                    }
                }

            } catch (Exception e) {}

            try { Thread.sleep(CHECK_MS); } catch (Exception e) {}
        }
    }
}
