import java.util.Calendar;
import java.util.TimeZone;

/** Uu tien boss khi dang TS; moi may tu luu va khoi phuc map/khu/auto cua minh. */
public final class AutoBossEvent implements Runnable {
    public static boolean isEnabled;
    /** 0 = mac dinh (tat ca), 1 = chi VDMQ+LangCo, 2 = chi MapNgoai, 3 = chi TheGioi */
    public static int eventPriority;
    private static boolean inEvent;
    private static boolean membersSentBack;
    private static Auto savedAuto;
    private static int savedMap = -1;
    private static int savedZone = -1;
    private static int savedZoneIndex = 0;
    private static int lastWindowKey = -1;
    private static boolean forceAllNext;
    private static boolean disableAfterTest;
    private static final long ROUND_RECHECK_TIME = 600000L;

    private AutoBossEvent() {}

    public static void toggle() {
        if (isEnabled) {
            isEnabled = false;
            if (inEvent) finishEvent(true);
            GameScr.gameAC("TSBoss: OFF");
        } else {
            isEnabled = true;
            new Thread(new AutoBossEvent()).start();
            lastWindowKey = -1;
            GameScr.gameAC("TSBoss: ON (" + priorityName() + ") - xong luot dau, quet lai 10 phut");
        }
    }

    /** Chon loai nao thi bat luon, an lai loai dang bat thi tat. */
    public static void togglePriority(int p) {
        if (isEnabled && eventPriority == p) {
            // Dang bat cung loai -> tat
            isEnabled = false;
            if (inEvent) finishEvent(true);
            GameScr.gameAC("TSBoss: OFF");
        } else {
            // Bat voi loai moi (hoac bat lan dau)
            eventPriority = p;
            if (!isEnabled) {
                isEnabled = true;
                new Thread(new AutoBossEvent()).start();
                lastWindowKey = -1;
            }
            GameScr.gameAC("TSBoss: ON (" + priorityName() + ")");
        }
    }

    public static String priorityName() {
        switch (eventPriority) {
            case 1: return "VDMQ+L\u00e0ngC\u1ed5";
            case 2: return "MapNgo\u00e0i";
            case 3: return "Th\u1ebf Gi\u1edbi";
            default: return "M\u1eb7c \u0111\u1ecbnh";
        }
    }

    public static void cancelAll() {
        isEnabled = false;
        forceAllNext = false;
        disableAfterTest = false;
        membersSentBack = false;
        if (inEvent) {
            AutoSanBoss.stopEventHunt();
            sendParty("pkm -3");
            if (TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.isLangCo(TileMap.mapID)) {
                AutoSanBoss.finishLangCoAndExit();
            }
        }
        inEvent = false;
        savedAuto = null;
        savedMap = -1;
        savedZone = -1;
    }

    public static void testNow() {
        if (inEvent) {
            GameScr.gameAC("TSBossTest: H\u1ee7y phi\u00ean c\u0169, kh\u1edfi \u0111\u1ed9ng l\u1ea1i...");
            AutoSanBoss.stopEventHunt();
            inEvent = false;
            sleep(700L);
        }
        if (!isLeader()) {
            GameScr.gameAC("TSBossTest: Ch\u1ec9 tr\u01b0\u1edfng nh\u00f3m \u0111\u01b0\u1ee3c d\u00f9ng!");
            return;
        }
        inEvent = true;
        disableAfterTest = !isEnabled;
        if (!isEnabled) {
            isEnabled = true;
            new Thread(new AutoBossEvent()).start();
        }
        new Thread(new Runnable() {
            public void run() {
                GameScr.gameAC("TSBoss Test: L\u01b0u v\u1ecb tr\u00ed & qu\u00e9t 1 map test...");
                saveLocalState();
                pauseLeaderAndWaitStable();
                inEvent = true;
                membersSentBack = false;
                AutoSanBoss.autoInviteFriends();
                sleep(1000L);
                sendParty("pkm -4");
                exitGatedMapIfNeeded();

                // Chon 1 map test: map 141 (VDMQ) hoac map 14 (Map ngoai)
                int sampleMap = 141;
                if (!AutoSanBoss.isMapEnabled(sampleMap)) {
                    sampleMap = 14;
                }
                AutoSanBoss.startEventHuntTest1Map(sampleMap);

                // Cho quet xong 1 map test
                while (isEnabled && inEvent) {
                    if (AutoSanBoss.consumeEventRoundCompleted()) break;
                    sleep(500L);
                }

                GameScr.gameAC("TSBoss Test: Xong map M" + sampleMap + ", quay l\u1ea1i TS!");
                finishEvent(false);
            }
        }).start();
    }

    public static void exitGatedMapIfNeeded() {
        int curMap = TileMap.mapID;
        boolean isGated = curMap == 196 || curMap == 192 || AutoVipMap.isEnabled || AutoTuLuyen.isEnabled
                || curMap == 135 || curMap == 136 || TileMap.isLangCo(curMap);
        if (!isGated) return;

        if (curMap == 135 || curMap == 136 || TileMap.isLangCo(curMap)) {
            AutoSanBoss.cleanKhaoDiLenh();
        }
        GameScr.gameAC("TSBoss: T\u1ef1 s\u00e1t v\u1ec1 l\u00e0ng \u0111\u1ec3 ra s\u0103n boss...");
        LockGame.gameBK();
        if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
            Code.gameAB = null;
        }
        try { Code.gameAN(); } catch (Exception e) {}
        sleep(1000L);
        for (int r = 0; r < 10; r++) {
            try {
                Char me = Char.getMyChar();
                if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                GameCanvas.endDlg();
                sleep(10L);
                Auto.gameAN.removeAllElements();
                Auto.gameAM = false;
                GameScr.gameAB(5, 0, 0);
                sleep(10L);
                Service.gI().gameAK();
                TileMap.gameAF();
                sleep(300L);
            } catch (Exception ex) {}
        }
        sleep(1000L);
    }

    public static void saveMemberState() {
        // Luon cap nhat savedMap/Zone/Auto moi nhat (ke ca khi inEvent da true)
        saveLocalState();
        inEvent = true;
        GameScr.gameAC("TSBoss: Da luu M" + savedMap + " K" + savedZone);
    }

    public static void returnMemberState() {
        // LUON dung party boss mode truoc
        AutoSanBoss.stopPartyMemberFully();
        Char.MuaCoLenh = false;
        Char.DungCoLenh = false;
        if (TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.isLangCo(TileMap.mapID)) {
            AutoSanBoss.cleanKhaoDiLenh();
            sleep(300L);
            try { Code.gameAN(); } catch (Exception e) {}
            sleep(800L);
            // Fallback: neu gameAN khong tu sat (item 35/37 chua xoa kip) -> gui truc tiep
            if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                try { Service.gI().gameAE(); } catch (Exception e) {}
                sleep(800L);
            }
            // Hoi sinh sau tu sat de tranh member bi ket trang thai chet
            ensureAlive();
        }
        // Hoi sinh neu dang chet (truong hop bi giet truoc khi nhan pkm -5)
        ensureAlive();
        // Neu co savedMap -> ve map cu
        if (savedMap >= 0 || savedAuto != null) {
            returnAndResume();
        } else {
            // Khong co state da luu -> chi dung auto, khong travel
            inEvent = false;
            GameScr.gameAC("TSBoss: Khong co map cu de quay ve!");
        }
    }

    private static void saveLocalState() {
        savedMap = TileMap.mapID;
        savedZone = TileMap.zoneID;
        savedZoneIndex = Code.gameAW; // Luu vi tri trong danh sach khu tuan tu
        // Traverse auto stack de tim auto that (TanSat/Stanima...)
        // Skip PkBoss va SanBossHolder vi do la wrapper cua mod
        Auto a = Code.gameAB;
        while (a != null && (a instanceof PkBoss || a instanceof SanBossHolder)) {
            a = a.reAB;
        }
        savedAuto = a;
    }

    private static void pauseLeaderAndWaitStable() {
        LockGame.gameBK();
        if (Code.gameAB == savedAuto) Code.gameAB = null;
        int lastMap = TileMap.mapID;
        int lastZone = TileMap.zoneID;
        int stable = 0;
        for (int i = 0; i < 50 && stable < 15; i++) {
            sleep(100L);
            int map = TileMap.mapID;
            int zone = TileMap.zoneID;
            if (map == lastMap && zone == lastZone) stable++;
            else {
                lastMap = map;
                lastZone = zone;
                stable = 0;
            }
        }
        savedMap = TileMap.mapID;
        savedZone = TileMap.zoneID;
        GameScr.gameAC("TSBoss: Da dung TS tai M" + savedMap + " K" + savedZone);
    }

    private static boolean isLeader() {
        try {
            Char me = Char.getMyChar();
            if (me == null) return false;
            if (GameScr.vParty == null || GameScr.vParty.size() == 0) return true;
            Party first = (Party)GameScr.vParty.firstElement();
            return first != null && first.charId == me.charID;
        } catch (Exception e) { return false; }
    }

    private static int currentWindowKey() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (c.get(Calendar.MINUTE) >= 40 || !isSpawnHour(hour)) return -1;
        return c.get(Calendar.YEAR) * 100000 + c.get(Calendar.DAY_OF_YEAR) * 100 + hour;
    }

    private static boolean isSpawnHour(int h) {
        int[] hours = {1,3,5,6,7,9,10,11,12,13,15,17,19,21,23};
        for (int i = 0; i < hours.length; i++) if (hours[i] == h) return true;
        return false;
    }

    public void run() {
        while (isEnabled) {
            try {
                int key = currentWindowKey();
                // CHI leader moi tu dong bat event khi den gio boss
                // Thanh vien chi nhan lenh tu truong nhom qua pkm -4/-5
                if (!inEvent && key >= 0 && key != lastWindowKey && isLeader()) {
                    lastWindowKey = key;
                    beginLeaderEvent();
                }
            } catch (Exception e) {}
            sleep(1000L);
        }
    }

    private static void beginLeaderEvent() {
        saveLocalState();
        pauseLeaderAndWaitStable();
        inEvent = true;
        membersSentBack = false;
        GameScr.gameAC("TSBoss: Den gio boss - luu M" + savedMap + " K" + savedZone);
        AutoSanBoss.autoInviteFriends();
        sleep(1000L);
        sendParty("pkm -4");
        exitGatedMapIfNeeded();

        // Auto re-invite moi 1 phut khi nhom chua du 6 nguoi
        new Thread(new Runnable() {
            public void run() {
                while (isEnabled && inEvent && !membersSentBack) {
                    sleep(60000L); // 1 phut
                    if (!isEnabled || !inEvent || membersSentBack) break;
                    int partySize = 0;
                    try {
                        if (GameScr.vParty != null) partySize = GameScr.vParty.size();
                    } catch (Exception e) {}
                    if (partySize < 6) {
                        GameScr.gameAC("TSBoss: Nh\u00f3m ch\u01b0a \u0111\u1ee7 (" + partySize + "/6), m\u1eddi l\u1ea1i...");
                        AutoSanBoss.autoInviteFriends();
                    }
                }
            }
        }).start();

        boolean huntAll = forceAllNext;
        forceAllNext = false;
        if (huntAll) {
            AutoSanBoss.startEventHuntAll();
        } else {
            switch (eventPriority) {
                case 1:
                    AutoSanBoss.startEventHuntVdmqLc();
                    break;
                case 2:
                    AutoSanBoss.startEventHuntMN();
                    break;
                case 3:
                    AutoSanBoss.startEventHuntTG();
                    break;
                default:
                    AutoSanBoss.startEventHuntAll();
                    break;
            }
        }

        // === Luot 1: Quet + goi ae fang boss ===
        while (isEnabled && inEvent) {
            if (AutoSanBoss.consumeEventRoundCompleted()) break;
            sleep(500L);
        }

        if (!isEnabled || !inEvent) { finishEvent(false); return; }

        // Xong luot 1: gui nhom ve farm
        membersSentBack = true;
        GameScr.gameAC("TSBoss: Xong l\u01b0\u1ee3t 1, g\u1eedi nh\u00f3m v\u1ec1 farm!");
        sendParty("pkm -5");
        sleep(3000L);
        AutoSanBoss.isPartyMode = false;

        // === Luot 2: Leader quet solo them 1 luot ===
        GameScr.gameAC("TSBoss: Leader qu\u00e9t th\u00eam l\u01b0\u1ee3t 2...");
        while (isEnabled && inEvent) {
            if (AutoSanBoss.consumeEventRoundCompleted()) break;
            sleep(500L);
        }

        // Xong luot 2 -> ve TS
        finishEvent(false);
    }

    private static void finishEvent(boolean disabledByUser) {
        if (!inEvent) return;
        AutoSanBoss.stopEventHunt();
        if (!membersSentBack) {
            sendParty("pkm -5");
        }
        membersSentBack = false;
        if (!disabledByUser) GameScr.gameAC("TSBoss: Ket thuc, quay lai TS");
        // Leader xu ly thoat Lang Co truoc khi ve map cu
        if (TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.isLangCo(TileMap.mapID)) {
            AutoSanBoss.finishLangCoAndExit();
        }
        returnAndResume();
        sleep(2000L);
        // Moi lai cac thanh vien bi thieu (neu co)
        AutoSanBoss.autoInviteFriends();
        if (disableAfterTest) {
            disableAfterTest = false;
            isEnabled = false;
        }
    }

    private static void returnAndResume() {
        // Caller (returnMemberState/finishEvent) da xu ly thoat Lang Co truoc khi goi
        final int map = savedMap;
        final int zone = savedZone;
        final Auto oldAuto = savedAuto;
        inEvent = false;
        savedMap = -1;
        savedZone = -1;
        savedAuto = null;
        if (map < 0) return;

        // === CHECK: Neu VipMap hoac TuLuyen dang bat, tu sat ve thon de auto re-enter ===
        final boolean useNpcReturn = AutoVipMap.isEnabled || AutoTuLuyen.isEnabled;

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Dong dialog/NPC neu dang mo (tranh chan travel)
                    try { GameCanvas.endDlg(); } catch (Exception e) {}

                    if (useNpcReturn) {
                        // === CHE DO VIP/TU LUYEN: Tu sat -> hoi sinh ve thon -> auto checkAndReturn ===
                        GameScr.gameAC("TSBoss: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o l\u1ea1i map farm...");
                        // Xoa auto hien tai
                        LockGame.gameBK();
                        if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                            Code.gameAB = null;
                        }
                        // Tu sat
                        try { Code.gameAN(); } catch (Exception e) {}
                        sleep(1000L);
                        // An "Ve lang" (gameAK) - KHONG dung HSL vi can ve thon co NPC
                        for (int r = 0; r < 10; r++) {
                            try {
                                Char me = Char.getMyChar();
                                if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                                GameCanvas.endDlg();
                                sleep(10L);
                                Auto.gameAN.removeAllElements();
                                Auto.gameAM = false;
                                GameScr.gameAB(5, 0, 0);
                                sleep(10L);
                                Service.gI().gameAK(); // Ve lang (LUON LUON)
                                TileMap.gameAF();
                                sleep(300L);
                            } catch (Exception ex) {}
                        }
                        sleep(2000L);
                        // Khoi phuc auto de VipMap/TuLuyen.checkAndReturn() co the detect
                        if (oldAuto != null) {
                            Code.gameAW = savedZoneIndex;
                            Code.gameAB = oldAuto;
                            AutoPickup.start();
                        } else {
                            try {
                                Code.gameAW = savedZoneIndex;
                                Code.gameAA(-1, (int)TileMap.mapID);
                                AutoPickup.start();
                            } catch (Exception e) {}
                        }
                        GameScr.gameAC("TSBoss: \u0110\u00e3 h\u1ed3i sinh, ch\u1edd auto v\u00e0o map...");
                        // checkAndReturn se tu dong chay trong game loop
                        return;
                    }

                    // === CHE DO BINH THUONG: Travel ve map cu ===
                    // Hoi sinh neu dang chet truoc khi travel
                    ensureAlive();
                    // Xoa lock va auto hien tai de tranh xung dot
                    LockGame.gameBK();
                    if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                        Code.gameAB = null;
                    }
                    // Travel ve map cu voi retry 3 lan
                    for (int retry = 0; retry < 3 && TileMap.mapID != map; retry++) {
                        if (retry > 0) {
                            GameScr.gameAC("TSBoss: Thu lai lan " + (retry + 1) + "...");
                            sleep(2000L);
                            try { GameCanvas.endDlg(); } catch (Exception e) {}
                            LockGame.gameBK();
                        }
                        PkBoss travel = new PkBoss(map);
                        Code.gameAB = travel;
                        for (int i = 0; i < 9000 && TileMap.mapID != map; i++) sleep(10L);
                        if (Code.gameAB == travel) Code.gameAB = null;
                    }
                    // Doi khu cu
                    if (TileMap.mapID == map && TileMap.zoneID != zone) {
                        Auto.gameAA(zone);
                        for (int i = 0; i < 1000 && TileMap.zoneID != zone; i++) sleep(10L);
                    }
                } catch (Exception e) {}
                // Khoi phuc auto: dung saved neu co, khong thi restart TS
                if (oldAuto != null) {
                    Code.gameAW = savedZoneIndex; // Khoi phuc vi tri khu tuan tu
                    Code.gameAB = oldAuto;
                    AutoPickup.start();
                    GameScr.gameAC("TSBoss: Ve M" + map + " K" + zone + " - tiep tuc TS");
                } else {
                    // Fallback: khong co auto cu -> restart TanSat tai map hien tai
                    try {
                        Code.gameAW = savedZoneIndex;
                        Code.gameAA(-1, (int)TileMap.mapID);
                        AutoPickup.start();
                    } catch (Exception e) {}
                    GameScr.gameAC("TSBoss: Ve M" + map + " K" + zone + " - bat lai TS moi");
                }
            }
        }).start();
    }

    private static void sendParty(String cmd) {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                Service.gI().gameAK(cmd);
                // Gui lai 2 lan nua de chac chan member nhan duoc
                // (phong truong hop lag/member dang loading map)
                for (int r = 0; r < 2; r++) {
                    sleep(2000L);
                    if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                        Service.gI().gameAK(cmd);
                    }
                }
            }
        } catch (Exception e) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }

    /** Hoi sinh nhanh neu nhan vat dang chet (statusMe==14 hoac cHP<=0). */
    private static void ensureAlive() {
        try {
            if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                for (int retry = 0; retry < 10; retry++) {
                    GameCanvas.endDlg();
                    sleep(10L);
                    Auto.gameAN.removeAllElements();
                    Auto.gameAM = false;
                    GameScr.gameAB(5, 0, 0);
                    sleep(10L);
                    if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                        Service.gI().gameAL();
                    } else {
                        Service.gI().gameAK();
                        TileMap.gameAF();
                    }
                    sleep(300L);
                    if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) return;
                }
            }
        } catch (Exception e) {}
    }
}
