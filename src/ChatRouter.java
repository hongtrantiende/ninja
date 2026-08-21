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
 * - tsp: Toggle Ts Pro (ts + gb all — danh ALL quai khong gioi han range)
 * - ts50/ts99: Auto Level (treo lv tu dong)
 * - tslv: Tat Auto Level
 */
public class ChatRouter {

    /** Hook cho nut Tat Auto trong menu GameScr. */
    public static void stopCurrentAuto() {
        AutoBossEvent.cancelAll();
        if (AutoLevel.isRunning) AutoLevel.stop();
        if (AutoSanBoss.isRunning) AutoSanBoss.stop();
        TsBoost.onTsStopped();
        AutoPickup.stop();
        // Khoi phuc hieu ung skill
        Code.timBG = false;
        Code.gameAF();
        GameScr.gameAC("Da tat toan bo Auto!");
    }

    /** Nhan pkm tu truong nhom; map -1 chi bat trang thai Auto San Boss. */
    public static void startPartyBoss(Auto auto) {
        if (auto != null && auto.mapID == -6) {
            Char.MuaCoLenh = false;
            Char.DungCoLenh = false;
            AutoSanBoss.cleanKhaoDiLenh();
            // returnMemberState() tu xu ly tu sat + hoi sinh + ve map cu
            // KHONG goi gameAN() o day tranh tu sat 2 lan
            AutoBossEvent.returnMemberState();
            return;
        }
        if (auto != null && (auto.mapID == 135 || auto.mapID == 136)) {
            // Set flags de game auto loop tu mua + dung Co Lenh (Code.java line 749-765)
            // KHONG goi ensureInLangCo() o day vi chat thread ko mua duoc shop
            Char.MuaCoLenh = true;
            Char.DungCoLenh = true;
            AutoSanBoss.restoreLangCoGraph();
        } else if (auto != null && auto.mapID > 0) {
            Char.MuaCoLenh = false;
            Char.DungCoLenh = false;
            if (TileMap.isLangCo(TileMap.mapID)) {
                AutoSanBoss.cleanKhaoDiLenh();
                try { Thread.sleep(300L); } catch (InterruptedException e) {}
                try { Code.gameAN(); } catch (Exception e) {}
                try { Thread.sleep(800L); } catch (InterruptedException e) {}
                // Fallback: neu gameAN khong tu sat (item chua xoa kip) -> gui truc tiep
                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                    try { Service.gI().gameAE(); } catch (Exception e) {}
                    try { Thread.sleep(800L); } catch (InterruptedException e) {}
                }
                respawnQuick();
            }
        }
        if (auto != null && auto.mapID == -5) {
            AutoBossEvent.returnMemberState();
            return;
        }
        if (auto != null && auto.mapID == -4) {
            AutoBossEvent.saveMemberState();
            return;
        }
        if (auto != null && auto.mapID == -3) {
            AutoSanBoss.stopPartyMemberFully();
            return;
        }
        if (auto != null && auto.mapID == -2) {
            AutoSanBoss.startPartyMemberTreo();
            // KHONG tat Co Lenh khi dang o Lang Co — pkm 135/136 se den ngay sau
            // Neu tat o day, game auto-exit da nhan vat ra truoc khi pkm 135 den
            return;
        }
        if (auto != null && auto.mapID == -1) {
            AutoSanBoss.startPartyMemberNormal();
            if (TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.mapID == 138 || TileMap.isLangCo(TileMap.mapID)) {
                Char.MuaCoLenh = false;
                Char.DungCoLenh = false;
                AutoSanBoss.cleanKhaoDiLenh();
                try { Thread.sleep(300L); } catch (InterruptedException e) {}
                try { Code.gameAN(); } catch (Exception e) {}
                try { Thread.sleep(800L); } catch (InterruptedException e) {}
                // Fallback: neu gameAN khong tu sat (item chua xoa kip) -> gui truc tiep
                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                    try { Service.gI().gameAE(); } catch (Exception e) {}
                    try { Thread.sleep(800L); } catch (InterruptedException e) {}
                }
                respawnQuick();
            }
            return;
        }
        AutoSanBoss.startPartyMember();
        if (auto != null) {
            LockGame.gameBK();
            Code.gameAA(auto);
        }
    }

    /** Hoi sinh nhanh trong ChatRouter (chat thread).
     *  Fix desync: GameScr.gameAB(5,0,0) truoc respawn packet de client dong bo. */
    private static void respawnQuick() {
        try {
            for (int i = 0; i < 15; i++) {
                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) return;
                GameCanvas.endDlg();
                Thread.sleep(10L);
                GameScr.gameAB(5, 0, 0);
                Thread.sleep(10L);
                if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                    Service.gI().gameAL();
                } else {
                    Service.gI().gameAK();
                    TileMap.gameAF();
                }
                Thread.sleep(300L);
            }
        } catch (Exception e) {}
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

        // === GHOST BOSS: danh boss vo hinh ===
        if (text.equals("gb") || text.equals("ghostboss")) {
            GhostBoss.toggle();
            return true;
        }
        if (text.startsWith("gb") && text.length() > 2) {
            // "gb all" — danh tat ca quai trong map
            if (text.equals("gb all") || text.equals("gball")) {
                GhostBoss.startAll();
                return true;
            }
            // "gb63" — ghost boss map cu the
            String numPart = text.substring(2);
            boolean isNum = true;
            for (int ci = 0; ci < numPart.length(); ci++) {
                if (numPart.charAt(ci) < '0' || numPart.charAt(ci) > '9') {
                    isNum = false;
                    break;
                }
            }
            if (isNum && numPart.length() > 0) {
                try {
                    int mapId = Integer.parseInt(numPart);
                    GhostBoss.startOnMap(mapId);
                    return true;
                } catch (Exception e) {}
            }
        }

        if (text.equals("radarboss") || text.equals("rboss")) {
            BossRadar.toggle();
            return true;
        }
        if (text.equals("tsbosstest")) {
            AutoBossEvent.testNow();
            return true;
        }
        if (text.equals("tsboss")) {
            AutoBossEvent.toggle();
            return true;
        }
        if (text.equals("tsbosstg")) {
            AutoBossEvent.togglePriority(3);
            return true;
        }

        // === FORCE BOSS COMMANDS ===
        if (text.equals("tspkball") || text.equals("all")) {
            AutoSanBoss.toggleALL();
            return true;
        }
        if (text.equals("tspkbvm") || text.equals("vm")) {
            AutoSanBoss.toggleVM();
            return true;
        }
        if (text.equals("tspkbmn")) {
            AutoSanBoss.toggleMN();
            return true;
        }
        if (text.equals("tspkblangco") || text.equals("langco")) {
            AutoSanBoss.toggleLangCo();
            return true;
        }
        if (text.equals("tspkbtg")) {
            AutoSanBoss.toggleTheGioi();
            return true;
        }
        if (text.equals("tstreo") || text.equals("treo")) {
            AutoSanBoss.toggleTreo();
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
        if (text.equals("treolangco")) {
            AutoSanBoss.toggleTreoLangCo();
            return true;
        }
        if (text.equals("treotg")) {
            AutoSanBoss.toggleTreoTheGioi();
            return true;
        }
        
        // === TS PRO: ts + gb all mode ===
        if (text.equals("tsp") || text.equals("tspro")) {
            TsBoost.toggleMode();
            return true;
        }
        
        // === NHAT DO NHANH ===
        if (text.equals("nhat")) {
            AutoPickup.toggle();
            return true;
        }
        if (text.equals("moinhom") || text.equals("mnb") || text.equals("mn")) {
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

        // === THONG KE 5 CHI SO UP ===
        if (text.equals("tt") || text.equals("thongke")) {
            TsBoost.showFullStats();
            return true;
        }
        
        // === INTERCEPT ts/tsn/ak: bat/tat nhat do + TsBoost tu dong ===
        if (text.equals("ts") || text.equals("tsn") || text.equals("ak")) {
            boolean isAk = text.equals("ak");
            boolean hadAuto = Code.gameAB != null;
            boolean handled = Code.gameAF(text);
            if (handled) {
                if (Code.gameAB != null) {
                    AutoPickup.start();
                    // Tat hieu ung skill de giam lag
                    Code.timBG = true;
                    if (!isAk) {
                        TsBoost.onTsStarted();
                    }
                    GameScr.gameAC(!isAk && TsBoost.isRunning ? "TS + Ts Pro!" : "TS ON!");
                } else if (hadAuto) {
                    TsBoost.stop();
                    AutoPickup.stop();
                    // Khoi phuc hieu ung skill
                    Code.timBG = false;
                    GameScr.gameAC("TS OFF!");
                } else {
                    AutoPickup.syncAfterAutoCommand();
                    if (!isAk) {
                        TsBoost.syncAfterTs();
                    }
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
