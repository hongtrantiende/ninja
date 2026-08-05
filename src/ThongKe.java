/**
 * ThongKe — Quan ly va hien thi bang thong ke Up trong Menu Pro (Dua Mod).
 * Hiển thị trực tiếp 2 dòng HUD trên màn hình game khi bật [x] Hiện Exp/Yên Khi Up.
 */
public class ThongKe {
    public static boolean isRunning = false;
    public static long startTime = 0L;
    public static long startExp = 0L;
    public static int startYen = 0;
    public static int startXu = 0;
    public static int startLuong = 0;
    public static int kills = 0;

    /** Toggle bat/tat thong ke up trong menu */
    public static void toggle() {
        isRunning = !isRunning;
        if (isRunning) {
            Char myChar = Char.getMyChar();
            if (myChar != null) {
                resetStats(myChar);
                GameScr.gameAC("Bật thống kê Up!");
            }
        } else {
            GameScr.gameAC("Tắt thống kê Up!");
        }
    }

    public static void resetStats(Char myChar) {
        if (myChar != null) {
            startExp = myChar.cEXP;
            startYen = myChar.yen;
            startXu = myChar.xu;
            startLuong = myChar.luong;
            startTime = System.currentTimeMillis();
            kills = 0;
        }
    }

    /** Goi khi giet quai de tang count */
    public static void addKills(int count) {
        if (count > 0) {
            kills += count;
        }
    }

    /**
     * Ve 2 dong thong ke HUD len man hinh (duoc goi tu GameScr.paint)
     * CHỈ HIỆN KHI ĐANG BẬT TÀN SÁT (Code.gameAB != null)!
     */
    public static void paint(mGraphics g) {
        if (Code.gameAB == null) {
            // Khi tat TS -> reset moc thoi gian & bo dem ve 0
            startTime = 0L;
            kills = 0;
            return;
        }
        if (!SetAuto.hienexp && !isRunning) return;
        Char myChar = Char.getMyChar();
        if (myChar == null) return;

        // Neu moc chua duoc tao (vua bat TS) -> reset khoi tao tu 0 ngay!
        if (startTime == 0L) {
            resetStats(myChar);
        }

        if (startTime == 0L) {
            resetStats(myChar);
        }

        long sec = (System.currentTimeMillis() - startTime) / 1000L;
        if (sec <= 0) sec = 1L;

        int gainYen = myChar.yen - startYen;
        if (gainYen < 0) gainYen = 0;
        int gainXu = myChar.xu - startXu;
        if (gainXu < 0) gainXu = 0;
        int gainLuong = myChar.luong - startLuong;
        if (gainLuong < 0) gainLuong = 0;

        long gainExp = myChar.cEXP - startExp;
        if (gainExp < 0) gainExp = 0;

        // Tinh yen/h, xu/h
        long yenPerH = (long)gainYen * 3600L / sec;
        long expPerH = gainExp * 3600L / sec;

        // Tinh exp %
        float expPercent = 0.0f;
        float expPercentPerH = 0.0f;
        try {
            long maxExp = GameScr.exps[myChar.clevel];
            if (maxExp > 0) {
                expPercent = (float)(gainExp * 10000L / maxExp) / 100.0f;
                expPercentPerH = (float)(expPerH * 10000L / maxExp) / 100.0f;
            }
        } catch (Exception e) {}

        String timeStr = NinjaUtil.gameAB((int)sec);

        // Dem quai map hien tai
        int aliveMapMobs = 0;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob m = (Mob) o;
                    if (m.hp > 0 && m.status != 0 && m.status != 1) {
                        aliveMapMobs++;
                    }
                }
            }
        } catch (Exception e) {}

        // Dong 1: Thoi gian bat TS va Quai map
        String line1 = "Thời gian: " + timeStr + " | Map: " + aliveMapMobs + " quái";

        // Dong 2: Yen, Xu, Luong
        String line2 = "Yên: +" + gainYen + " | Xu: +" + gainXu + " | Lượng: +" + gainLuong;

        // Dong 3: Exp% va So quai giet
        String line3 = "Exp: +" + expPercent + "% | Diệt: " + kills + " quái";

        int x = 2;
        int y = 112;

        try {
            mFont.tahoma_7_yellow.gameAA(g, line1, x, y, 0, mFont.tahoma_7_grey);
            mFont.tahoma_7_yellow.gameAA(g, line2, x, y + 12, 0, mFont.tahoma_7_grey);
            mFont.tahoma_7_yellow.gameAA(g, line3, x, y + 24, 0, mFont.tahoma_7_grey);
        } catch (Exception e) {}
    }
}
