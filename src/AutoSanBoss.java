import java.util.Calendar;

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

    // 4 loai boss theo lich server
    // Moi boss co: ten, int[] mapIDs, int[] hours
    private static final int TYPE_SERVER = 0;
    private static final int TYPE_THEGIOI = 1;
    private static final int TYPE_VDMQ = 2;
    private static final int TYPE_MAPNGOAI = 3;
    private static final int TYPE_ALL = 4;

    private static final String[] BOSS_NAMES = {"Server", "TheGioi", "VDMQ", "MapNgoai", "T\u1ea5t C\u1ea3"};

    // Map IDs cho moi loai boss
    private static final int[][] BOSS_MAPS = {
        {63},              // Server
        {65},              // TheGioi
        {141, 142, 143},   // VDMQ
        {}                 // MapNgoai
    };

    // Map IDs cua MapNgoai theo level
    private static final int[][] MAPNGOAI_BY_LEVEL = {
        {14, 15, 16},      // Lv45
        {44, 67, 70},      // Lv55
        {21, 41, 45},      // Lv65
        {18, 46, 54}       // Lv75
    };
    private static final int[] MAPNGOAI_LEVELS = {45, 55, 65, 75};

    // Khung gio spawn (gio)
    private static final int[][] BOSS_HOURS = {
        {10, 15, 20},                            // Server (moi)
        {5, 10, 15},                             // TheGioi (moi)
        {6, 13, 19, 23},                         // VDMQ
        {1, 4, 7, 10, 13, 16, 19, 22}            // MapNgoai
    };

    // Dummy Auto giu Code.gameAB != null -> menu hien "Tat Auto"
    private static SanBossHolder dummyAuto;
    private static final int BOSS_ALIVE_DURATION = 2400; // Boss ton tai 40 phut
    private static final int MAX_ZONES = 30;
    private static final int RECONNECT_TIMEOUT = 120; // Cho toi da 2 phut de reconnect

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
    public static void toggleSV() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_SERVER);
    }

    /**
     * tspkbtg - San boss TheGioi (M23) ngay lap tuc
     */
    public static void toggleTG() {
        toggleInternal(checkHasPartyOrFriends(), TYPE_THEGIOI);
    }

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
     * tspkball - San TAT CA boss (17 maps) 24/24 nguyen ngay
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

    /** treosv - Treo boss Server */
    public static void toggleTreoSV() {
        toggleTreoInternal(TYPE_SERVER);
    }

    /** treotg - Treo boss TheGioi */
    public static void toggleTreoTG() {
        toggleTreoInternal(TYPE_THEGIOI);
    }

    /** treovm - Treo boss VDMQ */
    public static void toggleTreoVM() {
        toggleTreoInternal(TYPE_VDMQ);
    }

    /** treomn - Treo boss MapNgoai */
    public static void toggleTreoMN() {
        toggleTreoInternal(TYPE_MAPNGOAI);
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
                    if (GameScr.vParty.size() > 0) {
                        Party first = (Party) GameScr.vParty.firstElement();
                        if (first != null && myChar != null && first.charId == myChar.charID) {
                            isLeader = true;
                        }
                    } else {
                        isLeader = true; // Chua co nhom -> Tu moi
                    }
                } catch (Exception e) {}
                
                if (isLeader) {
                    autoInviteFriends();
                    // Tin hieu khoi dong holder tren thanh vien, khong chuyen map.
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
                        if (GameScr.vParty.size() > 1) {
                            Service.gI().gameAK("pkm " + TileMap.mapID);
                        } else {
                            // Party bi gia'i ta'n do disconnect -> tu dong moi lai ban be trong vFriend
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
    public static void autoInviteFriends() {
        try {
            int invitedCount = 0;
            String myName = Char.getMyChar() != null ? Char.getMyChar().cName : "";

            // 1. Moi danh sach thanh vien nhom da luu (Code.gameAI)
            if (Code.gameAI != null && Code.gameAI.size() > 0) {
                for (int i = 0; i < Code.gameAI.size(); i++) {
                    String name = (String) Code.gameAI.elementAt(i);
                    if (name != null && name.length() > 0 && !name.equals(myName) && !isAlreadyInParty(name)) {
                        Service.gI().gameAF(name); // Packet 79 - Party Invite
                        invitedCount++;
                        sleep(250);
                    }
                }
            }

            // 2. Moi danh sach ban be trong GameScr.vFriend
            if (GameScr.vFriend != null && GameScr.vFriend.size() > 0) {
                for (int i = 0; i < GameScr.vFriend.size(); i++) {
                    try {
                        Friend f = (Friend) GameScr.vFriend.elementAt(i);
                        if (f != null && f.friendName != null && f.friendName.length() > 0
                                && !f.friendName.equals(myName) && !isAlreadyInParty(f.friendName)) {
                            Service.gI().gameAF(f.friendName); // Packet 79 - Party Invite
                            invitedCount++;
                            sleep(250);
                        }
                    } catch (Exception ex) {}
                }
            }

            if (invitedCount > 0) {
                GameScr.gameAC("TSB: \u0110\u00e3 m\u1eddi " + invitedCount + " ng\u01b0\u1eddi v\u00e0o nh\u00f3m!");
            }
        } catch (Exception e) {}
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
        // Boss MapNgoai spawn tren TAT CA 12 map, khong phan biet level
        return new int[] {14, 15, 16, 44, 67, 70, 21, 41, 45, 18, 46, 54};
    }

    /**
     * Kiem tra boss co dang trong khung gio spawn khong (40 phut sau gio spawn)
     */
    private boolean isBossActive(int bossType) {
        Calendar cal = Calendar.getInstance();
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
     * Dung AutoPickup vacuum mode (blast + tele).
     */
    private void grabAllItems() {
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
     * TREO MODE: Quet khu tuan tu K0->K29 tren 1 map.
     * Dung Auto.gameAA(zone) de doi khu (giong PkBoss bytecode).
     * KHONG danh boss, chi tim va dung cho.
     */
    private boolean treoScanMap(int mapID) {
        if (!checkStillRunning()) return false;
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
            // Doi khu bang Auto.gameAA(zone) — cung API PkBoss dung
            try {
                Auto.gameAA(zone);
            } catch (Exception e) {}
            // Doi zone load (100ms)
            sleep(100);
            // Doi zone chuyen xong (toi da 3s)
            for (int w = 0; w < 15 && checkStillRunning() && TileMap.zoneID != zone; w++) {
                sleep(200);
            }

            if (isDisconnected()) {
                if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
            }

            // Check boss — double check de tranh ao
            if (hasBossOnCurrentMap()) {
                sleep(300); // doi data on dinh
                if (hasBossOnCurrentMap()) {
                    // BOSS XAC NHAN!
                    GameScr.gameAC("TREO: Boss t\u1ea1i M" + mapID + " K" + zone + "!");
                    // Nhac lai mode TREO roi moi goi member den map.
                    sendPartyCommand("pkm -2");
                    sleep(50);
                    sendPartyCommand("pkm " + mapID);
                    sleep(300);
                    // Gui pkk de member chuyen zone
                    sendPartyCommand("pkk " + zone);
                    // Doi member den noi (3s)
                    sleep(3000);
                    // Gui pke de DUNG member — khong danh boss
                    sendPartyCommand("pke");

                    // Dung cho cho den khi tat hoac boss chet
                    while (checkStillRunning() && hasBossOnCurrentMap()) {
                        if (isDisconnected()) {
                            if (!waitForReconnect(RECONNECT_TIMEOUT)) return false;
                        }
                        restoreDummyAuto();
                        sleep(2000);
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
     * Bat PkBoss tren 1 map va doi cho den khi xong.
     * PkBoss TU DONG: quet khu, tim boss, danh boss, gui lenh nhom.
     * Return true neu PkBoss da chay du lau (co danh boss).
     */
    private boolean pkBossOnMap(int mapID) {
        if (!checkStillRunning()) return false;
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

        long elapsed = System.currentTimeMillis() - startTime;
        boolean fought = elapsed > 5000;
        if (fought) {
            GameScr.gameAC("TSB: Xong M" + mapID + " (" + (elapsed / 1000) + "s)");
            // Boss da chet -> nhat nhanh do roi
            grabAllItems();
        }
        return fought;
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
                    // === CHE DO FORCE ALL: San TAT CA 4 loai boss 24/24 ===
                    for (int b = 0; b < TYPE_ALL && checkStillRunning(); b++) {
                        huntBossType(b);
                    }

                    if (checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong 1 l\u01b0\u1ee3t ALL map, qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    }
                } else if (forcedBossType >= 0) {
                    // === CHE DO FORCE 1 LOAI: San 1 loai boss cu the, khong check gio ===
                    huntBossType(forcedBossType);

                    // Sau khi quet xong 1 round, doi 10s roi quet lai
                    if (checkStillRunning()) {
                        GameScr.gameAC("TSB: Xong " + BOSS_NAMES[forcedBossType] + ", qu\u00e9t l\u1ea1i sau 10s...");
                        sleepSeconds(10);
                    }
                } else {
                    // === CHE DO TU DONG: Quet theo lich spawn ===
                    boolean huntedAny = false;

                    for (int bossType = 0; bossType < TYPE_ALL && checkStillRunning(); bossType++) {
                        if (!isBossActive(bossType)) continue;

                        huntedAny = true;
                        huntBossType(bossType);
                    }

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
        isRunning = false;
        GameScr.gameAC("TSB: Da dung.");
    }

    /**
     * San 1 loai boss cu the: quet tung map trong danh sach
     */
    private void huntBossType(int bossType) {
        currentBossType = bossType;
        int[] maps = getMapsForBoss(bossType);
        String prefix = treoMode ? "TREO" : "TSB";
        GameScr.gameAC(prefix + ": San " + BOSS_NAMES[bossType] + " (" + maps.length + " maps)");

        for (int mi = 0; mi < maps.length && checkStillRunning(); mi++) {
            // Neu mode tu dong (forcedBossType == -1), check gio
            if (forcedBossType < 0 && !isBossActive(bossType)) break;
            if (treoMode) {
                treoScanMap(maps[mi]);
            } else {
                pkBossOnMap(maps[mi]);
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
