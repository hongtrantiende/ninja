/** Menu tien ich rieng cua ban mod, mo tu menu 3 gach. */
public final class NamMod implements IActionListener {
    private static final int BOSS_MENU = 120100;
    private static final int AUTO_BOSS = 120101;
    private static final int LICH_BOSS = 120102;
    private static final int BOSS_VDMQ = 120105;
    private static final int BOSS_MAP_NGOAI = 120106;
    private static final int BOSS_ALL = 120107;
    private static final int BOSS_THEGIOI = 120110;
    private static final int HUT_VP = 120108;
    private static final int MOI_NHOM = 120109;
    private static final int AUTO_LEVEL = 120113;
    private static final int TS_BOSS_MENU = 120128;
    private static final int TS_BOSS_DEFAULT = 120129;
    private static final int TS_BOSS_VDMQ_LC = 120130;
    private static final int TS_BOSS_MN = 120131;
    private static final int TS_BOSS_TG = 120132;
    private static final int TS_BOSS_TEST = 120133;
    private static final int TS_BOSS_LC = 120134;
    private static final int TS_BOSS_VDMQ = 120135;
    private static final int TS_BOSS_MV = 120136;

    private static final int HIDE_ITEM_DROP = 120122;
    private static final int TS_VIP_MAP = 120123;
    private static final int TS_TU_LUYEN = 120124;
    private static final int BOSS_LANG_CO = 120125;
    private static final int BOSS_MAP_VIP = 120126;

    // Cai dat San Boss: mo Form checkbox
    private static final int CFG_BOSS_MENU = 120140;

    // Cai dat CN Test (exploit)
    private static final int CFG_EXPLOIT_MENU = 120150;

    // Cai dat Tan Sat
    private static final int CFG_TS_MENU = 120160;


    private static final int AUTO_BOSS_NOTICE = 120170;

    private static final NamMod INSTANCE = new NamMod();

    private NamMod() {
    }

    public static void open() {
        MyVector items = new MyVector();

        // === Săn Boss (ấn vào mở sub-menu) ===
        String bossLabel = "S\u0103n Boss";
        if (AutoSanBoss.isRunning) {
            int f = AutoSanBoss.forcedBossType;
            bossLabel += ": ON (" + (f == -1 ? "Auto" : f == AutoSanBoss.TYPE_ALL ? "All" : f == 0 ? "VDMQ" : f == 1 ? "MN" : f == 2 ? "LC" : f == 3 ? "TG" : f == 4 ? "MV" : "?") + ")";
        } else {
            bossLabel += ": OFF";
        }
        bossLabel += " \u25b8";
        items.addElement(command(bossLabel, BOSS_MENU));
        String tsLabel = "TS \u01b0u ti\u00ean Boss: " + onOff(AutoBossEvent.isEnabled);
        if (AutoBossEvent.isEnabled) tsLabel += " (" + AutoBossEvent.priorityName() + ")";
        tsLabel += " \u25b8";
        items.addElement(command(tsLabel, TS_BOSS_MENU));
        // Cai dat San Boss
        int disCount = AutoSanBoss.disabledMaps.size();
        String cfgLabel = "C\u00e0i \u0111\u1eb7t S\u0103n Boss";
        if (disCount > 0) cfgLabel += " (" + disCount + " map t\u1eaft)";
        cfgLabel += " \u25b8";
        items.addElement(command(cfgLabel, CFG_BOSS_MENU));
        items.addElement(command("L\u1ecbch Boss: " + onOff(ThongTinBoss.isEnable), LICH_BOSS));


        // === Hút VP & Tiện ích ===
        items.addElement(command("H\u00FAt VP: " + onOff(AutoPickup.isRunning), HUT_VP));
        items.addElement(command("\u1ea8n VP r\u01a1i: " + onOff(Code.hideItemDrop), HIDE_ITEM_DROP));

        String lvStatus = AutoLevel.isRunning
            ? "ON (Lv" + AutoLevel.targetLevel + ")"
            : "OFF";
        items.addElement(command("Auto Level: " + lvStatus, AUTO_LEVEL));

        // === Cài đặt Tàn Sát ===
        items.addElement(command("C\u00e0i \u0111\u1eb7t T\u00e0n S\u00e1t \u25b8", CFG_TS_MENU));

        // === Tiện ích Khác ===
        items.addElement(command("TS VIP Map: " + onOff(AutoVipMap.isEnabled), TS_VIP_MAP));
        items.addElement(command("TS Tu Luy\u1ec7n: " + onOff(AutoTuLuyen.isEnabled), TS_TU_LUYEN));
        items.addElement(command("M\u1eddi nh\u00f3m", MOI_NHOM));

        // === Exploit / Test ===
        items.addElement(command("C\u00e0i \u0111\u1eb7t CN Test \u25b8", CFG_EXPLOIT_MENU));

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
        if (type == AutoSanBoss.TYPE_ALL) return (forced == AutoSanBoss.TYPE_ALL) ? "ON" : "OFF";
        if (forced == -1) return "Auto";
        return (forced == type) ? "ON" : "OFF";
    }


    private static void openBossMenu() {
        MyVector items = new MyVector();
        items.addElement(command("S\u0103n Boss theo gi\u1edd: " + onOff(AutoSanBoss.isRunning), AUTO_BOSS));
        items.addElement(command("S\u0103n VDMQ: " + bossStatus(0), BOSS_VDMQ));
        items.addElement(command("S\u0103n Map Ngo\u00e0i: " + bossStatus(1), BOSS_MAP_NGOAI));
        items.addElement(command("S\u0103n L\u00e0ng C\u1ed5: " + bossStatus(2), BOSS_LANG_CO));
        items.addElement(command("S\u0103n Th\u1ebf Gi\u1edbi: " + bossStatus(3), BOSS_THEGIOI));
        items.addElement(command("S\u0103n Map VIP: " + bossStatus(AutoSanBoss.TYPE_MAPVIP), BOSS_MAP_VIP));
        items.addElement(command("S\u0103n T\u1ea5t C\u1ea3: " + bossStatus(AutoSanBoss.TYPE_ALL), BOSS_ALL));
        GameCanvas.menu.gameAA(items);
    }

    private static void openTsBossMenu() {
        MyVector items = new MyVector();
        boolean on = AutoBossEvent.isEnabled;
        int p = AutoBossEvent.eventPriority;
        items.addElement(command("T\u1ea5t c\u1ea3" + (on && p == 0 ? " \u2714" : ""), TS_BOSS_DEFAULT));
        items.addElement(command("Ch\u1ec9 V\u0110MQ" + (on && p == 5 ? " \u2714" : ""), TS_BOSS_VDMQ));
        items.addElement(command("Ch\u1ec9 L\u00e0ng C\u1ed5" + (on && p == 4 ? " \u2714" : ""), TS_BOSS_LC));
        items.addElement(command("VDMQ + L\u00e0ng C\u1ed5" + (on && p == 1 ? " \u2714" : ""), TS_BOSS_VDMQ_LC));
        items.addElement(command("Ch\u1ec9 Map Ngo\u00e0i" + (on && p == 2 ? " \u2714" : ""), TS_BOSS_MN));
        items.addElement(command("Ch\u1ec9 Th\u1ebf Gi\u1edbi" + (on && p == 3 ? " \u2714" : ""), TS_BOSS_TG));
        items.addElement(command("Ch\u1ec9 Map VIP" + (on && p == 6 ? " \u2714" : ""), TS_BOSS_MV));
        items.addElement(command("Test TS Boss (1 Map)", TS_BOSS_TEST));
        GameCanvas.menu.gameAA(items);
    }

    public void perform(int id, Object parameter) {
        switch (id) {
            case BOSS_MENU:
                openBossMenu();
                return;
            case CFG_BOSS_MENU:
                BossConfig.select();
                return;
            case CFG_TS_MENU:
                TsConfig.select();
                return;
            case AUTO_BOSS:
                AutoSanBoss.toggle();
                return;
            case TS_BOSS_MENU:
                openTsBossMenu();
                return;
            case TS_BOSS_DEFAULT:
                AutoBossEvent.togglePriority(0);
                return;
            case TS_BOSS_VDMQ_LC:
                AutoBossEvent.togglePriority(1);
                return;
            case TS_BOSS_MN:
                AutoBossEvent.togglePriority(2);
                return;
            case TS_BOSS_TG:
                AutoBossEvent.togglePriority(3);
                return;
            case TS_BOSS_LC:
                AutoBossEvent.togglePriority(4);
                return;
            case TS_BOSS_VDMQ:
                AutoBossEvent.togglePriority(5);
                return;
            case TS_BOSS_MV:
                AutoBossEvent.togglePriority(6);
                return;
            case TS_BOSS_TEST:
                AutoBossEvent.testNow();
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
            case BOSS_THEGIOI:
                AutoSanBoss.toggleTheGioi();
                return;
            case BOSS_ALL:
                AutoSanBoss.toggleALL();
                return;
            case BOSS_MAP_VIP:
                AutoSanBoss.toggleMapVIP();
                return;
            case HUT_VP:
                AutoPickup.toggle();
                return;
            case HIDE_ITEM_DROP:
                Code.hideItemDrop = !Code.hideItemDrop;
                GameScr.gameAC(Code.hideItemDrop ? "\u1ea8n VP r\u01a1i: ON" : "\u1ea8n VP r\u01a1i: OFF");
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
            case TS_TU_LUYEN:
                AutoTuLuyen.toggle();
                return;
            case MOI_NHOM:
                AutoSanBoss.autoInviteFriends();
                return;

            case CFG_EXPLOIT_MENU:
                ExploitConfig.select();
                return;

            default:
                return;
        }
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