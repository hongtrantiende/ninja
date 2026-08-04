import java.util.Calendar;
import java.util.TimeZone;

public final class TB2ThongTinBoss {
    public static boolean enabled;

    private static final BossData[] BOSSES = new BossData[] {
        new BossData("Server", "M63", new int[] {12, 18, 20, 22}),
        new BossData("TheGioi", "M65", new int[] {11, 17, 19, 21}),
        new BossData("VDMQ", "M141-143", new int[] {6, 13, 19, 23}),
        new BossData("MapNgoai", "M14-16/44,67,70/21,41,45/18,46,54", new int[] {1, 4, 7, 10, 13, 16, 19, 22})
    };

    private TB2ThongTinBoss() {}

    public static void toggle() {
        enabled = !enabled;
        Class_ds.c(enabled ? "B\u1eadt L\u1ecbch Boss!" : "T\u1eaft L\u1ecbch Boss!");
    }

    public static void paint(Class_ae graphics) {
        if (!enabled || graphics == null) return;
        try {
            graphics.d(0, 0, Class_cx.z, Class_cx.aa);
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);
            int second = now.get(Calendar.SECOND);
            int current = hour * 3600 + minute * 60 + second;
            for (int i = 0; i < BOSSES.length; i++) BOSSES[i].update(current);
            for (int i = 0; i < BOSSES.length - 1; i++) {
                for (int j = i + 1; j < BOSSES.length; j++) {
                    if (BOSSES[i].secondsLeft > BOSSES[j].secondsLeft) {
                        BossData swap = BOSSES[i];
                        BOSSES[i] = BOSSES[j];
                        BOSSES[j] = swap;
                    }
                }
            }

            int width = 145;
            int x = Class_cx.z - width - 2;
            int y = 40;
            int height = 79;
            graphics.a(0x241500);
            graphics.c(x, y, width, height);
            graphics.a(0xD4A017);
            graphics.b(x, y, width, height);

            String clock = two(hour) + ":" + two(minute);
            Class_ad.d.a(graphics, "BOSS " + clock, x + width / 2, y + 3, 2);
            graphics.c(x + 3, y + 14, width - 6, 1);

            int drawY = y + 17;
            for (int i = 0; i < BOSSES.length; i++) {
                BossData boss = BOSSES[i];
                Class_ad font;
                String time;
                if (boss.live) {
                    font = Class_ad.a;
                    time = "DANG CO!";
                } else {
                    int minutes = boss.secondsLeft / 60;
                    int hours = minutes / 60;
                    minutes %= 60;
                    time = boss.nextHour + " -" + (hours > 0 ? hours + "h" + two(minutes) : String.valueOf(minutes)) + "p";
                    font = boss.secondsLeft <= 300 ? Class_ad.d : boss.secondsLeft <= 1800 ? Class_ad.n : Class_ad.i;
                }
                if (boss.map.length() > 10) {
                    font.a(graphics, boss.name + " " + time, x + 4, drawY, 0);
                    Class_ad.k.a(graphics, boss.map, x + 8, drawY + 12, 0);
                    drawY += 24;
                } else {
                    font.a(graphics, boss.name + "(" + boss.map + ") " + time, x + 4, drawY, 0);
                    drawY += 12;
                }
            }
        } catch (Exception ignored) {}
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static final class BossData {
        final String name;
        final String map;
        final int[] hours;
        int secondsLeft;
        boolean live;
        String nextHour;

        BossData(String name, String map, int[] hours) {
            this.name = name;
            this.map = map;
            this.hours = hours;
        }

        void update(int current) {
            live = false;
            secondsLeft = Integer.MAX_VALUE;
            nextHour = "";
            for (int i = 0; i < hours.length; i++) {
                int spawn = hours[i] * 3600;
                int since = current - spawn;
                if (since >= 0 && since < 2400) {
                    live = true;
                    secondsLeft = -1;
                    nextHour = two(hours[i]) + "h";
                    return;
                }
                int left = spawn - current;
                if (left < 0) left += 86400;
                if (left < secondsLeft) {
                    secondsLeft = left;
                    nextHour = two(hours[i]) + "h";
                }
            }
        }
    }
}
