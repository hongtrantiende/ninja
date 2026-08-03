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
 * - ts50/ts99: Auto Level (treo lv tu dong)
 * - tslv: Tat Auto Level
 */
public class ChatRouter {

    /** Hook cho nut Tat Auto trong menu GameScr. */
    public static void stopCurrentAuto() {
        if (AutoLevel.isRunning) {
            AutoLevel.stop();
        }
        if (AutoSanBoss.isRunning) {
            AutoSanBoss.stop();
        }
        Code.gameAF();
    }

    /** Nhan pkm tu truong nhom; map -1 chi bat trang thai Auto San Boss. */
    public static void startPartyBoss(Auto auto) {
        if (auto != null && auto.mapID == -3) {
            AutoSanBoss.stopPartyMemberFully();
            return;
        }
        if (auto != null && auto.mapID == -2) {
            AutoSanBoss.startPartyMemberTreo();
            return;
        }
        if (auto != null && auto.mapID == -1) {
            AutoSanBoss.startPartyMemberNormal();
            return;
        }
        AutoSanBoss.startPartyMember();
        if (auto != null) {
            Code.gameAA(auto);
        }
    }

    /** Nhan pkk: treo mode tu doi khu, mode thuong giao zone cho PkBoss. */
    public static void setPartyBossZone(int zone) {
        AutoSanBoss.setPartyBossZone(zone);
    }

    /** Nhan pke tu truong nhom; delegate xu ly cho AutoSanBoss. */
    public static void stopPartyBoss() {
        AutoSanBoss.stopPartyBoss();
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
        if (text.equals("tstreo") || text.equals("treo")) {
            AutoSanBoss.toggleTreo();
            return true;
        }
        if (text.equals("treosv")) {
            AutoSanBoss.toggleTreoSV();
            return true;
        }
        if (text.equals("treotg")) {
            AutoSanBoss.toggleTreoTG();
            return true;
        }
        if (text.equals("treovm")) {
            AutoSanBoss.toggleTreoVM();
            return true;
        }
        if (text.equals("treomn")) {
            AutoSanBoss.toggleTreoMN();
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
        
        // === MOB INFO: quet quai tren map hien tai ===
        if (text.equals("mobinfo")) {
            scanMobInfo();
            return true;
        }
        if (text.equals("scanmap")) {
            MapScanner.start();
            return true;
        }
        
        // === AUTO LEVEL: tslv + so (vd: tslv50, tslv99) ===
        if (text.startsWith("tslv") && text.length() > 4) {
            String numPart = text.substring(4);
            boolean isNumber = true;
            for (int i = 0; i < numPart.length(); i++) {
                if (numPart.charAt(i) < '0' || numPart.charAt(i) > '9') {
                    isNumber = false;
                    break;
                }
            }
            if (isNumber && numPart.length() > 0) {
                try {
                    int lvTarget = Integer.parseInt(numPart);
                    if (lvTarget >= 10 && lvTarget <= 99) {
                        AutoLevel.start(lvTarget);
                        return true;
                    }
                } catch (Exception e) {}
            }
        }
        if (text.equals("tslv")) {
            if (AutoLevel.isRunning) {
                AutoLevel.stop();
            } else {
                GameScr.gameAC("Auto Level ch\u01b0a b\u1eadt! G\u00f5 tslv50 ho\u1eb7c tslv99");
            }
            return true;
        }
        
        // === AUTO VT MAP 55 ===
        if (text.equals("avt55")) {
            AutoVT55.setup();
            return true;
        }
        if (text.equals("avt58")) {
            AutoVT55.setup58();
            return true;
        }
        
        // === INTERCEPT ts/tsn/ak: bat/tat nhat do tu dong ===
        if (text.equals("ts") || text.equals("tsn") || text.equals("ak")) {
            boolean handled = Code.gameAF(text);
            if (handled) {
                if (Code.gameAB != null) {
                    AutoPickup.start();
                    GameScr.gameAC("H\u00FAt VP ON!");
                } else {
                    AutoPickup.stop();
                    GameScr.gameAC("H\u00FAt VP OFF!");
                }
            }
            return handled;
        }
        
        // Fallback: goi Code.gameAF goc
        return Code.gameAF(text);
    }

    /**
     * Quet tat ca quai tren map hien tai, hien thi ten + level + templateId.
     * Goi bang lenh chat "mobinfo".
     */
    private static void scanMobInfo() {
        try {
            int mapID = TileMap.mapID;
            int zoneID = TileMap.zoneID;
            GameScr.gameAC("=== Map " + mapID + " Khu " + zoneID + " ===");
            
            int size = GameScr.vMob.size();
            if (size == 0) {
                GameScr.gameAC("Khong co quai tren map!");
                return;
            }
            
            // Dem so luong moi loai quai
            String info = "";
            int count = 0;
            // Hien thi tung con (toi da 8 dong)
            for (int i = 0; i < size && count < 8; i++) {
                try {
                    Mob mob = (Mob) GameScr.vMob.elementAt(i);
                    if (mob == null) continue;
                    String name = "?";
                    try { name = mob.getTemplate().name; } catch (Exception e) {}
                    info = name + " Lv" + mob.level + " (id:" + mob.templateId + ")";
                    GameScr.gameAC(info);
                    count++;
                } catch (Exception e) {}
            }
            GameScr.gameAC("Tong: " + size + " quai");
        } catch (Exception e) {
            GameScr.gameAC("Loi quet mob: " + e.getMessage());
        }
    }
}
