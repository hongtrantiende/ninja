import java.util.Calendar;

public class ThongTinBoss {
    public static boolean isEnable = false;

    public static class BossData {
        public String name;
        public String mapInfo;
        public int[] hours;
        public int secondsLeft;
        public boolean isLive;
        public String nextHourStr;

        public BossData(String name, String mapInfo, int[] hours) {
            this.name = name;
            this.mapInfo = mapInfo;
            this.hours = hours;
        }

        public void updateTime(int currentSecOfDay) {
            isLive = false;
            secondsLeft = Integer.MAX_VALUE;
            nextHourStr = "";

            for (int i = 0; i < hours.length; i++) {
                int h = hours[i];
                int spawnSec = h * 3600;

                int diffFromSpawn = currentSecOfDay - spawnSec;
                if (diffFromSpawn >= 0 && diffFromSpawn < 900) {
                    isLive = true;
                    secondsLeft = -1;
                    nextHourStr = (h < 10 ? "0" + h : "" + h) + "h00";
                    return;
                }

                int diff = spawnSec - currentSecOfDay;
                if (diff < 0) {
                    diff += 86400;
                }

                if (diff < secondsLeft) {
                    secondsLeft = diff;
                    nextHourStr = (h < 10 ? "0" + h : "" + h) + "h00";
                }
            }
        }
    }

    private static BossData[] bosses = new BossData[] {
        new BossData("Boss Server", "Map 3", new int[] {12, 18, 20, 22}),
        new BossData("Boss Th\u1ebf Gi\u1edbi", "Map 23", new int[] {12, 23}),
        new BossData("Boss L\u00e0ng C\u1ed5", "Map 135", new int[] {7, 12, 18, 23}),
        new BossData("Boss VDMQ", "Map 141-143", new int[] {9, 15, 17, 21}),
        new BossData("Boss 45", "Map 14,15,16", new int[] {6, 11, 17, 22}),
        new BossData("Boss 55", "Map 44,67,70", new int[] {6, 11, 17, 22}),
        new BossData("Boss 65", "Map 24,41,45", new int[] {6, 11, 17, 22}),
        new BossData("Boss 75", "Map 18,36,54", new int[] {6, 11, 17, 22})
    };

    public static void toggle() {
        isEnable = !isEnable;
        if (isEnable) {
            GameScr.gameAC("B\u1eadt Khung Th\u00f4ng Tin Boss!");
        } else {
            GameScr.gameAC("T\u1eaft Khung Th\u00f4ng Tin Boss!");
        }
    }

    public static void paint(mGraphics g) {
        if (!isEnable) return;

        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        int currentSecOfDay = h * 3600 + m * 60 + s;

        for (int i = 0; i < bosses.length; i++) {
            bosses[i].updateTime(currentSecOfDay);
        }

        for (int i = 0; i < bosses.length - 1; i++) {
            for (int j = i + 1; j < bosses.length; j++) {
                if (bosses[i].secondsLeft > bosses[j].secondsLeft) {
                    BossData temp = bosses[i];
                    bosses[i] = bosses[j];
                    bosses[j] = temp;
                }
            }
        }

        int panelX = 5;
        int panelY = 45;
        int panelW = 165;
        int itemH = 11;
        int panelH = 14 + bosses.length * itemH;

        g.gameAA(0x000000);
        g.gameAD(panelX, panelY, panelW, panelH);

        g.gameAA(0xFFD700);
        g.gameAD(panelX, panelY, panelW, 1);
        g.gameAD(panelX, panelY + panelH - 1, panelW, 1);
        g.gameAD(panelX, panelY, 1, panelH);
        g.gameAD(panelX + panelW - 1, panelY, 1, panelH);

        mFont.tahoma_7b_yellow.gameAA(g, "=== L\u1ecaCH BOSS SAP XUAT HIEN ===", panelX + panelW / 2, panelY + 2, 2);

        int drawY = panelY + 13;
        for (int i = 0; i < bosses.length; i++) {
            BossData b = bosses[i];
            String text = b.name + " (" + b.mapInfo + "): ";
            mFont nameFont = mFont.tahoma_7_white;

            if (b.isLive) {
                text += "\u0110ANG CO BOSS!";
                nameFont = mFont.tahoma_7_red;
            } else {
                int totalSec = b.secondsLeft;
                int minsLeft = totalSec / 60;
                int hoursLeft = minsLeft / 60;
                minsLeft = minsLeft % 60;

                String timeStr = b.nextHourStr + " (c\u00f2n ";
                if (hoursLeft > 0) {
                    timeStr += hoursLeft + "h" + (minsLeft < 10 ? "0" : "") + minsLeft + "m)";
                } else {
                    timeStr += minsLeft + "m)";
                }
                text += timeStr;

                if (totalSec <= 300) {
                    nameFont = mFont.tahoma_7_yellow;
                }
            }

            nameFont.gameAA(g, text, panelX + 3, drawY, 0);
            drawY += itemH;
        }
    }
}
