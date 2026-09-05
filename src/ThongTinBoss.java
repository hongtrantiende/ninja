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
                int totalMin = hours[i];
                int spawnSec = totalMin * 60;

                int diffFromSpawn = currentSecOfDay - spawnSec;
                if (diffFromSpawn >= 0 && diffFromSpawn < 2400) {
                    isLive = true;
                    secondsLeft = -1;
                    nextHourStr = AutoSanBoss.formatTime(totalMin);
                    return;
                }

                int diff = spawnSec - currentSecOfDay;
                if (diff < 0) {
                    diff += 86400;
                }

                if (diff < secondsLeft) {
                    secondsLeft = diff;
                    nextHourStr = AutoSanBoss.formatTime(totalMin);
                }
            }
        }
    }

    private static BossData[] bosses = new BossData[] {
        new BossData("VDMQ", "M141-143", new int[] {360, 840, 1140, 1260}, AutoSanBoss.TYPE_VDMQ),
        new BossData("MapNgoai", "14 map (Lv45-75)", new int[] {390, 930, 1290}, AutoSanBoss.TYPE_MAPNGOAI),
        new BossData("L\u00e0ng C\u1ed5", "M134-137 (10K)", new int[] {60, 720, 1200}, AutoSanBoss.TYPE_LANGCO),
        new BossData("L\u00e0ng TT", "M163-165", new int[] {480, 780, 960}, AutoSanBoss.TYPE_LANGTT)
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
