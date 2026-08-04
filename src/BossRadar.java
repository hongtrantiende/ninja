/** Radar boss phia client: phat hien boss trong map/khu dang duoc tai. */
public final class BossRadar implements Runnable {
    public static boolean isRunning;
    private static Thread thread;
    private static int lastBossKey = -1;
    private static long lastAlertAt;
    private static final long REPEAT_ALERT_MS = 60000L;

    private BossRadar() {}

    public static void toggle() {
        if (isRunning) stop();
        else start();
    }

    public static void start() {
        if (isRunning) return;
        isRunning = true;
        lastBossKey = -1;
        thread = new Thread(new BossRadar());
        thread.start();
        GameScr.gameAC("Boss Radar: ON");
    }

    public static void stop() {
        isRunning = false;
        thread = null;
        lastBossKey = -1;
        GameScr.gameAC("Boss Radar: OFF");
    }

    public void run() {
        while (isRunning) {
            boolean found = false;
            try {
                if (GameScr.vMob != null) {
                    for (int i = 0; i < GameScr.vMob.size(); i++) {
                        Mob mob = (Mob)GameScr.vMob.elementAt(i);
                        if (mob == null || !mob.isBoss || mob.hp <= 0 || mob.status == 0 || mob.status == 1) continue;
                        found = true;
                        int key = TileMap.mapID * 100000 + TileMap.zoneID * 1000 + mob.mobId;
                        long now = System.currentTimeMillis();
                        if (key != lastBossKey || now - lastAlertAt >= REPEAT_ALERT_MS) {
                            lastBossKey = key;
                            lastAlertAt = now;
                            String name = "Boss";
                            try { name = mob.getTemplate().name; } catch (Exception e) {}
                            GameScr.gameAC("RADAR: " + name + " M" + TileMap.mapID + " K" + TileMap.zoneID
                                + " HP " + mob.hp + "/" + mob.maxHp);
                        }
                        break;
                    }
                }
            } catch (Exception e) {}
            if (!found) lastBossKey = -1;
            sleep(500L);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
