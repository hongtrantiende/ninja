import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;

/**
 * TsConfig — Cai dat Tan Sat (TsBoost) + Auto Tu Sat + Auto Nhay.
 */
public final class TsConfig implements CommandListener {
    private static TsConfig instance;

    private Form form;
    private final Command cmdLuu;
    private final Command cmdHuy;
    private final Command cmdReset;

    // TsBoost fields
    private TextField tfAttackDelay;
    private TextField tfIdleDelay;
    private TextField tfRange;
    private TextField tfSkillReselect;
    private TextField tfMaxMob;
    private ChoiceGroup cgCooldown;
    private TextField tfCooldownMs;

    // AutoSuicide fields
    private ChoiceGroup cgSuicide;
    private ChoiceGroup cgTriggerMode;
    private TextField tfSuicideTimeout;
    private TextField tfSuicideCheck;

    // AutoJump fields
    private ChoiceGroup cgJump;
    private TextField tfJumpInterval;

    // Phan Than Lenh limiter fields
    private ChoiceGroup cgLimitPhanThan;
    private TextField tfLimitPhanThanMax;

    // Ghost Attack fields
    private ChoiceGroup cgGhostAttack;
    private TextField tfGhostRange;

    private TsConfig() {
        cmdLuu = new Command("L\u01b0u", Command.OK, 1);
        cmdHuy = new Command("H\u1ee7y", Command.BACK, 2);
        cmdReset = new Command("Reset", Command.SCREEN, 3);
        buildForm();
    }

    private void buildForm() {
        form = new Form("C\u00e0i \u0111\u1eb7t T\u00e0n S\u00e1t");
        form.addCommand(cmdLuu);
        form.addCommand(cmdHuy);
        form.addCommand(cmdReset);
        form.setCommandListener(this);

        // === TS Boost ===
        tfAttackDelay = new TextField("Delay \u0111\u00e1nh (ms)", "", 10, TextField.NUMERIC);
        form.append(tfAttackDelay);

        tfIdleDelay = new TextField("Delay h\u1ebft quai (ms)", "", 10, TextField.NUMERIC);
        form.append(tfIdleDelay);

        tfRange = new TextField("T\u1ea7m \u0111\u00e1nh (px)", "", 10, TextField.NUMERIC);
        form.append(tfRange);

        tfSkillReselect = new TextField("Ch\u1ecdn l\u1ea1i skill (ms)", "", 10, TextField.NUMERIC);
        form.append(tfSkillReselect);

        tfMaxMob = new TextField("Max quai/l\u1ea7n \u0111\u00e1nh", "", 10, TextField.NUMERIC);
        form.append(tfMaxMob);

        // === Mob Cooldown ===
        cgCooldown = new ChoiceGroup("Mob Cooldown (ch\u1ed1ng \u0111\u00e1nh tr\u01b0\u1ee3t)", Choice.MULTIPLE);
        cgCooldown.append("B\u1eadt cooldown", null);
        form.append(cgCooldown);

        tfCooldownMs = new TextField("Cooldown (ms)", "", 10, TextField.NUMERIC);
        form.append(tfCooldownMs);

        // === Auto Tu Sat ===
        cgSuicide = new ChoiceGroup("Auto T\u1ef1 S\u00e1t", Choice.MULTIPLE);
        cgSuicide.append("B\u1eadt khi \u0111\u1ee9ng im", null);
        form.append(cgSuicide);

        cgTriggerMode = new ChoiceGroup("K\u00edch ho\u1ea1t khi", Choice.EXCLUSIVE);
        cgTriggerMode.append("Ch\u1ec9 AK", null);
        cgTriggerMode.append("Ch\u1ec9 TS", null);
        cgTriggerMode.append("C\u1ea3 hai (AK + TS)", null);
        form.append(cgTriggerMode);

        tfSuicideTimeout = new TextField("Th\u1eddi gian \u0111\u1ee9ng im (gi\u00e2y)", "", 10, TextField.NUMERIC);
        form.append(tfSuicideTimeout);

        tfSuicideCheck = new TextField("Ki\u1ec3m tra m\u1ed7i (gi\u00e2y)", "", 10, TextField.NUMERIC);
        form.append(tfSuicideCheck);

        // === Auto Nhay ===
        cgJump = new ChoiceGroup("Auto Nh\u1ea3y (reset t\u1ecda \u0111\u1ed9)", Choice.MULTIPLE);
        cgJump.append("B\u1eadt nh\u1ea3y \u0111\u1ecbnh k\u1ef3", null);
        form.append(cgJump);

        tfJumpInterval = new TextField("Nh\u1ea3y m\u1ed7i (gi\u00e2y)", "", 10, TextField.NUMERIC);
        form.append(tfJumpInterval);

        // === Phan Than Lenh Limiter ===
        cgLimitPhanThan = new ChoiceGroup("Gi\u1edbi h\u1ea1n Ph\u00e2n Th\u00e2n L\u1ec7nh (ID 545)", Choice.MULTIPLE);
        cgLimitPhanThan.append("V\u1ee9t b\u1edbt khi v\u01b0\u1ee3t SL", null);
        form.append(cgLimitPhanThan);

        tfLimitPhanThanMax = new TextField("S\u1ed1 l\u01b0\u1ee3ng t\u1ed1i \u0111a", "", 5, TextField.NUMERIC);
        form.append(tfLimitPhanThanMax);

        // === Ghost Attack ===
        cgGhostAttack = new ChoiceGroup("Ghost Attack (\u0111\u00e1nh xa)", Choice.MULTIPLE);
        cgGhostAttack.append("B\u1eadt ghost move", null);
        form.append(cgGhostAttack);

        tfGhostRange = new TextField("T\u1ea7m ghost (px, 9999=full)", "", 5, TextField.NUMERIC);
        form.append(tfGhostRange);
    }

    private void loadCurrentState() {
        // TsBoost
        tfAttackDelay.setString(String.valueOf(TsBoost.ATTACK_DELAY_MS));
        tfIdleDelay.setString(String.valueOf(TsBoost.IDLE_DELAY_MS));
        tfRange.setString(String.valueOf(TsBoost.MAX_ATTACK_RANGE));
        tfSkillReselect.setString(String.valueOf(TsBoost.SKILL_RESELECT_MS));
        tfMaxMob.setString(String.valueOf(TsBoost.MAX_MOB_PER_ATTACK));

        // Cooldown
        cgCooldown.setSelectedIndex(0, TsBoost.isCooldownEnabled);
        tfCooldownMs.setString(String.valueOf(TsBoost.COOLDOWN_MS));

        // AutoSuicide
        cgSuicide.setSelectedIndex(0, AutoSuicide.isEnabled);
        cgTriggerMode.setSelectedIndex(AutoSuicide.triggerMode, true);
        tfSuicideTimeout.setString(String.valueOf(AutoSuicide.IDLE_TIMEOUT_MS / 1000));
        tfSuicideCheck.setString(String.valueOf(AutoSuicide.CHECK_INTERVAL_MS / 1000));

        // AutoJump
        cgJump.setSelectedIndex(0, AutoSuicide.isJumpEnabled);
        tfJumpInterval.setString(String.valueOf(AutoSuicide.JUMP_INTERVAL_MS / 1000));

        // Phan Than Lenh
        cgLimitPhanThan.setSelectedIndex(0, TsBoost.isLimitPhanThan);
        tfLimitPhanThanMax.setString(String.valueOf(TsBoost.LIMIT_PHANTHAN_MAX));

        // Ghost Attack
        cgGhostAttack.setSelectedIndex(0, TsBoost.isGhostAttack);
        tfGhostRange.setString(String.valueOf(TsBoost.GHOST_RANGE));
    }

    /** Mo form cai dat — goi tu NamMod menu */
    public static void select() {
        TsBoost.loadConfigFromRMS();
        AutoSuicide.loadConfigFromRMS();
        instance = new TsConfig();
        instance.loadCurrentState();
        Display.getDisplay(GameMidlet.instance).setCurrent(instance.form);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdLuu) {
            StringBuffer errors = new StringBuffer();

            // === TsBoost ===
            try {
                int v = safeParseInt(tfAttackDelay.getString(), -1);
                if (v >= 0 && v <= 5000) TsBoost.ATTACK_DELAY_MS = v;
                else errors.append("Delay danh(").append(v).append("), ");
            } catch (Exception e) { errors.append("Delay danh, "); }

            try {
                int v = safeParseInt(tfIdleDelay.getString(), -1);
                if (v >= 0 && v <= 5000) TsBoost.IDLE_DELAY_MS = v;
                else errors.append("Delay quai(").append(v).append("), ");
            } catch (Exception e) { errors.append("Delay quai, "); }

            try {
                int v = safeParseInt(tfRange.getString(), -1);
                if (v >= 10 && v <= 2000) TsBoost.MAX_ATTACK_RANGE = v;
                else errors.append("Tam(").append(v).append("), ");
            } catch (Exception e) { errors.append("Tam, "); }

            try {
                int v = safeParseInt(tfSkillReselect.getString(), -1);
                if (v >= 100 && v <= 60000) TsBoost.SKILL_RESELECT_MS = v;
                else errors.append("Skill(").append(v).append("), ");
            } catch (Exception e) { errors.append("Skill, "); }

            try {
                int v = safeParseInt(tfMaxMob.getString(), -1);
                if (v >= 1 && v <= 20) TsBoost.MAX_MOB_PER_ATTACK = v;
                else errors.append("MaxMob(").append(v).append("), ");
            } catch (Exception e) { errors.append("MaxMob, "); }

            // === Cooldown ===
            TsBoost.isCooldownEnabled = cgCooldown.isSelected(0);
            try {
                int v = safeParseInt(tfCooldownMs.getString(), -1);
                if (v >= 50 && v <= 30000) TsBoost.COOLDOWN_MS = v;
                else errors.append("CD(").append(v).append("), ");
            } catch (Exception e) { errors.append("CD, "); }
            TsBoost.saveConfigToRMS();

            // === AutoSuicide ===
            boolean wasEnabled = AutoSuicide.isEnabled;
            AutoSuicide.isEnabled = cgSuicide.isSelected(0);
            AutoSuicide.triggerMode = cgTriggerMode.getSelectedIndex();

            try {
                int v = safeParseInt(tfSuicideTimeout.getString(), -1);
                if (v >= 3 && v <= 600) AutoSuicide.IDLE_TIMEOUT_MS = v * 1000;
                else errors.append("Timeout(").append(v).append("), ");
            } catch (Exception e) { errors.append("Timeout, "); }

            try {
                int v = safeParseInt(tfSuicideCheck.getString(), -1);
                if (v >= 1 && v <= 120) AutoSuicide.CHECK_INTERVAL_MS = v * 1000;
                else errors.append("Check(").append(v).append("), ");
            } catch (Exception e) { errors.append("Check, "); }

            if (AutoSuicide.isEnabled && !wasEnabled) {
                AutoSuicide.start();
            } else if (!AutoSuicide.isEnabled && wasEnabled) {
                AutoSuicide.stop();
            }

            // === AutoJump ===
            boolean wasJump = AutoSuicide.isJumpEnabled;
            AutoSuicide.isJumpEnabled = cgJump.isSelected(0);

            try {
                int v = safeParseInt(tfJumpInterval.getString(), -1);
                if (v >= 3 && v <= 600) AutoSuicide.JUMP_INTERVAL_MS = v * 1000;
                else errors.append("Jump(").append(v).append("), ");
            } catch (Exception e) { errors.append("Jump, "); }

            if (AutoSuicide.isJumpEnabled && !wasJump) {
                AutoSuicide.startJump();
            } else if (!AutoSuicide.isJumpEnabled && wasJump) {
                AutoSuicide.stopJump();
            }

            AutoSuicide.saveConfigToRMS();

            // === Phan Than Lenh Limiter ===
            TsBoost.isLimitPhanThan = cgLimitPhanThan.isSelected(0);
            try {
                int v = safeParseInt(tfLimitPhanThanMax.getString(), -1);
                if (v >= 0 && v <= 99) TsBoost.LIMIT_PHANTHAN_MAX = v;
                else errors.append("PTL Max(").append(v).append("), ");
            } catch (Exception e) { errors.append("PTL Max, "); }

            // === Ghost Attack ===
            TsBoost.isGhostAttack = cgGhostAttack.isSelected(0);
            try {
                int v = safeParseInt(tfGhostRange.getString(), -1);
                if (v >= 100 && v <= 9999) TsBoost.GHOST_RANGE = v;
                else errors.append("GhostRange(").append(v).append("), ");
            } catch (Exception e) { errors.append("GhostRange, "); }

            TsBoost.saveConfigToRMS();

            if (errors.length() == 0) {
                GameScr.gameAC("TS Config: \u0110\u00e3 l\u01b0u!");
            } else {
                GameScr.gameAC("TS: L\u1ed7i: " + errors.toString());
            }
        } else if (c == cmdReset) {
            TsBoost.resetConfig();
            TsBoost.saveConfigToRMS();
            AutoSuicide.resetConfig();
            AutoSuicide.saveConfigToRMS();
            loadCurrentState();
            GameScr.gameAC("TS Config: \u0110\u00e3 reset v\u1ec1 m\u1eb7c \u0111\u1ecbnh");
            return;
        }
        Display.getDisplay(GameMidlet.instance).setCurrent(MotherCanvas.gI());
    }

    /**
     * An toan parse int tu TextField. Strip tat ca ky tu khong phai so.
     * J2ME Loader co the tra ve chuoi co ky tu an hoac space la.
     */
    private static int safeParseInt(String s, int fallback) {
        if (s == null) return fallback;
        // Strip moi thu khong phai digit
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
