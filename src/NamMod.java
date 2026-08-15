/** Menu tien ich rieng cua ban mod, mo tu menu 3 gach. */
public final class NamMod implements IActionListener {
    private static final int AUTO_BOSS = 120101;
    private static final int LICH_BOSS = 120102;
    private static final int BOSS_VDMQ = 120105;
    private static final int BOSS_MAP_NGOAI = 120106;
    private static final int BOSS_ALL = 120107;
    private static final int HUT_VP = 120108;
    private static final int MOI_NHOM = 120109;
    private static final int TACH_LE = 120110;
    private static final int THONG_TIN = 120111;
    private static final int AUTO_LEVEL = 120113;
    private static final int TREO_ALL = 120115;
    private static final int TREO_VM = 120118;
    private static final int TREO_MN = 120119;
    private static final int TS_BOSS = 120120;
    private static final int BOSS_RADAR = 120121;
    private static final int HIDE_ITEM_DROP = 120122;
    private static final int TS_VIP_MAP = 120123;
    private static final int BOSS_LANG_CO = 120125;
    private static final int TREO_LANG_CO = 120126;
    private static final int GIU_VP = 120127;

    private static final NamMod INSTANCE = new NamMod();

    private NamMod() {
    }

    public static void open() {
        MyVector items = new MyVector();

        // === Săn Boss ===
        items.addElement(command("S\u0103n Boss: " + onOff(AutoSanBoss.isRunning), AUTO_BOSS));
        items.addElement(command("TS \u01b0u ti\u00ean Boss: " + onOff(AutoBossEvent.isEnabled), TS_BOSS));
        items.addElement(command("L\u1ecbch Boss: " + onOff(ThongTinBoss.isEnable), LICH_BOSS));
        items.addElement(command("Radar Boss: " + onOff(BossRadar.isRunning), BOSS_RADAR));

        // Trang thai tung loai boss (0: VDMQ, 1: MapNgoai, 2: LangCo, 3: Tat Ca)
        items.addElement(command("S\u0103n VDMQ: " + bossStatus(0), BOSS_VDMQ));
        items.addElement(command("S\u0103n Map Ngo\u00e0i: " + bossStatus(1), BOSS_MAP_NGOAI));
        items.addElement(command("S\u0103n L\u00e0ng C\u1ed5: " + bossStatus(2), BOSS_LANG_CO));
        items.addElement(command("S\u0103n T\u1ea5t C\u1ea3: " + bossStatus(3), BOSS_ALL));

        // === Treo Boss (tìm boss, không đánh) ===
        items.addElement(command("Treo T\u1ea5t C\u1ea3: " + treoStatus(3), TREO_ALL));
        items.addElement(command("Treo VDMQ: " + treoStatus(0), TREO_VM));
        items.addElement(command("Treo Map Ngo\u00e0i: " + treoStatus(1), TREO_MN));
        items.addElement(command("Treo L\u00e0ng C\u1ed5: " + treoStatus(2), TREO_LANG_CO));

        // === Hút VP & Tiện ích ===
        items.addElement(command("H\u00FAt VP: " + onOff(AutoPickup.isRunning), HUT_VP));
        items.addElement(command("\u1ea8n VP r\u01a1i: " + onOff(Code.hideItemDrop), HIDE_ITEM_DROP));
        items.addElement(command("Gi\u1eef VP: " + onOff(Code.giuVP), GIU_VP));

        // === Auto Level ===
        String lvStatus = AutoLevel.isRunning
            ? "ON (Lv" + AutoLevel.targetLevel + ")"
            : "OFF";
        items.addElement(command("Auto Level: " + lvStatus, AUTO_LEVEL));

        // === Tiện ích Khác ===
        items.addElement(command("TS VIP Map: " + onOff(AutoVipMap.isEnabled), TS_VIP_MAP));
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

    private static String bossStatus(int type) {
        if (!AutoSanBoss.isRunning) return "OFF";
        int forced = AutoSanBoss.forcedBossType;
        if (type == 3) return (forced == 3) ? "ON" : "OFF";
        if (forced == -1) return "Auto";
        return (forced == type) ? "ON" : "OFF";
    }

    private static String treoStatus(int type) {
        if (!AutoSanBoss.isRunning || !AutoSanBoss.treoMode) return "OFF";
        int forced = AutoSanBoss.forcedBossType;
        if (type == 3) return (forced == 3) ? "ON" : "OFF";
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
            case BOSS_VDMQ:
                AutoSanBoss.toggleVM();
                return;
            case BOSS_MAP_NGOAI:
                AutoSanBoss.toggleMN();
                return;
            case BOSS_LANG_CO:
                AutoSanBoss.toggleLangCo();
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
            case GIU_VP:
                Code.giuVP = !Code.giuVP;
                if (Code.giuVP) Code.hideItemDrop = false;
                GameScr.gameAC(Code.giuVP ? "Gi\u1eef VP: ON" : "Gi\u1eef VP: OFF");
                return;
            case AUTO_LEVEL:
                if (AutoLevel.isRunning) {
                    AutoLevel.stop();
                } else {
                    openAutoLevelInput();
                }
                return;
            case TS_VIP_MAP:
                AutoVipMap.toggle();
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
            case TREO_VM:
                AutoSanBoss.toggleTreoVM();
                return;
            case TREO_MN:
                AutoSanBoss.toggleTreoMN();
                return;
            case TREO_LANG_CO:
                AutoSanBoss.toggleTreoLangCo();
                return;
            default:
                return;
        }
    }

    private static void showModInfo() {
        String info = "Nam Mod v2";
        if (AutoSanBoss.isRunning) {
            int f = AutoSanBoss.forcedBossType;
            info += " | Boss:" + (f == -1 ? "Auto" : f == 2 ? "ALL" : f == 0 ? "VDMQ" : "MapNgoai");
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
                        GameScr.gameAC("Nam Mod: Level ph\u1ea3i t\u1eebr 10 \u0111\u1ebfn 99!");
                    }
                } catch (Exception e) {
                    GameScr.gameAC("Nam Mod: Level kh\u00f4ng h\u1ee3p l\u1ec7!");
                }
                GameCanvas.endDlg();
            }
        }, 120114, null), 1);
        GameCanvas.inputDlg.tfInput.gameAA("99");
    }
}