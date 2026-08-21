import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;

/**
 * BossConfig — Man hinh cai dat San Boss kieu Form + ChoiceGroup (checkbox).
 * Giong SetAuto cua game goc: Form voi cac ChoiceGroup MULTIPLE,
 * nut Luu va Huy. Hien thi len man hinh thay cho GameCanvas.
 *
 * Moi loai boss la 1 ChoiceGroup rieng, hien thi cac map ID dang checkbox.
 * Khi an Luu: cap nhat AutoSanBoss.disabledMaps theo trang thai checkbox.
 * Khi an Huy: quay ve game, khong thay doi gi.
 */
public final class BossConfig implements CommandListener {
    private static BossConfig instance;

    private final Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;

    // 4 ChoiceGroup cho 4 loai boss
    private ChoiceGroup cgVDMQ;
    private ChoiceGroup cgMapNgoai;
    private ChoiceGroup cgLangCo;
    private ChoiceGroup cgTheGioi;

    // Luu map IDs tuong ung voi tung ChoiceGroup
    private final int[] mapsVDMQ;
    private final int[] mapsMN;
    private final int[] mapsLC;
    private final int[] mapsTG;

    private BossConfig() {
        form = new Form("C\u00e0i \u0111\u1eb7t S\u0103n Boss");

        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.setCommandListener(this);

        // === VDMQ ===
        mapsVDMQ = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_VDMQ);
        cgVDMQ = new ChoiceGroup("VDMQ", Choice.MULTIPLE);
        for (int i = 0; i < mapsVDMQ.length; i++) {
            cgVDMQ.append("Map " + mapsVDMQ[i], null);
        }
        form.append(cgVDMQ);

        // === Map Ngoai ===
        mapsMN = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_MAPNGOAI);
        cgMapNgoai = new ChoiceGroup("Map Ngo\u00e0i", Choice.MULTIPLE);
        for (int i = 0; i < mapsMN.length; i++) {
            cgMapNgoai.append("Map " + mapsMN[i], null);
        }
        form.append(cgMapNgoai);

        // === Lang Co ===
        mapsLC = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_LANGCO);
        cgLangCo = new ChoiceGroup("L\u00e0ng C\u1ed5", Choice.MULTIPLE);
        for (int i = 0; i < mapsLC.length; i++) {
            cgLangCo.append("Map " + mapsLC[i], null);
        }
        form.append(cgLangCo);

        // === The Gioi ===
        mapsTG = AutoSanBoss.getAllMapsForType(AutoSanBoss.TYPE_THEGIOI);
        cgTheGioi = new ChoiceGroup("Th\u1ebf Gi\u1edbi", Choice.MULTIPLE);
        for (int i = 0; i < mapsTG.length; i++) {
            cgTheGioi.append("Map " + mapsTG[i], null);
        }
        form.append(cgTheGioi);
    }

    /** Load trang thai tu AutoSanBoss.disabledMaps vao checkbox */
    private void loadCurrentState() {
        loadGroup(cgVDMQ, mapsVDMQ);
        loadGroup(cgMapNgoai, mapsMN);
        loadGroup(cgLangCo, mapsLC);
        loadGroup(cgTheGioi, mapsTG);
    }

    private void loadGroup(ChoiceGroup cg, int[] maps) {
        for (int i = 0; i < maps.length; i++) {
            // Checked = map duoc bat (khong nam trong disabledMaps)
            cg.setSelectedIndex(i, AutoSanBoss.isMapEnabled(maps[i]));
        }
    }

    /** Mo form cai dat — goi tu NamMod menu */
    public static void select() {
        if (instance == null) {
            instance = new BossConfig();
        }
        instance.loadCurrentState();
        // Hien form len man hinh (giong SetAuto.timelite)
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    /** Xu ly nut Luu / Huy */
    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            // Luu: xoa disabledMaps, roi them lai cac map khong duoc chon
            AutoSanBoss.disabledMaps.removeAllElements();
            saveGroup(cgVDMQ, mapsVDMQ);
            saveGroup(cgMapNgoai, mapsMN);
            saveGroup(cgLangCo, mapsLC);
            saveGroup(cgTheGioi, mapsTG);

            int disabled = AutoSanBoss.disabledMaps.size();
            if (disabled == 0) {
                GameScr.gameAC("Boss Config: \u0110\u00e3 l\u01b0u (t\u1ea5t c\u1ea3 b\u1eadt)");
            } else {
                GameScr.gameAC("Boss Config: \u0110\u00e3 l\u01b0u (" + disabled + " map t\u1eaft)");
            }
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
