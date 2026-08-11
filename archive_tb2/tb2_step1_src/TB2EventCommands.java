public final class TB2EventCommands {
    private TB2EventCommands() {}

    public static boolean handle(String text) {
        if (text == null) return false;
        text = text.trim().toLowerCase();
        if (text.equals("tspkball") || text.equals("all")) {
            TB2AutoSanBoss.toggleHuntType(4);
            return true;
        }
        if (text.equals("tspkbsv") || text.equals("sv")) {
            TB2AutoSanBoss.toggleHuntType(0);
            return true;
        }
        if (text.equals("tspkbtg") || text.equals("tg")) {
            TB2AutoSanBoss.toggleHuntType(1);
            return true;
        }
        if (text.equals("tspkbvm") || text.equals("vm")) {
            TB2AutoSanBoss.toggleHuntType(2);
            return true;
        }
        if (text.equals("tspkbmn") || text.equals("mn")) {
            TB2AutoSanBoss.toggleHuntType(3);
            return true;
        }
        if (text.equals("treosv")) {
            TB2AutoSanBoss.toggleTreoType(0);
            return true;
        }
        if (text.equals("treotg")) {
            TB2AutoSanBoss.toggleTreoType(1);
            return true;
        }
        if (text.equals("treovm")) {
            TB2AutoSanBoss.toggleTreoType(2);
            return true;
        }
        if (text.equals("treomn")) {
            TB2AutoSanBoss.toggleTreoType(3);
            return true;
        }
        if (text.equals("tspkb")) {
            TB2AutoSanBoss.toggleHunt();
            return true;
        }
        if (text.equals("treoboss") || text.equals("tstreo") || text.equals("treo")) {
            TB2AutoSanBoss.toggleTreo();
            return true;
        }
        if (text.equals("moinhom") || text.equals("mnb")) {
            TB2AutoSanBoss.autoInviteFriends();
            return true;
        }
        if (text.equals("tk") || text.equals("thongke")) {
            TB2ThongKe.toggle();
            return true;
        }
        if (text.equals("ttb")) {
            TB2ThongTinBoss.toggle();
            return true;
        }
        if (text.equals("nhat")) {
            TB2AutoPickup.toggle();
            return true;
        }
        if (text.equals("gaoda")) {
            TB2AutoGaoDa.toggle();
            return true;
        }
        if (text.equals("doidiem")) {
            TB2AutoDoiDiem.toggle();
            return true;
        }
        return false;
    }
}
