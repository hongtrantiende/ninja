/**
 * AutoBanVP — Tu dong tu sat ve lang ban vat pham cho NPC 46 khi dat nguong so luong:
 * 1. Chuyen tinh thach (CTT - ID 454) -> NPC 46, Menu 1 = o 5, Menu 2 = o 0
 * 2. Tu tinh thach cao (TTC - ID 457) -> NPC 46, Menu 1 = o 5, Menu 2 = o 1
 * 3. Tu tinh thach trung (TTT - ID 456) -> NPC 46, Menu 1 = o 5, Menu 2 = o 2
 * 4. Tu tinh thach so (TTS - ID 455) -> NPC 46, Menu 1 = o 5, Menu 2 = o 3
 * 5. Phan than lenh (PTL - ID 545) -> NPC 46, Menu 1 = o 5, Menu 2 = o 4
 *
 * Che do ban: Ban tat ca hoac Ban le 1 cai de test.
 * Khi ban xong: Khoi phuc lai Tan Sat map goc va ve toa do cu de farm tiep;
 * neu o Map VIP thi khoi phuc Map VIP de bot tu vao lai farm tiep.
 */
public final class AutoBanVP implements Runnable {
    // Che do ban
    public static final int MODE_ALL = 0; // Ban tat ca
    public static final int MODE_ONE = 1; // Ban le 1 cai de test

    // Config flags
    public static boolean isEnabled = false;
    public static int sellMode = MODE_ALL;
    public static int threshold = 10;
    public static boolean[] sellItems = new boolean[]{true, true, true, true, true};

    // State flags
    public static boolean isSelling = false;
    public static boolean isRunning = false;
    private static Thread workerThread = null;

    // Saved state for return
    public static int savedMap = -1;
    public static int savedZone = -1;
    public static int savedX = -1;
    public static int savedY = -1;
    public static Auto savedAuto = null;
    public static int savedZoneIndex = 0;
    public static boolean wasVipMap = false;

    // Item definitions (theo dung thu tu menu o 2 cua NPC 46)
    public static final int[] ITEM_IDS = {454, 457, 456, 455, 545};
    public static final String[] ITEM_CODES = {"CTT", "TTC", "TTT", "TTS", "PTL"};
    public static final String[] ITEM_NAMES = {
        "Chuy\u1ec3n tinh th\u1ea1ch",
        "T\u1eed tinh th\u1ea1ch cao",
        "T\u1eed tinh th\u1ea1ch trung",
        "T\u1eed tinh th\u1ea1ch s\u01a1",
        "Ph\u00e2n th\u00e2n l\u1ec7nh"
    };

    static {
        loadConfigFromRMS();
    }

    public AutoBanVP() {}

    /** Bat/Tat tu dong ban VP */
    public static void toggle() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            start();
            GameScr.gameAC("T\u1ef1 B\u00e1n VP: ON (SL>=" + threshold + ", " + (sellMode == MODE_ONE ? "B\u00e1n 1 c\u00e1i" : "B\u00e1n t\u1ea5t") + ")");
        } else {
            stop();
            GameScr.gameAC("T\u1ef1 B\u00e1n VP: OFF");
        }
        saveConfigToRMS();
    }

    public static void start() {
        if (isRunning || !isEnabled) return;
        isRunning = true;
        workerThread = new Thread(new AutoBanVP());
        workerThread.start();
    }

    public static void stop() {
        isRunning = false;
        workerThread = null;
    }

    /** Kich hoat ban ngay (cho chuc nang Test) */
    public static void startSellNow() {
        new Thread(new Runnable() {
            public void run() {
                doSellRoutine(true);
            }
        }).start();
    }

    public void run() {
        try { Thread.sleep(3000); } catch (Exception e) {}

        while (isRunning && isEnabled) {
            try {
                Thread.sleep(2000);
                checkAndTriggerSell();
            } catch (Exception e) {
                try { Thread.sleep(2000); } catch (Exception ex) {}
            }
        }
        isRunning = false;
        workerThread = null;
    }

    /** Kiem tra dieu kien kich hoat tu dong ve lang ban */
    private static void checkAndTriggerSell() {
        if (!isEnabled || isSelling) return;

        // Khong ban khi dang san boss de tranh pha hunt boss
        if (AutoSanBoss.isRunning || AutoBossEvent.inEvent || Code.gameAB instanceof PkBoss) return;

        // Nhan vat phai con song va ket noi
        Char myChar = Char.getMyChar();
        if (myChar == null || myChar.cName == null) return;
        if (myChar.statusMe == 14 || myChar.cHP <= 0) return;

        // Chi ban khi dang treo farm (Code.gameAB != null hoac AutoVipMap dang bat)
        boolean isFarming = (Code.gameAB != null || AutoVipMap.isEnabled);
        if (!isFarming) return;

        // Quet 5 vat pham xem co mon nao vuot nguong khong
        boolean needSell = false;
        for (int i = 0; i < ITEM_IDS.length; i++) {
            if (sellItems[i]) {
                int count = ThongKe.countItemInBag(ITEM_IDS[i]);
                if (count >= threshold) {
                    needSell = true;
                    break;
                }
            }
        }

        if (needSell) {
            doSellRoutine(false);
        }
    }

    /**
     * Quy trinh tu sat ve lang ban vat pham va quay lai:
     * @param forceAll Neu true: ban bat ky mon nao co sl > 0 (dung cho test)
     */
    public static void doSellRoutine(final boolean forceAll) {
        if (isSelling) return;
        isSelling = true;

        new Thread(new Runnable() {
            public void run() {
                try {
                    // 1. Luu vi tri va trang thai farm
                    saveCurrentFarmState();

                    // 2. Tu sat ve lang (neu khong phai o thon)
                    suicideAndReturnToVillage();

                    // 3. Ban cac vat pham cho NPC 46
                    sellItemsToNpc(forceAll);

                    // 4. Cap nhat thong ke hanh trang
                    try { ThongKe.updateItemCounts(null); } catch (Exception e) {}

                    // 5. Khoi phuc lai farm ve map cu hoac Map VIP
                    restoreFarmState();

                } catch (Exception e) {
                    GameScr.gameAC("B\u00e1n VP: L\u1ed7i - " + e.getMessage());
                } finally {
                    isSelling = false;
                }
            }
        }).start();
    }

    /** Luu vi tri va trang thai farm hien tai */
    private static void saveCurrentFarmState() {
        savedMap = TileMap.mapID;
        savedZone = TileMap.zoneID;
        Char me = Char.getMyChar();
        savedX = (me != null) ? me.cx : -1;
        savedY = (me != null) ? me.cy : -1;
        savedAuto = (Code.gameAB instanceof PkBoss) ? null : Code.gameAB;
        savedZoneIndex = Code.gameAW;
        wasVipMap = AutoVipMap.isEnabled || (TileMap.mapID == AutoVipMap.targetMapID);

        GameScr.gameAC("B\u00e1n VP: \u0110\u1ea1t ng\u01b0\u1ee1ng! L\u01b0u M" + savedMap + " K" + savedZone + " -> V\u1ec1 l\u00e0ng...");
    }

    /** Tu sat va hoi sinh ve lang */
    private static void suicideAndReturnToVillage() {
        int curMap = TileMap.mapID;

        // Neu dang o Lang Co thi thoat khao di lenh truoc
        if (TileMap.isLangCo(curMap) || curMap == 135 || curMap == 136 || curMap == 138) {
            try { AutoSanBoss.cleanKhaoDiLenh(); } catch (Exception e) {}
            sleep(300);
        }

        // Neu chua o thon (M22 hoac lang) thi tu sat ve lang
        if (curMap != 22) {
            LockGame.gameBK();
            if (Code.gameAB != null && !(Code.gameAB instanceof SanBossHolder)) {
                Code.gameAB = null;
            }

            try { GameCanvas.endDlg(); } catch (Exception e) {}
            try { Code.gameAN(); } catch (Exception e) {}
            sleep(500);

            if (Char.getMyChar() != null && Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) {
                try { Service.gI().gameAE(); } catch (Exception e) {}
            }

            // Cho nhan vat chet (statusMe == 14 hoac cHP <= 0, toi da 4s)
            for (int d = 0; d < 40; d++) {
                sleep(100);
                Char me = Char.getMyChar();
                if (me != null && (me.statusMe == 14 || me.cHP <= 0)) break;
            }

            // Hoi sinh ve lang bang packet gameAK (tuyet doi khong dung luong)
            for (int r = 0; r < 20; r++) {
                Char me = Char.getMyChar();
                if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                try { GameCanvas.endDlg(); } catch (Exception e) {}
                sleep(30);
                Auto.gameAN.removeAllElements();
                Auto.gameAM = false;
                LockGame.gameAA = true;
                Service.gI().gameAK(); // Hoi sinh ve lang
                TileMap.gameAF();
                LockGame.gameAA = false;
                sleep(300);
            }
        }

        // Cho nhan vat on dinh sau khi ve lang
        sleep(1000);
        try { GameCanvas.endDlg(); } catch (Exception e) {}
        try { InfoDlg.gameAD(); } catch (Exception e) {}
    }

    /**
     * Goi NPC 46 va ban lan luot cac vat pham can ban
     */
    private static void sellItemsToNpc(boolean forceAll) {
        for (int i = 0; i < ITEM_IDS.length; i++) {
            if (!sellItems[i] && !forceAll) continue;

            int currentCount = ThongKe.countItemInBag(ITEM_IDS[i]);
            if (!forceAll && currentCount < threshold) continue;
            if (currentCount <= 0) continue;

            int sellQty = (sellMode == MODE_ONE) ? 1 : currentCount;
            GameScr.gameAC("B\u00e1n VP: B\u00e1n " + sellQty + " " + ITEM_CODES[i] + "...");

            // 1. Dong dialog cu & focus NPC 46
            try { GameCanvas.endDlg(); } catch (Exception e) {}
            try { InfoDlg.gameAD(); } catch (Exception e) {}
            sleep(200);

            try {
                for (int n = 0; n < GameScr.vNpc.size(); n++) {
                    Npc npc = (Npc) GameScr.vNpc.elementAt(n);
                    if (npc != null && npc.template != null && npc.template.npcTemplateId == 46) {
                        Char.getMyChar().npcFocus = npc;
                        break;
                    }
                }
            } catch (Exception e) {}

            // Mo NPC 46
            Service.gI().gameAH(46);

            // Cho Menu 1 hien len
            for (int w = 0; w < 10 && (GameCanvas.menu == null || !GameCanvas.menu.showMenu); w++) {
                sleep(50);
            }

            // Chon Menu 1: o 5 (Bán vật phẩm)
            if (GameCanvas.menu != null && GameCanvas.menu.showMenu) {
                for (int t = 0; t < 4 && GameCanvas.menu.showMenu; t++) {
                    GameCanvas.menu.menuSelectedItem = 5;
                    GameCanvas.keyPressedz[5] = true;
                    GameCanvas.keyPressedz[12] = true;
                    sleep(50);
                }
            }
            Service.gI().gameAC(46, 5, 0);

            // Cho Menu 2 (Sub-menu danh sach vat pham) hien len
            sleep(200);
            for (int w = 0; w < 15 && (GameCanvas.menu == null || !GameCanvas.menu.showMenu); w++) {
                sleep(50);
            }

            // Chon Menu 2: o i (Vat pham can ban)
            if (GameCanvas.menu != null && GameCanvas.menu.showMenu) {
                for (int t = 0; t < 4 && GameCanvas.menu.showMenu; t++) {
                    GameCanvas.menu.menuSelectedItem = i;
                    GameCanvas.keyPressedz[5] = true;
                    GameCanvas.keyPressedz[12] = true;
                    sleep(50);
                }
            }
            // Gui packet chon Menu 2 truc tiep toi server
            Service.gI().gameAC(46, i, 0);
            Service.gI().gameAC(46, 5, i);

            // 3. Cho InputDlg xuat hien tu server (toi da 3s)
            boolean dlgReady = false;
            for (int w = 0; w < 30; w++) {
                sleep(100);
                if (GameCanvas.currentDialog == GameCanvas.inputDlg) {
                    dlgReady = true;
                    break;
                }
                // Neu qua 500ms chua co InputDlg ma Menu 2 van con mo, retry chon o i
                if (w == 5 && GameCanvas.menu != null && GameCanvas.menu.showMenu) {
                    GameCanvas.menu.menuSelectedItem = i;
                    GameCanvas.keyPressedz[5] = true;
                    GameCanvas.keyPressedz[12] = true;
                    Service.gI().gameAC(46, i, 0);
                    Service.gI().gameAC(46, 5, i);
                }
            }

            // 4. Nhap so luong can ban va bam dong y
            if (dlgReady && GameCanvas.inputDlg != null) {
                GameCanvas.inputDlg.tfInput.gameAA(String.valueOf(sellQty));
                sleep(150);
                if (GameCanvas.inputDlg.center != null) {
                    GameCanvas.inputDlg.center.gameAA();
                } else {
                    GameCanvas.endDlg();
                }
                GameScr.gameAC("B\u00e1n VP: \u0110\u00e3 g\u1eedi b\u00e1n " + sellQty + " " + ITEM_CODES[i]);
            } else {
                GameScr.gameAC("B\u00e1n VP: Kh\u00f4ng m\u1edf \u0111\u01b0\u1ee3c h\u1ed9p tho\u1ea1i b\u00e1n " + ITEM_CODES[i]);
                try { GameCanvas.endDlg(); } catch (Exception e) {}
            }

            // Cho server xu ly packet
            sleep(500);
            try { GameCanvas.endDlg(); } catch (Exception e) {}
            try { InfoDlg.gameAD(); } catch (Exception e) {}
            sleep(200);
        }
    }

    /** Khoi phuc lai trang thai farm ban dau (map goc hoac Map VIP) */
    private static void restoreFarmState() {
        isSelling = false; // Giai phong co ban de AutoVipMap / auto khac hoat dong

        // Case A: Truoc do o Map VIP -> Khoi phuc Map VIP
        if (wasVipMap) {
            GameScr.gameAC("B\u00e1n VP: Kh\u00f4i ph\u1ee5c Map VIP...");
            AutoVipMap.isEnabled = true;
            AutoVipMap.checkAndReturn();

            // Cho vao Map VIP roi khoi phuc auto farm
            new Thread(new Runnable() {
                public void run() {
                    for (int w = 0; w < 200 && TileMap.mapID != AutoVipMap.targetMapID; w++) {
                        sleep(100);
                    }
                    if (TileMap.mapID == AutoVipMap.targetMapID) {
                        sleep(500);
                        resumeFarmAuto(AutoVipMap.targetMapID, savedZone);
                        GameScr.gameAC("B\u00e1n VP: \u0110\u00e3 v\u00e0o Map VIP -> Ti\u1ebfp t\u1ee5c farm!");
                    }
                }
            }).start();
            return;
        }

        // Case B: Truoc do o Map thuong / khong phai Map VIP -> Quay ve map cu
        if (savedMap >= 0) {
            GameScr.gameAC("B\u00e1n VP: Quay l\u1ea1i M" + savedMap + " K" + savedZone + "...");
            ensureAlive();

            LockGame.gameBK();
            if (Code.gameAB != null) {
                Code.gameAB = null;
            }

            // Neu map dich la Lang Co, dam bao vao Lang Co truoc
            if (savedMap >= 134 && savedMap <= 138) {
                try {
                    AutoSanBoss.ensureInLangCo();
                } catch (Exception e) {}
            }

            // Travel ve map cu truc tiep bang TileMap.GoMap (tuyet doi KHONG dung PkBoss de tranh quet boss / doi khu)
            for (int attempt = 0; attempt < 30 && TileMap.mapID != savedMap; attempt++) {
                ensureAlive();
                if (TileMap.mapID == savedMap) break;

                try {
                    GameCanvas.endDlg();
                    TileMap.GoMap(savedMap);
                } catch (Exception e) {}

                for (int w = 0; w < 30 && TileMap.mapID != savedMap; w++) {
                    sleep(100);
                    if (isDead()) {
                        ensureAlive();
                        break;
                    }
                }
            }

            // Khi da ve dung map goc
            if (TileMap.mapID == savedMap) {
                // Doi khu cu
                if (savedZone >= 0 && TileMap.zoneID != savedZone) {
                    Auto.gameAA(savedZone);
                    for (int z = 0; z < 1000 && TileMap.zoneID != savedZone; z++) sleep(10);
                }

                // Di chuyen ve toa do cu
                if (savedX > 0 && savedY > 0) {
                    try {
                        Char.gameAE(savedX, savedY);
                        Char.getMyChar().cx = savedX;
                        Char.getMyChar().cy = savedY;
                        Service.gI().gameAC(savedX, savedY);
                        sleep(200);
                    } catch (Exception e) {}
                }

                // Khoi phuc lai auto farm (Tan Sat / farm goc)
                resumeFarmAuto(savedMap, savedZone);
                GameScr.gameAC("B\u00e1n VP: \u0110\u00e3 v\u1ec1 M" + savedMap + " K" + (savedZone >= 0 ? savedZone : TileMap.zoneID) + " -> Ti\u1ebfp t\u1ee5c farm!");
            } else {
                GameScr.gameAC("B\u00e1n VP: Ch\u01b0a v\u1ec1 \u0111\u01b0\u1ee3c M" + savedMap + "!");
            }
        }
    }

    /** Khoi phuc Tan Sat hoac auto farm goc sau khi tro ve map */
    private static void resumeFarmAuto(int mapId, int zoneId) {
        try {
            int targetZone = (zoneId >= 0) ? zoneId : (int) TileMap.zoneID;
            if (savedAuto instanceof Stanima) {
                Code.gameAW = savedZoneIndex;
                Code.gameAB = savedAuto;
                Code.timBG = true;
            } else {
                int mobId = -1;
                if (savedAuto instanceof TanSat) {
                    mobId = ((TanSat) savedAuto).templateId;
                }
                LockGame.gameBK();
                Code.gameAB = null;
                Code.gameAW = savedZoneIndex;
                Code.gameAA(mobId, mapId, targetZone);
            }
            TsBoost.onTsStarted();
            AutoPickup.start();
        } catch (Exception e) {
            try {
                Code.gameAA(-1, mapId);
                TsBoost.onTsStarted();
                AutoPickup.start();
            } catch (Exception ex) {}
        }
    }

    private static boolean isDead() {
        Char me = Char.getMyChar();
        return me != null && (me.statusMe == 14 || me.cHP <= 0);
    }

    private static void ensureAlive() {
        try {
            Char me = Char.getMyChar();
            if (me != null && (me.statusMe == 14 || me.cHP <= 0)) {
                for (int r = 0; r < 10; r++) {
                    GameCanvas.endDlg();
                    LockGame.gameAA = true;
                    Service.gI().gameAK();
                    TileMap.gameAF();
                    LockGame.gameAA = false;
                    sleep(200);
                    me = Char.getMyChar();
                    if (me != null && me.statusMe != 14 && me.cHP > 0) break;
                }
            }
        } catch (Exception e) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }

    /** Luu config vao RMS */
    public static void saveConfigToRMS() {
        try {
            String data = (isEnabled ? "1" : "0") + ";"
                + sellMode + ";"
                + threshold + ";"
                + (sellItems[0] ? "1" : "0") + ";"
                + (sellItems[1] ? "1" : "0") + ";"
                + (sellItems[2] ? "1" : "0") + ";"
                + (sellItems[3] ? "1" : "0") + ";"
                + (sellItems[4] ? "1" : "0");
            RMS.gameAA("auto_ban_vp_cfg", data);
        } catch (Exception e) {}
    }

    /** Load config tu RMS */
    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("auto_ban_vp_cfg");
            if (data != null && data.length() > 0) {
                int[] v = new int[8];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 8; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        v[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 3) {
                    isEnabled = (v[0] == 1);
                    sellMode = v[1];
                    threshold = v[2];
                }
                if (idx >= 8) {
                    for (int i = 0; i < 5; i++) {
                        sellItems[i] = (v[3 + i] == 1);
                    }
                }
            }
        } catch (Exception e) {}
        if (isEnabled && !isRunning) {
            start();
        }
    }
}
