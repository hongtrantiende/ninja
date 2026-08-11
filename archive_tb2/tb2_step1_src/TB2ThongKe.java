/**
 * TB2ThongKe — Quan ly va hien thi bang thong ke Up (Exp, Yen, Xu, Luong, Thoi gian treo).
 * Port 1:1 tu ThongKe ban goc, anh xa sang TB2 obfuscated classes.
 * 
 * Tu dong hien thi 3 dong HUD tren man hinh khi bat Tan Sat / Auto (Class_am.b != null) hoac Thong ke.
 */
public final class TB2ThongKe {
    public static boolean isRunning = false;
    public static long startTime = 0L;
    public static long startExp = 0L;
    public static int startYen = 0;
    public static int startXu = 0;
    public static int startLuong = 0;
    public static int kills = 0;

    private static long lastCalcTime = 0;
    private static String cachedLine1 = "";
    private static String cachedLine2 = "";
    private static String cachedLine3 = "";

    private TB2ThongKe() {}

    public static void toggle() {
        isRunning = !isRunning;
        if (isRunning) {
            Class_dk player = Class_dk.g();
            if (player != null) {
                resetStats(player);
                Class_ds.c("B\u1eadt th\u1ed1ng k\u00ea Up!");
            }
        } else {
            Class_ds.c("T\u1eaft th\u1ed1ng k\u00ea Up!");
        }
    }

    public static void resetStats(Class_dk player) {
        if (player != null) {
            startExp = player.i;      // cEXP (long)
            startYen = player.bs;     // yen (int)
            startXu = player.bp;      // xu (int)
            startLuong = player.br;   // luong (int)
            startTime = System.currentTimeMillis();
            kills = 0;
        }
    }

    public static void addKills(int count) {
        if (count > 0) {
            kills += count;
        }
    }

    public static void paint(Class_ae graphics) {
        // Tu dong hien thi khi bat Tan Sat / Auto (Class_am.b != null) hoac switch isRunning
        if (Class_am.b == null && !isRunning) {
            startTime = 0L;
            kills = 0;
            return;
        }

        int x = 2;
        int y = 155;

        long now = System.currentTimeMillis();
        if (now - lastCalcTime > 1000) {
            lastCalcTime = now;
            recalcStats();
        }

        try {
            // Class_ad.j = tahoma_7_yellow, Class_ad.k = tahoma_7_grey (bong chu)
            Class_ad.j.a(graphics, cachedLine1, x, y, 0, Class_ad.k);
            Class_ad.j.a(graphics, cachedLine2, x, y + 12, 0, Class_ad.k);
            Class_ad.j.a(graphics, cachedLine3, x, y + 24, 0, Class_ad.k);
        } catch (Exception ignored) {}
    }

    private static void recalcStats() {
        try {
            Class_dk player = Class_dk.g();
            if (player == null) return;

            if (startTime == 0L) {
                resetStats(player);
            }
            if (startTime == 0L) return;

            long sec = (System.currentTimeMillis() - startTime) / 1000L;
            if (sec <= 0) sec = 1L;

            int gainYen = player.bs - startYen;
            if (gainYen < 0) gainYen = 0;
            int gainXu = player.bp - startXu;
            if (gainXu < 0) gainXu = 0;
            int gainLuong = player.br - startLuong;
            if (gainLuong < 0) gainLuong = 0;

            long gainExp = player.i - startExp;
            if (gainExp < 0) gainExp = 0;

            float expPercent = 0.0f;
            try {
                if (Class_ds.cm != null && player.x >= 0 && player.x < Class_ds.cm.length) {
                    long maxExp = Class_ds.cm[player.x];
                    if (maxExp > 0) {
                        expPercent = (float)(gainExp * 10000L / maxExp) / 100.0f;
                    }
                }
            } catch (Exception ignored) {}

            String timeStr = formatTime((int)sec);

            int aliveMapMobs = 0;
            try {
                if (Class_ds.ag != null) {
                    int size = Class_ds.ag.size();
                    for (int i = 0; i < size; i++) {
                        Object o = Class_ds.ag.elementAt(i);
                        if (o instanceof Class_fk) {
                            Class_fk mob = (Class_fk) o;
                            if (mob.c > 0 && mob.h != 0 && mob.h != 1) {
                                aliveMapMobs++;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            cachedLine1 = "T: " + timeStr + " | Map: " + aliveMapMobs + " qu\u00e1i";
            cachedLine2 = "Y\u00ean: +" + gainYen + " | Xu: +" + gainXu + " | L\u01b0\u1ee3ng: +" + gainLuong;
            cachedLine3 = "Exp: +" + expPercent + "% | Di\u1ec7t: " + kills;
        } catch (Exception ignored) {}
    }

    private static String formatTime(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        int h = m / 60;
        m %= 60;
        int d = h / 24;
        h %= 24;

        if (d > 0) {
            return d + "d" + h + "h";
        } else if (h > 0) {
            return h + "h" + (m < 10 ? "0" + m : "" + m) + "'";
        } else {
            return (m < 10 ? "0" + m : "" + m) + ":" + (s < 10 ? "0" + s : "" + s);
        }
    }
}
