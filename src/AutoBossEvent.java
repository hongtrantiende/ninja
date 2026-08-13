import java.util.Calendar;
import java.util.TimeZone;

/** Uu tien boss khi dang TS; moi may tu luu va khoi phuc map/khu/auto cua minh. */
public final class AutoBossEvent implements Runnable {
    public static boolean isEnabled;
    private static boolean inEvent;
    private static boolean membersSentBack;
    private static Auto savedAuto;
    private static int savedMap = -1;
    private static int savedZone = -1;
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
            GameScr.gameAC("TSBoss: ON - xong luot dau, quet lai 10 phut");
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
            GameScr.gameAC("TSBossTest: Huy phien cu, khoi dong lai...");
            AutoSanBoss.stopEventHunt();
            inEvent = false;
            sleep(700L);
        }
        if (!isLeader()) {
            GameScr.gameAC("TSBossTest: Chi truong nhom duoc dung!");
            return;
        }
        inEvent = true;
        forceAllNext = true;
        disableAfterTest = !isEnabled;
        if (!isEnabled) {
            isEnabled = true;
            new Thread(new AutoBossEvent()).start();
        }
        new Thread(new Runnable() {
            public void run() {
                GameScr.gameAC("TSBossTest: Quet ALL, khong cat ngang luot");
                beginLeaderEvent();
            }
        }).start();
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
            try { Code.gameAN(); } catch (Exception e) {}
            sleep(1000L);
        }
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
        int[] hours = {1,3,5,6,7,9,10,11,13,15,17,19,21,23};
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
        if (huntAll) AutoSanBoss.startEventHuntAll();
        else AutoSanBoss.startEventHunt();
        long retryEnd = 0L;
        boolean completedFirstRound = false;
        while (isEnabled && inEvent) {
            if (AutoSanBoss.consumeEventRoundCompleted()) {
                long now = System.currentTimeMillis();
                if (!completedFirstRound) {
                    completedFirstRound = true;
                    retryEnd = now + ROUND_RECHECK_TIME;
                    // Xong luot dau: gui nhom ve farm, leader tiep tuc quet solo
                    membersSentBack = true;
                    GameScr.gameAC("TSBoss: Xong luot dau, gui nhom ve farm!");
                    sendParty("pkm -5");
                    sleep(3000L);
                    // Khong moi lai nhom — thanh vien dang travel ve map cu
                    // Auto party manager (Code.java) se tu dong xu ly party reconnect
                    AutoSanBoss.isPartyMode = false;
                    GameScr.gameAC("TSBoss: Leader quet tiep 10 phut...");
                }
                if (completedFirstRound && now >= retryEnd) break;
            }
            sleep(500L);
        }
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
        if (TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.isLangCo(TileMap.mapID)) {
            AutoSanBoss.finishLangCoAndExit();
        }
        final int map = savedMap;
        final int zone = savedZone;
        final Auto oldAuto = savedAuto;
        inEvent = false;
        savedMap = -1;
        savedZone = -1;
        savedAuto = null;
        if (map < 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Dong dialog/NPC neu dang mo (tranh chan travel)
                    try { GameCanvas.endDlg(); } catch (Exception e) {}
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
                    Code.gameAB = oldAuto;
                    AutoPickup.start();
                    GameScr.gameAC("TSBoss: Ve M" + map + " K" + zone + " - tiep tuc TS");
                } else {
                    // Fallback: khong co auto cu -> restart TanSat tai map hien tai
                    try {
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
}
