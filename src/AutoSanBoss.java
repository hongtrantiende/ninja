import java.util.Calendar;
import java.util.TimeZone;

/**
 * AutoSanBoss - Tu dong san boss theo khung gio
 * Lenh chat: tspkb (bat/tat)
 * 
 * Logic:
 * 1. Kiem tra khung gio spawn cua 4 loai boss (Server, TheGioi, VDMQ, MapNgoai)
 * 2. Khi boss spawn: chuyen map -> quet tat ca khu -> tim boss -> PK boss
 * 3. Danh xong 1 boss: quet lai cac khu khac / map khac
 * 4. Chet: hoi sinh va quay lai map boss
 * 5. Het boss: cho boss loai tiep theo
 */
public class AutoSanBoss implements Runnable {
    public static boolean isRunning = false;
    public static boolean isPartyMode = false;
    public static boolean treoMode = false; // true = tim boss nhung khong danh, chi goi nhom roi dung cho
    public static int forcedBossType = -1; // -1 = auto schedule, 0-3 = force loai boss cu the
    private static Thread thread;
    private static Thread memberMoveThread;
    private static int memberTargetZone = -1;
    private static boolean eventHuntMode;
    private static boolean eventRoundCompleted;
    /** Override thu tu uu tien boss cho TS event (null = dung HUNT_PRIORITY mac dinh) */
    static int[] eventHuntTypes;

    // 3 loai boss (VDMQ, MapNgoai, LangCo)
    public static final int TYPE_VDMQ = 0;
    public static final int TYPE_MAPNGOAI = 1;
    public static final int TYPE_LANGCO = 2;
    public static final int TYPE_ALL = 3;

    private static final String[] BOSS_NAMES = {"VDMQ", "MapNgoai", "L\u00e0ng C\u1ed5", "T\u1ea5t C\u1ea3"};

    /** Thu tu uu tien quet boss: Lang Co > VDMQ > MapNgoai */
    private static final int[] HUNT_PRIORITY = {TYPE_LANGCO, TYPE_VDMQ, TYPE_MAPNGOAI};

    // Map IDs cho moi loai boss
    private static final int[][] BOSS_MAPS = {
        {141, 142, 143},   // VDMQ
        {},                // MapNgoai
        {135, 136}         // Làng Cổ
    };

    // Map IDs cua MapNgoai theo level (12 maps)
    private static final int[][] MAPNGOAI_BY_LEVEL = {
        {14, 15, 16},                  // Lv45: Xích Phiến Thiên Long (ID 115)
        {44, 67, 70},                  // Lv55: Thần Thố (ID 114)
        {24, 41, 45},                  // Lv65: Samurai Chiến Tướng (ID 116)
        {18, 36, 54}                   // Lv75: Hỏa Ngưu Vương (ID 139)
    };
    private static final int[] MAPNGOAI_LEVELS = {45, 55, 65, 75};

    // Khung gio spawn (gio)
    private static final int[][] BOSS_HOURS = {
        {6, 13, 19, 23},                                       // VDMQ
        {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23},            // MapNgoai (gio le)
        {7, 10, 15, 23}                                        // Làng Cổ
    };

    // Dummy Auto giu Code.gameAB != null -> menu hien "Tat Auto"
    private static SanBossHolder dummyAuto;
    private static final int BOSS_ALIVE_DURATION = 2400; // Boss ton tai 40 phut
    private static final int MAX_ZONES = 30;
    private static final int RECONNECT_TIMEOUT = 120; // Cho toi da 2 phut de reconnect

    /** Cleans Khao Di Lenh / Co Lenh (ID 490, 35, 37) from both inventory (arrItemBag) and trunk (arrItemBox) to exit Lang Co cleanly. */
    public static void cleanKhaoDiLenh() {
        Char.MuaCoLenh = false;
        Char.DungCoLenh = false;
        try {
            Char myChar = Char.getMyChar();
            if (myChar != null) {
                // 1. Quet Hanh trang (arrItemBag)
                if (myChar.arrItemBag != null) {
                    for (int i = 0; i < myChar.arrItemBag.length; i++) {
                        Item item = myChar.arrItemBag[i];
                        if (item != null && item.template != null && (item.template.id == 490 || item.template.id == 35 || item.template.id == 37)) {
                            try { Service.gI().gameAE(item.indexUI); } catch (Exception e) {}
                            try { Service.gI().gameAR(item.indexUI); } catch (Exception e) {}
                            myChar.arrItemBag[i] = null;
                        }
                    }
                }
                // 2. Quet Tu do / Ruong (arrItemBox)
                if (myChar.arrItemBox != null) {
                    for (int i = 0; i < myChar.arrItemBox.length; i++) {
                        Item item = myChar.arrItemBox[i];
                        if (item != null && item.template != null && (item.template.id == 490 || item.template.id == 35 || item.template.id == 37)) {
                            try { Service.gI().gameAE(item.indexUI); } catch (Exception e) {}
                            try { Service.gI().gameAR(item.indexUI); } catch (Exception e) {}
                            myChar.arrItemBox[i] = null;
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    /** Finishes Lang Co hunt, sends pkm -6 to party, and uses Khao Di Lenh / Co Lenh or suicides back to village to exit Lang Co immediately. */
    public static void finishLangCoAndExit() {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                Service.gI().gameAK("pkm -6");
            }
        } catch (Exception e) {}

        Char.MuaCoLenh = false;
        Char.DungCoLenh = false;

        // 1. Uu tien su dụng Khả Dị Lệnh / Cổ Lệnh (ID 35, 37, 490) de dich chuyen khoi Lang Co ngay lap tuc
        try {
            Item item = getCoLenhInBag();
            if (item != null) {
                Service.gI().useItem(item.indexUI);
                TileMap.gameAF();
                sleep(1000L);
            }
        } catch (Exception e) {}

        // 2. Neu van con o Lang Co -> Tu sat de ve tone/lang ngoai
        if (TileMap.isLangCo(TileMap.mapID)) {
            try { Code.gameAN(); } catch (Exception e) {}
            sleep(1000L);
            // Hoi sinh sau tu sat
            respawnIfDead();
            sleep(500L);
        }

        // Double check: neu van con o Lang Co -> tu sat lan 2
        if (TileMap.isLangCo(TileMap.mapID)) {
            try { Code.gameAN(); } catch (Exception e) {}
            sleep(1000L);
            // Hoi sinh sau tu sat
            respawnIfDead();
            sleep(500L);
        }
    }

    /** Hoi sinh nhanh neu dang chet - dung cho finishLangCoAndExit */
    private static void respawnIfDead() {
        try {
            if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                for (int retry = 0; retry < 10; retry++) {
                    GameCanvas.endDlg();
                    GameScr.gameAB(5, 0, 0);
                    Service.gI().gameAF();
                    sleep(200L);
                    if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) return;
                }
            }
        } catch (Exception e) {}
    }

    /** Find Co Lenh / Khao Di Lenh item (ID 490, 35, 37) directly in inventory bag. */
    public static Item getCoLenhInBag() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar != null && myChar.arrItemBag != null) {
                for (int i = 0; i < myChar.arrItemBag.length; i++) {
                    Item item = myChar.arrItemBag[i];
                    if (item != null && item.template != null) {
                        int id = item.template.id;
                        if (id == 490 || id == 35 || id == 37) {
                            return item;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    /** Doi khu trong Lang Co bang cach truyen Co Lenh / Khao Di Lenh indexUI qua Service.gameAA(zoneID, indexUI) */
    public static void changeZoneLangCo(int zoneID) {
        try {
            Item item = getCoLenhInBag();
            if (item != null) {
                Service.gI().gameAA(zoneID, item.indexUI);
                TileMap.gameAF();
            } else {
                Auto.gameAA(zoneID);
            }
        } catch (Exception e) {}
    }

    /** Ensures character is inside Lang Co. If currently outside, buys Co Lenh / Khao Di Lenh (ID 490/35/37) and uses it to enter Lang Co. */
    public static boolean ensureInLangCo() {
        Char.MuaCoLenh = true;
        Char.DungCoLenh = true;

        // Dam bao graph map Lang Co duoc thiet lap
        restoreLangCoGraph();

        // Neu da o trong Lang Co -> KHONG mua / KHONG useItem lai!
        if (TileMap.isLangCo(TileMap.mapID)) return true;

        // 1. Tim Co Lenh / Khao Di Lenh (ID 490, 35, 37) bang ca 2 cach
        Item item = findCoLenhItem();

        // 2. Neu chua co -> Mua tu Shop Goshu (14, 29, 2)
        if (item == null) {
            GameScr.gameAC("LC: Mua C\u1ed5 L\u1ec7nh...");
            try {
                GameScr.gameAB(4, 0, 0);
                Service.gI().gameAB(14, 29, 2);
                LockGame.gameAG();
            } catch (Exception e) {}

            // Cho server update inventory (retry tim item toi da 5 lan, 500ms/lan)
            for (int retry = 0; retry < 5; retry++) {
                sleep(500L);
                item = findCoLenhItem();
                if (item != null) break;
            }

            if (item == null) {
                // Thu mua tu Tabemono NPC (9, 6, 1) neu Goshu ko co
                try {
                    GameScr.gameAB(4, 0, 0);
                    Service.gI().gameAB(9, 6, 1);
                    LockGame.gameAG();
                } catch (Exception e) {}
                sleep(1000L);
                item = findCoLenhItem();
            }
        }

        if (item == null) {
            GameScr.gameAC("LC: Kh\u00f4ng t\u00ecm th\u1ea5y C\u1ed5 L\u1ec7nh!");
            return false;
        }

        // 3. Dung Co Lenh de vao Lang Co
        GameScr.gameAC("LC: D\u00f9ng C\u1ed5 L\u1ec7nh v\u00e0o L\u00e0ng C\u1ed5...");
        for (int attempt = 0; attempt < 3; attempt++) {
            if (TileMap.isLangCo(TileMap.mapID)) return true;

            // Dong moi dialog truoc khi dung item
            try { GameCanvas.endDlg(); } catch (Exception e) {}
            sleep(200L);

            // Tim lai item (co the indexUI thay doi sau mua)
            item = findCoLenhItem();
            if (item == null) break;

            try {
                Service.gI().useItem(item.indexUI);
                TileMap.gameAF();
            } catch (Exception e) {}

            // Cho vao Lang Co (toi da 8s moi lan thu)
            for (int i = 0; i < 80; i++) {
                sleep(100L);
                if (TileMap.isLangCo(TileMap.mapID)) {
                    GameScr.gameAC("LC: \u0110\u00e3 v\u00e0o L\u00e0ng C\u1ed5! (M" + TileMap.mapID + ")");
                    return true;
                }
            }
            GameScr.gameAC("LC: Ch\u01b0a v\u00e0o \u0111\u01b0\u1ee3c, th\u1eed l\u1ea1i l\u1ea7n " + (attempt + 2) + "...");
        }

        return TileMap.isLangCo(TileMap.mapID);
    }

    /** Tim Co Lenh / Khao Di Lenh bang ca Char.gameAF (giong game goc) va scan arrItemBag. */
    private static Item findCoLenhItem() {
        Item item = null;
        // Cach 1: Dung Char.gameAF (game engine) - chinh xac nhat
        try {
            item = Char.gameAF(490);
            if (item == null) item = Char.gameAF(37);
            if (item == null) item = Char.gameAF(35);
        } catch (Exception e) {}
        // Cach 2: Fallback scan truc tiep arrItemBag
        if (item == null) {
            item = getCoLenhInBag();
        }
        return item;
    }

    // Trang thai hien tai
    private int currentBossType = -1;
    private int currentMapIndex = 0;
    private int lastDeathMapID = -1;
    private int lastDeathZoneID = -1;

    private static boolean checkHasPartyOrFriends() {
        try {
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) return true;
            if (Code.gameAI != null && Code.gameAI.size() > 0) return true;
            if (GameScr.vFriend != null && GameScr.vFriend.size() > 0) return true;
        } catch (Exception e) {}
        return false;
    }

    /**
     * tspkb - Toggle san boss tu dong theo lich
     */
    public static void toggle() {
        toggleInternal(checkHasPartyOrFriends(), -1);
    }

    /**
     * tspkbsv - San boss Server (M3) ngay lap tuc
     */
    /**
     * tspkbvm - San boss VDMQ (M141-143) ngay lap tuc
     */
    public static void toggleVM() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_VDMQ);
    }

    /**
     * tspkbmn - San boss MapNgoai (12 maps) ngay lap tuc
     */
    public static void toggleMN() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_MAPNGOAI);
    }

    /**
     * tspkblangco / langco - San boss LangCo (M135-136) ngay lap tuc
     */
    public static void toggleLangCo() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_LANGCO);
    }

    /**
     * tspkball - San TAT CA boss 24/24 nguyen ngay
     */
    public static void toggleALL() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_ALL);
    }

    /**
     * tstreo / treo - Tim ALL boss nhung KHONG danh, chi goi nhom roi dung cho.
     */
    public static void toggleTreo() {
        toggleTreoInternal(TYPE_ALL);
    }

    /** treovm - Treo boss VDMQ */
    public static void toggleTreoVM() {
        toggleTreoInternal(TYPE_VDMQ);
    }

    /** treomn - Treo boss MapNgoai */
    public static void toggleTreoMN() {
        toggleTreoInternal(TYPE_MAPNGOAI);
    }

    /** treolangco - Treo boss LangCo */
    public static void toggleTreoLangCo() {
        toggleTreoInternal(TYPE_LANGCO);
    }

    private static void toggleTreoInternal(int bossType) {
        if (isRunning) {
            toggleInternal(true, -1); // tat
        } else {
            treoMode = true;
            toggleInternal(true, bossType);
        }
    }

    public static void toggleParty() {
        toggleInternal(true, -1);
    }

    public static void startEventHunt() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        toggleInternal(true, -1);
    }

    public static void startEventHuntAll() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        toggleInternal(true, TYPE_ALL);
    }

    public static void stopEventHunt() {
        eventHuntMode = false;
        stop();
    }

    /** TS Boss chi san VDMQ + Lang Co */
    public static void startEventHuntVdmqLc() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        // Override HUNT_PRIORITY TRUOC khi start thread (tranh race condition)
        eventHuntTypes = new int[]{TYPE_LANGCO, TYPE_VDMQ};
        toggleInternal(true, TYPE_ALL);
    }

    /** TS Boss chi san MapNgoai */
    public static void startEventHuntMN() {
        if (isRunning) {
            stop();
            sleep(500L);
        }
        eventHuntMode = true;
        eventRoundCompleted = false;
        eventHuntTypes = null;
        toggleInternal(true, TYPE_MAPNGOAI);
    }

    public static boolean consumeEventRoundCompleted() {
        if (!eventRoundCompleted) return false;
        eventRoundCompleted = false;
        return true;
    }

    /** Nhan pke tu truong nhom; pop PkBoss roi tat holder/thread thanh vien. */
    public static void stopPartyBoss() {
        Code.gameAC();
        if (AutoSanBoss.isRunning && AutoSanBoss.treoMode) {
            // Treo mode: chi pop PkBoss, giu thread song de dung cho
            AutoSanBoss.restoreDummyAuto();
        } else {
            AutoSanBoss.stop();
        }
    }

    /** Bat che do thanh vien thuong khi nhan pkm -1. */
    public static void startPartyMember() {
        if (!isRunning) {
            treoMode = false;
            toggleInternal(true, -1);
        }
    }

    /** Ep thanh vien ve che do DANH khi nhan pkm -1. */
    public static void startPartyMemberNormal() {
        treoMode = false;
        if (!isRunning) {
            toggleInternal(true, -1);
        }
    }

    /** Dung han AutoSanBoss thanh vien khi nhan pkm -3. */
    public static void stopPartyMemberFully() {
        if (Code.gameAB instanceof PkBoss) {
            try { Code.gameAC(); } catch (Exception e) {}
        }
        stop();
    }

    /** Bat che do thanh vien TREO khi nhan pkm -2. */
    public static void startPartyMemberTreo() {
        if (!isRunning) {
            treoMode = true;
            toggleInternal(true, -1);
        } else {
            treoMode = true;
        }
    }

    /**
     * Thanh vien treo: PkBoss chi dua toi map. Khi toi map thi pop PkBoss,
     * sau do tu doi khu bang Auto.gameAA de khong tele/khong danh boss.
     */
    public static void setPartyBossZone(final int zone) {
        if (!isRunning || !treoMode) {
            if (Code.gameAB != null) Code.gameAB.zoneID = zone;
            return;
        }

        memberTargetZone = zone;
        final Auto travelAuto = Code.gameAB;
        if (!(travelAuto instanceof PkBoss)) {
            try { Auto.gameAA(zone); } catch (Exception e) {}
            return;
        }
        final int targetMap = travelAuto.mapID;

        memberMoveThread = new Thread(new Runnable() {
            public void run() {
                // PkBoss chi dua nhan vat toi map; poll nhanh de pop truoc khi danh.
                for (int i = 0; i < 3000 && isRunning && treoMode; i++) {
                    if (TileMap.mapID == targetMap) break;
                    sleep(10);
                }

                if (!isRunning || !treoMode || TileMap.mapID != targetMap) return;
                if (Code.gameAB == travelAuto) {
                    try { Code.gameAC(); } catch (Exception e) {}
                }
                restoreDummyAuto();

                int targetZone = memberTargetZone;
                try { Auto.gameAA(targetZone); } catch (Exception e) {}
                for (int i = 0; i < 300 && isRunning && treoMode && TileMap.zoneID != targetZone; i++) {
                    sleep(10);
                }
                GameScr.gameAC("TREO: Da toi M" + targetMap + " K" + targetZone + ", dung cho!");
            }
        });
        memberMoveThread.start();
    }

    /**
     * @param forcedType -1 = auto schedule, 0-3 = force boss type
     */
    private static void toggleInternal(boolean partyMode, int forcedType) {
        if (isRunning) {
            isRunning = false;
            isPartyMode = false;
            treoMode = false;
            forcedBossType = -1;
            if (Code.gameAB == dummyAuto) {
                Code.gameAB = null;
            }
            dummyAuto = null;
            // Gui lenh tat cho nhom
            if (GameScr.vParty != null && GameScr.vParty.size() > 1) {
                try {
                    Service.gI().gameAK("pkm -3");
                    Service.gI().gameAK("pke");
                } catch (Exception e) {}
            }
            GameScr.gameAC("T\u1eaft T\u1ef1 S\u0103n Boss!");
        } else {
            isRunning = true;
            isPartyMode = partyMode;
            forcedBossType = forcedType;
            dummyAuto = new SanBossHolder();
            Code.gameAB = dummyAuto;
            AutoSanBoss instance = new AutoSanBoss();
            thread = new Thread(instance);
            thread.start();
            // Thong bao
            if (treoMode) {
                GameScr.gameAC("B\u1eadt TREO Boss! T\u00ecm boss → g\u1ecdi nh\u00f3m → \u0111\u1ee9ng ch\u1edd!");
            } else {
                String modeName = partyMode ? " NH\u00d3M" : "";
                if (forcedType >= 0) {
                    GameScr.gameAC("S\u0103n " + BOSS_NAMES[forcedType] + modeName + " ngay!");
                } else if (partyMode) {
                    GameScr.gameAC("B\u1eadt S\u0103n Boss NH\u00d3M!");
                } else {
                    GameScr.gameAC("B\u1eadt T\u1ef1 S\u0103n Boss!");
                }
            }
            // Gui pkm va moi ban be vao nhom
            if (partyMode) {
                boolean isLeader = false;
                try {
                    Char myChar = Char.getMyChar();
                    if (GameScr.vParty != null && GameScr.vParty.size() > 0) {
                        Party first = (Party) GameScr.vParty.firstElement();
                        if (first != null && myChar != null && first.charId == myChar.charID) {
                            isLeader = true;
                        }
                    } else {
                        isLeader = true; // Chua co nhom -> Tu moi (se thanh nhom truong)
                    }
                } catch (Exception e) {}
                
                if (isLeader && !eventHuntMode) {
                    // Moi ban be / thanh vien chua co trong nhom (KHONG roi nhom hien tai)
                    autoInviteFriends();
                    
                    // Gui pkm de khoi dong member (du da co nhom hay chua)
                    try { Service.gI().gameAK("pkm " + (treoMode ? -2 : -1)); } catch (Exception e) {}
                }
            }
        }
    }

    public static void stop() {
        if (isRunning) {
            isRunning = false;
            isPartyMode = false;
            treoMode = false;
            forcedBossType = -1;
            eventHuntTypes = null;
            if (Code.gameAB == dummyAuto) {
                Code.gameAB = null;
            }
            dummyAuto = null;
            // Khong can gui pke vi thao tac nay co the la tat ca nhan
        }
    }

    /**
     * Gui lenh party chat neu dang o che do nhom
     */
    private void sendPartyCommand(String cmd) {
        if (!isPartyMode) return;
        if (GameScr.vParty.size() <= 1) return;
        try {
            Service.gI().gameAK(cmd);
        } catch (Exception e) {}
    }

    /**
     * Kiem tra AutoSanBoss con dang chay khong.
     * Detect ca truong hop user nhan "Tat Auto" tu menu (Code.gameAF() set gameAB = null).
     */
    private boolean checkStillRunning() {
        if (!isRunning) return false;
        return true;
    }

    /**
     * Dam bao SanBossHolder luon ton tai de giu menu "Tat Auto".
     * Phuc hoi khi gameAB bi null HOAC bi ghi de boi auto khac (khong phai PkBoss).
     * PkBoss duoc giu nguyen vi no la phan cua flow san boss nhom.
     */
    public static void restoreDummyAuto() {
        if (!isRunning || dummyAuto == null) return;
        Auto current = Code.gameAB;
        if (current == null) {
            // gameAB bi null (bi pe, hoac auto khac tat)
            Code.gameAB = dummyAuto;
        } else if (current != dummyAuto && !(current instanceof PkBoss)) {
            // gameAB bi ghi de boi auto khac (khong phai PkBoss)
            // Giu lai reAB chain: dummyAuto.reAB = current (de pop dung)
            dummyAuto.reAB = current;
            Code.gameAB = dummyAuto;
        }
    }

    /**
     * Kiem tra mat ket noi: Char chua load hoac session chua san sang
     */
    private boolean isDisconnected() {
        try {
            Char c = Char.getMyChar();
            if (c == null || c.cName == null) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Cho game tu reconnect (Char.ReConnect xu ly).
     * Tra ve true neu reconnect thanh cong, false neu timeout.
     */
    private boolean waitForReconnect(int maxWaitSec) {
        GameScr.gameAC("TSB: M\u1ea5t k\u1ebft n\u1ed1i! Ch\u1edd \u0111\u0103ng nh\u1eadp l\u1ea1i...");
        for (int i = 0; i < maxWaitSec && isRunning; i++) {
            sleep(1000);
            if (!isDisconnected()) {
                // Reconnect thanh cong, doi game load xong
                sleep(5000);
                // Khoi phuc dummyAuto de menu hien "Tat Auto"
                if (dummyAuto == null) {
                    dummyAuto = new SanBossHolder();
                }
                Code.gameAB = dummyAuto;
                GameScr.gameAC("TSB: \u0110\u00e3 k\u1ebft n\u1ed1i l\u1ea1i! Ti\u1ebfp t\u1ee5c s\u0103n boss...");
                // Gui lai pkm cho nhom neu dang party mode
                if (isPartyMode) {
                    try {
                        // Doi vParty sync tu server (toi da 3s) truoc khi quyet dinh moi lai
                        for (int ps = 0; ps < 30 && GameScr.vParty.size() <= 1; ps++) {
                            sleep(100);
                        }
                        if (GameScr.vParty.size() > 1) {
                            // Nhom van con -> chi gui pkm de sync member
                            Service.gI().gameAK("pkm " + TileMap.mapID);
                        } else {
                            // Party that su bi giai tan do disconnect -> moi lai
                            autoInviteFriends();
                        }
                    } catch (Exception e) {}
                }
                return true;
            }
        }
        GameScr.gameAC("TSB: Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i l\u1ea1i. D\u1eebng.");
        return false;
    }

    /**
     * Tu dong moi danh sach thanh vien (Code.gameAI) va ban be (GameScr.vFriend) vao nhom.
     * Su dung Service.gI().gameAF(name) (Packet 79 - Party Invite chuan Ninja School).
     */
    private static Thread inviteThread = null;

    public static void autoInviteFriends() {
        // Cancel thread invite cu (neu dang chay) de tranh leak thread
        if (inviteThread != null && inviteThread.isAlive()) {
            inviteThread.interrupt();
            inviteThread = null;
        }
        inviteThread = new Thread(new Runnable() {
            public void run() {
                try {
                    // === Dung DUNG co che cua lenh "pt" trong Code.java ===

                    // Check nhom truong: Code.gameAH = ten nhom truong da luu bang "sn"
                    String myName = Char.getMyChar() != null ? Char.getMyChar().cName : "";
                    if (Code.gameAH != null && Code.gameAH.length() > 0
                            && !myName.equals(Code.gameAH)) {
                        GameScr.gameAC("B\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                        return;
                    }

                    // Check co danh sach thanh vien da luu khong
                    if (Code.gameAI == null || Code.gameAI.size() == 0) {
                        GameScr.gameAC("Ch\u01b0a l\u01b0u nh\u00f3m! D\u00f9ng 'addn' th\u00eam t\u1eebng ng\u01b0\u1eddi ho\u1eb7c 'sn' l\u01b0u nh\u00f3m hi\u1ec7n t\u1ea1i.");
                        return;
                    }

                    int invitedCount = 0;
                    // Duyet gameAI, check Code.gameAD(name) = da trong party chua
                    for (int i = 0; i < Code.gameAI.size(); i++) {
                        String name = (String) Code.gameAI.elementAt(i);
                        if (name == null || name.length() == 0) continue;
                        // Code.gameAD(name) = true neu name DA trong party -> skip
                        if (!Code.gameAD(name)) {
                            Service.gI().gameAF(name);
                            invitedCount++;
                            sleep(300);
                        }
                    }

                    if (invitedCount > 0) {
                        GameScr.gameAC("\u0110\u00e3 m\u1eddi " + invitedCount + " ng\u01b0\u1eddi v\u00e0o nh\u00f3m!");
                    } else {
                        GameScr.gameAC("Nh\u00f3m \u0111\u00e3 \u0111\u1ee7 ng\u01b0\u1eddi!");
                    }
                } catch (Exception e) {}
            }
        });
        inviteThread.start();
    }


    private static boolean isAlreadyInParty(String name) {
        try {
            if (GameScr.vParty != null) {
                for (int p = 0; p < GameScr.vParty.size(); p++) {
                    Party partyMember = (Party) GameScr.vParty.elementAt(p);
                    if (partyMember != null && partyMember.name != null && partyMember.name.equals(name)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Tach do le tu Tu do (arrItemBox) hoac Hanh trang (arrItemBag).
     * Tach thanh tung mon le (so luong 1) cho den 'count' lan.
     */
    public static void tachDoLeById(final int itemId, final int count) {
        if (count <= 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar == null) return;

                    Item item = null;
                    int tUI = 3;
                    int idxUI = -1;

                    // 1. Tim trong Hanh trang
                    if (myChar.arrItemBag != null) {
                        for (int i = 0; i < myChar.arrItemBag.length; i++) {
                            if (myChar.arrItemBag[i] != null && myChar.arrItemBag[i].template != null && myChar.arrItemBag[i].template.id == itemId) {
                                item = myChar.arrItemBag[i];
                                tUI = 3;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    // 2. Tim trong Tu do
                    if (item == null && myChar.arrItemBox != null) {
                        for (int i = 0; i < myChar.arrItemBox.length; i++) {
                            if (myChar.arrItemBox[i] != null && myChar.arrItemBox[i].template != null && myChar.arrItemBox[i].template.id == itemId) {
                                item = myChar.arrItemBox[i];
                                tUI = 4;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    if (item == null || idxUI < 0) {
                        GameScr.gameAC("TSB: Kh\u00f4ng t\u00ecm th\u1ea5y v\u1eadt ph\u1ea9m c\u00f3 ID " + itemId + "!");
                        return;
                    }

                    final int typeUI = tUI;
                    final int indexUI = idxUI;

                    String itemName = item.template.name;
                    GameScr.gameAC("TSB: \u0110ang t\u00e1ch l\u1ebb " + count + " m\u00f3n " + itemName + "...");

                    for (int i = 0; i < count; i++) {
                        Service.gI().gameAA(typeUI, indexUI, 1);
                        Thread.sleep(150);
                    }

                    GameScr.gameAC("TSB: \u0110\u00e3 t\u00e1ch xong " + count + " m\u00f3n l\u1ebb " + itemName + "!");
                } catch (Exception e) {
                    GameScr.gameAC("TSB: L\u1ed7i khi t\u00e1ch \u0111\u1ed3!");
                }
            }
        }).start();
    }

    public static void tachDoLe(final int count) {
        if (count <= 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar == null) return;

                    Item item = null;
                    int tUI = 3;
                    int idxUI = -1;

                    // 1. Uu tien chon tu Hanh trang (Bag = 3)
                    if (myChar.arrItemBag != null && GameScr.gameBM >= 0 && GameScr.gameBM < myChar.arrItemBag.length && myChar.arrItemBag[GameScr.gameBM] != null) {
                        item = myChar.arrItemBag[GameScr.gameBM];
                        tUI = 3;
                        idxUI = item.indexUI;
                    } 
                    // 2. Kiem tra trong Tu do (Box = 4) neu dang chon
                    else if (myChar.arrItemBox != null && GameScr.gameBM >= 0 && GameScr.gameBM < myChar.arrItemBox.length && myChar.arrItemBox[GameScr.gameBM] != null) {
                        item = myChar.arrItemBox[GameScr.gameBM];
                        tUI = 4;
                        idxUI = item.indexUI;
                    }

                    // 3. Fallback: tim item dau tien trong Hanh trang
                    if (item == null && myChar.arrItemBag != null) {
                        for (int i = 0; i < myChar.arrItemBag.length; i++) {
                            if (myChar.arrItemBag[i] != null) {
                                item = myChar.arrItemBag[i];
                                tUI = 3;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    // 4. Fallback: tim item dau tien trong Tu do
                    if (item == null && myChar.arrItemBox != null) {
                        for (int i = 0; i < myChar.arrItemBox.length; i++) {
                            if (myChar.arrItemBox[i] != null) {
                                item = myChar.arrItemBox[i];
                                tUI = 4;
                                idxUI = item.indexUI;
                                break;
                            }
                        }
                    }

                    if (idxUI < 0) {
                        GameScr.gameAC("TSB: H\u00e3y ch\u1ecdn v\u1eadt ph\u1ea9m trong T\u1ee7 \u0111\u1ed3 ho\u1eb7c H\u00e0nh trang \u0111\u1ec3 t\u00e1ch!");
                        return;
                    }

                    final int typeUI = tUI;
                    final int indexUI = idxUI;

                    String itemName = item.template != null ? item.template.name : "v\u1eadt ph\u1ea9m";
                    GameScr.gameAC("TSB: \u0110ang t\u00e1ch l\u1ebb " + count + " m\u00f3n " + itemName + "...");

                    for (int i = 0; i < count; i++) {
                        Service.gI().gameAA(typeUI, indexUI, 1);
                        sleep(150); // Delay 150ms giua cac lan tach 1 mon
                    }

                    GameScr.gameAC("TSB: \u0110\u00e3 t\u00e1ch xong " + count + " m\u00f3n l\u1ebb " + itemName + "!");
                } catch (Exception e) {
                    GameScr.gameAC("TSB: L\u1ed7i khi t\u00e1ch \u0111\u1ed3!");
                }
            }
        }).start();
    }

    /**
     * Hoi sinh nhanh: dong dialog, gui lenh hoi sinh, retry toi da 10 lan
     */
    private void respawnFast() {
        for (int retry = 0; retry < 10 && isRunning; retry++) {
            if (isDisconnected()) return;
            try {
                GameCanvas.endDlg();
                sleep(10);
                GameScr.gameAB(5, 0, 0);
                sleep(10);
                Service.gI().gameAF();
                sleep(50);
                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                    return;
                }
            } catch (Exception e) {
                return;
            }
            sleep(50);
        }
    }


    /**
     * Lay danh sach map ID cho boss MapNgoai dua tren level nhan vat
     */
    private int[] getMapNgoaiMaps() {
        // Boss MapNgoai spawn tren 12 map (da loc)
        return new int[] {
            14, 15, 16,
            44, 67, 70,
            24, 41, 45,
            18, 36, 54
        };
    }

    /**
     * Kiem tra boss co dang trong khung gio spawn khong (40 phut sau gio spawn)
     */
    private boolean isBossActive(int bossType) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);
        int currentSec = h * 3600 + m * 60 + s;

        int[] hours = BOSS_HOURS[bossType];
        for (int i = 0; i < hours.length; i++) {
            int spawnSec = hours[i] * 3600;
            int diff = currentSec - spawnSec;
            if (diff >= 0 && diff < BOSS_ALIVE_DURATION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tim boss type nao dang active, uu tien theo thu tu
     * Tra ve -1 neu khong co boss nao
     */
    private int findActiveBoss() {
        for (int i = 0; i < TYPE_ALL; i++) {
            if (isBossActive(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Lay map IDs cho 1 loai boss
     */
    private int[] getMapsForBoss(int bossType) {
        if (bossType == TYPE_MAPNGOAI) {
            return getMapNgoaiMaps();
        }
        return BOSS_MAPS[bossType];
    }

    /**
     * Kiem tra tren map hien tai co boss khong
     */
    private boolean hasBossOnCurrentMap() {
        try {
            if (GameScr.vMob == null) return false;
            for (int i = 0; i < GameScr.vMob.size(); i++) {
                Mob mob = (Mob)GameScr.vMob.elementAt(i);
                if (mob != null && mob.isBoss && mob.hp > 0 && mob.status != 0) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Ghim boss: set Char.mobFocus = boss mob de nhan vat luon danh boss.
     * Goi lien tuc moi 200ms trong luc PkBoss dang danh.
     */
    private void lockBossFocus() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null || GameScr.vMob == null) return;
            for (int i = 0; i < GameScr.vMob.size(); i++) {
                Mob mob = (Mob)GameScr.vMob.elementAt(i);
                if (mob != null && mob.isBoss && mob.hp > 0 && mob.status != 0) {
                    myChar.mobFocus = mob;
                    return;
                }
            }
        } catch (Exception e) {}
    }

    /**
     * Nhat nhanh tat ca item tren dat sau khi boss chet.
     * Cho 1.5s de do roi het, roi blast pickup 1 lan.
     */
    private void grabAllItems() {
        sleep(1500);
        AutoPickup.grabOnce();
    }

    /**
     * Doi cho den khi chuyen map xong
     */
    private boolean waitForMap(int targetMapID, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (isRunning && TileMap.mapID != targetMapID) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            sleep(200);
        }
        return isRunning && TileMap.mapID == targetMapID;
    }

    /**
     * Doi cho den khi chuyen khu xong
     */
    private boolean waitForZone(int targetZone, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (isRunning && TileMap.zoneID != targetZone) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                return false;
            }
            sleep(50);
        }
        // Doi mob load nhanh
        sleep(100);
        return isRunning;
    }

    /**
     * Navigate den dung map boss Lang Co (135/136).
     * Tu M138, chay ra cong se random vao 134/135/136/137.
     *
     * Dung TileMap.gameAJ(0) thay GoMap — di den waypoint exit 1 lan,
     * KHONG pathfind, KHONG chay nguoc. GoMap chay thread BFS rieng nen
     * khi vao sai map no tu dong neck quay lai (bug 2 lan neck).
     * gameAJ chi di den cong + Service.gameAC() roi dung.
     */
    private boolean navigateToLangCoMap(int targetMap) {
        for (int retry = 0; retry < 10 && checkStillRunning(); retry++) {
            if (TileMap.mapID == targetMap) return true;

            // Ensure in Lang Co
            if (!TileMap.isLangCo(TileMap.mapID)) {
                ensureInLangCo();
                if (!TileMap.isLangCo(TileMap.mapID)) return false;
                if (TileMap.mapID == targetMap) return true;
            }

            // Phai o M138 truoc
            if (TileMap.mapID != 138) {
                // Tu map sai -> di ra cong de ve M138 (1 neck)
                try {
                    TileMap.gameAJ(0);
                    TileMap.gameAF();
                } catch (Exception e) {}
                for (int w = 0; w < 100 && checkStillRunning() && TileMap.mapID != 138; w++) {
                    sleep(100);
                }
                if (TileMap.mapID != 138) continue;
                sleep(500);
            }

            // Tu M138: di den cong exit (gameAJ = 1 neck duy nhat)
            GameScr.gameAC("TSB: Neck M" + targetMap + " (l\u1ea7n " + (retry + 1) + ")");
            try {
                TileMap.gameAJ(0);
                TileMap.gameAF();
            } catch (Exception e) {}

            // Cho vao phong random (toi da 10s)
            for (int w = 0; w < 100 && checkStillRunning() && TileMap.mapID == 138; w++) {
                sleep(100);
            }

            // Cho map load 1s
            sleep(1000);

            // Check ngay trong phong
            if (TileMap.mapID == targetMap) {
                GameScr.gameAC("TSB: \u0110\u00e3 v\u00e0o M" + targetMap + "!");
                return true;
            }

            if (TileMap.mapID == 138) {
                GameScr.gameAC("TSB: Ch\u01b0a ra \u0111\u01b0\u1ee3c c\u1ed5ng, th\u1eed l\u1ea1i...");
            } else {
                GameScr.gameAC("TSB: V\u00e0o nh\u1ea7m M" + TileMap.mapID + ", quay v\u1ec1...");
            }
        }
        return TileMap.mapID == targetMap;
    }


    /** Restore duong nguoc 134..137 -> 138 de GoMap(138) va zone change hoat dong. */
    public static void restoreLangCoGraph() {
        try {
            if (TileMap.gameBZ != null) {
                // M138 exit ra 4 map
                if (TileMap.gameBZ.length > 138) {
                    TileMap.gameBZ[138] = new short[] {134, 135, 136, 137};
                }
                // 4 map quay ve M138
                for (int m = 134; m <= 137 && m < TileMap.gameBZ.length; m++) {
                    TileMap.gameBZ[m] = new short[] {138};
                }
            }
        } catch (Exception e) {}
    }

    /**
     * TREO MODE: Quet khu tuan tu K0->K29 tren 1 map.
     * Dung Auto.gameAA(zone) de doi khu (giong PkBoss bytecode).
     * KHONG danh boss, chi tim va dung cho.
     */
    private boolean treoScanMap(int mapID) {
        if (!checkStillRunning()) return false;

        // === XU LY LANG CO RIENG ===
        if (mapID == 135 || mapID == 136) {
            ensureInLangCo();
            // Dung navigateToLangCoMap thay vi PkBoss
            if (!navigateToLangCoMap(mapID)) {
                GameScr.gameAC("TREO: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M" + mapID);
                return false;
            }
            GameScr.gameAC("TREO: \u0110\u00e3 v\u00e0o M" + mapID + ", qu\u00e9t 3 khu...");

            // Tat PkBoss neu con
            if (Code.gameAB instanceof PkBoss) {
                Code.gameAC();
            }
            restoreDummyAuto();

            // Quet NHANH 3 khu K0->K2 (dung Auto.gameAA nhu PkBoss)
            for (int zone = 0; zone < 3 && checkStillRunning(); zone++) {
                // Chuyen khu nhanh — giong PkBoss engine
                try { Auto.gameAA(zone); } catch (Exception e) {}
                sleep(300); // cho zone load (PkBoss dung ~0ms, ta cho 300ms de an toan)

                // Kiem tra con dung map khong (tranh bi thoat map)
                if (TileMap.mapID != mapID) {
                    GameScr.gameAC("TREO: B\u1ecb tho\u00e1t M" + mapID + " -> M" + TileMap.mapID);
                    return false;
                }

                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                }

                // Check boss
                if (hasBossOnCurrentMap()) {
                    sleep(150);
                    if (hasBossOnCurrentMap()) {
                        GameScr.gameAC("TREO: Boss t\u1ea1i M" + mapID + " K" + TileMap.zoneID + "!");
                        sendPartyCommand("pkm -2");
                        sleep(50);
                        sendPartyCommand("pkm " + mapID);
                        sleep(300);
                        sendPartyCommand("pkk " + TileMap.zoneID);
                        sleep(1500);
                        sendPartyCommand("pke");

                        while (checkStillRunning() && hasBossOnCurrentMap()) {
                            if (isDisconnected()) {
                                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                            }
                            restoreDummyAuto();
                            sleep(500);
                        }
                        GameScr.gameAC("TREO: Boss M" + mapID + " K" + TileMap.zoneID + " \u0111\u00e3 ch\u1ebft!");
                        grabAllItems();
                        return true;
                    }
                }
                restoreDummyAuto();
            }
            GameScr.gameAC("TREO: M" + mapID + " h\u1ebft boss");
            return false;
        }

        // === MAP THUONG (khong phai Lang Co) ===
        if (TileMap.isLangCo(TileMap.mapID)) {
            finishLangCoAndExit();
        }
        GameScr.gameAC("TREO: Qu\u00e9t M" + mapID + "...");

        // 1. Dung PkBoss CHI DE di chuyen den map
        try {
            Code.gameAA(new PkBoss(mapID));
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                Code.gameAA(new PkBoss(mapID));
            } else {
                return false;
            }
        }

        // Doi den khi den map (check 50ms, toi da 30s)
        for (int w = 0; w < 600 && checkStillRunning(); w++) {
            sleep(50);
            if (TileMap.mapID == mapID) break;
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                Code.gameAA(new PkBoss(mapID));
            }
        }

        // 2. Tat PkBoss NGAY khi den map (trong 50ms) — KHONG cho quet/danh
        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        if (TileMap.mapID != mapID) {
            GameScr.gameAC("TREO: Kh\u00f4ng \u0111\u1ebfn \u0111\u01b0\u1ee3c M" + mapID);
            return false;
        }

        // 3. Quet khu tuan tu K0 -> K29
        for (int zone = 0; zone < MAX_ZONES && checkStillRunning(); zone++) {
            try { Auto.gameAA(zone); } catch (Exception e) {}
            sleep(100);
            for (int w = 0; w < 15 && checkStillRunning() && TileMap.zoneID != zone; w++) {
                sleep(200);
            }

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            // Check boss — double check de tranh ao
            if (hasBossOnCurrentMap()) {
                sleep(150);
                if (hasBossOnCurrentMap()) {
                    GameScr.gameAC("TREO: Boss t\u1ea1i M" + mapID + " K" + zone + "!");
                    sendPartyCommand("pkm -2");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(300);
                    sendPartyCommand("pkk " + zone);
                    sleep(1500);
                    sendPartyCommand("pke");

                    while (checkStillRunning() && hasBossOnCurrentMap()) {
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        restoreDummyAuto();
                        sleep(500);
                    }
                    GameScr.gameAC("TREO: Boss M" + mapID + " K" + zone + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();
                    return true;
                }
            }
            restoreDummyAuto();
        }

        GameScr.gameAC("TREO: Kh\u00f4ng th\u1ea5y boss M" + mapID);
        return false;
    }

    /**
     * Chuyen biet cho Lang Co (M135 & M136):
     * Dung navigateToLangCoMap() de vao dung map (retry khi random sai map).
     * Sau do quet chinh xac 3 khu K0, K1, K2.
     */
    private boolean pkLangCoMap(int mapID) {
        if (!checkStillRunning()) return false;
        ensureInLangCo();
        GameScr.gameAC("TSB: Qu\u00e9t L\u00e0ng C\u1ed5 M" + mapID + "...");

        // Tat PkBoss ngam truoc khi di chuyen
        if (Code.gameAB instanceof PkBoss) {
            Code.gameAC();
        }
        restoreDummyAuto();

        // 1. Navigate den dung map boss (retry khi random sai map)
        if (!navigateToLangCoMap(mapID)) {
            GameScr.gameAC("TSB: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M" + mapID + " sau nhi\u1ec1u l\u1ea7n th\u1eed");
            return false;
        }

        GameScr.gameAC("TSB: \u0110\u00e3 v\u00e0o M" + mapID + ", qu\u00e9t 3 khu...");

        // 2. Quet NHANH 3 khu K0->K2 (dung Auto.gameAA nhu PkBoss engine)
        for (int zone = 0; zone < 3 && checkStillRunning(); zone++) {
            // Chuyen khu nhanh — giong PkBoss engine
            try { Auto.gameAA(zone); } catch (Exception e) {}
            sleep(300); // cho zone load

            // Kiem tra con dung map khong (tranh bi thoat map)
            if (TileMap.mapID != mapID) {
                GameScr.gameAC("TSB: B\u1ecb tho\u00e1t M" + mapID + " -> M" + TileMap.mapID);
                return false;
            }

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            // Check Boss
            if (hasBossOnCurrentMap()) {
                sleep(150);
                if (hasBossOnCurrentMap()) {
                    GameScr.gameAC("TSB: Boss L\u00e0ng C\u1ed5 M" + mapID + " K" + TileMap.zoneID + "!");
                    // Goi nhom sang map & zone (giong flow boss thuong)
                    sendPartyCommand("pkm -2");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(300);
                    sendPartyCommand("pkk " + TileMap.zoneID);
                    sleep(1500); // Cho ae vao map + den khu
                    sendPartyCommand("pke");

                    // Bat PkBoss solo tren khu nay de danh boss
                    try {
                        PkBoss pk = new PkBoss(mapID);
                        pk.zoneID = TileMap.zoneID;
                        Code.gameAA(pk);
                    } catch (Exception e) {}

                    // Doi danh xong boss
                    while (checkStillRunning() && hasBossOnCurrentMap()) {
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        lockBossFocus();
                        sleep(500); // poll nhanh boss chet
                    }

                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    restoreDummyAuto();

                    GameScr.gameAC("TSB: Boss M" + mapID + " K" + TileMap.zoneID + " \u0111\u00e3 ch\u1ebft!");
                    grabAllItems();

                    // Giai tan nhom: tat Co Lenh cho ae + quay ve trang thai binh thuong
                    sendPartyCommand("pkm -6");

                    return true;
                }
            }
            restoreDummyAuto();
        }

        GameScr.gameAC("TSB: M" + mapID + " h\u1ebft boss");
        return false;
    }

    /**
     * Bat PkBoss tren 1 map va doi cho den khi xong.
     * PkBoss TU DONG: quet khu, tim boss, danh boss, gui lenh nhom.
     * Return true neu PkBoss da chay du lau (co danh boss).
     */
    private boolean pkBossOnMap(int mapID) {
        if (!checkStillRunning()) return false;
        if (mapID == 135 || mapID == 136) {
            return pkLangCoMap(mapID);
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
        }
        GameScr.gameAC("TSB: PK M" + mapID);

        // Leader start PkBoss solo - quet khu, tim boss
        try {
            Code.gameAA(new PkBoss(mapID));
        } catch (Exception e) {
            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                Code.gameAA(new PkBoss(mapID));
            } else {
                return false;
            }
        }

        long startTime = System.currentTimeMillis();
        boolean sentPartyCmd = false;
        boolean bossKilled = false;

        while (checkStillRunning() && Code.gameAB instanceof PkBoss) {
            try {
                // Detect disconnect trong khi PkBoss dang chay
                if (isDisconnected()) {
                    GameScr.gameAC("TSB: M\u1ea5t k\u1ebft n\u1ed1i khi PK M" + mapID + "!");
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    startTime = System.currentTimeMillis();
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }

                // Chi khi tim thay boss -> gui lenh nhom 1 lan duy nhat (mode binh thuong)
                if (!sentPartyCmd && hasBossOnCurrentMap()) {
                    sentPartyCmd = true;
                    GameScr.gameAC("TSB: Boss! Goi nhom M" + mapID + " K" + TileMap.zoneID);
                    // Ep member ve mode DANH, tranh giu treoMode tu phien truoc.
                    sendPartyCommand("pkm -1");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(500);
                    sendPartyCommand("pkk " + TileMap.zoneID);
                }

                // GHIM BOSS: lien tuc set mobFocus = boss de khong chuyen target
                if (sentPartyCmd) {
                    lockBossFocus();
                }

                // Boss DA CHET: force stop PkBoss ngay, khong cho quet khu thua
                if (sentPartyCmd && !hasBossOnCurrentMap()) {
                    bossKilled = true;
                    if (Code.gameAB instanceof PkBoss) {
                        Code.gameAC();
                    }
                    break;
                }

                // Respawn nhanh neu chet
                if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                    respawnFast();
                    if (isDisconnected()) {
                        if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    }
                    // Restart PkBoss ngay de quay lai boss map
                    if (isRunning && !(Code.gameAB instanceof PkBoss)) {
                        Code.gameAA(new PkBoss(mapID));
                    }
                }
            } catch (Exception e) {
                // Exception co the do disconnect
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                    sentPartyCmd = false;
                    startTime = System.currentTimeMillis();
                    Code.gameAA(new PkBoss(mapID));
                    continue;
                }
            }
            sleep(200);
        }

        // PkBoss xong, khoi phuc dummy
        restoreDummyAuto();

        if (bossKilled) {
            long elapsed = System.currentTimeMillis() - startTime;
            GameScr.gameAC("TSB: Xong M" + mapID + " (" + (elapsed / 1000) + "s)");
            grabAllItems();
            return true;
        }
        return false;
    }

    private void sleepSeconds(int seconds) {
        for (int w = 0; w < seconds && checkStillRunning(); w++) {
            sleep(1000);
            restoreDummyAuto();

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) {
                    isRunning = false;
                }
                break;
            }
        }
    }

    public void run() {
        sleep(2000);

        while (checkStillRunning()) {
            try {
                // Kiem tra disconnect dau moi vong lap
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                }

                restoreDummyAuto();

                // Kiem tra xem co phai la thanh vien nhom (khong phai truong nhom) hay khong
                boolean isMember = false;
                try {
                    Char myChar = Char.getMyChar();
                    if (myChar != null && GameScr.vParty.size() > 1) {
                        Party first = (Party) GameScr.vParty.firstElement();
                        if (first != null && first.charId != myChar.charID) {
                            isMember = true;
                        }
                    }
                } catch (Exception e) {}

                if (isMember) {
                    // === TREO MODE cho thanh vien ===
                    // Leader gui pkm -> pkk -> doi 3s -> pke
                    // stopPartyBoss() se pop PkBoss nhung giu treo thread song
                    // -> TV tu dong dung tai map/zone boss
                    if (treoMode && Code.gameAB instanceof PkBoss) {
                        GameScr.gameAC("TREO: \u0110ang di t\u1edbi map/khu boss...");
                    }
                    // Thanh vien khong tu quet map, chi giu menu doi lenh
                    sleepSeconds(2);
                    continue;
                }

                if (forcedBossType == TYPE_ALL) {
                    // === CHE DO FORCE ALL: San theo danh sach uu tien ===
                    int[] types = (eventHuntTypes != null) ? eventHuntTypes : HUNT_PRIORITY;
                    boolean huntedAnyAll = false;

                    if (eventHuntTypes != null) {
                        // Co danh sach uu tien cu the (VD: VDMQ+LangCo)
                        // CHI quet loai boss nao DANG DEN GIO spawn
                        for (int i = 0; i < types.length && checkStillRunning(); i++) {
                            if (isBossActive(types[i])) {
                                huntedAnyAll = true;
                                huntBossType(types[i]);
                            }
                        }
                    } else {
                        // Khong co override -> quet tat ca nhu cu (khong check gio)
                        for (int i = 0; i < types.length && checkStillRunning(); i++) {
                            huntBossType(types[i]);
                        }
                        huntedAnyAll = true;
                    }

                    if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;

                    if (huntedAnyAll && checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong 1 l\u01b0\u1ee3t, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    } else if (!huntedAnyAll && checkStillRunning()) {
                        // Chua den gio boss nao trong danh sach uu tien -> doi 30s
                        GameScr.gameAC("TSB: Ch\u01b0a \u0111\u1ebfn gi\u1edd boss, \u0111\u1ee3i 30s...");
                        sleepSeconds(30);
                    }
                } else if (forcedBossType >= 0) {
                    // === CHE DO FORCE 1 LOAI: San 1 loai boss cu the, khong check gio ===
                    huntBossType(forcedBossType);

                    // Sau khi quet xong 1 round, doi 10s roi quet lai
                    if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;
                    if (checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong " + BOSS_NAMES[forcedBossType] + ", qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    }
                } else {
                    // === CHE DO TU DONG: Quet theo lich spawn ===
                    boolean huntedAny = false;

                    for (int i = 0; i < HUNT_PRIORITY.length && checkStillRunning(); i++) {
                        int bossType = HUNT_PRIORITY[i];
                        if (!isBossActive(bossType)) continue;

                        huntedAny = true;
                        huntBossType(bossType);
                    }

                    if (eventHuntMode && checkStillRunning()) eventRoundCompleted = true;

                    if (huntedAny && checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong 1 l\u01b0\u1ee3t, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    } else if (!huntedAny && checkStillRunning()) {
                        // Khong co boss nao -> doi 30s
                        sleepSeconds(30);
                    }
                }

            } catch (Exception e) {
                // Exception co the do disconnect
                if (isDisconnected()) {
                    if (!waitForReconnect(RECONNECT_TIMEOUT)) break;
                } else {
                    sleep(5000);
                }
            }
        }

        // Cleanup
        if (dummyAuto != null && Code.gameAB == dummyAuto) {
            Code.gameAB = null;
        }
        dummyAuto = null;
        forcedBossType = -1;
        eventHuntTypes = null;
        isRunning = false;
        GameScr.gameAC("TSB: Da dung.");
    }

    /**
     * San 1 loai boss cu the: quet tung map trong danh sach
     */
    private void huntBossType(int bossType) {
        currentBossType = bossType;
        if (bossType == TYPE_LANGCO) {
            Char.MuaCoLenh = true;
            Char.DungCoLenh = true;
        } else {
            if (TileMap.isLangCo(TileMap.mapID)) {
                finishLangCoAndExit();
            }
        }
        int[] maps = getMapsForBoss(bossType);
        String prefix = treoMode ? "TREO" : "TSB";
        GameScr.gameAC(prefix + ": San " + BOSS_NAMES[bossType] + " (" + maps.length + " maps)");

        for (int mi = 0; mi < maps.length && checkStillRunning(); mi++) {
            // Neu mode tu dong (forcedBossType == -1), check gio
            if (!eventHuntMode && forcedBossType < 0 && !isBossActive(bossType)) break;
            if (treoMode) {
                treoScanMap(maps[mi]);
            } else {
                pkBossOnMap(maps[mi]);
            }
        }

        if (bossType == TYPE_LANGCO) {
            finishLangCoAndExit();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
