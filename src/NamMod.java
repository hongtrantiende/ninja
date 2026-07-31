/** Menu tien ich rieng cua ban mod, mo tu menu 3 gach. */
public final class NamMod implements IActionListener {
    private static final int AUTO_BOSS = 120101;
    private static final int LICH_BOSS = 120102;
    private static final int BOSS_SERVER = 120103;
    private static final int BOSS_THE_GIOI = 120104;
    private static final int BOSS_VDMQ = 120105;
    private static final int BOSS_MAP_NGOAI = 120106;
    private static final int BOSS_ALL = 120107;
    private static final int NHAT_NHANH = 120108;
    private static final int MOI_NHOM = 120109;
    private static final int TACH_LE = 120110;
    private static final int THONG_TIN = 120111;

    private static final NamMod INSTANCE = new NamMod();

    private NamMod() {
    }

    public static void open() {
        MyVector items = new MyVector();
        items.addElement(command("Săn Boss: " + onOff(AutoSanBoss.isRunning), AUTO_BOSS));
        items.addElement(command("Lịch Boss: " + onOff(ThongTinBoss.isEnable), LICH_BOSS));
        items.addElement(command("Săn Server", BOSS_SERVER));
        items.addElement(command("Săn Thế Giới", BOSS_THE_GIOI));
        items.addElement(command("Săn VDMQ", BOSS_VDMQ));
        items.addElement(command("Săn Map Ngoài", BOSS_MAP_NGOAI));
        items.addElement(command("Săn Tất Cả", BOSS_ALL));
        items.addElement(command("Nhặt nhanh: " + onOff(AutoPickup.isRunning), NHAT_NHANH));
        items.addElement(command("Mời nhóm", MOI_NHOM));
        items.addElement(command("Tách đồ lẻ", TACH_LE));
        items.addElement(command("Thông tin Nam Mod", THONG_TIN));
        GameCanvas.menu.gameAA(items);
    }

    private static Command command(String caption, int id) {
        return new Command(caption, INSTANCE, id, null);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    public void perform(int id, Object parameter) {
        switch (id) {
            case AUTO_BOSS:
                AutoSanBoss.toggle();
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
            case NHAT_NHANH:
                AutoPickup.toggle();
                return;
            case MOI_NHOM:
                AutoSanBoss.autoInviteFriends();
                return;
            case TACH_LE:
                openSplitInput();
                return;
            case THONG_TIN:
                GameScr.gameAC("Nam Mod - Săn Boss, Lịch Boss, Nhặt nhanh, Tách lẻ");
                return;
            default:
                return;
        }
    }

    private static void openSplitInput() {
        GameCanvas.inputDlg.gameAA("Nhập số lượng tách lẻ", new Command("Tách", new IActionListener() {
            public void perform(int id, Object parameter) {
                try {
                    int count = Integer.parseInt(GameCanvas.inputDlg.tfInput.gameAD().trim());
                    SplitPatcher.doTachLeDirect(count);
                } catch (Exception e) {
                    GameScr.gameAC("Nam Mod: Số lượng không hợp lệ!");
                }
                GameCanvas.endDlg();
            }
        }, 120112, null), 1);
        GameCanvas.inputDlg.tfInput.gameAA("3");
    }
}