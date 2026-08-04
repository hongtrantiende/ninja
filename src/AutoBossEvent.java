import java.util.Calendar;
import java.util.TimeZone;

/** Uu tien boss khi dang TS; moi may tu luu va khoi phuc map/khu/auto cua minh. */
public final class AutoBossEvent implements Runnable {
    public static boolean isEnabled;
    private static boolean inEvent;
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
        if (inEvent) {
            AutoSanBoss.stopEventHunt();
            sendParty("pkm -3");
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
        if (!inEvent) {
            saveLocalState();
            inEvent = true;
            GameScr.gameAC("TSBoss: Da luu M" + savedMap + " K" + savedZone);
        }
    }

    public static void returnMemberState() {
        if (!inEvent && savedMap < 0) return;
        AutoSanBoss.stopPartyMemberFully();
        returnAndResume();
    }

    private static void saveLocalState() {
        savedMap = TileMap.mapID;
        savedZone = TileMap.zoneID;
        savedAuto = Code.gameAB;
        if (savedAuto instanceof PkBoss || savedAuto instanceof SanBossHolder) savedAuto = null;
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
        int[] hours = {1,4,6,7,10,11,12,13,16,17,18,19,20,21,22,23};
        for (int i = 0; i < hours.length; i++) if (hours[i] == h) return true;
        return false;
    }

    public void run() {
        while (isEnabled) {
            try {
                int key = currentWindowKey();
                if (!inEvent && key >= 0 && key != lastWindowKey) {
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
        GameScr.gameAC("TSBoss: Den gio boss - luu M" + savedMap + " K" + savedZone);
        AutoSanBoss.autoInviteFriends();
        sleep(1000L);
        sendParty("pkm -4");
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
                    GameScr.gameAC("TSBoss: Xong luot dau, nghi 10s roi quet lai trong 10 phut");
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
        sendParty("pkm -5");
        if (!disabledByUser) GameScr.gameAC("TSBoss: Ket thuc, quay lai TS");
        returnAndResume();
        if (disableAfterTest) {
            disableAfterTest = false;
            isEnabled = false;
        }
    }

    private static void returnAndResume() {
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
                    if (TileMap.mapID != map) {
                        PkBoss travel = new PkBoss(map);
                        Code.gameAB = travel;
                        for (int i = 0; i < 9000 && TileMap.mapID != map; i++) sleep(10L);
                        if (Code.gameAB == travel) Code.gameAB = null;
                    }
                    if (TileMap.mapID == map && TileMap.zoneID != zone) {
                        Auto.gameAA(zone);
                        for (int i = 0; i < 1000 && TileMap.zoneID != zone; i++) sleep(10L);
                    }
                } catch (Exception e) {}
                Code.gameAB = oldAuto;
                GameScr.gameAC("TSBoss: Ve M" + map + " K" + zone + " - tiep tuc TS");
            }
        }).start();
    }

    private static void sendParty(String cmd) {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) Service.gI().gameAK(cmd);
        } catch (Exception e) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
