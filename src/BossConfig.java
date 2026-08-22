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

    // 4 ChoiceGroup cho 4 loai boss
    private ChoiceGroup cgVDMQ;
    private ChoiceGroup cgMapNgoai;
    private ChoiceGroup cgLangCo;
    private ChoiceGroup cgTheGioi;

    // 4 TextField cho gio spawn
    private TextField tfVDMQ;
    private TextField tfMapNgoai;
    private TextField tfLangCo;
    private TextField tfTheGioi;

    // Luu map IDs tuong ung voi tung ChoiceGroup
    private final int[] mapsVDMQ;
    private final int[] mapsMN;
    private final int[] mapsLC;
    private final int[] mapsTG;

    private BossConfig() {
        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        cmdReset = new Command("Reset gi\u1edd", Command.SCREEN, 3);

        mapsVDMQ = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_VDMQ);
        mapsMN = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPNGOAI);
        mapsLC = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGCO);
        mapsTG = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_THEGIOI);

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
    }

    /** Load trang thai tu AutoSanBoss vao checkbox + textfield */
    private void loadCurrentState() {
        loadGroup(cgVDMQ, mapsVDMQ);
        loadGroup(cgMapNgoai, mapsMN);
        loadGroup(cgLangCo, mapsLC);
        loadGroup(cgTheGioi, mapsTG);

        // Load gio spawn
        tfVDMQ.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_VDMQ));
        tfMapNgoai.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_MAPNGOAI));
        tfLangCo.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_LANGCO));
        tfTheGioi.setString(AutoSanBoss.getBossHoursStr(AutoSanBoss.TYPE_THEGIOI));
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
            AutoSanBoss.saveToRMS();

            // Luu gio spawn
            int errCount = 0;
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_VDMQ, tfVDMQ.getString())) errCount++;
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_MAPNGOAI, tfMapNgoai.getString())) errCount++;
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_LANGCO, tfLangCo.getString())) errCount++;
            if (!AutoSanBoss.setBossHoursFromStr(AutoSanBoss.TYPE_THEGIOI, tfTheGioi.getString())) errCount++;
            AutoSanBoss.saveBossHoursToRMS();

            int disabled = AutoSanBoss.disabledMaps.size();
            StringBuffer msg = new StringBuffer("Boss Config: \u0110\u00e3 l\u01b0u");
            if (disabled > 0) msg.append(" (").append(disabled).append(" map t\u1eaft)");
            if (errCount > 0) msg.append(" | ").append(errCount).append(" gi\u1edd l\u1ed7i");
            GameScr.gameAC(msg.toString());
        } else if (c == cmdReset) {
            // Reset gio spawn ve mac dinh
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_VDMQ);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_MAPNGOAI);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_LANGCO);
            AutoSanBoss.resetBossHours(AutoSanBoss.TYPE_THEGIOI);
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
}
