/** Menu tien ich rieng cua ban mod, mo tu menu 3 gach. */
public final class NamMod implements IActionListener {
    private static final int AUTO_BOSS = 120101;
    private static final int LICH_BOSS = 120102;
    private static final int BOSS_SERVER = 120103;
    private static final int BOSS_THE_GIOI = 120104;
    private static final int BOSS_VDMQ = 120105;
    private static final int BOSS_MAP_NGOAI = 120106;
    private static final int BOSS_ALL = 120107;
    private static final int HUT_VP = 120108;
    private static final int MOI_NHOM = 120109;
    private static final int TACH_LE = 120110;
    private static final int THONG_TIN = 120111;
    private static final int AUTO_LEVEL = 120113;
    private static final int TREO_ALL = 120115;
    private static final int TREO_SV = 120116;
    private static final int TREO_TG = 120117;
    private static final int TREO_VM = 120118;
    private static final int TREO_MN = 120119;
    private static final int TS_BOSS = 120120;
    private static final int BOSS_RADAR = 120121;
    private static final int HIDE_ITEM_DROP = 120122;
    private static final int MUA_MAP_VIP = 120123;
    private static final int DUNG_MAP_VIP = 120124;
    private static final int HIDE_SKILL_EFFECT = 120125;

    private static final NamMod INSTANCE = new NamMod();

    private NamMod() {
    }

    public static void open() {
        MyVector items = new MyVector();

        // === H\u00FAt VP & \u1ea8n Skill ===
        items.addElement(command("H\u00FAt VP: " + onOff(AutoPickup.isRunning), HUT_VP));
        items.addElement(command("\u1ea8n VP r\u01a1i: " + onOff(Code.hideItemDrop), HIDE_ITEM_DROP));
        items.addElement(command("\u1ea8n skill \u0111\u00e1nh: " + onOff(Code.hideSkillEffect), HIDE_SKILL_EFFECT));

        // === S\u0103n Boss ===
        items.addElement(command("S\u0103n Boss: " + onOff(AutoSanBoss.isRunning), AUTO_BOSS));
        items.addElement(command("TS \u01b0u ti\u00ean Boss: " + onOff(AutoBossEvent.isEnabled), TS_BOSS));
        items.addElement(command("L\u1ecbch Boss: " + onOff(ThongTinBoss.isEnable), LICH_BOSS));
        items.addElement(command("Radar Boss: " + onOff(BossRadar.isRunning), BOSS_RADAR));

        // Trang thai tung loai boss - hien ON neu dang chay loai do
        items.addElement(command("S\u0103n Server: " + bossStatus(0), BOSS_SERVER));
        items.addElement(command("S\u0103n Th\u1ebf Gi\u1edbi: " + bossStatus(1), BOSS_THE_GIOI));
        items.addElement(command("S\u0103n VDMQ: " + bossStatus(2), BOSS_VDMQ));
        items.addElement(command("S\u0103n Map Ngo\u00e0i: " + bossStatus(3), BOSS_MAP_NGOAI));
        items.addElement(command("S\u0103n T\u1ea5t C\u1ea3: " + bossStatus(4), BOSS_ALL));

        // === Treo Boss (t\u00ecm boss, kh\u00f4ng \u0111\u00e1nh) ===
        items.addElement(command("Treo T\u1ea5t C\u1ea3: " + treoStatus(4), TREO_ALL));
        items.addElement(command("Treo Server: " + treoStatus(0), TREO_SV));
        items.addElement(command("Treo Th\u1ebf Gi\u1edbi: " + treoStatus(1), TREO_TG));
        items.addElement(command("Treo VDMQ: " + treoStatus(2), TREO_VM));
        items.addElement(command("Treo Map Ngo\u00e0i: " + treoStatus(3), TREO_MN));

        // === Auto Level ===
        String lvStatus = AutoLevel.isRunning
            ? "ON (Lv" + AutoLevel.targetLevel + ")"
            : "OFF";
        items.addElement(command("Auto Level: " + lvStatus, AUTO_LEVEL));

        // === Ti\u1ec7n \u00edch ===
        items.addElement(command("Mua th\u1ebb map vip: " + onOff(Code.MuaMapVip), MUA_MAP_VIP));
        items.addElement(command("D\u00f9ng th\u1ebb map vip: " + onOff(Code.DungMapVip), DUNG_MAP_VIP));
        items.addElement(command("M\u1eddi nh\u00f3m", MOI_NHOM));
        items.addElement(command("T\u00e1ch \u0111\u1ed3 l\u1ebb", TACH_LE));
        items.addElement(command("Th\u00f4ng tin Nam Mod", THONG_TIN));
        GameCanvas.menu.gameAA(items);
    }

    private static Command command(String caption, int id) {
        return new Command(caption, INSTANCE, id, null);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    /**
     * Trang thai loai boss: ON neu dang chay va forcedBossType trung.
     * -1 = auto schedule (hien ON khi isRunning + forcedBossType == -1).
     */
    private static String bossStatus(int type) {
        if (!AutoSanBoss.isRunning) return "OFF";
        int forced = AutoSanBoss.forcedBossType;
        // Type 4 = Tat Ca (TYPE_ALL)
        if (type == 4) return (forced == 4) ? "ON" : "OFF";
        // Auto schedule (-1) hien ON cho nut Sán Boss chinh
        if (forced == -1) return "Auto";
        return (forced == type) ? "ON" : "OFF";
    }

    private static String treoStatus(int type) {
        if (!AutoSanBoss.isRunning || !AutoSanBoss.treoMode) return "OFF";
        int forced = AutoSanBoss.forcedBossType;
        if (type == 4) return (forced == 4) ? "ON" : "OFF";
        return (forced == type) ? "ON" : "OFF";
    }

    public void perform(int id, Object parameter) {
        switch (id) {
            case AUTO_BOSS:
                AutoSanBoss.toggle();
                return;
            case TS_BOSS:
                AutoBossEvent.toggle();
                return;
            case BOSS_RADAR:
                BossRadar.toggle();
                return;
            case LICH_BOSS:
                ThongTinBoss.toggle();
                return;
            case BOSS_SERVER:
                AutoSanBoss.toggleSV();
                return;
            case BOSS_THE_GIOI:
                AutoSanBoss.toggleTG();
                return;
            case BOSS_VDMQ:
                AutoSanBoss.toggleVM();
                return;
            case BOSS_MAP_NGOAI:
                AutoSanBoss.toggleMN();
                return;
            case BOSS_ALL:
                AutoSanBoss.toggleALL();
                return;
            case HUT_VP:
                AutoPickup.toggle();
                return;
            case HIDE_ITEM_DROP:
                Code.hideItemDrop = !Code.hideItemDrop;
                GameScr.gameAC(Code.hideItemDrop ? "\u1ea8n VP r\u01a1i: ON" : "\u1ea8n VP r\u01a1i: OFF");
                return;
            case HIDE_SKILL_EFFECT:
                Code.hideSkillEffect = !Code.hideSkillEffect;
                GameScr.gameAC(Code.hideSkillEffect ? "\u1ea8n skill \u0111\u00e1nh: ON" : "\u1ea8n skill \u0111\u00e1nh: OFF");
                return;
            case AUTO_LEVEL:
                if (AutoLevel.isRunning) {
                    AutoLevel.stop();
                } else {
                    openAutoLevelInput();
                }
                return;
            case MUA_MAP_VIP:
                Code.MuaMapVip = !Code.MuaMapVip;
                GameScr.gameAC(Code.MuaMapVip ? "Mua th\u1ebb map vip: ON" : "Mua th\u1ebb map vip: OFF");
                return;
            case DUNG_MAP_VIP:
                Code.DungMapVip = !Code.DungMapVip;
                if (Code.DungMapVip) {
                    Code.startVipMapWatcher();
                }
                GameScr.gameAC(Code.DungMapVip ? "D\u00f9ng th\u1ebb map vip: ON" : "D\u00f9ng th\u1ebb map vip: OFF");
                return;
            case MOI_NHOM:
                AutoSanBoss.autoInviteFriends();
                return;
            case TACH_LE:
                openSplitInput();
                return;
            case THONG_TIN:
                showModInfo();
                return;
            case TREO_ALL:
                AutoSanBoss.toggleTreo();
                return;
            case TREO_SV:
                AutoSanBoss.toggleTreoSV();
                return;
            case TREO_TG:
                AutoSanBoss.toggleTreoTG();
                return;
            case TREO_VM:
                AutoSanBoss.toggleTreoVM();
                return;
            case TREO_MN:
                AutoSanBoss.toggleTreoMN();
                return;
            default:
                return;
        }
    }

    private static void showModInfo() {
        String info = "Nam Mod v2";
        if (AutoSanBoss.isRunning) {
            int f = AutoSanBoss.forcedBossType;
            info += " | Boss:" + (f == -1 ? "Auto" : f == 4 ? "ALL" : "F" + f);
        }
        if (AutoPickup.isRunning) info += " | H\u00FAtVP:ON";
        if (ThongTinBoss.isEnable) info += " | TTB:ON";
        if (AutoLevel.isRunning) info += " | ALv:" + AutoLevel.targetLevel;
        GameScr.gameAC(info);
    }

    private static void openSplitInput() {
        GameCanvas.inputDlg.gameAA("Nh\u1eadp s\u1ed1 l\u01b0\u1ee3ng t\u00e1ch l\u1ebb", new Command("T\u00e1ch", new IActionListener() {
            public void perform(int id, Object parameter) {
                try {
                    int count = Integer.parseInt(GameCanvas.inputDlg.tfInput.gameAD().trim());
                    SplitPatcher.doTachLeDirect(count);
                } catch (Exception e) {
                    GameScr.gameAC("Nam Mod: S\u1ed1 l\u01b0\u1ee3ng kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                GameCanvas.endDlg();
            }
        }, 120112, null), 1);
        GameCanvas.inputDlg.tfInput.gameAA("3");
    }

    private static void openAutoLevelInput() {
        GameCanvas.inputDlg.gameAA("Nh\u1eadp level m\u1ee5c ti\u00eau (10-99)", new Command("B\u1eaft \u0111\u1ea7u", new IActionListener() {
            public void perform(int id, Object parameter) {
                try {
                    int lv = Integer.parseInt(GameCanvas.inputDlg.tfInput.gameAD().trim());
                    if (lv >= 10 && lv <= 99) {
                        AutoLevel.start(lv);
                    } else {
                        GameScr.gameAC("Level ph\u1ea3i t\u1eeb 10-99!");
                    }
                } catch (Exception e) {
                    GameScr.gameAC("S\u1ed1 kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                GameCanvas.endDlg();
            }
        }, 120114, null), 1);
        GameCanvas.inputDlg.tfInput.gameAA("99");
    }
}