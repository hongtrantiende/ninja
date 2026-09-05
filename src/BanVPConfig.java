import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

/**
 * BanVPConfig — Giao dien Form cai dat Tu Ban VP (NPC 46).
 */
public final class BanVPConfig implements CommandListener {
    private static BanVPConfig instance;

    private Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;
    private final Command cmdTest;

    private ChoiceGroup cgEnable;
    private ChoiceGroup cgSellMode;
    private TextField tfThreshold;
    private ChoiceGroup cgItems;

    private BanVPConfig() {
        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        cmdTest = new Command("B\u00e1n ngay (Test)", Command.SCREEN, 3);
        buildForm();
    }

    private void buildForm() {
        form = new Form("C\u00e0i \u0111\u1eb7t B\u00e1n VP (NPC 46)");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.addCommand(cmdTest);
        form.setCommandListener(this);

        cgEnable = new ChoiceGroup("T\u1ef1 B\u00e1n VP", Choice.MULTIPLE);
        cgEnable.append("B\u1eadt t\u1ef1 b\u00e1n khi \u0111\u1ee7 SL", null);
        form.append(cgEnable);

        cgSellMode = new ChoiceGroup("Ch\u1ebf \u0111\u1ed9 b\u00e1n khi v\u1ec1 l\u00e0ng", Choice.EXCLUSIVE);
        cgSellMode.append("B\u00e1n t\u1ea5t c\u1ea3 (to\u00e0n b\u1ed9)", null);
        cgSellMode.append("B\u00e1n l\u1ebb 1 c\u00e1i (\u0111\u1ec3 test)", null);
        form.append(cgSellMode);

        tfThreshold = new TextField("SL \u0111\u1ea1t \u0111\u1ebfn \u0111\u1ec3 v\u1ec1 b\u00e1n", "", 6, TextField.NUMERIC);
        form.append(tfThreshold);

        cgItems = new ChoiceGroup("V\u1eadt ph\u1ea9m b\u00e1n (Menu NPC 46)", Choice.MULTIPLE);
        cgItems.append("Chuy\u1ec3n tinh th\u1ea1ch (454 - \u00d4 0)", null);
        cgItems.append("T\u1eed tinh th\u1ea1ch cao (457 - \u00d4 1)", null);
        cgItems.append("T\u1eed tinh th\u1ea1ch trung (456 - \u00d4 2)", null);
        cgItems.append("T\u1eed tinh th\u1ea1ch s\u01a1 (455 - \u00d4 3)", null);
        cgItems.append("Ph\u00e2n th\u00e2n l\u1ec7nh (545 - \u00d4 4)", null);
        form.append(cgItems);
    }

    /** Mo form cai dat */
    public static void select() {
        AutoBanVP.loadConfigFromRMS();
        instance = new BanVPConfig();
        instance.loadCurrentState();
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    private void loadCurrentState() {
        cgEnable.setSelectedIndex(0, AutoBanVP.isEnabled);
        int mode = (AutoBanVP.sellMode == AutoBanVP.MODE_ONE) ? 1 : 0;
        cgSellMode.setSelectedIndex(mode, true);
        tfThreshold.setString(String.valueOf(AutoBanVP.threshold));
        for (int i = 0; i < 5; i++) {
            cgItems.setSelectedIndex(i, AutoBanVP.sellItems[i]);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            AutoBanVP.isEnabled = cgEnable.isSelected(0);
            AutoBanVP.sellMode = cgSellMode.getSelectedIndex();
            int v = safeParseInt(tfThreshold.getString(), 10);
            if (v <= 0) v = 1;
            AutoBanVP.threshold = v;
            for (int i = 0; i < 5; i++) {
                AutoBanVP.sellItems[i] = cgItems.isSelected(i);
            }
            AutoBanVP.saveConfigToRMS();

            if (AutoBanVP.isEnabled) {
                AutoBanVP.start();
                GameScr.gameAC("B\u00e1n VP: ON (SL>=" + AutoBanVP.threshold + ", " + (AutoBanVP.sellMode == AutoBanVP.MODE_ONE ? "B\u00e1n 1 c\u00e1i" : "B\u00e1n t\u1ea5t") + ")");
            } else {
                AutoBanVP.stop();
                GameScr.gameAC("B\u00e1n VP: OFF");
            }
            Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
            return;
        }

        if (c == cmdTest) {
            AutoBanVP.sellMode = cgSellMode.getSelectedIndex();
            int v = safeParseInt(tfThreshold.getString(), 10);
            if (v <= 0) v = 1;
            AutoBanVP.threshold = v;
            for (int i = 0; i < 5; i++) {
                AutoBanVP.sellItems[i] = cgItems.isSelected(i);
            }
            AutoBanVP.saveConfigToRMS();
            Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
            GameScr.gameAC("B\u00e1n VP: B\u1eaft \u0111\u1ea7u test b\u00e1n ngay...");
            AutoBanVP.startSellNow();
            return;
        }

        // Huy
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
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
