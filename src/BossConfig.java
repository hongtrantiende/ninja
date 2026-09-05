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

    // 3 ChoiceGroup cho 3 loai boss
    private ChoiceGroup cgVDMQ;
    private ChoiceGroup cgMapNgoai;
    private ChoiceGroup cgLangCo;
    private ChoiceGroup cgLangTT;

    // TextField cho gio spawn
    private TextField tfVDMQ;
    private TextField tfMapNgoai;
    private TextField tfLangCo;
    private TextField tfLangTT;

    // Extra rounds cho TS Boss uu tien
    private TextField tfExtraRounds;
    // So giay truoc spawn bat dau san
    private TextField tfPreSpawnBreak;
    // Toc do chuyen khu khi tim boss
    private TextField tfZoneDelay;
    // Pham vi khu quet boss (VD: 0-29 hoac 1-29)
    private TextField tfZoneRange;



    // So lan hoi sinh toi da khi danh boss (0=vo han)
    private TextField tfMaxDeathRevive;

    // San nguoc map (MapNgoai & VDMQ)
    private ChoiceGroup cgReverseHunt;

    // Luu map IDs tuong ung voi tung ChoiceGroup
    private final int[] mapsVDMQ;
    private final int[] mapsMN;
    private final int[] mapsLC;
    private final int[] mapsLTT;

    private BossConfig() {
        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        cmdReset = new Command("Reset gi\u1edd", Command.SCREEN, 3);

        mapsVDMQ = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_VDMQ);
        mapsMN = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPNGOAI);
        mapsLC = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGCO);
        mapsLTT = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGTT);

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

        // === Lang TT ===
        tfLangTT = new TextField("Gi\u1edd L\u00e0ng TT", "", 100, TextField.ANY);
        form.append(tfLangTT);
        cgLangTT = new ChoiceGroup("L\u00e0ng TT - Map", Choice.MULTIPLE);
        for (int i = 0; i < mapsLTT.length; i++) {
            cgLangTT.append("Map " + mapsLTT[i], null);
        }
        form.append(cgLangTT);


        // === TS Boss uu tien: so luot quet them ===
        tfExtraRounds = new TextField("L\u01b0\u1ee3t qu\u00e9t th\u00eam (0=kh\u00f4ng, 1=m\u1eb7c \u0111\u1ecbnh)", "", 5, TextField.NUMERIC);
        form.append(tfExtraRounds);

        // === So giay truoc spawn bat dau san ===
        tfPreSpawnBreak = new TextField("S\u0103n tr\u01b0\u1edbc spawn (gi\u00e2y, 0-30)", "", 5, TextField.NUMERIC);
        form.append(tfPreSpawnBreak);

        // === Toc do chuyen khu (ms) ===
        tfZoneDelay = new TextField("T\u1ed1c \u0111\u1ed9 chuy\u1ec3n khu (ms, 10-5000)", "", 6, TextField.NUMERIC);
        form.append(tfZoneDelay);

        // === Pham vi khu quet (VD: 0-29 hoac 1-29) ===
        tfZoneRange = new TextField("Ph\u1ea1m vi khu (VD: 0-29 ho\u1eb7c 1-29)", "", 10, TextField.ANY);
        form.append(tfZoneRange);



        // === So lan HS toi da (0=vo han) ===
        tfMaxDeathRevive = new TextField("S\u1ed1 l\u1ea7n HS t\u1ed1i \u0111a (0=v\u00f4 h\u1ea1n)", "", 5, TextField.NUMERIC);
        form.append(tfMaxDeathRevive);

        // === San nguoc map (MapNgoai & VDMQ) ===
        cgReverseHunt = new ChoiceGroup("Th\u1ee9 t\u1ef1 s\u0103n map", Choice.MULTIPLE);
        cgReverseHunt.append("S\u0103n ng\u01b0\u1ee3c (VDMQ & MapNgo\u00e0i)", null);
        form.append(cgReverseHunt);
    }

    /** Load trang thai tu AutoSanBoss vao checkbox + textfield */
    private void loadCurrentState() {
        loadGroup(cgVDMQ, mapsVDMQ);
        loadGroup(cgMapNgoai, mapsMN);
        loadGroup(cgLangCo, mapsLC);
        loadGroup(cgLangTT, mapsLTT);

        // Load gio spawn
        tfVDMQ.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_VDMQ));
        tfMapNgoai.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_MAPNGOAI));
        tfLangCo.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_LANGCO));
        tfLangTT.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_LANGTT));

        // Extra rounds
        tfExtraRounds.setString(String.valueOf(AutoBossEvent.extraRounds));

        // Pre-spawn break
        tfPreSpawnBreak.setString(String.valueOf(AutoBossEvent.preSpawnBreakSec));

        // Zone delay (ms)
        tfZoneDelay.setString(String.valueOf(AutoSanBoss.zoneChangeDelayMs));

        // Zone range
        tfZoneRange.setString(AutoSanBoss.getZoneRangeStr());


        // So lan HS toi da
        tfMaxDeathRevive.setString(String.valueOf(AutoSanBoss.maxDeathRevive));

        // San nguoc map
        cgReverseHunt.setSelectedIndex(0, AutoSanBoss.isReverseMapHunt);
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
            saveGroup(cgLangTT, mapsLTT);
            AutoSanBoss.saveToRMS();

            // Luu gio spawn
            StringBuffer errMsg = new StringBuffer();
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_VDMQ, tfVDMQ.getString()))
                errMsg.append("VDMQ, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_MAPNGOAI, tfMapNgoai.getString()))
                errMsg.append("MapNgoai, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_LANGCO, tfLangCo.getString()))
                errMsg.append("LangCo, ");
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_LANGTT, tfLangTT.getString()))
                errMsg.append("LangTT, ");
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

            // Luu pre-spawn break
            try {
                String s2 = tfPreSpawnBreak.getString();
                int v2 = safeParseInt(s2, -1);
                if (v2 >= 0 && v2 <= 30) {
                    AutoBossEvent.preSpawnBreakSec = v2;
                } else {
                    if (errMsg.length() > 0) errMsg.append(", ");
                    errMsg.append("PreSpawn(").append(v2).append(")");
                }
            } catch (Exception e) {
                errMsg.append("PreSpawn");
            }
            AutoBossEvent.saveConfigToRMS();

            // Luu toc do chuyen khu (ms)
            try {
                String sDelay = tfZoneDelay.getString();
                int vDelay = safeParseInt(sDelay, -1);
                if (vDelay >= 10 && vDelay <= 5000) {
                    AutoSanBoss.zoneChangeDelayMs = vDelay;
                } else {
                    if (errMsg.length() > 0) errMsg.append(", ");
                    errMsg.append("Delay(").append(vDelay).append(")");
                }
            } catch (Exception e) {
                errMsg.append("Delay");
            }
            AutoSanBoss.saveToRMS();

            // Luu pham vi khu (VD: 0-29 hoac 1-29)
            if (!AutoSanBoss.setZoneRangeFromStr(tfZoneRange.getString())) {
                if (errMsg.length() > 0) errMsg.append(", ");
                errMsg.append("ZoneRange");
            }



            // Luu so lan HS toi da (0=vo han)
            try {
                int md = safeParseInt(tfMaxDeathRevive.getString(), 100);
                if (md >= 0) {
                    AutoSanBoss.maxDeathRevive = md;
                }
            } catch (Exception e) {}

            // Luu san nguoc map
            AutoSanBoss.isReverseMapHunt = cgReverseHunt.isSelected(0);
            AutoSanBoss.saveToRMS();

            int disabled = AutoSanBoss.disabledMaps.size();
            StringBuffer msg = new StringBuffer("Boss Config: \u0110\u00e3 l\u01b0u");
            if (disabled > 0) msg.append(" (").append(disabled).append(" map t\u1eaft)");
            msg.append(" | Delay: ").append(AutoSanBoss.zoneChangeDelayMs).append("ms");
            msg.append(" | Khu: ").append(AutoSanBoss.getZoneRangeStr());
            msg.append(" | HS max: ").append(AutoSanBoss.maxDeathRevive == 0 ? "V\u00f4 h\u1ea1n" : String.valueOf(AutoSanBoss.maxDeathRevive));
            msg.append(" | S\u0103n ng\u01b0\u1ee3c: ").append(AutoSanBoss.isReverseMapHunt ? "B\u1eadt" : "T\u1eaft");
            if (errMsg.length() > 0) msg.append(" | Gi\u1edd l\u1ed7i: ").append(errMsg);
            GameScr.gameAC(msg.toString());
        } else if (c == cmdReset) {
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_VDMQ);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_MAPNGOAI);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_LANGCO);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_LANGTT);
            AutoSanBoss.saveBossHoursToRMS();
            AutoSanBoss.zoneChangeDelayMs = 10;
            AutoSanBoss.scanZoneStart = 0;
            AutoSanBoss.scanZoneEnd = 29;
            AutoSanBoss.maxDeathRevive = 0;
            AutoSanBoss.isReverseMapHunt = false;
            AutoSanBoss.saveToRMS();
            // Cap nhat lai text field
            loadCurrentState();
            GameScr.gameAC("Boss Config: \u0110\u00e3 reset v\u1ec1 m\u1eb7c \u0111\u1ecbnh (10ms, 0-29, HS v\u00f4 h\u1ea1n, S\u0103n xu\u00f4i)");
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
