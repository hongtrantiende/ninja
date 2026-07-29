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
    private static Thread thread;

    // 4 loai boss (khong tim Lang Co)
    // Moi boss co: ten, int[] mapIDs, int[] hours
    private static final int TYPE_SERVER = 0;
    private static final int TYPE_THEGIOI = 1;
    private static final int TYPE_VDMQ = 2;
    private static final int TYPE_MAPNGOAI = 3;

    private static final String[] BOSS_NAMES = {"Server", "TheGioi", "VDMQ", "MapNgoai"};

    // Map IDs cho moi loai boss
    private static final int[][] BOSS_MAPS = {
        {3},               // Server
        {23},              // TheGioi
        {141, 142, 143},   // VDMQ
        {}                 // MapNgoai - dynamic based on level
    };

    // Map IDs cua MapNgoai theo level
    private static final int[][] MAPNGOAI_BY_LEVEL = {
        {14, 15, 16},      // Lv45
        {44, 67, 70},      // Lv55
        {24, 41, 45},      // Lv65
        {18, 36, 54}       // Lv75
    };
    private static final int[] MAPNGOAI_LEVELS = {45, 55, 65, 75};

    // Khung gio spawn (gio)
    private static final int[][] BOSS_HOURS = {
        {12, 18, 20, 22},   // Server
        {12, 23},            // TheGioi
        {9, 15, 17, 21},    // VDMQ
        {6, 11, 17, 22}     // MapNgoai
    };

    // Boss ton tai 15 phut (900 giay) sau khi spawn
    private static final int BOSS_ALIVE_DURATION = 900;
    private static final int MAX_ZONES = 30;

    // Trang thai hien tai
    private int currentBossType = -1;
    private int currentMapIndex = 0;
    private int lastDeathMapID = -1;
    private int lastDeathZoneID = -1;

    public static void toggle() {
        if (isRunning) {
            isRunning = false;
            GameScr.gameAC("T\u1eaft T\u1ef1 S\u0103n Boss!");
        } else {
            isRunning = true;
            AutoSanBoss instance = new AutoSanBoss();
            thread = new Thread(instance);
            thread.start();
            GameScr.gameAC("B\u1eadt T\u1ef1 S\u0103n Boss!");
        }
    }

    /**
     * Lay danh sach map ID cho boss MapNgoai dua tren level nhan vat
     */
    private int[] getMapNgoaiMaps() {
        int myLevel = Char.getMyChar().clevel;
        // Tim nhom map phu hop nhat voi level
        int bestIdx = 0;
        for (int i = MAPNGOAI_LEVELS.length - 1; i >= 0; i--) {
            if (myLevel >= MAPNGOAI_LEVELS[i]) {
                bestIdx = i;
                break;
            }
        }
        return MAPNGOAI_BY_LEVEL[bestIdx];
    }

    /**
     * Kiem tra boss co dang trong khung gio spawn khong (15 phut sau gio spawn)
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
        for (int i = 0; i < 4; i++) {
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
        for (int i = 0; i < GameScr.vMob.size(); i++) {
            Mob mob = (Mob)GameScr.vMob.elementAt(i);
            if (mob.isBoss && mob.hp > 0 && mob.status != 0) {
                return true;
            }
        }
        return false;
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
     * Quet 1 khu, return true neu tim thay boss
     */
    private boolean scanZone(int zone) {
        if (TileMap.zoneID != zone) {
            Service.gI().gameAA(zone, -1);
            TileMap.gameAF();
            if (!waitForZone(zone, 3000)) {
                return false;
            }
        }
        // Doi load mob nhanh
        sleep(100);
        return hasBossOnCurrentMap();
    }

    /**
     * Bat dau PkBoss va doi cho den khi xong
     */
    private void startPkBossAndWait(int mapID) {
        GameScr.gameAC("TSB: PK Boss M" + mapID + " K" + TileMap.zoneID);
        // Luu vi tri de quay lai neu chet
        lastDeathMapID = mapID;
        lastDeathZoneID = TileMap.zoneID;

        // Bat dau PkBoss
        Code.gameAA(new PkBoss(mapID));

        // Gui lenh cho nhom
        if (Code.gameAH != null && Char.getMyChar().cName.equals(Code.gameAH) && GameScr.vParty.size() > 1) {
            Service.gI().gameAK("pkm " + mapID);
            Service.gI().gameAK("pkk " + TileMap.zoneID);
        }

        // Doi PkBoss ket thuc
        while (isRunning && Code.gameAB instanceof PkBoss) {
            // Kiem tra nhan vat chet (statusMe == 14 = Kiet suc)
            if (Char.getMyChar().statusMe == 14 || Char.getMyChar().cHP <= 0) {
                // BUOC 1: Dong dialog "Kiet suc" NGAY
                GameCanvas.endDlg();
                sleep(100);

                // BUOC 2: Mo menu respawn
                GameScr.gameAB(5, 0, 0);
                sleep(100);

                // BUOC 3: Gui lenh "Ve nha"
                Service.gI().gameAF();
                sleep(300);

                // BUOC 4: Quay lai map boss NGAY
                if (isRunning && TileMap.mapID != mapID) {
                    GameScr.gameAC("TSB: Quay lai M" + mapID);
                    waitForMap(mapID, 15000);
                }
            }
            sleep(200);
        }
    }

    /**
     * Quet tat ca khu tren 1 map, tim va danh boss
     * Return true neu da danh it nhat 1 boss
     */
    private boolean scanAndFightOnMap(int mapID) {
        GameScr.gameAC("TSB: Quet M" + mapID);
        boolean foundAny = false;

        // Quet tat ca 30 khu
        for (int zone = 0; zone < MAX_ZONES && isRunning; zone++) {
            if (scanZone(zone)) {
                // Tim thay boss!
                GameScr.gameAC("TSB: Boss M" + mapID + " K" + zone + "!");
                startPkBossAndWait(mapID);
                foundAny = true;

                // Sau khi PkBoss xong, kiem tra boss con spawn khong
                if (!isRunning || !isBossActive(currentBossType)) {
                    break; // Het gio boss
                }
                // Tiep tuc quet cac khu con lai
            }
        }

        return foundAny;
    }

    /**
     * Chuyen den map muc tieu
     */
    private boolean travelToMap(int mapID) {
        if (TileMap.mapID == mapID) {
            return true;
        }
        GameScr.gameAC("TSB: Di M" + mapID);

        // Tao 1 Auto tam de chuyen map
        // Su dung ky thuat tu PkBoss: gameAA(mapID, -2, -1, -1)
        // Nhung vi khong the goi truc tiep Auto.gameAA tu ngoai,
        // ta dung PkBoss constructor de chuyen map
        Code.gameAA(new PkBoss(mapID));
        // Doi chuyen map
        boolean arrived = waitForMap(mapID, 15000);
        // Dung PkBoss tam
        if (Code.gameAB instanceof PkBoss) {
            // Xoa auto de ta tu quan ly
            Code.gameAB = null;
        }
        return arrived;
    }

    public void run() {
        sleep(2000); // Doi khoi tao

        while (isRunning) {
            try {
                // Buoc 1: Tim boss nao dang active
                int activeBoss = findActiveBoss();

                if (activeBoss == -1) {
                    // Khong co boss nao, doi 30 giay roi check lai
                    sleep(30000);
                    continue;
                }

                currentBossType = activeBoss;
                int[] maps = getMapsForBoss(activeBoss);

                GameScr.gameAC("TSB: " + BOSS_NAMES[activeBoss] + " dang spawn!");

                // Buoc 2: Quet tat ca map cua boss nay
                boolean bossStillActive = true;
                while (bossStillActive && isRunning) {
                    boolean foundOnAnyMap = false;

                    for (int mi = 0; mi < maps.length && isRunning; mi++) {
                        currentMapIndex = mi;
                        int targetMap = maps[mi];

                        // Kiem tra boss con trong khung gio khong
                        if (!isBossActive(activeBoss)) {
                            bossStillActive = false;
                            break;
                        }

                        // Chuyen map
                        if (!travelToMap(targetMap)) {
                            GameScr.gameAC("TSB: Khong den duoc M" + targetMap);
                            continue;
                        }

                        // Quet va danh boss tren map nay
                        if (scanAndFightOnMap(targetMap)) {
                            foundOnAnyMap = true;
                        }
                    }

                    // Neu khong tim thay boss o bat ky map nao, doi 30s roi quet lai
                    if (!foundOnAnyMap || !isBossActive(activeBoss)) {
                        bossStillActive = false;
                    }
                }

                // Boss da het gio hoac da danh het, doi 30s roi check boss tiep theo
                if (isRunning) {
                    GameScr.gameAC("TSB: Het " + BOSS_NAMES[activeBoss] + ", doi boss tiep...");
                    sleep(30000);
                }

            } catch (Exception e) {
                // An toan: khong crash thread
                sleep(5000);
            }
        }

        GameScr.gameAC("TSB: Da dung.");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
