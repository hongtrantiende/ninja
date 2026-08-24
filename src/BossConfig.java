import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.TextField;

/**
 * BossConfig — Man hinh cai dat San Boss kieu Form + ChoiceGroup (checkbox).
 * Giong SetAuto cua game goc: Form voi cac ChoiceGroup MULTIPLE,
 * nut Luu va Huy. Hien thi len man hinh thay cho GameCanvas.
 *
 * Moi loai boss la 1 ChoiceGroup rieng, hien thi cac map ID dang checkbox.
 * Gio spawn cua moi loai boss hien thi trong TextField, co the chinh sua.
 * Khi an Luu: cap nhat AutoSanBoss.disabledMaps + BOSS_HOURS theo trang thai.
 * Khi an Huy: quay ve game, khong thay doi gi.
 */
public final class BossConfig implements CommandListener {
    private static BossConfig instance;

    private Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;
    private final Command cmdReset;

    // 6 ChoiceGroup cho 6 loai boss
    private ChoiceGroup cgVDMQ;
    private ChoiceGroup cgMapNgoai;
    private ChoiceGroup cgLangCo;
    private ChoiceGroup cgTheGioi;
    private ChoiceGroup cgMapVIP;
    private ChoiceGroup cgMapVIP2;

    // 4 TextField cho gio spawn
    private TextField tfVDMQ;
    private TextField tfMapNgoai;
    private TextField tfLangCo;
    private TextField tfTheGioi;
    private TextField tfMapVIP;
    private TextField tfMapVIP2;

    // Extra rounds cho TS Boss uu tien
    private TextField tfExtraRounds;



    // Luu map IDs tuong ung voi tung ChoiceGroup
    private final int[] mapsVDMQ;
    private final int[] mapsMN;
    private final int[] mapsLC;
    private final int[] mapsTG;
    private final int[] mapsMV;
    private final int[] mapsMV2;

    private BossConfig() {
        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        cmdReset = new Command("Reset gi\u1edd", Command.SCREEN, 3);

        mapsVDMQ = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_VDMQ);
        mapsMN = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPNGOAI);
        mapsLC = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGCO);
        mapsTG = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_THEGIOI);
        mapsMV = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPVIP);
        mapsMV2 = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPVIP2);

        buildForm();
    }

    private void buildForm() {
        form = new Form("C\u00e0i \u0111\u1eb7t S\u0103n Boss");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.addCommand(cmdReset);
        form.setCommandListener(this);

        // === VDMQ ===
        tfVDMQ = new TextField("Gi\u1edd VDMQ", "", 100, TextField.ANY);
        form.append(tfVDMQ);
        cgVDMQ = new ChoiceGroup("VDMQ - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsVDMQ.length; i++) {
            cgVDMQ.append("Map " + mapsVDMQ[i], null);
        }
        form.append(cgVDMQ);

        // === Map Ngoai ===
        tfMapNgoai = new TextField("Gi\u1edd MapNgo\u00e0i", "", 100, TextField.ANY);
        form.append(tfMapNgoai);
        cgMapNgoai = new ChoiceGroup("MapNgo\u00e0i - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsMN.length; i++) {
            cgMapNgoai.append("Map " + mapsMN[i], null);
        }
        form.append(cgMapNgoai);

        // === Lang Co ===
        tfLangCo = new TextField("Gi\u1edd L\u00e0ng C\u1ed5", "", 100, TextField.ANY);
        form.append(tfLangCo);
        cgLangCo = new ChoiceGroup("L\u00e0ng C\u1ed5 - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsLC.length; i++) {
            cgLangCo.append("Map " + mapsLC[i], null);
        }
        form.append(cgLangCo);

        // === The Gioi ===
        tfTheGioi = new TextField("Gi\u1edd Th\u1ebf Gi\u1edbi", "", 100, TextField.ANY);
        form.append(tfTheGioi);
        cgTheGioi = new ChoiceGroup("Th\u1ebf Gi\u1edbi - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsTG.length; i++) {
            cgTheGioi.append("Map " + mapsTG[i], null);
        }
        form.append(cgTheGioi);

        // === Map VIP ===
        tfMapVIP = new TextField("Gi\u1edd Map VIP", "", 100, TextField.ANY);
        form.append(tfMapVIP);
        cgMapVIP = new ChoiceGroup("Map VIP - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsMV.length; i++) {
            cgMapVIP.append("Map " + mapsMV[i], null);
        }
        form.append(cgMapVIP);

        // === Map VIP2 (M196 - VIP 6-7) ===
        tfMapVIP2 = new TextField("Gi\u1edd Map VIP2", "", 100, TextField.ANY);
        form.append(tfMapVIP2);
        cgMapVIP2 = new ChoiceGroup("Map VIP2 - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsMV2.length; i++) {
            cgMapVIP2.append("Map " + mapsMV2[i], null);
        }
        form.append(cgMapVIP2);

        // === TS Boss uu tien: so luot quet them ===
        tfExtraRounds = new TextField("L\u01b0\u1ee3t qu\u00e9t th\u00eam (0=kh\u00f4ng, 1=m\u1eb7c \u0111\u1ecbnh)", "", 5, TextField.NUMERIC);
        form.append(tfExtraRounds);
    }

    /** Load trang thai tu AutoSanBoss vao checkbox + textfield */
    private void loadCurrentState() {
        loadGroup(cgVDMQ, mapsVDMQ);
        loadGroup(cgMapNgoai, mapsMN);
        loadGroup(cgLangCo, mapsLC);
        loadGroup(cgTheGioi, mapsTG);
        loadGroup(cgMapVIP, mapsMV);
        loadGroup(cgMapVIP2, mapsMV2);

        // Load gio spawn
        tfVDMQ.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_VDMQ));
        tfMapNgoai.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_MAPNGOAI));
        tfLangCo.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_LANGCO));
        tfTheGioi.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_THEGIOI));
        tfMapVIP.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_MAPVIP));
        tfMapVIP2.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_MAPVIP2));

        // Extra rounds
        tfExtraRounds.setString(String.valueOf(AutoBossEvent.extraRounds));
    }

    private void loadGroup(ChoiceGroup cg, int[] maps) {
        for (int i = 0; i < maps.length; i++) {
            // Checked = map duoc bat (khong nam trong disabledMaps)
            cg.setSelectedIndex(i, AutoSanBoss.isMapEnabled(maps[i]));
        }
    }

    /** Mo form cai dat — goi tu NamMod menu */
    public static void select() {
        AutoSanBoss.loadFromRMS();
        AutoSanBoss.loadBossHoursFromRMS();
        AutoBossEvent.loadConfigFromRMS();
        if (instance == null) {
            instance = new BossConfig();
        }
        instance.loadCurrentState();
        // Hien form len man hinh (giong SetAuto.timelite)
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    /** Xu ly nut Luu / Huy / Reset */
    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            // Luu maps
            AutoSanBoss.disabledMaps.removeAllElements();
            saveGroup(cgVDMQ, mapsVDMQ);
            saveGroup(cgMapNgoai, mapsMN);
            saveGroup(cgLangCo, mapsLC);
            saveGroup(cgTheGioi, mapsTG);
            saveGroup(cgMapVIP, mapsMV);
            saveGroup(cgMapVIP2, mapsMV2);
            AutoSanBoss.saveToRMS();

            // Luu gio spawn
            StringBuffer errMsg = new StringBuffer();
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_VDMQ, tfVDMQ.getString()))
                errMsg.append("VDMQ, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_MAPNGOAI, tfMapNgoai.getString()))
                errMsg.append("MapNgoai, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_LANGCO, tfLangCo.getString()))
                errMsg.append("LangCo, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_THEGIOI, tfTheGioi.getString()))
                errMsg.append("TheGioi, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_MAPVIP, tfMapVIP.getString()))
                errMsg.append("MapVIP, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_MAPVIP2, tfMapVIP2.getString()))
                errMsg.append("MapVIP2, ");
            AutoSanBoss.saveBossHoursToRMS();

            // Luu extra rounds
            try {
                String s = tfExtraRounds.getString();
                int v = safeParseInt(s, -1);
                if (v >= 0 && v <= 9) {
                    AutoBossEvent.extraRounds = v;
                } else {
                    if (errMsg.length() > 0) errMsg.append(", ");
                    errMsg.append("ExtraRounds(").append(v).append(")");
                }
            } catch (Exception e) {
                errMsg.append("ExtraRounds");
            }
            AutoBossEvent.saveConfigToRMS();

            int disabled = AutoSanBoss.disabledMaps.size();
            StringBuffer msg = new StringBuffer("Boss Config: \u0110\u00e3 l\u01b0u");
            if (disabled > 0) msg.append(" (").append(disabled).append(" map t\u1eaft)");
            if (errMsg.length() > 0) msg.append(" | Gi\u1edd l\u1ed7i: ").append(errMsg);
            GameScr.gameAC(msg.toString());
        } else if (c == cmdReset) {
            // Reset gio spawn ve mac dinh
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_VDMQ);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_MAPNGOAI);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_LANGCO);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_THEGIOI);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_MAPVIP);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_MAPVIP2);
            AutoSanBoss.saveBossHoursToRMS();
            // Cap nhat lai text field
            loadCurrentState();
            GameScr.gameAC("Boss Config: \u0110\u00e3 reset gi\u1edd v\u1ec1 m\u1eb7c \u0111\u1ecbnh");
            return; // Khong dong form
        }
        // Quay ve man hinh game (giong SetAuto)
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
    }

    private void saveGroup(ChoiceGroup cg, int[] maps) {
        for (int i = 0; i < maps.length; i++) {
            if (!cg.isSelected(i)) {
                // Map khong duoc chon = disabled
                AutoSanBoss.disabledMaps.addElement(new Integer(maps[i]));
            }
        }
    }

    /** An toan parse int tu TextField. Strip ky tu khong phai so. */
    private static int safeParseInt(String s, int fallback) {
        if (s == null) return fallback;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append(ch);
        }
        if (sb.length() == 0) return fallback;
        try {
            return Integer.parseInt(sb.toString());
        } catch (Exception e) {
            return fallback;
        }
    }
}
