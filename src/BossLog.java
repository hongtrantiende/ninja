import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * BossLog — Bo dem so luong boss da an & Nhat ky nhat do boss.
 * Ghi nhan lich su tieu diet boss, so lan chet va vat pham roi.
 */
public class BossLog {
    public static int countMapNgoai = 0;
    public static int countVDMQ = 0;
    public static int countLangCo = 0;
    public static int countTheGioi = 0;
    public static int countMapVIP = 0;
    public static int countMapVIP2 = 0;

    public static class BossRecord {
        public String time;
        public String bossName;
        public int mapID;
        public int zoneID;
        public int deathCount;
        public String drops;
    }

    public static MyVector records = new MyVector();

    static {
        loadFromRMS();
    }

    public static int getTotalKills() {
        return countMapNgoai + countVDMQ + countLangCo + countTheGioi + countMapVIP + countMapVIP2;
    }

    public static void loadFromRMS() {
        try {
            String data = RMS.gameAC("boss_log_stats");
            if (data != null && data.length() > 0) {
                int[] vals = new int[6];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 6; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        vals[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 6) {
                    countMapNgoai = vals[0];
                    countVDMQ = vals[1];
                    countLangCo = vals[2];
                    countTheGioi = vals[3];
                    countMapVIP = vals[4];
                    countMapVIP2 = vals[5];
                }
            }
        } catch (Exception e) {}
    }

    public static void saveToRMS() {
        try {
            String data = countMapNgoai + ";" + countVDMQ + ";" + countLangCo + ";"
                + countTheGioi + ";" + countMapVIP + ";" + countMapVIP2;
            RMS.gameAA("boss_log_stats", data);
        } catch (Exception e) {}
    }

    public static void resetStats() {
        countMapNgoai = 0;
        countVDMQ = 0;
        countLangCo = 0;
        countTheGioi = 0;
        countMapVIP = 0;
        countMapVIP2 = 0;
        records.removeAllElements();
        saveToRMS();
    }

    /**
     * Ghi nhan 1 lan ha guc boss
     */
    public static void recordBossKill(int bossType, int mapID, int zoneID, int deathCount) {
        switch (bossType) {
            case AutoSanBoss.TYPE_MAPNGOAI: countMapNgoai++; break;
            case AutoSanBoss.TYPE_VDMQ: countVDMQ++; break;
            case AutoSanBoss.TYPE_LANGCO: countLangCo++; break;
            case AutoSanBoss.TYPE_THEGIOI: countTheGioi++; break;
            case AutoSanBoss.TYPE_MAPVIP: countMapVIP++; break;
            case AutoSanBoss.TYPE_MAPVIP2: countMapVIP2++; break;
            default: countMapNgoai++; break;
        }
        saveToRMS();

        String drops = scanDrops();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        String timeStr = (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;

        String bName = "Boss";
        if (bossType >= 0 && bossType < AutoSanBoss.BOSS_NAMES.length) {
            bName = AutoSanBoss.BOSS_NAMES[bossType];
        }

        BossRecord rec = new BossRecord();
        rec.time = timeStr;
        rec.bossName = bName;
        rec.mapID = mapID;
        rec.zoneID = zoneID;
        rec.deathCount = deathCount;
        rec.drops = drops.length() > 0 ? drops : "Kh\u00f4ng c\u00f3 VP";

        if (records.size() >= 30) {
            records.removeElementAt(records.size() - 1);
        }
        records.insertElementAt(rec, 0);

        GameScr.gameAC("TSB: H\u1ea1 Boss " + bName + " M" + mapID + " K" + zoneID + " (Ch\u1ebft " + deathCount + ")! T\u1ed5ng: " + getTotalKills() + " con");
        if (drops.length() > 0) {
            GameScr.gameAC("R\u01a1i: " + drops);
        }
    }

    /**
     * Quet cac vat pham roi tren mat dat truoc khi nhat
     */
    public static String scanDrops() {
        try {
            if (GameScr.vItemMap == null || GameScr.vItemMap.size() == 0) return "";
            StringBuffer sb = new StringBuffer();
            int count = 0;
            for (int i = 0; i < GameScr.vItemMap.size() && count < 6; i++) {
                Object obj = GameScr.vItemMap.elementAt(i);
                if (obj instanceof ItemMap) {
                    ItemMap im = (ItemMap) obj;
                    if (im.template != null && im.template.name != null && im.template.name.length() > 0) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(im.template.name);
                        count++;
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    /**
     * Hien thi Form xem Nhat ky va Thong ke Boss
     */
    public static void showLogForm() {
        try {
            Form form = new Form("Nh\u1eadt K\u00fd S\u0103n Boss");
            String summary = "=== T\u1ed4NG K\u1ebeT: " + getTotalKills() + " BOSS ===\n"
                + "- Map Ngo\u00e0i: " + countMapNgoai + "\n"
                + "- VDMQ: " + countVDMQ + "\n"
                + "- L\u00e0ng C\u1ed5: " + countLangCo + "\n"
                + "- Map VIP 1: " + countMapVIP + " | VIP 2: " + countMapVIP2 + "\n"
                + "- Th\u1ebf Gi\u1edbi: " + countTheGioi + "\n"
                + "==========================\n";
            form.append(summary);

            if (records.size() == 0) {
                form.append("Ch\u01b0a c\u00f3 l\u1ecbch s\u1eed di\u1ec7t boss n\u00e0o!");
            } else {
                form.append("L\u1ecach s\u1eed " + records.size() + " con g\u1ea7n nh\u1ea5t:\n");
                for (int i = 0; i < records.size(); i++) {
                    BossRecord r = (BossRecord) records.elementAt(i);
                    String itemTxt = (i + 1) + ". [" + r.time + "] " + r.bossName + " M" + r.mapID + " K" + r.zoneID
                        + " (HS: " + r.deathCount + ")\n   -> R\u01a1i: " + r.drops + "\n";
                    form.append(itemTxt);
                }
            }

            Command cmdDong = new Command("\u0110\u00f3ng", Command.BACK, 1);
            Command cmdReset = new Command("Reset \u0110\u1ebfm", Command.SCREEN, 2);
            form.addCommand(cmdDong);
            form.addCommand(cmdReset);
            form.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    if (c == cmdReset) {
                        resetStats();
                        GameScr.gameAC("Nh\u1eadt k\u00fd boss: \u0110\u00e3 reset v\u1ec1 0!");
                        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
                    } else {
                        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
                    }
                }
            });
            Display.getDisplay(GameMidlet.instance).setCurrent(form);
        } catch (Exception e) {}
    }
}
