import java.util.Calendar;
import java.util.TimeZone;

public class ThongTinBoss {
    public static boolean isEnable = false;

    public static class BossData {
        public String name;
        public String mapInfo;
        public int[] hours;
        public int bossType; // AutoSanBoss.TYPE_VDMQ, etc.
        public int secondsLeft;
        public boolean isLive;
        public String nextHourStr;

        public BossData(String name, String mapInfo, int[] hours, int bossType) {
            this.name = name;
            this.mapInfo = mapInfo;
            this.hours = hours;
            this.bossType = bossType;
        }

        public void updateTime(int currentSecOfDay) {
            isLive = false;
            secondsLeft = Integer.MAX_VALUE;
            nextHourStr = "";

            for (int i = 0; i < hours.length; i++) {
                int h = hours[i];
                int spawnSec = h * 3600;

                int diffFromSpawn = currentSecOfDay - spawnSec;
                if (diffFromSpawn >= 0 && diffFromSpawn < 2400) {
                    isLive = true;
                    secondsLeft = -1;
                    nextHourStr = (h < 10 ? "0" + h : "" + h) + "h";
                    return;
                }

                int diff = spawnSec - currentSecOfDay;
                if (diff < 0) {
                    diff += 86400;
                }

                if (diff < secondsLeft) {
                    secondsLeft = diff;
                    nextHourStr = (h < 10 ? "0" + h : "" + h) + "h";
                }
            }
        }
    }

    private static BossData[] bosses = new BossData[] {
        new BossData("VDMQ", "M141-143", new int[] {6, 13, 19, 23}, AutoSanBoss.TYPE_VDMQ),
        new BossData("MapNgoai", "Lv45: 14,15,16 | Lv55: 44,67,70 | Lv65: 24,41,45 | Lv75: 18,36,54", new int[] {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23}, AutoSanBoss.TYPE_MAPNGOAI),
        new BossData("L\u00e0ng C\u1ed5", "M135-136 (3K)", new int[] {7, 10, 15, 23}, AutoSanBoss.TYPE_LANGCO),
        new BossData("Th\u1ebf Gi\u1edbi", "M20", new int[] {12, 21}, AutoSanBoss.TYPE_THEGIOI),
        new BossData("Map VIP", "M195", new int[] {6, 12, 20, 23}, AutoSanBoss.TYPE_MAPVIP)
    };

    public static void toggle() {
        isEnable = !isEnable;
        if (isEnable) {
            GameScr.gameAC("B\u1eadt L\u1ecbch Boss!");
        } else {
            GameScr.gameAC("T\u1eaft L\u1ecbch Boss!");
        }
    }

    /** Dong bo gio boss tu AutoSanBoss.BOSS_HOURS vao bosses[].hours */
    private static void syncBossHours() {
        for (int i = 0; i < bosses.length; i++) {
            bosses[i].hours = AutoSanBoss.BOSS_HOURS[bosses[i].bossType];
        }
    }

    public static void paint(mGraphics g) {
        if (!isEnable) return;

        // Sync gio tu AutoSanBoss.BOSS_HOURS (user co the da chinh trong BossConfig)
        syncBossHours();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int curH = cal.get(Calendar.HOUR_OF_DAY);
        int curM = cal.get(Calendar.MINUTE);
        int curS = cal.get(Calendar.SECOND);
        int currentSecOfDay = curH * 3600 + curM * 60 + curS;

        for (int i = 0; i < bosses.length; i++) {
            bosses[i].updateTime(currentSecOfDay);
        }

        // Bubble sort by secondsLeft (nearest first)
        for (int i = 0; i < bosses.length - 1; i++) {
            for (int j = i + 1; j < bosses.length; j++) {
                if (bosses[i].secondsLeft > bosses[j].secondsLeft) {
                    BossData temp = bosses[i];
                    bosses[i] = bosses[j];
                    bosses[j] = temp;
                }
            }
        }

        // ===== COMPACT PANEL LAYOUT =====
        int lineH = 12;
        int headerH = 13;
        int pad = 3;
        int panelW = 145;

        // Calculate height: MapNgoai gets 2 lines, others get 1
        int totalLines = 0;
        for (int i = 0; i < bosses.length; i++) {
            totalLines += (bosses[i].mapInfo.length() > 10) ? 2 : 1;
        }
        int panelH = pad + headerH + totalLines * lineH + pad;

        // Position: RIGHT side, below top bar
        int panelX = GameCanvas.w - panelW - 2;
        int panelY = 40;

        // ===== DRAW GAME-STYLE PANEL =====
        Paint.gameAA(panelX, panelY, panelW, panelH, g);

        // ===== HEADER with clock =====
        String clock = (curH < 10 ? "0" : "") + curH + ":"
            + (curM < 10 ? "0" : "") + curM;
        mFont.tahoma_7b_yellow.gameAA(g,
            "BOSS " + clock,
            panelX + panelW / 2, panelY + pad, 2);

        // Separator
        g.gameAA(0xD4A017);
        g.gameAD(panelX + 3, panelY + pad + headerH - 2, panelW - 6, 1);

        // ===== BOSS ROWS =====
        int drawY = panelY + pad + headerH;
        for (int i = 0; i < bosses.length; i++) {
            BossData b = bosses[i];
            boolean isLongMap = b.mapInfo.length() > 10;

            // Build countdown string
            String timeStr;
            mFont font;

            if (b.isLive) {
                timeStr = "DANG CO!";
                font = mFont.tahoma_7b_red;
            } else {
                int totalSec = b.secondsLeft;
                int mLeft = totalSec / 60;
                int hLeft = mLeft / 60;
                mLeft = mLeft % 60;

                StringBuffer tb = new StringBuffer();
                tb.append(b.nextHourStr);
                tb.append(" -");
                if (hLeft > 0) {
                    tb.append(hLeft);
                    tb.append("h");
                    if (mLeft < 10) tb.append("0");
                    tb.append(mLeft);
                } else {
                    tb.append(mLeft);
                }
                tb.append("p");
                timeStr = tb.toString();

                if (totalSec <= 300) {
                    font = mFont.tahoma_7b_yellow;
                } else if (totalSec <= 1800) {
                    font = mFont.tahoma_7_green;
                } else {
                    font = mFont.tahoma_7_white;
                }
            }

            if (isLongMap) {
                // MapNgoai: 2 lines
                // Line 1: "MapNgoai 17h -2h30p"
                font.gameAA(g, b.name + " " + timeStr, panelX + 4, drawY, 0);
                drawY += lineH;
                // Line 2: map info in grey
                mFont.tahoma_7_grey.gameAA(g, b.mapInfo, panelX + 8, drawY, 0);
                drawY += lineH;
            } else {
                // Normal: 1 line "Name(Map) 17h -2h30p"
                font.gameAA(g, b.name + "(" + b.mapInfo + ") " + timeStr, panelX + 4, drawY, 0);
                drawY += lineH;
            }
        }
    }
}
