import os
import sys
import glob
import shutil
import zipfile
import subprocess

root = os.path.abspath(os.path.dirname(__file__))
build_dir = os.path.join(root, "build")
unpacked_dir = os.path.join(build_dir, "unpacked")
stubs_dir = os.path.join(build_dir, "stubs_compiled")
share_jar_path = os.path.join(root, "Aeharuna_share.jar")
base_jar_path = os.path.join(root, "Aeharuna.jar")
src_backup_dir = os.path.join(build_dir, "src_backup")

print("=== [SHARE BUILD] 1. Backing up original src/ ===")
if os.path.exists(src_backup_dir):
    shutil.rmtree(src_backup_dir)
shutil.copytree(os.path.join(root, "src"), src_backup_dir)

try:
    print("=== [SHARE BUILD] 2. Applying share-specific configs (VIP Map 160ms, native PkBoss for normal maps, simplified BossConfig) in src/ ===")

    # 1. Write simplified BossConfig.java (only Map VIP 1 and Map VIP 2 checkboxes)
    bossconfig_content = """import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;

public final class BossConfig implements CommandListener {
    private static BossConfig instance;

    private Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;

    private ChoiceGroup cgMapVIP;
    private ChoiceGroup cgMapVIP2;

    private BossConfig() {
        cmdLuu = new Command("L\\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\\u1ee7y", Command.BACK, 2);
        buildForm();
    }

    private void buildForm() {
        form = new Form("C\\u00e0i \\u0111\\u1eb7t S\\u0103n Boss");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.setCommandListener(this);

        cgMapVIP = new ChoiceGroup("Map VIP", Choice.MULTIPLE);
        cgMapVIP.append("S\\u0103n Map VIP (M195)", null);
        form.append(cgMapVIP);

        cgMapVIP2 = new ChoiceGroup("Map VIP2", Choice.MULTIPLE);
        cgMapVIP2.append("S\\u0103n Map VIP2 (M196)", null);
        form.append(cgMapVIP2);
    }

    private void loadCurrentState() {
        cgMapVIP.setSelectedIndex(0, AutoSanBoss.isMapEnabled(195));
        cgMapVIP2.setSelectedIndex(0, AutoSanBoss.isMapEnabled(196));
    }

    public static void select() {
        AutoSanBoss.loadFromRMS();
        if (instance == null) {
            instance = new BossConfig();
        }
        instance.loadCurrentState();
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            boolean en195 = cgMapVIP.isSelected(0);
            boolean en196 = cgMapVIP2.isSelected(0);

            for (int i = AutoSanBoss.disabledMaps.size() - 1; i >= 0; i--) {
                Integer m = (Integer) AutoSanBoss.disabledMaps.elementAt(i);
                if (m != null && (m.intValue() == 195 || m.intValue() == 196)) {
                    AutoSanBoss.disabledMaps.removeElementAt(i);
                }
            }
            if (!en195) AutoSanBoss.disabledMaps.addElement(new Integer(195));
            if (!en196) AutoSanBoss.disabledMaps.addElement(new Integer(196));
            AutoSanBoss.saveToRMS();

            GameScr.gameAC("Boss Config: \\u0110\\u00e3 l\\u01b0u (VIP1: " + (en195 ? "B\\u1eadt" : "T\\u1eaft") + ", VIP2: " + (en196 ? "B\\u1eadt" : "T\\u1eaft") + ")");
        }
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
    }
}
"""
    bossconfig_file = os.path.join(root, "src", "BossConfig.java")
    with open(bossconfig_file, "w", encoding="utf-8") as f:
        f.write(bossconfig_content)

    # 2. Write simplified ExploitConfig.java (only Spam NPC, keep CN Test in NamMod)
    exploitconfig_share_content = r"""import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;

/**
 * ExploitConfig (Ban Share) — Chi gom chuc nang Spam NPC Test.
 */
public final class ExploitConfig implements CommandListener {
    private static ExploitConfig instance;

    private Form form;
    private final javax.microedition.lcdui.Command cmdLuu;
    private final javax.microedition.lcdui.Command cmdHuy;
    private final javax.microedition.lcdui.Command cmdReset;
    private final javax.microedition.lcdui.Command cmdTestNpc;

    // === UI Fields (Spam NPC) ===
    private ChoiceGroup cgNpcRepeat;
    private TextField tfNpcRepeatCount;
    private TextField tfNpcType;
    private ChoiceGroup cgNpcOpt1Enable;
    private TextField tfNpcOpt1;
    private TextField tfNpcDelay1;
    private ChoiceGroup cgNpcOpt2Enable;
    private TextField tfNpcOpt2;
    private TextField tfNpcDelay2;
    private ChoiceGroup cgNpcOpt3Enable;
    private TextField tfNpcOpt3;
    private TextField tfNpcDelay3;

    // === CONFIG FIELDS ===
    public static boolean isNpcRepeat = false;
    public static int NPC_REPEAT_COUNT = 5;
    public static int NPC_TYPE = 47;
    public static boolean isNpcOpt1Enable = true;
    public static int NPC_OPT1 = 0;
    public static int NPC_DELAY1 = 1000;
    public static boolean isNpcOpt2Enable = true;
    public static int NPC_OPT2 = 2;
    public static int NPC_DELAY2 = 1000;
    public static boolean isNpcOpt3Enable = false;
    public static int NPC_OPT3 = 0;
    public static int NPC_DELAY3 = 1000;

    // Compatibility stubs for other classes
    public static boolean isFastAttack = false;
    public static int FAST_ATTACK_COUNT = 3;
    public static boolean isDupePickup = false;
    public static int DUPE_PICKUP_COUNT = 3;
    public static boolean isMultiHit = false;
    public static int MULTI_HIT_COUNT = 3;
    public static int triggerMode = 0;
    public static boolean isActive() { return false; }

    private static final int DEF_NPC_REPEAT_COUNT = 5;
    private static final int DEF_NPC_TYPE = 47;
    private static final int DEF_NPC_OPT1 = 0;
    private static final int DEF_NPC_OPT2 = 2;

    static {
        loadConfigFromRMS();
    }

    private ExploitConfig() {
        cmdLuu = new javax.microedition.lcdui.Command("L\u01b0u", javax.microedition.lcdui.Command.OK, 1);
        cmdHuy = new javax.microedition.lcdui.Command("H\u1ee7y", javax.microedition.lcdui.Command.BACK, 2);
        cmdReset = new javax.microedition.lcdui.Command("Reset", javax.microedition.lcdui.Command.SCREEN, 3);
        cmdTestNpc = new javax.microedition.lcdui.Command("Test NPC Spam", javax.microedition.lcdui.Command.SCREEN, 4);
        buildForm();
    }

    private void buildForm() {
        form = new Form("C\u00e0i \u0111\u1eb7t Spam NPC");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.addCommand(cmdReset);
        form.addCommand(cmdTestNpc);
        form.setCommandListener(this);

        cgNpcRepeat = new ChoiceGroup("# Spam NPC", Choice.MULTIPLE);
        cgNpcRepeat.append("B\u1eadt", null);
        form.append(cgNpcRepeat);
        tfNpcRepeatCount = new TextField("S\u1ed1 l\u1ea7n l\u1eb7p chu k\u1ef3", "", 5, TextField.NUMERIC);
        form.append(tfNpcRepeatCount);
        tfNpcType = new TextField("NPC Type (47=VIP)", "", 5, TextField.NUMERIC);
        form.append(tfNpcType);

        cgNpcOpt1Enable = new ChoiceGroup("\u00d4 1", Choice.MULTIPLE);
        cgNpcOpt1Enable.append("B\u1eadt \u00f4 1", null);
        form.append(cgNpcOpt1Enable);
        tfNpcOpt1 = new TextField("Index \u00f4 1 (0=\u00f4 1)", "", 5, TextField.NUMERIC);
        form.append(tfNpcOpt1);
        tfNpcDelay1 = new TextField("Delay sau \u00f4 1 (ms)", "", 6, TextField.NUMERIC);
        form.append(tfNpcDelay1);

        cgNpcOpt2Enable = new ChoiceGroup("\u00d4 2", Choice.MULTIPLE);
        cgNpcOpt2Enable.append("B\u1eadt \u00f4 2", null);
        form.append(cgNpcOpt2Enable);
        tfNpcOpt2 = new TextField("Index \u00f4 2 (0=\u00f4 1)", "", 5, TextField.NUMERIC);
        form.append(tfNpcOpt2);
        tfNpcDelay2 = new TextField("Delay sau \u00f4 2 (ms)", "", 6, TextField.NUMERIC);
        form.append(tfNpcDelay2);

        cgNpcOpt3Enable = new ChoiceGroup("\u00d4 3 (\u0110\u1ed3ng \u00fd/C\u00f3)", Choice.MULTIPLE);
        cgNpcOpt3Enable.append("B\u1eadt \u00f4 3 (\u0110\u1ed3ng \u00fd/C\u00f3)", null);
        form.append(cgNpcOpt3Enable);
        tfNpcOpt3 = new TextField("Index \u00f4 3 (0=C\u00f3/Menu con)", "", 5, TextField.NUMERIC);
        form.append(tfNpcOpt3);
        tfNpcDelay3 = new TextField("Delay sau \u00f4 3 (ms)", "", 6, TextField.NUMERIC);
        form.append(tfNpcDelay3);

        form.append("\u1ea4n 'Test NPC Spam': Ch\u1ec9 th\u1ef1c hi\u1ec7n c\u00e1c \u00f4 \u0111\u01b0\u1ee3c B\u1eacT. \u00d4 3 s\u1ebd t\u1ef1 \u0111\u1ed9ng x\u00e1c nh\u1eadn 'C\u00f3'/'\u0110\u1ed3ng \u00fd' tr\u00ean b\u1ea3ng.");
    }

    private void loadCurrentState() {
        cgNpcRepeat.setSelectedIndex(0, isNpcRepeat);
        tfNpcRepeatCount.setString(String.valueOf(NPC_REPEAT_COUNT));
        tfNpcType.setString(String.valueOf(NPC_TYPE));

        cgNpcOpt1Enable.setSelectedIndex(0, isNpcOpt1Enable);
        tfNpcOpt1.setString(String.valueOf(NPC_OPT1));
        tfNpcDelay1.setString(String.valueOf(NPC_DELAY1));

        cgNpcOpt2Enable.setSelectedIndex(0, isNpcOpt2Enable);
        tfNpcOpt2.setString(String.valueOf(NPC_OPT2));
        tfNpcDelay2.setString(String.valueOf(NPC_DELAY2));

        cgNpcOpt3Enable.setSelectedIndex(0, isNpcOpt3Enable);
        tfNpcOpt3.setString(String.valueOf(NPC_OPT3));
        tfNpcDelay3.setString(String.valueOf(NPC_DELAY3));
    }

    public static void select() {
        loadConfigFromRMS();
        instance = new ExploitConfig();
        instance.loadCurrentState();
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    public void commandAction(javax.microedition.lcdui.Command c, Displayable d) {
        if (c == cmdLuu) {
            StringBuffer errors = new StringBuffer();

            isNpcRepeat = cgNpcRepeat.isSelected(0);
            try { int v = safeParseInt(tfNpcRepeatCount.getString(), -1);
                if (v >= 1 && v <= 200) NPC_REPEAT_COUNT = v;
                else errors.append("NpcCnt(").append(v).append("), ");
            } catch (Exception e) { errors.append("NpcCnt, "); }
            try { int v = safeParseInt(tfNpcType.getString(), -1);
                if (v >= 0 && v <= 255) NPC_TYPE = v;
                else errors.append("NpcT(").append(v).append("), ");
            } catch (Exception e) { errors.append("NpcT, "); }

            isNpcOpt1Enable = cgNpcOpt1Enable.isSelected(0);
            try { int v = safeParseInt(tfNpcOpt1.getString(), -1);
                if (v >= 0 && v <= 255) NPC_OPT1 = v;
                else errors.append("Opt1(").append(v).append("), ");
            } catch (Exception e) { errors.append("Opt1, "); }
            try { int v = safeParseInt(tfNpcDelay1.getString(), -1);
                if (v >= 0 && v <= 10000) NPC_DELAY1 = v;
                else errors.append("Dly1(").append(v).append("), ");
            } catch (Exception e) { errors.append("Dly1, "); }

            isNpcOpt2Enable = cgNpcOpt2Enable.isSelected(0);
            try { int v = safeParseInt(tfNpcOpt2.getString(), -1);
                if (v >= 0 && v <= 255) NPC_OPT2 = v;
                else errors.append("Opt2(").append(v).append("), ");
            } catch (Exception e) { errors.append("Opt2, "); }
            try { int v = safeParseInt(tfNpcDelay2.getString(), -1);
                if (v >= 0 && v <= 10000) NPC_DELAY2 = v;
                else errors.append("Dly2(").append(v).append("), ");
            } catch (Exception e) { errors.append("Dly2, "); }

            isNpcOpt3Enable = cgNpcOpt3Enable.isSelected(0);
            try { int v = safeParseInt(tfNpcOpt3.getString(), -1);
                if (v >= 0 && v <= 255) NPC_OPT3 = v;
                else errors.append("Opt3(").append(v).append("), ");
            } catch (Exception e) { errors.append("Opt3, "); }
            try { int v = safeParseInt(tfNpcDelay3.getString(), -1);
                if (v >= 0 && v <= 10000) NPC_DELAY3 = v;
                else errors.append("Dly3(").append(v).append("), ");
            } catch (Exception e) { errors.append("Dly3, "); }

            saveConfigToRMS();

            if (errors.length() == 0) {
                GameScr.gameAC("CN Test: \u0110\u00e3 l\u01b0u!");
            } else {
                GameScr.gameAC("CN: L\u1ed7i: " + errors.toString());
            }
        } else if (c == cmdReset) {
            resetConfig();
            saveConfigToRMS();
            loadCurrentState();
            GameScr.gameAC("CN Test: \u0110\u00e3 reset");
            return;
        } else if (c == cmdTestNpc) {
            isNpcRepeat = cgNpcRepeat.isSelected(0);
            try { NPC_REPEAT_COUNT = Integer.parseInt(tfNpcRepeatCount.getString().trim()); } catch (Exception e) {}
            try { NPC_TYPE = Integer.parseInt(tfNpcType.getString().trim()); } catch (Exception e) {}
            isNpcOpt1Enable = cgNpcOpt1Enable.isSelected(0);
            try { NPC_OPT1 = Integer.parseInt(tfNpcOpt1.getString().trim()); } catch (Exception e) {}
            try { NPC_DELAY1 = Integer.parseInt(tfNpcDelay1.getString().trim()); } catch (Exception e) {}
            isNpcOpt2Enable = cgNpcOpt2Enable.isSelected(0);
            try { NPC_OPT2 = Integer.parseInt(tfNpcOpt2.getString().trim()); } catch (Exception e) {}
            try { NPC_DELAY2 = Integer.parseInt(tfNpcDelay2.getString().trim()); } catch (Exception e) {}
            isNpcOpt3Enable = cgNpcOpt3Enable.isSelected(0);
            try { NPC_OPT3 = Integer.parseInt(tfNpcOpt3.getString().trim()); } catch (Exception e) {}
            try { NPC_DELAY3 = Integer.parseInt(tfNpcDelay3.getString().trim()); } catch (Exception e) {}
            saveConfigToRMS();
            Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
            startNpcSpam();
            return;
        }
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
    }

    public static void saveConfigToRMS() {
        try {
            String data = "0;0;0;0;0;0;"
                + (isNpcRepeat?1:0) + ";" + NPC_REPEAT_COUNT + ";" + NPC_DELAY1 + ";"
                + "0;0;0;"
                + NPC_TYPE + ";" + NPC_OPT1 + ";" + NPC_OPT2 + ";"
                + "0;"
                + (isNpcOpt3Enable?1:0) + ";" + NPC_OPT3 + ";"
                + NPC_DELAY2 + ";" + NPC_DELAY3 + ";"
                + (isNpcOpt1Enable?1:0) + ";" + (isNpcOpt2Enable?1:0);
            RMS.gameAA("exploit_cfg", data);
        } catch (Exception e) {}
    }

    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("exploit_cfg");
            if (data != null && data.length() > 0) {
                int[] v = new int[30];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 30; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        v[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 9) { isNpcRepeat = v[6]==1; NPC_REPEAT_COUNT = v[7]; NPC_DELAY1 = v[8]; }
                if (idx >= 15) { NPC_TYPE = v[12]; NPC_OPT1 = v[13]; NPC_OPT2 = v[14]; }
                if (idx >= 18) { isNpcOpt3Enable = v[16] == 1; NPC_OPT3 = v[17]; }
                if (idx >= 20) { NPC_DELAY2 = v[18]; NPC_DELAY3 = v[19]; }
                if (idx >= 22) { isNpcOpt1Enable = v[20] == 1; isNpcOpt2Enable = v[21] == 1; }
            }
        } catch (Exception e) {}
    }

    public static void resetConfig() {
        isNpcRepeat = false; NPC_REPEAT_COUNT = DEF_NPC_REPEAT_COUNT; NPC_DELAY1 = 1000; NPC_DELAY2 = 1000; NPC_DELAY3 = 1000;
        NPC_TYPE = DEF_NPC_TYPE; NPC_OPT1 = DEF_NPC_OPT1; NPC_OPT2 = DEF_NPC_OPT2;
        isNpcOpt1Enable = true; isNpcOpt2Enable = true; isNpcOpt3Enable = false;
    }

    public static boolean tryAcceptDialog() {
        try {
            Dialog dlg = GameCanvas.currentDialog;
            if (dlg == null) {
                dlg = GameCanvas.msgdlg;
            }
            if (dlg != null) {
                Command cmd = dlg.left;
                if (cmd == null) {
                    cmd = dlg.center;
                }
                if (cmd != null) {
                    cmd.gameAA();
                    if (cmd.idAction == 8890 && cmd.p instanceof Integer) {
                        try {
                            Service.gI().gameAO(((Integer) cmd.p).intValue());
                        } catch (Exception ex) {}
                    }
                    if (cmd.idAction == 88842) {
                        try {
                            Service.gI().gameBC();
                        } catch (Exception ex) {}
                    }
                    GameCanvas.endDlg();
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    private static void startNpcSpam() {
        final int npcT = NPC_TYPE;
        final boolean opt1Enable = isNpcOpt1Enable;
        final int opt1 = NPC_OPT1;
        final int d1 = NPC_DELAY1 > 0 ? NPC_DELAY1 : 1000;
        final boolean opt2Enable = isNpcOpt2Enable;
        final int opt2 = NPC_OPT2;
        final int d2 = NPC_DELAY2 > 0 ? NPC_DELAY2 : 1000;
        final boolean opt3Enable = isNpcOpt3Enable;
        final int opt3 = NPC_OPT3;
        final int d3 = NPC_DELAY3 > 0 ? NPC_DELAY3 : 1000;
        final int count = NPC_REPEAT_COUNT;

        new Thread() {
            public void run() {
                try {
                    GameScr.gameAC("NPC Spam: B\u1eaft \u0111\u1ea7u (" + count + " l\u1ea7n)");
                    Thread.sleep(500);

                    for (int i = 0; i < count; i++) {
                        Service.gI().gameAH(npcT);
                        Thread.sleep(150);

                        if (opt1Enable) {
                            Service.gI().gameAC(npcT, opt1, 0);
                            Thread.sleep(d1);
                        }

                        if (opt2Enable) {
                            Service.gI().gameAC(npcT, opt2, 0);
                            Thread.sleep(d2);
                        }

                        if (opt3Enable) {
                            boolean accepted = false;
                            long waitStart = System.currentTimeMillis();
                            long maxWait = d3 > 500 ? d3 : 1500;
                            while (System.currentTimeMillis() - waitStart < maxWait) {
                                if (tryAcceptDialog()) {
                                    accepted = true;
                                    break;
                                }
                                Thread.sleep(50L);
                            }

                            if (!accepted) {
                                Service.gI().gameAC(npcT, opt3, 0);
                                for (int t = 0; t < 10; t++) {
                                    Thread.sleep(50L);
                                    if (tryAcceptDialog()) {
                                        accepted = true;
                                        break;
                                    }
                                }
                            }
                            Thread.sleep(d3);
                        }
                    }

                    GameScr.gameAC("NPC Spam: Xong! (" + count + " l\u1ea7n)");
                } catch (Exception e) {
                    GameScr.gameAC("NPC Spam: L\u1ed7i - " + e.getMessage());
                }
            }
        }.start();
    }

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
"""
    exploitconfig_file = os.path.join(root, "src", "ExploitConfig.java")
    with open(exploitconfig_file, "w", encoding="utf-8") as f:
        f.write(exploitconfig_share_content)

    # 2b. Write simplified NamMod.java (only Spam NPC in menu)
    nammod_share_content = r"""public final class NamMod implements IActionListener {
    private static final int CFG_EXPLOIT_MENU = 120150;
    private static final NamMod INSTANCE = new NamMod();

    private NamMod() {
    }

    public static void open() {
        MyVector items = new MyVector();
        items.addElement(command("Spam NPC \u25b8", CFG_EXPLOIT_MENU));
        GameCanvas.menu.gameAA(items);
    }

    private static Command command(String caption, int id) {
        return new Command(caption, INSTANCE, id, null);
    }

    public void perform(int id, Object parameter) {
        if (id == CFG_EXPLOIT_MENU) {
            ExploitConfig.select();
        }
    }
}
"""
    nammod_file = os.path.join(root, "src", "NamMod.java")
    with open(nammod_file, "w", encoding="utf-8") as f:
        f.write(nammod_share_content)

    # 2c. Disable AutoPickup (Hut VP) completely in share build
    autopickup_share_content = r"""public class AutoPickup implements Runnable {
    public static boolean isRunning = false;
    public static void toggle() {}
    public static void start() {}
    public static void syncAfterAutoCommand() {}
    public static void stop() {}
    public static void grabOnce() {}
    public void run() {}
}
"""
    autopickup_file = os.path.join(root, "src", "AutoPickup.java")
    with open(autopickup_file, "w", encoding="utf-8") as f:
        f.write(autopickup_share_content)

    # 2d. Remove Hut VP button from SplitPatcher.java in share build
    splitpatcher_file = os.path.join(root, "src", "SplitPatcher.java")
    with open(splitpatcher_file, "r", encoding="utf-8") as f:
        sp_code = f.read()

    old_hutvp_hook = r"""                // Hook nut "Nhat Xa / Hut VP" goc (1100080) -> AutoPickup.toggle()
                if (cmd.idAction == 1100080) {
                    String label = AutoPickup.isRunning ? "H\u00fat VP: ON" : "H\u00fat VP: OFF";
                    Command hutVp = new Command(label, new IActionListener() {
                        public void perform(int id, Object p) {
                            AutoPickup.toggle();
                        }
                    }, 1100080, null);
                    var1.setElementAt(hutVp, i);
                }"""
    new_hutvp_hook = r"""                // Xoa bo nut Hut VP / Nhat Xa trong ban share
                if (cmd.idAction == 1100080) {
                    var1.removeElementAt(i);
                    i--;
                    continue;
                }"""
    if old_hutvp_hook in sp_code:
        sp_code = sp_code.replace(old_hutvp_hook, new_hutvp_hook)
    else:
        print("[WARN] old_hutvp_hook not found in SplitPatcher.java!")
    with open(splitpatcher_file, "w", encoding="utf-8") as f:
        f.write(sp_code)

    # 2e. Turn off default gameAQ in Code.java in share build
    code_file = os.path.join(root, "src", "Code.java")
    with open(code_file, "r", encoding="utf-8") as f:
        c_code = f.read()
    c_code = c_code.replace("gameAQ = true;", "gameAQ = false;")
    with open(code_file, "w", encoding="utf-8") as f:
        f.write(c_code)

    # 3. Modify AutoBossEvent.java: disable pre-spawn in share (set to 0s)
    autobosseven_file = os.path.join(root, "src", "AutoBossEvent.java")
    with open(autobosseven_file, "r", encoding="utf-8") as f:
        event_code = f.read()

    event_code = event_code.replace("private static final int PRE_SPAWN_SECONDS = 30;", "private static final int PRE_SPAWN_SECONDS = 0;")
    with open(autobosseven_file, "w", encoding="utf-8") as f:
        f.write(event_code)

    # 4. Modify AutoSanBoss.java: Map VIP zone scan delay = 160ms, native PkBoss for normal maps
    autosanboss_file = os.path.join(root, "src", "AutoSanBoss.java")
    with open(autosanboss_file, "r", encoding="utf-8") as f:
        sanboss_code = f.read()

    # Map VIP delay = 160ms
    sanboss_code = sanboss_code.replace("try { Auto.gameAA(zone); } catch (Exception e) {}\n            sleep(50);", "try { Auto.gameAA(zone); } catch (Exception e) {}\n            sleep(100);")

    # Restore native PkBoss for normal maps in pkBossOnMap
    pk_start_idx = sanboss_code.find("private boolean pkBossOnMap(int mapID) {")
    pk_end_idx = sanboss_code.find("private void sleepSeconds(int seconds) {")

    if pk_start_idx != -1 and pk_end_idx != -1:
        native_pk_method = """private boolean pkBossOnMap(int mapID) {
        if (!checkStillRunning()) return false;
        if (mapID == 135 || mapID == 136) {
            return pkLangCoMap(mapID);
        } else if (mapID == 195) {
            return pkBossMapVIP();
        } else if (mapID == 196) {
            return pkBossMapVIP2();
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
            if (TileMap.mapID == 195 || TileMap.mapID == 196) {
                GameScr.gameAC("TSB: Tho\\u00e1t Map VIP \\u0111\\u1ec3 s\\u0103n map kh\\u00e1c...");
                suicideAndEnsureAlive();
            }
        }
        GameScr.gameAC("TSB: PK M" + mapID);

        // Leader start PkBoss solo - quet khu, tim boss bang PkBoss goc
        try {
            Code.gameAA(new PkBoss(mapID));
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                Code.gameAA(new PkBoss(mapID));
            } else {
                return false;
            }
        }

        long startTime = System.currentTimeMillis();
        boolean sentPartyCmd = false;
        boolean bossKilled = false;
        boolean keepFighting = true;
        int deathCount = 0;
        final int MAX_DEATH_RETRIES = 10;
        final long MAX_FIGHT_TIME_MS = 10 * 60 * 1000;

        while (checkStillRunning() && keepFighting) {
            if (System.currentTimeMillis() - startTime > MAX_FIGHT_TIME_MS) {
                GameScr.gameAC("TSB: Timeout M" + mapID + " (10 phut)");
                break;
            }

            if (!(Code.gameAB instanceof PkBoss)) {
                if (sentPartyCmd && deathCount < MAX_DEATH_RETRIES && checkStillRunning()) {
                    sleep(500);
                    if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                        GameScr.gameAC("TSB: Chet lan " + deathCount + "! Hoi sinh...");
                        respawnFast();
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        sleep(1000);
                    }
                    if (checkStillRunning() && Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                        GameScr.gameAC("TSB: Quay lai M" + mapID + " (lan " + (deathCount + 1) + ")");
                        Code.gameAA(new PkBoss(mapID));
                        if (TileMap.mapID != mapID) {
                            sentPartyCmd = false;
                        }
                        sleep(500);
                        continue;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            try {
                if (isDisconnected()) {
                    GameScr.gameAC("TSB: M\\u1ea5t k\\u1ebft n\\u1ed1i khi PK M" + mapID + "!");
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }

                if (!sentPartyCmd && hasBossOnCurrentMap()) {
                    sentPartyCmd = true;
                    GameScr.gameAC("TSB: Boss! Goi nhom M" + mapID + " K" + TileMap.zoneID);
                    sendPartyCommand("pkm -1");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(500);
                    sendPartyCommand("pkk " + TileMap.zoneID);
                }

                if (sentPartyCmd) {
                    lockBossFocus();
                }

                if (sentPartyCmd && !hasBossOnCurrentMap()) {
                    bossKilled = true;
                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    break;
                }

                if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                    deathCount++;
                    GameScr.gameAC("TSB: Chet lan " + deathCount + "! Hoi sinh...");
                    respawnFast();
                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }
                    sleep(500);
                    continue;
                }
            } catch (Exception e) {
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }
            }
            sleep(200);
        }

        restoreDummyAuto();

        if (bossKilled) {
            long elapsed = System.currentTimeMillis() - startTime;
            GameScr.gameAC("TSB: Xong M" + mapID + " (" + (elapsed / 1000) + "s)");
            grabAllItems();
            return true;
        }
        if (deathCount >= MAX_DEATH_RETRIES) {
            GameScr.gameAC("TSB: M" + mapID + " chet " + deathCount + " lan, bo qua");
        }
        return false;
    }

    """
        sanboss_code = sanboss_code[:pk_start_idx] + native_pk_method + sanboss_code[pk_end_idx:]

    with open(autosanboss_file, "w", encoding="utf-8") as f:
        f.write(sanboss_code)

    print("=== [SHARE BUILD] 3. Clean & Unpack base Aeharuna.jar ===")
    if os.path.exists(unpacked_dir):
        shutil.rmtree(unpacked_dir)
    if os.path.exists(stubs_dir):
        shutil.rmtree(stubs_dir)
    os.makedirs(unpacked_dir, exist_ok=True)
    os.makedirs(stubs_dir, exist_ok=True)

    with zipfile.ZipFile(base_jar_path, 'r') as z:
        z.extractall(unpacked_dir)

    for f in glob.glob(os.path.join(unpacked_dir, "AutoPickup*.class")):
        os.remove(f)
    for f in glob.glob(os.path.join(unpacked_dir, "NamMod*.class")):
        os.remove(f)

    print("=== [SHARE BUILD] 4. Compile Stubs & Src ===")
    stubs_files = glob.glob(os.path.join(root, "stubs", "**", "*.java"), recursive=True)
    src_files = glob.glob(os.path.join(root, "src", "**", "*.java"), recursive=True)

    cmd_stubs = ["javac", "--release", "8", "-encoding", "UTF-8", "-d", stubs_dir] + stubs_files
    subprocess.run(cmd_stubs, check=True)

    cp_path = os.pathsep.join([unpacked_dir, stubs_dir])
    cmd_src = ["javac", "--release", "8", "-encoding", "UTF-8", "-cp", cp_path, "-d", unpacked_dir] + src_files
    subprocess.run(cmd_src, check=True)

    print("=== [SHARE BUILD] 5. Run Python Patches ===")
    subprocess.run(["python3", "scripts/patch_class_j2me.py", unpacked_dir], check=True)

    effect_auto_class = os.path.join(unpacked_dir, "EffectAuto.class")
    if os.path.exists(effect_auto_class):
        subprocess.run(["python3", "scripts/patch_effectauto.py", effect_auto_class], check=True)

    print("=== [SHARE BUILD] 6. Pack Aeharuna_share.jar ===")
    for root_d, dirs_d, files_d in os.walk(unpacked_dir):
        for f in files_d:
            if f.endswith(".bak") or "bak_effects" in f:
                os.remove(os.path.join(root_d, f))

    manifest_file = os.path.join(unpacked_dir, "META-INF", "MANIFEST.MF")
    subprocess.run(["jar", "cfm", share_jar_path, manifest_file, "."], cwd=unpacked_dir, check=True)
    print(f"=== SUCCESS! Aeharuna_share.jar created: {os.path.getsize(share_jar_path)} bytes ===")

finally:
    print("=== [SHARE BUILD] 7. Restoring original src/ files from backup ===")
    if os.path.exists(src_backup_dir):
        shutil.rmtree(os.path.join(root, "src"))
        shutil.copytree(src_backup_dir, os.path.join(root, "src"))
        shutil.rmtree(src_backup_dir)
    print("=== Original src/ restored cleanly with 50ms boss hunting! ===")
