import java.util.Calendar;
import java.util.TimeZone;

/**
 * TB2AutoSanBoss - Tu dong san boss theo khung gio (ban TB2).
 * Port 1:1 tu AutoSanBoss ban goc, anh xa sang obfuscated classes TB2.
 *
 * Anh xa:
 *   Code          = Class_am;   Code.gameAB (auto hien tai) = Class_am.b
 *   Auto          = Class_af;   Auto.mapID = b, Auto.zoneID = c, Auto.reAB = j
 *   PkBoss        = Class_cc
 *   TileMap.mapID  = Class_fq.o;   TileMap.zoneID = Class_fq.l
 *   GameScr.vMob   = Class_ds.ag;  GameScr.vParty = Class_ds.y
 *   GameScr.gameAC = Class_ds.c(String)
 *   Mob            = Class_fk;  isBoss=y, hp=c, status=h
 *   Char           = Class_dk;  getMyChar()=g(), mobFocus=co, cName=bc, statusMe=p
 *   Service        = Class_di.a();  gameAK(party chat)=k(String)
 *   Auto.gameAA(zone) = Class_af.a(int)
 *   Code.gameAA(auto) = Class_am.a(Class_af)
 *   Code.gameAC()     = Class_am.c()
 *   vFriend=Class_ds.aa, Friend=Class_cv, friendName=a
 *   Service.gameAF(invite)=Class_di.a().f(String)
 *   Service.gameAU(refresh friends)=Class_di.a().u()
 */
public final class TB2AutoSanBoss implements Runnable {
    public static volatile boolean enabled;
    public static volatile boolean treoMode;
    public static volatile int forcedType = -1;
    private static volatile boolean memberTreo;
    private static volatile int memberMap = -1;
    private static volatile int memberZone = -1;
    private static Thread thread;

    // Dummy Auto giu menu "Tat Auto"
    private static TB2SanBossHolder dummyAuto;

    private static final int TYPE_SERVER = 0;
    private static final int TYPE_THEGIOI = 1;
    private static final int TYPE_VDMQ = 2;
    private static final int TYPE_MAPNGOAI = 3;
    private static final int TYPE_ALL = 4;

    private static final String[] BOSS_NAMES = {"Server", "Th\u1ebf Gi\u1edbi", "VDMQ", "MapNgo\u00e0i", "T\u1ea5t C\u1ea3"};

    private static final int[][] MAPS = {
        {63}, {65}, {141, 142, 143},
        {14, 15, 16, 44, 67, 70, 21, 41, 45, 18, 46, 54}
    };
    private static final int[][] HOURS = {
        {12, 18, 20, 22}, {11, 17, 19, 21}, {6, 13, 19, 23},
        {1, 4, 7, 10, 13, 16, 19, 22}
    };

    private static final int BOSS_ALIVE_DURATION = 2400; // 40 phut
    private static final int MAX_ZONES = 30;
    private static final int RECONNECT_TIMEOUT = 120;

    private TB2AutoSanBoss() {}

    // === TOGGLE METHODS ===

    public static void toggleHunt() {
        if (enabled) { toggleOff(); }
        else { toggleOn(false, -1); }
    }

    public static void toggleTreo() {
        if (enabled) { toggleOff(); }
        else {
            treoMode = true;
            toggleOn(true, TYPE_ALL);
        }
    }

    public static void toggleHuntType(int type) {
        if (enabled) { toggleOff(); }
        else { toggleOn(false, type); }
    }

    public static void toggleTreoType(int type) {
        if (enabled) { toggleOff(); }
        else {
            treoMode = true;
            toggleOn(true, type);
        }
    }

    private static void toggleOn(boolean treo, int type) {
        enabled = true;
        treoMode = treo;
        forcedType = type;
        memberTreo = false;
        dummyAuto = new TB2SanBossHolder();
        Class_am.b = dummyAuto;

        thread = new Thread(new TB2AutoSanBoss());
        thread.start();

        // Thong bao
        if (treo) {
            Class_ds.c("B\u1eadt TREO Boss! T\u00ecm boss \u2192 g\u1ecdi nh\u00f3m \u2192 \u0111\u1ee9ng ch\u1edd!");
        } else {
            if (type >= 0 && type < 4) {
                Class_ds.c("S\u0103n " + BOSS_NAMES[type] + " ngay!");
            } else if (type == TYPE_ALL) {
                Class_ds.c("B\u1eadt S\u0103n T\u1ea5t C\u1ea3 Boss!");
            } else {
                Class_ds.c("B\u1eadt S\u0103n Boss!");
            }
        }

        // Moi ban be va gui tin hieu nhom
        autoInviteFriends(true);
    }

    private static void toggleOff() {
        enabled = false;
        treoMode = false;
        forcedType = -1;
        memberTreo = false;
        memberMap = -1;
        memberZone = -1;
        // Tat PkBoss dang chay (neu co)
        if (Class_am.b instanceof Class_cc) {
            try { Class_am.c(); } catch (Exception e) {}
        }
        if (Class_am.b == dummyAuto) {
            Class_am.b = null;
        }
        dummyAuto = null;
        // Interrupt thread de thoat sleep ngay lap tuc
        if (thread != null) {
            try { thread.interrupt(); } catch (Exception e) {}
        }
        // Gui lenh tat cho nhom
        sendParty("pkm -3");
        sendParty("pke");
        Class_ds.c("T\u1eaft S\u0103n/Treo Boss!");
    }

    public static void stop() {
        if (enabled) {
            enabled = false;
            treoMode = false;
            forcedType = -1;
            // Tat PkBoss dang chay (neu co)
            if (Class_am.b instanceof Class_cc) {
                try { Class_am.c(); } catch (Exception e) {}
            }
            if (Class_am.b == dummyAuto) {
                Class_am.b = null;
            }
            dummyAuto = null;
            // Interrupt thread
            if (thread != null) {
                try { thread.interrupt(); } catch (Exception e) {}
            }
        }
    }

    // === DUMMY AUTO ===

    /**
     * Dam bao TB2SanBossHolder luon ton tai de giu menu "Tat Auto".
     * Phuc hoi khi Class_am.b bi null HOAC bi ghi de boi auto khac (khong phai Class_cc).
     */
    public static void restoreDummyAuto() {
        if (!enabled || dummyAuto == null) return;
        Class_af current = Class_am.b;
        if (current == null) {
            Class_am.b = dummyAuto;
        } else if (current != dummyAuto && !(current instanceof Class_cc)) {
            dummyAuto.j = current; // reAB chain
            Class_am.b = dummyAuto;
        }
    }

    // === DISCONNECT / RECONNECT ===

    private boolean isDisconnected() {
        try {
            Class_dk c = Class_dk.g();
            if (c == null || c.bc == null) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean waitForReconnect(int maxWaitSec) {
        Class_ds.c("TSB: M\u1ea5t k\u1ebft n\u1ed1i! Ch\u1edd k\u1ebft n\u1ed1i l\u1ea1i...");
        for (int i = 0; i < maxWaitSec && enabled; i++) {
            sleep(1000L);
            if (!isDisconnected()) {
                sleep(5000L);
                if (dummyAuto == null) {
                    dummyAuto = new TB2SanBossHolder();
                }
                Class_am.b = dummyAuto;
                Class_ds.c("TSB: \u0110\u00e3 k\u1ebft n\u1ed1i l\u1ea1i! Ti\u1ebfp t\u1ee5c...");
                // Re-invite party neu can
                if (Class_ds.y != null && Class_ds.y.size() <= 1) {
                    autoInviteFriends(false);
                }
                return true;
            }
        }
        Class_ds.c("TSB: Kh\u00f4ng k\u1ebft n\u1ed1i l\u1ea1i \u0111\u01b0\u1ee3c. D\u1eebng.");
        return false;
    }

    // === RESPAWN ===

    private boolean isDead() {
        try {
            Class_dk player = Class_dk.g();
            return player != null && player.p == 14;
        } catch (Exception e) { return false; }
    }

    // === SLEEP WITH CHECK ===

    private void sleepSeconds(int seconds) {
        for (int w = 0; w < seconds && enabled; w++) {
            sleep(1000L);
            restoreDummyAuto();
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) {
                    enabled = false;
                }
                break;
            }
        }
    }

    // === BOSS DETECTION ===

    private static boolean hasBoss() {
        try {
            if (Class_ds.ag == null) return false;
            for (int i = 0; i < Class_ds.ag.size(); i++) {
                Class_fk mob = (Class_fk)Class_ds.ag.elementAt(i);
                if (mob != null && mob.y && mob.c > 0 && mob.h != 0) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void lockBoss() {
        try {
            Class_dk player = Class_dk.g();
            if (player == null || Class_ds.ag == null) return;
            for (int i = 0; i < Class_ds.ag.size(); i++) {
                Class_fk mob = (Class_fk)Class_ds.ag.elementAt(i);
                if (mob != null && mob.y && mob.c > 0 && mob.h != 0) {
                    player.co = mob;
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    // === BOSS SCHEDULE ===

    private static boolean isBossActive(int type) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        int currentSec = h * 3600 + m * 60 + s;
        int[] hours = HOURS[type];
        for (int i = 0; i < hours.length; i++) {
            int spawnSec = hours[i] * 3600;
            int diff = currentSec - spawnSec;
            if (diff >= 0 && diff < BOSS_ALIVE_DURATION) return true;
        }
        return false;
    }

    // === PARTY ===

    private static void sendParty(String command) {
        try {
            if (Class_ds.y != null && Class_ds.y.size() > 1) Class_di.a().k(command);
        } catch (Exception ignored) {}
    }

    // === INVITE FRIENDS ===

    public static void autoInviteFriends() {
        autoInviteFriends(false);
    }

    private static void autoInviteFriends(final boolean signalAfter) {
        new Thread(new Runnable() {
            public void run() {
                int count = 0;
                int friendCount = 0;
                Class_du invited = new Class_du();
                String myName = "";
                try {
                    Class_dk player = Class_dk.g();
                    if (player != null && player.bc != null) myName = player.bc;
                } catch (Exception ignored) {}
                try {
                    Class_di.a().u();
                    for (int wait = 0; wait < 30; wait++) {
                        if (Class_ds.aa != null && Class_ds.aa.size() > 0) break;
                        sleep(100L);
                    }
                    if (Class_ds.aa != null) {
                        friendCount = Class_ds.aa.size();
                        for (int i = 0; i < Class_ds.aa.size(); i++) {
                            try {
                                Class_cv friend = (Class_cv)Class_ds.aa.elementAt(i);
                                if (friend != null && inviteName(friend.a, myName, invited)) count++;
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
                Class_ds.c("TSB: B\u1ea1n b\u00e8 " + friendCount + ", \u0111\u00e3 m\u1eddi " + count + "!");
                if (signalAfter && enabled) sendParty(treoMode ? "pkm -2" : "pkm -1");
            }
        }).start();
    }

    private static boolean inviteName(String name, String myName, Class_du invited) {
        if (name == null) return false;
        name = name.trim();
        if (name.length() == 0 || name.equals(myName) || invited.contains(name)) return false;
        try {
            Class_di.a().f(name);
            invited.addElement(name);
            Thread.sleep(250L);
            return true;
        } catch (Exception ignored) { return false; }
    }

    // === PARTY COMMANDS (member nhan tu leader) ===

    public static boolean handlePartyCommand(String sender, String message) {
        if (message == null) return false;
        String text = message.trim().toLowerCase();
        try {
            if (text.equals("pkm -2")) {
                enabled = true;
                treoMode = true;
                memberTreo = true;
                // Khoi phuc dummy
                if (dummyAuto == null) dummyAuto = new TB2SanBossHolder();
                Class_am.b = dummyAuto;
                return true;
            }
            if (text.equals("pkm -1")) {
                enabled = true;
                treoMode = false;
                memberTreo = false;
                if (dummyAuto == null) dummyAuto = new TB2SanBossHolder();
                Class_am.b = dummyAuto;
                return true;
            }
            if (text.equals("pkm -3")) {
                enabled = false;
                treoMode = false;
                memberTreo = false;
                if (Class_am.b instanceof Class_cc) Class_am.c();
                if (Class_am.b == dummyAuto) Class_am.b = null;
                dummyAuto = null;
                return true;
            }
            if (text.startsWith("pkm ")) {
                int map = Integer.parseInt(text.substring(4).trim());
                if (map < 0) return true;
                memberMap = map;
                if (memberTreo) startMemberTravel(map);
                else Class_am.a(new Class_cc(map));
                return true;
            }
            if (text.startsWith("pkk ")) {
                int zone = Integer.parseInt(text.substring(4).trim());
                memberZone = zone;
                if (memberTreo) moveMemberTreo(zone);
                else if (Class_am.b instanceof Class_cc) Class_am.b.c = zone;
                return true;
            }
            if (text.equals("pke")) {
                if (Class_am.b instanceof Class_cc) Class_am.c();
                if (memberTreo) {
                    // Treo mode: chi pop PkBoss, giu holder song
                    restoreDummyAuto();
                } else {
                    // Danh mode: pke = dung hoan toan
                    // Khong set enabled=false vi leader quyet dinh
                }
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void startMemberTravel(final int map) {
        final Class_cc travel = new Class_cc(map);
        Class_am.a(travel);
        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 3000 && memberTreo; i++) {
                    if (Class_fq.o == map) break;
                    sleep(10L);
                }
                if (memberTreo && Class_fq.o == map && Class_am.b == travel) Class_am.c();
                restoreDummyAuto();
                if (memberZone >= 0) changeZone(memberZone);
                Class_ds.c("TREO: \u0110\u00e3 t\u1edbi M" + map + " K" + memberZone + ", \u0111\u1ee9ng ch\u1edd!");
            }
        }).start();
    }

    private static void moveMemberTreo(final int zone) {
        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 3000 && memberTreo; i++) {
                    if (Class_fq.o == memberMap) break;
                    sleep(10L);
                }
                if (!memberTreo || Class_fq.o != memberMap) return;
                if (Class_am.b instanceof Class_cc) Class_am.c();
                restoreDummyAuto();
                changeZone(zone);
                Class_ds.c("TREO: M" + memberMap + " K" + zone + " - \u0111\u1ee9ng ch\u1edd!");
            }
        }).start();
    }

    private static void changeZone(int zone) {
        try { Class_af.a(zone); } catch (Exception ignored) {}
    }

    // === GRAB ITEMS ===

    private void grabAllItems() {
        try {
            if (Class_ds.af == null) return;
            Class_dk player = Class_dk.g();
            if (player == null) return;
            int origX = player.k;
            int origY = player.l;
            int count = 0;
            for (int pass = 0; pass < 5; pass++) {
                int size = Class_ds.af.size();
                if (size == 0) break;
                for (int i = 0; i < size; i++) {
                    try {
                        Class_bp item = (Class_bp)Class_ds.af.elementAt(i);
                        if (item == null) continue;
                        int dx = Math.abs(origX - item.c);
                        int dy = Math.abs(origY - item.d);
                        if (dx > 50 || dy > 50) Class_dk.e(item.c, item.d);
                        Class_di.a().q(item.g);
                        count++;
                    } catch (Exception ignored) {}
                    sleep(5L);
                }
                sleep(50L);
            }
            // Ve vi tri goc
            Class_dk.e(origX, origY);
            player.k = origX;
            player.l = origY;
            if (count > 0) Class_ds.c("TSB: Nh\u1eb7t " + count + " VP!");
        } catch (Exception ignored) {}
    }

    // === MAIN RUN LOOP ===

    public void run() {
        sleep(2000L);

        while (enabled) {
            try {
                // Check disconnect
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                }

                restoreDummyAuto();

                // Check thanh vien nhom (khong tu quet, chi doi lenh)
                boolean isMember = false;
                try {
                    Class_dk myChar = Class_dk.g();
                    if (myChar != null && Class_ds.y != null && Class_ds.y.size() > 1) {
                        // TODO: check charId cua truong nhom
                        // Tam thoi: member khong tu quet khi memberTreo=true
                    }
                } catch (Exception e) {}

                if (memberTreo) {
                    // Thanh vien treo: khong tu quet, chi doi lenh
                    sleepSeconds(2);
                    continue;
                }

                if (forcedType == TYPE_ALL) {
                    // === FORCE ALL: San 4 loai 24/24 ===
                    for (int b = 0; b < TYPE_ALL && enabled; b++) {
                        huntBossType(b);
                    }
                    if (enabled) {
                        Class_ds.c("TSB: Xong 1 l\u01b0\u1ee3t ALL map, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    }
                } else if (forcedType >= 0 && forcedType < 4) {
                    // === FORCE 1 LOAI ===
                    huntBossType(forcedType);
                    if (enabled) {
                        Class_ds.c("TSB: Xong " + BOSS_NAMES[forcedType] + ", qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    }
                } else {
                    // === TU DONG THEO LICH ===
                    boolean huntedAny = false;
                    for (int bossType = 0; bossType < TYPE_ALL && enabled; bossType++) {
                        if (!isBossActive(bossType)) continue;
                        huntedAny = true;
                        huntBossType(bossType);
                    }
                    if (huntedAny && enabled) {
                        Class_ds.c("TSB: Xong 1 l\u01b0\u1ee3t, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    } else if (!huntedAny && enabled) {
                        sleepSeconds(30);
                    }
                }

            } catch (Exception e) {
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                } else {
                    sleep(5000L);
                }
            }
        }

        // Cleanup
        if (dummyAuto != null && Class_am.b == dummyAuto) {
            Class_am.b = null;
        }
        dummyAuto = null;
        forcedType = -1;
        enabled = false;
        Class_ds.c("TSB: \u0110\u00e3 d\u1eebng.");
    }

    // === HUNT 1 LOAI BOSS ===

    private void huntBossType(int bossType) {
        int[] maps = MAPS[bossType];
        String prefix = treoMode ? "TREO" : "TSB";
        Class_ds.c(prefix + ": S\u0103n " + BOSS_NAMES[bossType] + " (" + maps.length + " maps)");

        for (int mi = 0; mi < maps.length && enabled; mi++) {
            // Mode tu dong: check gio
            if (forcedType < 0 && !isBossActive(bossType)) break;
            if (treoMode) {
                treoScanMap(maps[mi]);
            } else {
                pkBossOnMap(maps[mi]);
            }
        }
    }

    // === PK BOSS TREN 1 MAP (DANH) ===
    // PkBoss TB2 chi quet K29->K15 roi dung. Ta tu quet khu thu cong K29->K0,
    // chi bat PkBoss de di chuyen + danh khi tim thay boss.

    private void pkBossOnMap(int mapID) {
        if (!enabled) return;
        Class_ds.c("TSB: PK M" + mapID);

        // 1. Dung PkBoss CHI DE di chuyen den map
        try {
            Class_am.a(new Class_cc(mapID));
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
                Class_am.a(new Class_cc(mapID));
            } else {
                return;
            }
        }

        // Doi den map (toi da 30s)
        for (int w = 0; w < 600 && enabled; w++) {
            sleep(50L);
            if (Class_fq.o == mapID) break;
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
                Class_am.a(new Class_cc(mapID));
            }
        }

        // 2. Tat PkBoss NGAY khi den map — KHONG cho no tu quet
        if (Class_am.b instanceof Class_cc) {
            Class_am.c();
        }
        restoreDummyAuto();

        if (Class_fq.o != mapID) {
            Class_ds.c("TSB: Kh\u00f4ng \u0111\u1ebfn \u0111\u01b0\u1ee3c M" + mapID);
            return;
        }

        // 3. Quet khu thu cong K29 -> K0
        for (int zone = MAX_ZONES - 1; zone >= 0 && enabled; zone--) {
            try {
                Class_af.a(zone);
            } catch (Exception e) {}
            sleep(50L);
            // Doi zone chuyen xong (toi da 1s)
            for (int w = 0; w < 10 && enabled && Class_fq.l != zone; w++) {
                sleep(100L);
            }

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
            }

            // Check boss — double check de tranh false positive
            if (!hasBoss()) {
                restoreDummyAuto();
                continue;
            }
            sleep(300L);
            if (!hasBoss()) {
                restoreDummyAuto();
                continue;
            }

            // === BOSS XAC NHAN! ===
            Class_ds.c("TSB: Boss t\u1ea1i M" + mapID + " K" + zone + "! \u0110\u00e1nh!");

            // Gui lenh nhom
            sendParty("pkm -1");
            sleep(50L);
            sendParty("pkm " + mapID);
            sleep(500L);
            sendParty("pkk " + zone);

            // Bat PkBoss tai zone nay de DANH (khong quet nua)
            Class_cc fighter = new Class_cc(mapID);
            fighter.c = zone; // set zone truc tiep, khong quet
            Class_am.a(fighter);

            long fightStart = System.currentTimeMillis();

            // Doi PkBoss danh xong (boss chet hoac timeout 3 phut)
            while (enabled && Class_am.b instanceof Class_cc) {
                if (System.currentTimeMillis() - fightStart > 180000L) break;

                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
                    // Restart PkBoss tai boss zone
                    Class_cc retry = new Class_cc(mapID);
                    retry.c = zone;
                    Class_am.a(retry);
                    continue;
                }

                // Lock boss focus lien tuc
                lockBoss();

                // Check chet
                if (isDead()) {
                    sleep(3000L);
                    if (enabled && !(Class_am.b instanceof Class_cc)) {
                        Class_cc respawn = new Class_cc(mapID);
                        respawn.c = zone;
                        Class_am.a(respawn);
                    }
                }

                sleep(200L);
            }

            // PkBoss xong
            restoreDummyAuto();

            long elapsed = System.currentTimeMillis() - fightStart;
            if (elapsed > 5000) {
                Class_ds.c("TSB: Xong M" + mapID + " K" + zone + " (" + (elapsed / 1000) + "s)");
                grabAllItems();
            }
            return; // Da danh boss, thoat map nay
        }

        Class_ds.c("TSB: Kh\u00f4ng th\u1ea5y boss M" + mapID);
    }

    // === TREO SCAN MAP (TIM BOSS KHONG DANH) ===

    private void treoScanMap(int mapID) {
        if (!enabled) return;
        Class_ds.c("TREO: Qu\u00e9t M" + mapID + "...");

        // Dung PkBoss CHI DE di chuyen den map
        Class_cc travel = new Class_cc(mapID);
        try {
            Class_am.a(travel);
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
                Class_am.a(travel);
            } else {
                return;
            }
        }

        // Doi den map (toi da 30s)
        for (int w = 0; w < 600 && enabled; w++) {
            sleep(50L);
            if (Class_fq.o == mapID) break;
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
            }
        }

        // Tat PkBoss ngay khi den map
        if (Class_am.b instanceof Class_cc) {
            Class_am.c();
        }
        restoreDummyAuto();

        if (Class_fq.o != mapID) {
            Class_ds.c("TREO: Kh\u00f4ng \u0111\u1ebfn \u0111\u01b0\u1ee3c M" + mapID);
            return;
        }

        // Quet khu thu cong K29 -> K0
        for (int zone = MAX_ZONES - 1; zone >= 0 && enabled; zone--) {
            try {
                Class_af.a(zone);
            } catch (Exception e) {}
            sleep(50L);
            for (int w = 0; w < 10 && enabled && Class_fq.l != zone; w++) {
                sleep(100L);
            }

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
            }

            // Check boss — double check
            if (hasBoss()) {
                sleep(300L);
                if (hasBoss()) {
                    Class_ds.c("TREO: Boss t\u1ea1i M" + mapID + " K" + zone + "!");
                    sendParty("pkm -2");
                    sleep(50L);
                    sendParty("pkm " + mapID);
                    sleep(300L);
                    sendParty("pkk " + zone);
                    sleep(3000L);
                    sendParty("pke");

                    // Dung cho cho den khi boss chet hoac tat
                    while (enabled && hasBoss()) {
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return;
                        }
                        restoreDummyAuto();
                        sleep(2000L);
                    }
                    Class_ds.c("TREO: Boss M" + mapID + " K" + zone + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();
                    return;
                }
            }
            restoreDummyAuto();
        }

        Class_ds.c("TREO: Kh\u00f4ng th\u1ea5y boss M" + mapID);
    }

    // === UTILITY ===

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (Exception ignored) {}
    }
}
