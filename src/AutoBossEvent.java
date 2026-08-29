import java.util.Calendar;
import java.util.TimeZone;

/** Uu tien boss khi dang TS; moi may tu luu va khoi phuc map/khu/auto cua minh. */
public final class AutoBossEvent implements Runnable {
    public static boolean isEnabled;
    /** 0=tat ca, 1=VDMQ+LC, 2=MapNgoai, 3=TheGioi, 4=LangCo, 5=VDMQ, 6=MapVIP, 7=MapVIP2 */
    public static int eventPriority;
    /** So luot quet them sau luot 1 (0 = khong quet them, 1 = mac dinh, 2-9 = nhieu luot) */
    public static int extraRounds = 1;
    /** So giay truoc khi boss spawn se bat dau san (0 = mac dinh, 0-30) */
    public static int preSpawnBreakSec = 0;
    public static boolean inEvent;
    private static boolean membersSentBack;
    private static Auto savedAuto;
    private static int savedMap = -1;
    private static int savedZone = -1;
    private static int savedX = -1;
    private static int savedY = -1;
    private static int savedZoneIndex = 0;
    private static int lastWindowKey = -1;
    private static boolean forceAllNext;
    private static boolean disableAfterTest;
    private static final long ROUND_RECHECK_TIME = 600000L;
    /** Lưu eventPriority gốc khi triggerImmediate ghi đè */
    private static int savedEventPriority = -1;
    /** Nếu TS Boss chưa bật trước triggerImmediate → tắt lại sau hunt */
    private static boolean wasEnabledBeforeTrigger = true;

    static {
        loadConfigFromRMS();
    }

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
        saveConfigToRMS();
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
        saveConfigToRMS();
    }

    public static String priorityName() {
        switch (eventPriority) {
            case 1: return "VDMQ+L\u00e0ngC\u1ed5";
            case 2: return "MapNgo\u00e0i";
            case 3: return "Th\u1ebf Gi\u1edbi";
            case 4: return "L\u00e0ng C\u1ed5";
            case 5: return "V\u0110MQ";
            case 6: return "Map VIP";
            case 7: return "Map VIP2";
            default: return "T\u1ea5t c\u1ea3";
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
        AutoSanBoss.ignoreBossHourCheck = false;
        preSpawnTriggered = false;
        saveConfigToRMS();
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
                    if (!AutoSanBoss.isRunning) break;
                    sleep(500L);
                }

                GameScr.gameAC("TSBoss Test: Xong map M" + sampleMap + ", quay l\u1ea1i TS!");
                finishEvent(false);
            }
        }).start();
    }

    /**
     * Kich hoat phien san boss NGAY LAP TUC (bo qua kiem tra gio spawn).
     * Dung khi doc vi boss tu Chat Notice / Server notification.
     */
    public static void triggerImmediate(int p) {
        AutoSanBoss.ignoreBossHourCheck = true;
        // Lưu priority gốc của user (chỉ lưu lần đầu, tránh ghi đè khi gọi liên tiếp)
        if (savedEventPriority < 0) {
            savedEventPriority = eventPriority;
        }
        wasEnabledBeforeTrigger = isEnabled;
        eventPriority = p;
        if (inEvent) {
            AutoSanBoss.stopEventHunt();
            inEvent = false;
            sleep(700L);
        }
        if (!isEnabled) {
            isEnabled = true;
            new Thread(new AutoBossEvent()).start();
        }
        new Thread(new Runnable() {
            public void run() {
                GameScr.gameAC("TSBoss: K\u00edch ho\u1ea1t s\u0103n boss " + priorityName() + " ngay l\u1eadp t\u1ee9c!");
                lastWindowKey = -1;
                beginLeaderEvent();
            }
        }).start();
    }

    public static void exitGatedMapIfNeeded() {
        int curMap = TileMap.mapID;
        boolean isGated = curMap == 195 || curMap == 196 || curMap == 192 || AutoVipMap.isEnabled || AutoTuLuyen.isEnabled
                || curMap == 135 || curMap == 136 || TileMap.isLangCo(curMap);
        if (!isGated) return;

        // Neu priority la MapVIP/MapVIP2 → luon giu nguyen
        if (eventPriority == 6 || eventPriority == 7) {
            if (TileMap.isLangCo(curMap)) {
                AutoSanBoss.cleanKhaoDiLenh();
                sleep(300L);
            }
            LockGame.gameBK();
            if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                Code.gameAB = null;
            }
            return;
        }

        // Priority "Tat ca" dang o VIP map: CHI giu lai neu boss VIP THUC SU dang active
        // hoac SAP spawn trong PRE_SPAWN_SECONDS (tranh tu sat ra ngoai khi dang cho san)
        if (eventPriority == 0 && (curMap == 195 || curMap == 196)) {
            int vipType = (curMap == 195) ? AutoSanBoss.TYPE_MAPVIP : AutoSanBoss.TYPE_MAPVIP2;
            // Kiem tra gio spawn THUC TE (khong dung ignoreBossHourCheck)
            boolean vipActive = false;
            try {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                int nowSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND);
                int[] hrs = AutoSanBoss.BOSS_HOURS[vipType];
                for (int i = 0; i < hrs.length; i++) {
                    int d = nowSec - hrs[i] * 3600;
                    // FIX: d >= -PRE_SPAWN_SECONDS cho phep giu lai map VIP khi boss sap spawn
                    // (truoc day d >= 0 bo sot pre-spawn window → tu sat ra VDMQ)
                    if (d >= -PRE_SPAWN_SECONDS && d < 2400) { vipActive = true; break; }
                }
            } catch (Exception e) {}
            if (vipActive) {
                // Boss VIP dang spawn → o lai map, san VIP truoc
                LockGame.gameBK();
                if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                    Code.gameAB = null;
                }
                return;
            }
            // Boss VIP CHUA spawn → fall through, tu sat de di san boss khac
        }

        if (curMap == 135 || curMap == 136 || TileMap.isLangCo(curMap)) {
            // Dung auto truoc khi exit
            LockGame.gameBK();
            if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                Code.gameAB = null;
            }
            GameScr.gameAC("TSBoss: Tho\u00e1t L\u00e0ng C\u1ed5 qua NPC...");
            AutoSanBoss.finishLangCoAndExit();
            sleep(500L);
            return;
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
                sleep(20L);
                Auto.gameAN.removeAllElements();
                Auto.gameAM = false;
                LockGame.gameAA = true;
                Service.gI().gameAK();
                TileMap.gameAF();
                LockGame.gameAA = false;
                sleep(300L);
            } catch (Exception ex) {}
        }
        sleep(1000L);
    }

    public static void saveMemberState() {
        if (!inEvent || savedMap < 0) {
            saveLocalState();
        }
        inEvent = true;
        GameScr.gameAC("TSBoss: Da luu M" + savedMap + " K" + savedZone + (savedX > 0 ? " (" + savedX + "," + savedY + ")" : ""));
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
        if (savedMap < 0) {
            loadSavedStateFromRMS();
        }
        // Fallback: neu co bat TS Map VIP hoac Tu Luyen
        if (savedMap < 0) {
            if (AutoVipMap.isEnabled) savedMap = AutoVipMap.targetMapID;
            else if (AutoTuLuyen.isEnabled) savedMap = 192;
        }
        // Neu co savedMap -> ve map cu
        if (savedMap >= 0 || savedAuto != null || AutoVipMap.isEnabled || AutoTuLuyen.isEnabled) {
            returnAndResume();
        } else {
            // Khong co state da luu -> chi dung auto, khong travel
            inEvent = false;
            GameScr.gameAC("TSBoss: Khong co map cu de quay ve!");
        }
    }

    private static void saveLocalState() {
        int curMap = TileMap.mapID;
        // Khong luu map gated / map boss Lang Co (135, 136, 138)
        if (curMap == 135 || curMap == 136 || curMap == 138 || TileMap.isLangCo(curMap)) {
            return;
        }
        savedMap = curMap;
        savedZone = TileMap.zoneID;
        try {
            Char me = Char.getMyChar();
            if (me != null) {
                savedX = me.cx;
                savedY = me.cy;
            }
        } catch (Exception e) {}
        savedZoneIndex = Code.gameAW; // Luu vi tri trong danh sach khu tuan tu
        // Traverse auto stack de tim auto that (TanSat/Stanima...)
        // Skip PkBoss va SanBossHolder vi do la wrapper cua mod
        Auto a = Code.gameAB;
        while (a != null && (a instanceof PkBoss || a instanceof SanBossHolder)) {
            a = a.reAB;
        }
        if (a != null) {
            savedAuto = a;
        }
        saveSavedStateToRMS();
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
        int curMap = TileMap.mapID;
        if (curMap != 135 && curMap != 136 && curMap != 138 && !TileMap.isLangCo(curMap)) {
            savedMap = curMap;
            savedZone = TileMap.zoneID;
            try {
                Char me = Char.getMyChar();
                if (me != null) {
                    savedX = me.cx;
                    savedY = me.cy;
                }
            } catch (Exception e) {}
            saveSavedStateToRMS();
        }
        GameScr.gameAC("TSBoss: Da dung TS tai M" + savedMap + " K" + savedZone + (savedX > 0 ? " (" + savedX + "," + savedY + ")" : ""));
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

    /** Check xem co boss nao dang active (trong 40P) phu hop voi eventPriority hien tai khong */
    private static boolean anyBossActiveForPriority() {
        switch (eventPriority) {
            case 1: // VDMQ + Lang Co
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_VDMQ)
                    || AutoSanBoss.isBossActive(AutoSanBoss.TYPE_LANGCO);
            case 2: // MapNgoai
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPNGOAI);
            case 3: // TheGioi
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_THEGIOI);
            case 4: // Lang Co only
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_LANGCO);
            case 5: // VDMQ only
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_VDMQ);
            case 6: // Map VIP (check ca 2 VIP map)
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPVIP)
                    || AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPVIP2);
            case 7: // Map VIP2 only
                return AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPVIP2);
            default: // Mac dinh = tat ca
                for (int i = 0; i < AutoSanBoss.TYPE_ALL; i++) {
                    if (AutoSanBoss.isBossActive(i)) return true;
                }
                return false;
        }
    }

    private static int currentWindowKey() {
        if (!anyBossActiveForPriority()) return -1;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return c.get(Calendar.YEAR) * 100000 + c.get(Calendar.DAY_OF_YEAR) * 100 + hour;
    }

    /** Kiem tra mat ket noi */
    private static boolean isDisconnected() {
        try {
            Char c = Char.getMyChar();
            if (c == null || c.cName == null) return true;
            return false;
        } catch (Exception e) { return true; }
    }

    /** Cho game tu reconnect, tra ve true neu ok, false neu timeout */
    private static boolean waitForReconnect() {
        GameScr.gameAC("TSBoss: M\u1ea5t k\u1ebft n\u1ed1i! Ch\u1edd...");
        for (int i = 0; i < 120 && isEnabled; i++) {
            sleep(1000L);
            if (!isDisconnected()) {
                sleep(3000L);
                GameScr.gameAC("TSBoss: \u0110\u00e3 k\u1ebft n\u1ed1i l\u1ea1i!");
                return true;
            }
        }
        return false;
    }

    /** Flag: da trigger pre-spawn cho gio boss tiep theo (tranh lap) */
    private static boolean preSpawnTriggered = false;
    /** Thoi gian chuan bi truoc khi boss spawn (giay) */
    private static final int PRE_SPAWN_SECONDS = 30;

    /**
     * Tra ve so giay con lai toi gio boss gan nhat phu hop voi eventPriority.
     * Tra ve Integer.MAX_VALUE neu khong co boss nao sap spawn.
     */
    private static int getSecondsTillNextForPriority() {
        int min = Integer.MAX_VALUE;
        switch (eventPriority) {
            case 1: // VDMQ + Lang Co
                min = Math.min(
                    AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_VDMQ),
                    AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_LANGCO));
                break;
            case 2: // MapNgoai
                min = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPNGOAI);
                break;
            case 3: // TheGioi
                min = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_THEGIOI);
                break;
            case 4: // Lang Co only
                min = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_LANGCO);
                break;
            case 5: // VDMQ only
                min = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_VDMQ);
                break;
            case 6: // Map VIP (ca 2 VIP map)
                min = Math.min(
                    AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPVIP),
                    AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPVIP2));
                break;
            case 7: // Map VIP2
                min = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPVIP2);
                break;
            default: // Tat ca
                for (int i = 0; i < AutoSanBoss.TYPE_ALL; i++) {
                    int s = AutoSanBoss.getSecondsTillNextBoss(i);
                    if (s < min) min = s;
                }
                break;
        }
        return min;
    }

    public static void travelToMap(int targetMap) {
        if (TileMap.mapID == targetMap) return;
        if (targetMap == 138 || TileMap.isLangCo(targetMap)) {
            AutoSanBoss.ensureInLangCo();
            return;
        }
        PkBoss travel = new PkBoss(targetMap);
        Code.gameAB = travel;
        for (int i = 0; i < 600 && isEnabled && inEvent && TileMap.mapID != targetMap; i++) {
            sleep(100L);
        }
        if (Code.gameAB == travel) Code.gameAB = null;
    }

    /**
     * Pre-spawn: vao Map VIP (M195) qua NPC truoc khi boss spawn.
     * Tu sat ve thon → hoi sinh → goi NPC VIP → vao M195.
     */
    private static void preSpawnEnterMapVIP() {
        if (TileMap.mapID == 195) return;

        int curMap = TileMap.mapID;

        // Neu da o thon (M22) thi goi NPC luon
        if (curMap != 22) {
            // Dung auto dang chay
            LockGame.gameBK();
            if (Code.gameAB != null && !(Code.gameAB instanceof SanBossHolder)) {
                Code.gameAB = null;
            }

            // Tu sat ve thon
            GameScr.gameAC("TSBoss: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o M195 ch\u1edd s\u1eb5n...");
            if (TileMap.isLangCo(curMap)) {
                AutoSanBoss.cleanKhaoDiLenh();
            }
            try { Code.gameAN(); } catch (Exception e) {}
            // Doi nhan vat chet that su (toi da 3s)
            for (int d = 0; d < 30 && isEnabled; d++) {
                sleep(100L);
                try {
                    Char me2 = Char.getMyChar();
                    if (me2 != null && (me2.statusMe == 14 || me2.cHP <= 0)) break;
                } catch (Exception e) {}
            }

            // Hoi sinh tai thon
            for (int r = 0; r < 15 && isEnabled; r++) {
                try {
                    Char me = Char.getMyChar();
                    if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                    GameCanvas.endDlg();
                    sleep(20L);
                    LockGame.gameAA = true;
                    if (Code.HoiSinhLuong && me != null && me.luong > 0) {
                        Service.gI().gameAL();
                    } else {
                        Service.gI().gameAK();
                        TileMap.gameAF();
                    }
                    LockGame.gameAA = false;
                    sleep(150L);
                } catch (Exception e) { break; }
            }
            sleep(200L);
        }

        if (TileMap.mapID == 195) return;

        // Goi NPC VIP (type 47, option 4) vao M195
        GameScr.gameAC("TSBoss: G\u1ecdi NPC VIP v\u00e0o M195...");
        for (int retry = 0; retry < 3 && isEnabled; retry++) {
            if (TileMap.mapID == 195) return;
            try {
                GameScr.gameAB(AutoVipMap.npcType, AutoVipMap.menuOption, 0);
            } catch (Exception e) {
                sleep(3000L);
                continue;
            }
            // Cho vao map (toi da 15s)
            for (int w = 0; w < 150 && isEnabled; w++) {
                sleep(100L);
                if (TileMap.mapID == 195) {
                    GameScr.gameAC("TSBoss: \u0110\u00e3 v\u00e0o M195, ch\u1edd boss spawn...");
                    return;
                }
            }
            sleep(2000L);
        }
    }

    /**
     * Pre-spawn: vao Map VIP 2 (M196) qua NPC truoc khi boss spawn.
     */
    private static void preSpawnEnterMapVIP2() {
        if (TileMap.mapID == 196) return;

        int curMap = TileMap.mapID;

        if (curMap != 22) {
            LockGame.gameBK();
            if (Code.gameAB != null && !(Code.gameAB instanceof SanBossHolder)) {
                Code.gameAB = null;
            }
            GameScr.gameAC("TSBoss: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o M196 ch\u1edd s\u1eb5n...");
            if (TileMap.isLangCo(curMap)) {
                AutoSanBoss.cleanKhaoDiLenh();
            }
            try { Code.gameAN(); } catch (Exception e) {}
            // Doi nhan vat chet that su (toi da 3s)
            for (int d = 0; d < 30 && isEnabled; d++) {
                sleep(100L);
                try {
                    Char me2 = Char.getMyChar();
                    if (me2 != null && (me2.statusMe == 14 || me2.cHP <= 0)) break;
                } catch (Exception e) {}
            }
            for (int r = 0; r < 15 && isEnabled; r++) {
                try {
                    Char me = Char.getMyChar();
                    if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                    GameCanvas.endDlg();
                    sleep(20L);
                    LockGame.gameAA = true;
                    if (Code.HoiSinhLuong && me != null && me.luong > 0) {
                        Service.gI().gameAL();
                    } else {
                        Service.gI().gameAK();
                        TileMap.gameAF();
                    }
                    LockGame.gameAA = false;
                    sleep(150L);
                } catch (Exception e) { break; }
            }
            sleep(200L);
        }

        if (TileMap.mapID == 196) return;

        // Goi NPC VIP (type 47, option 5) vao M196
        GameScr.gameAC("TSBoss: G\u1ecdi NPC VIP v\u00e0o M196...");
        for (int retry = 0; retry < 3 && isEnabled; retry++) {
            if (TileMap.mapID == 196) return;
            try {
                GameScr.gameAB(AutoVipMap.npcType, AutoVipMap.menuOption + 1, 0);
            } catch (Exception e) {
                sleep(1000L);
                continue;
            }
            for (int w = 0; w < 150 && isEnabled; w++) {
                sleep(100L);
                if (TileMap.mapID == 196) {
                    GameScr.gameAC("TSBoss: \u0110\u00e3 v\u00e0o M196, ch\u1edd boss spawn...");
                    return;
                }
            }
            sleep(500L);
        }
    }

    private static int getFirstMapForPriority() {
        return getFirstMapForPriority(eventPriority);
    }

    /** Tra ve map dau tien cho 1 priority cu the. Tach ra de default goi lai theo boss gan nhat. */
    private static int getFirstMapForPriority(int priority) {
        switch (priority) {
            case 1: // VDMQ + Lang Co
                if (AutoSanBoss.isMapEnabled(141)) return 141;
                if (AutoSanBoss.isMapEnabled(142)) return 142;
                if (AutoSanBoss.isMapEnabled(143)) return 143;
                return 138;
            case 2: // MapNgoai
                int[] mnMaps = AutoSanBoss.getMapNgoaiMaps();
                for (int i = 0; i < mnMaps.length; i++) {
                    if (AutoSanBoss.isMapEnabled(mnMaps[i])) return mnMaps[i];
                }
                return 14;
            case 3: // TheGioi
                return 20;
            case 4: // Lang Co
                return 138;
            case 5: // VDMQ
                if (AutoSanBoss.isMapEnabled(141)) return 141;
                if (AutoSanBoss.isMapEnabled(142)) return 142;
                if (AutoSanBoss.isMapEnabled(143)) return 143;
                return 141;
            case 6: // Map VIP — vao qua NPC, khong can GoMap truoc
                return -1;
            case 7: // Map VIP2 — vao qua NPC, khong can GoMap truoc
                return -1;
            default: {
                // Tat ca: dung HUNT_PRIORITY (VIP2 > VIP > LC > VDMQ > TG > MN)
                // 1. Uu tien boss DANG ACTIVE theo thu tu priority
                int[] hp = AutoSanBoss.getHuntPriority();
                for (int i = 0; i < hp.length; i++) {
                    if (AutoSanBoss.isBossActive(hp[i])) {
                        return mapForBossType(hp[i]);
                    }
                }
                // 2. Khong co boss active → tim boss SAP SPAWN GAN NHAT
                int closestType = -1;
                int closestSec = Integer.MAX_VALUE;
                for (int i = 0; i < hp.length; i++) {
                    int s = AutoSanBoss.getSecondsTillNextBoss(hp[i]);
                    if (s < closestSec) {
                        closestSec = s;
                        closestType = hp[i];
                    } else if (s == closestSec && closestType >= 0) {
                        // Cung gio spawn → uu tien theo HUNT_PRIORITY (hp[i] co index thap hon = uu tien cao hon)
                        // hp[i] da duyet theo thu tu uu tien nen khong can doi
                    }
                }
                if (closestType >= 0) return mapForBossType(closestType);
                // Fallback
                if (AutoSanBoss.isMapEnabled(141)) return 141;
                return 14;
            }
        }
    }

    /** Tra ve map dau tien cho 1 boss type cu the */
    private static int mapForBossType(int bossType) {
        if (bossType == AutoSanBoss.TYPE_VDMQ) {
            return getFirstMapForPriority(5);
        } else if (bossType == AutoSanBoss.TYPE_MAPNGOAI) {
            return getFirstMapForPriority(2);
        } else if (bossType == AutoSanBoss.TYPE_THEGIOI) {
            return 20;
        } else if (bossType == AutoSanBoss.TYPE_LANGCO) {
            return 138;
        } else if (bossType == AutoSanBoss.TYPE_MAPVIP || bossType == AutoSanBoss.TYPE_MAPVIP2) {
            return -1; // VIP vao qua NPC
        }
        return 14;
    }


    public void run() {
        while (isEnabled) {
            try {
                // Check disconnect dau moi vong
                if (isDisconnected()) {
                    if (!waitForReconnect()) break;
                    // Sau reconnect, reset lastWindowKey de co the trigger lai
                    lastWindowKey = -1;
                    preSpawnTriggered = false;
                    continue;
                }

                int key = currentWindowKey();
                // CHI leader moi tu dong bat event khi den gio boss
                // Thanh vien chi nhan lenh tu truong nhom qua pkm -4/-5
                if (!inEvent && key >= 0 && key != lastWindowKey && isLeader()) {
                    lastWindowKey = key;
                    preSpawnTriggered = false;
                    beginLeaderEvent();
                }

                // === PRE-SPAWN: 30s truoc gio boss → chay ra map cho san ===
                if (!inEvent && !preSpawnTriggered && isLeader()) {
                    int secLeft = getSecondsTillNextForPriority();
                    if (secLeft > 0 && secLeft <= PRE_SPAWN_SECONDS) {
                        preSpawnTriggered = true;
                        // Tinh gio boss sap toi (khong dung Calendar.add vi J2ME khong co)
                        Calendar fc = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        int curSec = fc.get(Calendar.HOUR_OF_DAY) * 3600 + fc.get(Calendar.MINUTE) * 60 + fc.get(Calendar.SECOND);
                        int spawnHour = ((curSec + secLeft) / 3600) % 24;
                        // Tinh day offset neu qua nua dem
                        int dayOff = (curSec + secLeft >= 86400) ? 1 : 0;
                        lastWindowKey = fc.get(Calendar.YEAR) * 100000 + (fc.get(Calendar.DAY_OF_YEAR) + dayOff) * 100 + spawnHour;
                        GameScr.gameAC("TSBoss: C\u00f2n " + secLeft + "s, chu\u1ea9n b\u1ecb s\u0103n boss!");
                        beginLeaderEvent();
                    }
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
        GameScr.gameAC("TSBoss: Chu\u1ea9n b\u1ecb s\u0103n boss! L\u01b0u M" + savedMap + " K" + savedZone);
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

        // Neu chua den gio (dang o che do pre-spawn): chay den map cho san va dung doi
        // SKIP pre-spawn wait khi ignoreBossHourCheck = true (trigger tu Chat Notice / TB Boss)
        // vi boss DA spawn hoac SAP spawn, khong can cho them
        int preSec = getSecondsTillNextForPriority();
        if (!AutoSanBoss.ignoreBossHourCheck && preSec > 0 && preSec <= PRE_SPAWN_SECONDS) {
            int firstMap = getFirstMapForPriority();
            if (firstMap > 0) {
                GameScr.gameAC("TSBoss: Ra M" + firstMap + " ch\u1edd s\u1eb5n...");
                travelToMap(firstMap);
            } else if (firstMap == -1) {
                // VIP map: vao qua NPC. Neu DA dung o M195/M196 → o lai, khong can tele
                int curMap = TileMap.mapID;
                if (curMap == 195 || curMap == 196) {
                    GameScr.gameAC("TSBoss: \u0110ang \u1edf M" + curMap + ", ch\u1edd boss t\u1ea1i \u0111\u00e2y...");
                } else {
                    // Xac dinh M195 hay M196
                    boolean needVIP2 = (eventPriority == 7);
                    if (!needVIP2 && eventPriority == 0) {
                        // Default/All: check boss VIP nao gan nhat
                        int sVIP1 = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPVIP);
                        int sVIP2 = AutoSanBoss.getSecondsTillNextBoss(AutoSanBoss.TYPE_MAPVIP2);
                        if (AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPVIP2)) needVIP2 = true;
                        else if (!AutoSanBoss.isBossActive(AutoSanBoss.TYPE_MAPVIP) && sVIP2 < sVIP1) needVIP2 = true;
                    }
                    if (!needVIP2 && AutoSanBoss.isMapEnabled(196) && !AutoSanBoss.isMapEnabled(195)) needVIP2 = true;
                    if (needVIP2) {
                        preSpawnEnterMapVIP2();
                    } else {
                        preSpawnEnterMapVIP();
                    }
                }
            }
            // Cho den 4s truoc gio boss spawn — bat dau san som de quet khu truoc
            while (isEnabled && inEvent) {
                int s = getSecondsTillNextForPriority();
                if (s <= preSpawnBreakSec || s > PRE_SPAWN_SECONDS) break;
                GameScr.gameAC("TSBoss: Ch\u1edd t\u1ea1i M" + TileMap.mapID + " (" + s + "s)...");
                for (int w = 0; w < 10 && isEnabled && inEvent; w++) sleep(100L);
            }
            // Doi boss thuc su spawn truoc khi bat dau san
            // (neu preSpawnBreakSec > 0, bot thoat wait loop som vai giay truoc spawn)
            if (preSpawnBreakSec > 0) {
                GameScr.gameAC("TSBoss: \u0110\u1ee3i " + preSpawnBreakSec + "s boss spawn...");
                for (int ws = 0; ws < preSpawnBreakSec * 10 && isEnabled && inEvent; ws++) sleep(100L);
            }
            // Doi them 2s sau gio spawn de boss thuc su xuat hien tren map
            GameScr.gameAC("TSBoss: \u0110\u1ee3i boss xu\u1ea5t hi\u1ec7n...");
            for (int ws2 = 0; ws2 < 20 && isEnabled && inEvent; ws2++) sleep(100L);
            GameScr.gameAC("TSBoss: Boss spawn! B\u1eaft \u0111\u1ea7u s\u0103n theo \u01b0u ti\u00ean!");
        }
        // ignoreBossHourCheck KHONG bi thay doi o day:
        // - Pre-spawn path: da la false tu truoc → isBossActive() chi tra true cho boss DANG RA
        // - triggerImmediate: da set true → giu nguyen de hunt boss tu Chat Notice (co the ngoai gio)
        // eventHuntTypes KHONG set: luot 1 quet TAT CA boss active theo HUNT_PRIORITY

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
                case 4:
                    AutoSanBoss.startEventHuntLC();
                    break;
                case 5:
                    AutoSanBoss.startEventHuntVDMQ();
                    break;
                case 6:
                    // Map VIP: check cai dat xem bat M196 hay M195
                    if (AutoSanBoss.isMapEnabled(196)) {
                        AutoSanBoss.startEventHuntMapVIP2();
                    } else {
                        AutoSanBoss.startEventHuntMapVIP();
                    }
                    break;
                case 7:
                    AutoSanBoss.startEventHuntMapVIP2();
                    break;
                default:
                    AutoSanBoss.startEventHuntAll();
                    break;
            }
        }

        // === Luot 1: Quet TAT CA boss active theo HUNT_PRIORITY + goi ae fang boss ===
        while (isEnabled && inEvent) {
            if (AutoSanBoss.consumeEventRoundCompleted()) break;
            // Neu AutoSanBoss da dung (disconnect/error) -> thoat luot
            if (!AutoSanBoss.isRunning) {
                GameScr.gameAC("TSBoss: AutoSanBoss d\u1eebng, k\u1ebft th\u00fac l\u01b0\u1ee3t");
                break;
            }
            if (isDisconnected()) {
                if (!waitForReconnect()) { finishEvent(false); return; }
                // Reconnect ok nhung AutoSanBoss da chet -> ket thuc event, cho trigger lai
                if (!AutoSanBoss.isRunning) {
                    lastWindowKey = -1;
                    finishEvent(false);
                    return;
                }
            }
            sleep(500L);
        }

        if (!isEnabled || !inEvent) { finishEvent(false); return; }
        // Neu AutoSanBoss da chet (disconnect/loi) -> ket thuc som, reset key de trigger lai
        if (!AutoSanBoss.isRunning) {
            lastWindowKey = -1;
            finishEvent(false);
            return;
        }

        // Xong luot 1: reset ignoreBossHourCheck de luot sau check gio binh thuong
        AutoSanBoss.ignoreBossHourCheck = false;
        membersSentBack = true;
        GameScr.gameAC("TSBoss: Xong l\u01b0\u1ee3t 1, g\u1eedi nh\u00f3m v\u1ec1 farm!");
        sendParty("pkm -5");
        sleep(3000L);
        AutoSanBoss.isPartyMode = false;

        // === Luot them: Leader quet solo ===
        for (int round = 0; round < extraRounds && isEnabled && inEvent; round++) {
            GameScr.gameAC("TSBoss: Leader qu\u00e9t l\u01b0\u1ee3t " + (round + 2) + "/" + (extraRounds + 1) + "...");
            while (isEnabled && inEvent) {
                if (AutoSanBoss.consumeEventRoundCompleted()) break;
                if (!AutoSanBoss.isRunning) break;
                if (isDisconnected()) {
                    if (!waitForReconnect()) { finishEvent(false); return; }
                    if (!AutoSanBoss.isRunning) {
                        lastWindowKey = -1;
                        finishEvent(false);
                        return;
                    }
                }
                sleep(500L);
            }
        }

        // Xong tat ca luot -> ve TS
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
        // Khôi phục priority gốc sau khi hunt từ TB Boss xong
        if (savedEventPriority >= 0) {
            eventPriority = savedEventPriority;
            savedEventPriority = -1;
            GameScr.gameAC("TSBoss: Kh\u00f4i ph\u1ee5c \u01b0u ti\u00ean: " + priorityName());
        }
        // Nếu TS Boss chưa bật trước trigger → tắt lại
        if (!wasEnabledBeforeTrigger) {
            isEnabled = false;
            wasEnabledBeforeTrigger = true;
        }
        AutoSanBoss.ignoreBossHourCheck = false;
        preSpawnTriggered = false;
    }

    private static void returnAndResume() {
        if (savedMap < 0) {
            loadSavedStateFromRMS();
        }
        // Fallback: neu co bat TS Map VIP hoac Tu Luyen
        if (savedMap < 0) {
            if (AutoVipMap.isEnabled) savedMap = AutoVipMap.targetMapID;
            else if (AutoTuLuyen.isEnabled) savedMap = 192;
        }
        final int map = savedMap;
        final int zone = savedZone;
        final int targetX = savedX;
        final int targetY = savedY;
        final Auto oldAuto = savedAuto;
        inEvent = false;
        savedMap = -1;
        savedZone = -1;
        savedX = -1;
        savedY = -1;
        savedAuto = null;
        clearSavedStateRMS();
        if (map < 0 && !AutoVipMap.isEnabled && !AutoTuLuyen.isEnabled) return;

        // Xac dinh neu day la Map VIP hoac Tu Luyen (vao qua NPC)
        final int targetNpcMap;
        final int npcOption;
        final boolean isNpcMap;
        if (map == 196 || (AutoVipMap.isEnabled && AutoVipMap.targetMapID == 196)) {
            targetNpcMap = 196;
            npcOption = 5;
            isNpcMap = true;
        } else if (map == 195 || (AutoVipMap.isEnabled && AutoVipMap.targetMapID == 195)) {
            targetNpcMap = 195;
            npcOption = 4;
            isNpcMap = true;
        } else if (map == 192 || AutoTuLuyen.isEnabled) {
            targetNpcMap = 192;
            npcOption = 3;
            isNpcMap = true;
        } else {
            targetNpcMap = map;
            npcOption = -1;
            isNpcMap = false;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Dong dialog/NPC neu dang mo (tranh chan travel)
                    try { GameCanvas.endDlg(); } catch (Exception e) {}

                    if (isNpcMap) {
                        // === CHE DO VIP / TU LUYEN: Vao qua NPC 47 ===
                        if (TileMap.mapID != targetNpcMap) {
                            GameScr.gameAC("TSBoss: T\u1ef1 s\u00e1t v\u1ec1 th\u00f4n \u0111\u1ec3 v\u00e0o M" + targetNpcMap + "...");
                            // Xoa auto hien tai
                            LockGame.gameBK();
                            if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss)) {
                                Code.gameAB = null;
                            }
                            // Clean Lang Co neu con sot
                            if (TileMap.isLangCo(TileMap.mapID) || TileMap.mapID == 135 || TileMap.mapID == 136 || TileMap.mapID == 138) {
                                AutoSanBoss.cleanKhaoDiLenh();
                                sleep(300L);
                            }
                            // Tu sat
                            try { Code.gameAN(); } catch (Exception e) {}
                            sleep(500L);
                            // Cho nhan vat chet (statusMe == 14 hoac cHP <= 0)
                            for (int d = 0; d < 30; d++) {
                                sleep(100L);
                                try {
                                    Char me2 = Char.getMyChar();
                                    if (me2 != null && (me2.statusMe == 14 || me2.cHP <= 0)) break;
                                } catch (Exception e) {}
                            }
                            // Hoi sinh ve lang (gameAK) - LUON LUON de ve thon co NPC 47
                            for (int r = 0; r < 15; r++) {
                                try {
                                    Char me = Char.getMyChar();
                                    if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                                    GameCanvas.endDlg();
                                    sleep(20L);
                                    Auto.gameAN.removeAllElements();
                                    Auto.gameAM = false;
                                    LockGame.gameAA = true;
                                    Service.gI().gameAK(); // Ve lang (LUON LUON)
                                    TileMap.gameAF();
                                    LockGame.gameAA = false;
                                    sleep(150L);
                                } catch (Exception ex) { break; }
                            }
                            sleep(500L);

                            // Goi NPC 47 de vao targetNpcMap
                            GameScr.gameAC("TSBoss: G\u1ecdi NPC VIP v\u00e0o M" + targetNpcMap + "...");
                            for (int retry = 0; retry < 3 && TileMap.mapID != targetNpcMap; retry++) {
                                try {
                                    GameScr.gameAB(47, npcOption, 0);
                                } catch (Exception e) {
                                    sleep(1000L);
                                    continue;
                                }
                                for (int w = 0; w < 150 && TileMap.mapID != targetNpcMap; w++) {
                                    sleep(100L);
                                }
                                if (TileMap.mapID != targetNpcMap) sleep(1000L);
                            }
                        }

                        // Doi khu cu neu da vao dung map
                        if (TileMap.mapID == targetNpcMap && zone >= 0 && TileMap.zoneID != zone) {
                            Auto.gameAA(zone);
                            for (int i = 0; i < 1000 && TileMap.zoneID != zone; i++) sleep(10L);
                        }
                        // Di chuyen ve toa do (x, y) da luu
                        if (TileMap.mapID == targetNpcMap && targetX > 0 && targetY > 0) {
                            try {
                                Char.gameAE(targetX, targetY);
                                Char.getMyChar().cx = targetX;
                                Char.getMyChar().cy = targetY;
                                Service.gI().gameAC(targetX, targetY);
                                sleep(200L);
                            } catch (Exception ex) {}
                        }

                        // Khoi phuc auto
                        if (oldAuto != null) {
                            Code.gameAW = savedZoneIndex;
                            Code.gameAB = oldAuto;
                            AutoPickup.start();
                            TsBoost.onTsStarted();
                            GameScr.gameAC("TSBoss: V\u1ec1 M" + targetNpcMap + " K" + (zone >= 0 ? zone : TileMap.zoneID) + (targetX > 0 ? " (" + targetX + "," + targetY + ")" : "") + " - ti\u1ebfp t\u1ee5c TS");
                        } else {
                            try {
                                Code.gameAW = savedZoneIndex;
                                Code.gameAA(-1, (int)TileMap.mapID);
                                AutoPickup.start();
                                TsBoost.onTsStarted();
                            } catch (Exception e) {}
                            GameScr.gameAC("TSBoss: V\u1ec1 M" + targetNpcMap + " K" + (zone >= 0 ? zone : TileMap.zoneID) + " - ti\u1ebfp t\u1ee5c TS");
                        }
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
                            GameScr.gameAC("TSBoss: Th\u1eed l\u1ea1i l\u1ea7n " + (retry + 1) + "...");
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
                    if (TileMap.mapID == map && zone >= 0 && TileMap.zoneID != zone) {
                        Auto.gameAA(zone);
                        for (int i = 0; i < 1000 && TileMap.zoneID != zone; i++) sleep(10L);
                    }
                    // Di chuyen ve toa do (x, y) da luu
                    if (TileMap.mapID == map && targetX > 0 && targetY > 0) {
                        try {
                            Char.gameAE(targetX, targetY);
                            Char.getMyChar().cx = targetX;
                            Char.getMyChar().cy = targetY;
                            Service.gI().gameAC(targetX, targetY);
                            sleep(200L);
                        } catch (Exception ex) {}
                    }
                } catch (Exception e) {}
                // Khoi phuc auto: dung saved neu co, khong thi restart TS
                if (oldAuto != null) {
                    Code.gameAW = savedZoneIndex; // Khoi phuc vi tri khu tuan tu
                    Code.gameAB = oldAuto;
                    AutoPickup.start();
                    TsBoost.onTsStarted();
                    GameScr.gameAC("TSBoss: V\u1ec1 M" + map + " K" + (zone >= 0 ? zone : TileMap.zoneID) + (targetX > 0 ? " (" + targetX + "," + targetY + ")" : "") + " - ti\u1ebfp t\u1ee5c TS");
                } else {
                    // Fallback: khong co auto cu -> restart TanSat tai map hien tai
                    try {
                        Code.gameAW = savedZoneIndex;
                        Code.gameAA(-1, (int)TileMap.mapID);
                        AutoPickup.start();
                        TsBoost.onTsStarted();
                    } catch (Exception e) {}
                    GameScr.gameAC("TSBoss: V\u1ec1 M" + map + " K" + (zone >= 0 ? zone : TileMap.zoneID) + " - b\u1eadt l\u1ea1i TS m\u1edbi");
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

    // === RMS ===

    /** Luu vi tri farm (Map, Khu, X, Y) vao RMS de phong mat ket noi / crash */
    public static void saveSavedStateToRMS() {
        try {
            if (savedMap >= 0) {
                RMS.gameAA("boss_saved_pos", savedMap + ";" + savedZone + ";" + savedX + ";" + savedY + ";" + savedZoneIndex);
            }
        } catch (Exception e) {}
    }

    /** Load vi tri farm tu RMS khi khoi dong lai hoac reconnect */
    public static void loadSavedStateFromRMS() {
        try {
            String data = RMS.gameAC("boss_saved_pos");
            if (data != null && data.length() > 0) {
                int[] v = new int[5];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 5; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        v[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 4 && savedMap < 0) {
                    savedMap = v[0];
                    savedZone = v[1];
                    savedX = v[2];
                    savedY = v[3];
                    if (idx >= 5) savedZoneIndex = v[4];
                }
            }
        } catch (Exception e) {}
    }

    /** Xoa vi tri farm trong RMS khi da quay ve hoac tat auto */
    public static void clearSavedStateRMS() {
        try {
            RMS.gameAA("boss_saved_pos", "");
        } catch (Exception e) {}
    }

    /** Luu isEnabled + eventPriority + extraRounds + preSpawnBreakSec vao RMS */
    public static void saveConfigToRMS() {
        try {
            RMS.gameAA("boss_event_cfg", (isEnabled ? 1 : 0) + ";" + eventPriority + ";" + extraRounds + ";" + preSpawnBreakSec);
        } catch (Exception e) {}
    }

    /** Load isEnabled + eventPriority + extraRounds + preSpawnBreakSec tu RMS. Auto-start thread neu enabled. */
    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("boss_event_cfg");
            if (data != null && data.length() > 0) {
                int[] vals = new int[4];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 4; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        vals[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 2) {
                    isEnabled = vals[0] == 1;
                    eventPriority = vals[1];
                }
                if (idx >= 3) {
                    extraRounds = vals[2];
                }
                if (idx >= 4) {
                    preSpawnBreakSec = vals[3];
                    if (preSpawnBreakSec < 0) preSpawnBreakSec = 0;
                    if (preSpawnBreakSec > 30) preSpawnBreakSec = 30;
                }
            }
        } catch (Exception e) {}
        loadSavedStateFromRMS();
        // Auto-start monitor thread neu config da luu enabled
        if (isEnabled) {
            new Thread(new AutoBossEvent()).start();
            lastWindowKey = -1;
        }
    }

    /** Hoi sinh nhanh neu nhan vat dang chet (statusMe==14 hoac cHP<=0). */
    private static void ensureAlive() {
        try {
            if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                for (int retry = 0; retry < 10; retry++) {
                    GameCanvas.endDlg();
                    sleep(20L);
                    Auto.gameAN.removeAllElements();
                    Auto.gameAM = false;
                    LockGame.gameAA = true;
                    if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                        Service.gI().gameAL();
                    } else {
                        Service.gI().gameAK();
                        TileMap.gameAF();
                    }
                    LockGame.gameAA = false;
                    sleep(300L);
                    if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) return;
                }
            }
        } catch (Exception e) {}
    }
}
