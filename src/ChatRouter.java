/**
 * ChatRouter - Xu ly cac lenh chat mo rong.
 * 
 * Thay the Code.gameAF(String) trong GameScr:
 * GameScr goi ChatRouter.checkAll(text) thay vi Code.gameAF(text)
 * checkAll check lenh mo rong TRUOC, roi fallback Code.gameAF goc.
 * 
 * Lenh mo rong:
 * - tspkbsv/tg/vm/mn: Force san boss
 * - nhat: Toggle nhat do nhanh (AutoPickup)
 * - ts/tsn/ak: Intercept de tu dong bat nhat do khi bat auto
 */
public class ChatRouter {

    /** Hook cho nut Tat Auto trong menu GameScr. */
    public static void stopCurrentAuto() {
        if (AutoSanBoss.isRunning) {
            AutoSanBoss.stop();
        }
        Code.gameAF();
    }

    /** Nhan pkm tu truong nhom; map -1 chi bat trang thai Auto San Boss. */
    public static void startPartyBoss(Auto auto) {
        AutoSanBoss.startPartyMember();
        if (auto != null && auto.mapID != -1) {
            Code.gameAA(auto);
        }
    }

    /** Nhan pke tu truong nhom; pop PkBoss roi tat holder/thread thanh vien. */
    public static void stopPartyBoss() {
        Code.gameAC();
        AutoSanBoss.stop();
    }

    /**
     * Thay the Code.gameAF(String) - check lenh mo rong TRUOC, fallback goc SAU.
     * CUNG SIGNATURE: (Ljava/lang/String;)Z
     */
    public static boolean checkAll(String text) {
        if (text == null) return false;
        
        // === FORCE BOSS COMMANDS ===
        if (text.equals("tspkball") || text.equals("all")) {
            AutoSanBoss.toggleALL();
            return true;
        }
        if (text.equals("tspkbsv") || text.equals("sv")) {
            AutoSanBoss.toggleSV();
            return true;
        }
        if (text.equals("tspkbtg") || text.equals("tg")) {
            AutoSanBoss.toggleTG();
            return true;
        }
        if (text.equals("tspkbvm") || text.equals("vm")) {
            AutoSanBoss.toggleVM();
            return true;
        }
        if (text.equals("tspkbmn") || text.equals("mn")) {
            AutoSanBoss.toggleMN();
            return true;
        }
        
        // === NHAT DO NHANH ===
        if (text.equals("nhat")) {
            AutoPickup.toggle();
            return true;
        }
        if (text.equals("moinhom") || text.equals("mnb")) {
            AutoSanBoss.autoInviteFriends();
            return true;
        }
        if (text.startsWith("tach") || text.startsWith("tl")) {
            String[] parts = text.split(" ");
            int count = 30;
            if (parts.length > 1) {
                try {
                    count = Integer.parseInt(parts[1]);
                } catch (Exception e) {}
            }
            AutoSanBoss.tachDoLe(count);
            return true;
        }
        
        // === INTERCEPT ts/tsn/ak: bat nhat do tu dong ===
        if (text.equals("ts") || text.equals("tsn") || text.equals("ak")) {
            // Goi Code.gameAF goc de xu ly ts/tsn/ak binh thuong
            boolean handled = Code.gameAF(text);
            if (handled) {
                // Kiem tra: neu auto DANG chay (gameAB != null) -> bat nhat do
                // Neu auto KHONG chay (gameAB == null) -> tat nhat do
                if (Code.gameAB != null) {
                    if (!AutoPickup.isRunning) {
                        AutoPickup.start();
                        GameScr.gameAC("Auto nh\u1eb7t nhanh ON!");
                    }
                } else {
                    if (AutoPickup.isRunning) {
                        AutoPickup.stop();
                        GameScr.gameAC("Auto nh\u1eb7t nhanh OFF!");
                    }
                }
            }
            return handled;
        }
        
        // Fallback: goi Code.gameAF goc
        return Code.gameAF(text);
    }
}
