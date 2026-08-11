/** STEP 1: menu Nam Mod toi gian, bien dich truc tiep tren API TB2 goc. */
public final class NamModMenu implements Class_bn {
    private static final int OPEN = 120001;
    private static final int CHECK = 120002;
    private static final int TACH_DO_LE = 120110;
    private static final int GAO_DA = 120120;
    private static final int DOI_DIEM = 120121;
    private static final int HUT_VP = 120122;
    private static final int LICH_BOSS = 120123;
    private static final int AUTO_BOSS = 120124;
    private static final int TREO_BOSS = 120125;
    private static final int MOI_NHOM = 120126;
    private static final int SAN_SV = 120127;
    private static final int SAN_TG = 120128;
    private static final int SAN_VM = 120129;
    private static final int SAN_MN = 120130;
    private static final int SAN_ALL = 120131;
    private static final int TREO_SV = 120132;
    private static final int TREO_TG = 120133;
    private static final int TREO_VM = 120134;
    private static final int TREO_MN = 120135;
    private static final int TREO_ALL = 120136;
    private static final int THONG_KE = 120137;
    private static final NamModMenu INSTANCE = new NamModMenu();

    private NamModMenu() {}

    public static void inject(Class_du items) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            Object value = items.elementAt(i);
            if (!(value instanceof Class_db)) continue;
            Class_db command = (Class_db)value;
            if (command.d == OPEN || command.d == TACH_DO_LE) return;
            if (command.d == 110021) {
                items.insertElementAt(new Class_db("Nam Mod", INSTANCE, OPEN, null), i + 1);
                return;
            }
            if (command.d == 110244) {
                items.insertElementAt(new Class_db("T\u00e1ch \u0111\u1ed3 l\u1ebb", INSTANCE, TACH_DO_LE, null), i + 1);
                return;
            }
        }
    }

    public static void open() {
        Class_du items = new Class_du();
        items.addElement(new Class_db("G\u1ea1o \u0111\u00e1", INSTANCE, GAO_DA, null));
        items.addElement(new Class_db("\u0110\u1ed5i \u0111i\u1ec3m", INSTANCE, DOI_DIEM, null));
        items.addElement(new Class_db("H\u00fat VP: " + (TB2AutoPickup.enabled ? "ON" : "OFF"), INSTANCE, HUT_VP, null));
        items.addElement(new Class_db("S\u0103n Boss: " + (TB2AutoSanBoss.enabled && !TB2AutoSanBoss.treoMode ? "ON" : "OFF"), INSTANCE, AUTO_BOSS, null));
        items.addElement(new Class_db("Treo Boss: " + (TB2AutoSanBoss.enabled && TB2AutoSanBoss.treoMode ? "ON" : "OFF"), INSTANCE, TREO_BOSS, null));
        items.addElement(new Class_db("S\u0103n Server: " + bossState(false, 0), INSTANCE, SAN_SV, null));
        items.addElement(new Class_db("S\u0103n Th\u1ebf Gi\u1edbi: " + bossState(false, 1), INSTANCE, SAN_TG, null));
        items.addElement(new Class_db("S\u0103n VDMQ: " + bossState(false, 2), INSTANCE, SAN_VM, null));
        items.addElement(new Class_db("S\u0103n Map Ngo\u00e0i: " + bossState(false, 3), INSTANCE, SAN_MN, null));
        items.addElement(new Class_db("S\u0103n T\u1ea5t C\u1ea3: " + bossState(false, 4), INSTANCE, SAN_ALL, null));
        items.addElement(new Class_db("Treo Server: " + bossState(true, 0), INSTANCE, TREO_SV, null));
        items.addElement(new Class_db("Treo Th\u1ebf Gi\u1edbi: " + bossState(true, 1), INSTANCE, TREO_TG, null));
        items.addElement(new Class_db("Treo VDMQ: " + bossState(true, 2), INSTANCE, TREO_VM, null));
        items.addElement(new Class_db("Treo Map Ngo\u00e0i: " + bossState(true, 3), INSTANCE, TREO_MN, null));
        items.addElement(new Class_db("Treo T\u1ea5t C\u1ea3: " + bossState(true, 4), INSTANCE, TREO_ALL, null));
        items.addElement(new Class_db("Th\u1ed1ng k\u00ea Up: " + (TB2ThongKe.isRunning ? "ON" : "OFF"), INSTANCE, THONG_KE, null));
        items.addElement(new Class_db("L\u1ecbch Boss: " + (TB2ThongTinBoss.enabled ? "ON" : "OFF"), INSTANCE, LICH_BOSS, null));
        items.addElement(new Class_db("M\u1eddi nh\u00f3m", INSTANCE, MOI_NHOM, null));
        items.addElement(new Class_db("Nam Mod OK", INSTANCE, CHECK, null));
        Class_cx.ae.a(items);
    }

    private static String bossState(boolean treo, int type) {
        return TB2AutoSanBoss.enabled && TB2AutoSanBoss.treoMode == treo && TB2AutoSanBoss.forcedType == type ? "ON" : "OFF";
    }

    public void a(int id, Object parameter) {
        if (id == OPEN) open();
        else if (id == TACH_DO_LE) TB2TachDoLe.openInput();
        else if (id == GAO_DA) TB2AutoGaoDa.toggle();
        else if (id == DOI_DIEM) TB2AutoDoiDiem.toggle();
        else if (id == HUT_VP) TB2AutoPickup.toggle();
        else if (id == AUTO_BOSS) TB2AutoSanBoss.toggleHunt();
        else if (id == TREO_BOSS) TB2AutoSanBoss.toggleTreo();
        else if (id == SAN_SV) TB2AutoSanBoss.toggleHuntType(0);
        else if (id == SAN_TG) TB2AutoSanBoss.toggleHuntType(1);
        else if (id == SAN_VM) TB2AutoSanBoss.toggleHuntType(2);
        else if (id == SAN_MN) TB2AutoSanBoss.toggleHuntType(3);
        else if (id == SAN_ALL) TB2AutoSanBoss.toggleHuntType(4);
        else if (id == TREO_SV) TB2AutoSanBoss.toggleTreoType(0);
        else if (id == TREO_TG) TB2AutoSanBoss.toggleTreoType(1);
        else if (id == TREO_VM) TB2AutoSanBoss.toggleTreoType(2);
        else if (id == TREO_MN) TB2AutoSanBoss.toggleTreoType(3);
        else if (id == TREO_ALL) TB2AutoSanBoss.toggleTreoType(4);
        else if (id == THONG_KE) TB2ThongKe.toggle();
        else if (id == LICH_BOSS) TB2ThongTinBoss.toggle();
        else if (id == MOI_NHOM) TB2AutoSanBoss.autoInviteFriends();
        else if (id == CHECK) Class_ds.c("Nam Mod STEP1 hoat dong");
    }
}
