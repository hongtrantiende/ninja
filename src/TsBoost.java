/**
 * TsBoost v6 — Bo sung tang toc cho TS/AK goc. Toi uu cho 20 instances.
 *
 * Chay song song voi TanSat (Code.gameAB):
 * - TS goc: xu ly target, di chuyen, heal, nhat do, hoi sinh, next map, buff, attack
 * - TsBoost: CHI spam AoE vao quai MA TS GOC KHONG DANH (ngoai range cua TS goc)
 *
 * v6: Fix xung dot voi TS goc:
 *     1. KHONG tu set timBG — de TS goc quan ly flag nay hoan toan
 *     2. KHONG auto buff — de TS goc tu buff (tranh double buff packet)
 *     3. Chi attack quai ngoai range TS goc HOAC khi TS goc dang khong attack
 *     4. Khong watchdog, khong GoMap, khong tu sat
 *
 * Lenh: tsp (bat/tat mode boost)
 */
public class TsBoost implements Runnable {
    public static boolean modeEnabled = true;   // Mac dinh ON
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG (co the chinh sua, luu RMS) ===
    private static final int DEF_ATTACK_DELAY_MS = 200;
    private static final int DEF_IDLE_DELAY_MS = 500;
    private static final int DEF_MAX_ATTACK_RANGE = 400;
    private static final int DEF_SKILL_RESELECT_MS = 15000;
    private static final int DEF_MAX_MOB_PER_ATTACK = 6;
    private static final int DEF_COOLDOWN_MS = 1500;  // 1.5s cooldown per mob

    public static int ATTACK_DELAY_MS = DEF_ATTACK_DELAY_MS;
    public static int IDLE_DELAY_MS = DEF_IDLE_DELAY_MS;
    public static int MAX_ATTACK_RANGE = DEF_MAX_ATTACK_RANGE;
    public static int SKILL_RESELECT_MS = DEF_SKILL_RESELECT_MS;
    public static int MAX_MOB_PER_ATTACK = DEF_MAX_MOB_PER_ATTACK;
    public static boolean isCooldownEnabled = false;
    public static int COOLDOWN_MS = DEF_COOLDOWN_MS;
    private static final int ATTACK_KILL_WINDOW_MS = 800;  // Window kill tracking

    // === PHAN THAN LENH LIMITER ===
    public static boolean isLimitPhanThan = false;
    public static int LIMIT_PHANTHAN_MAX = 5;
    private static final int PHANTHAN_ITEM_ID = 545;
    private static final int LIMIT_CHECK_INTERVAL = 10000; // 10s
    private static long lastLimitCheckTime = 0;

    // === MOB COOLDOWN TRACKING (circular buffer) ===
    private static final int CD_SIZE = 16;
    private static final int[] cdX = new int[CD_SIZE];
    private static final int[] cdY = new int[CD_SIZE];
    private static final long[] cdTime = new long[CD_SIZE];
    private static int cdIdx = 0;

    static {
        loadConfigFromRMS();
    }

    /** Luu config TS vao RMS. Format: "val1;val2;val3;val4;val5;cooldownEnabled;cooldownMs" */
    public static void saveConfigToRMS() {
        try {
            String data = ATTACK_DELAY_MS + ";" + IDLE_DELAY_MS + ";"
                + MAX_ATTACK_RANGE + ";" + SKILL_RESELECT_MS + ";" + MAX_MOB_PER_ATTACK
                + ";" + (isCooldownEnabled ? 1 : 0) + ";" + COOLDOWN_MS
                + ";" + (isLimitPhanThan ? 1 : 0) + ";" + LIMIT_PHANTHAN_MAX;
            RMS.gameAA("ts_boost_cfg", data);
        } catch (Exception e) {}
    }

    /** Load config TS tu RMS */
    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("ts_boost_cfg");
            if (data != null && data.length() > 0) {
                int[] vals = new int[9];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 9; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        vals[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 5) {
                    ATTACK_DELAY_MS = vals[0];
                    IDLE_DELAY_MS = vals[1];
                    MAX_ATTACK_RANGE = vals[2];
                    SKILL_RESELECT_MS = vals[3];
                    MAX_MOB_PER_ATTACK = vals[4];
                }
                if (idx >= 6) {
                    isCooldownEnabled = vals[5] == 1;
                }
                if (idx >= 7) {
                    COOLDOWN_MS = vals[6];
                }
                if (idx >= 8) {
                    isLimitPhanThan = vals[7] == 1;
                }
                if (idx >= 9) {
                    LIMIT_PHANTHAN_MAX = vals[8];
                    if (LIMIT_PHANTHAN_MAX < 0) LIMIT_PHANTHAN_MAX = 0;
                    if (LIMIT_PHANTHAN_MAX > 99) LIMIT_PHANTHAN_MAX = 99;
                }
            }
        } catch (Exception e) {}
    }

    /** Reset config ve mac dinh */
    public static void resetConfig() {
        ATTACK_DELAY_MS = DEF_ATTACK_DELAY_MS;
        IDLE_DELAY_MS = DEF_IDLE_DELAY_MS;
        MAX_ATTACK_RANGE = DEF_MAX_ATTACK_RANGE;
        SKILL_RESELECT_MS = DEF_SKILL_RESELECT_MS;
        MAX_MOB_PER_ATTACK = DEF_MAX_MOB_PER_ATTACK;
        isCooldownEnabled = false;
        COOLDOWN_MS = DEF_COOLDOWN_MS;
        isLimitPhanThan = false;
        LIMIT_PHANTHAN_MAX = 5;
    }

    /** Danh dau mob tai (x,y) vua bi danh */
    private static void markMobCooldown(int mx, int my) {
        cdX[cdIdx] = mx;
        cdY[cdIdx] = my;
        cdTime[cdIdx] = System.currentTimeMillis();
        cdIdx = (cdIdx + 1) % CD_SIZE;
    }

    /** Kiem tra mob tai (x,y) co dang cooldown khong */
    private static boolean isMobOnCooldown(int mx, int my) {
        if (!isCooldownEnabled) return false;
        long now = System.currentTimeMillis();
        for (int i = 0; i < CD_SIZE; i++) {
            if (cdX[i] == mx && cdY[i] == my && now - cdTime[i] < COOLDOWN_MS) {
                return true;
            }
        }
        return false;
    }

    // === REUSABLE VECTORS (tranh GC) ===
    private static final MyVector reusableMobs = new MyVector();
    private static final MyVector reusableChars = new MyVector();

    // === STATE ===
    private static int cachedSkillId = -1;
    private static long lastSkillSelectTime = 0;

    // === KILL TRACKING ===
    private static int totalKills = 0;
    private static int sessionKills = 0;
    private static int lastMobCount = -1;
    private static long sessionStartTime = 0;
    private static long startExp = 0;
    private static int startYen = 0;
    private static int startXu = 0;
    private static int startLuong = 0;
    private static int attacksSent = 0;

    // =============================================
    // LIFECYCLE
    // =============================================

    /** Toggle mode on/off. */
    public static void toggleMode() {
        modeEnabled = !modeEnabled;
        if (modeEnabled) {
            safeNotify("Ts Pro: ON!");
            if (Code.gameAB != null && !isRunning) {
                start();
            }
        } else {
            stop();
            safeNotify("Ts Pro: OFF");
        }
    }

    /** Bat boost thread. */
    public static void start() {
        if (isRunning) return;
        if (!modeEnabled) return;

        Char myChar = Char.getMyChar();
        if (myChar != null) {
            startExp = myChar.cEXP;
            startYen = myChar.yen;
            startXu = myChar.xu;
            startLuong = myChar.luong;
            sessionKills = 0;
            sessionStartTime = System.currentTimeMillis();
            ThongKe.resetStats(myChar);
        }

        isRunning = true;
        cachedSkillId = -1;
        lastMobCount = -1;
        attacksSent = 0;
        // KHONG set Code.timBG o day — de TS goc quan ly flag nay
        thread = new Thread(new TsBoost());
        thread.start();
    }

    /** Tat boost thread. */
    public static void stop() {
        isRunning = false;
        thread = null;
        // KHONG set Code.timBG = false o day — de TS goc quan ly flag nay
        // Neu TS goc con chay thi timBG phai giu true
        // Neu TS goc tat thi Code.gameAF() da set timBG = false roi
    }

    /** Hook: goi khi ts/ak bat. */
    public static void onTsStarted() {
        if (!modeEnabled || isRunning) return;
        if (Code.gameAB != null) {
            start();
        } else {
            syncAfterTs();
        }
    }

    /** Doi TanSat xuat hien (toi da 30s) roi bat. */
    public static void syncAfterTs() {
        if (!modeEnabled) return;
        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 60; i++) {
                    if (!modeEnabled) return;
                    if (Code.gameAB != null) {
                        if (!isRunning) {
                            start();
                            safeNotify("Ts Pro ON theo TS!");
                        }
                        return;
                    }
                    try { Thread.sleep(500L); } catch (Exception e) {}
                }
            }
        }).start();
    }

    /** Hook: goi khi ts/ak tat. */
    public static void onTsStopped() {
        stop();
    }

    // =============================================
    // MAIN LOOP — chi spam AoE bo sung, KHONG buff, KHONG tu sat, KHONG GoMap
    // =============================================

    public void run() {
        sleep(500);

        // Doi Code.gameAB xuat hien
        for (int wait = 0; wait < 20 && isRunning && Code.gameAB == null; wait++) {
            sleep(500);
        }

        totalKills = 0;
        lastMobCount = -1;
        long lastAttackTime = 0;

        while (isRunning) {
            try {
                long loopStart = System.currentTimeMillis();

                // TS goc da tat -> dung
                if (Code.gameAB == null) {
                    sleep(1500);
                    if (Code.gameAB == null) break;
                }

                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    sleep(1500);
                    continue;
                }

                // Dang chet -> doi TS goc hoi sinh
                if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                    sleep(1000);
                    continue;
                }

                // === COLLECT MOBS TRONG RANGE (chi quai song, bo quai dang duoc TS goc nham) ===
                MyVector mobs = collectMobsInRange(myChar);

                if (mobs.size() > 0) {
                    // Kill tracking (gon nhe)
                    int currentMobCount = mobs.size();
                    if (lastMobCount >= 0 && currentMobCount < lastMobCount) {
                        int dropped = lastMobCount - currentMobCount;
                        if (loopStart - lastAttackTime < ATTACK_KILL_WINDOW_MS) {
                            totalKills += dropped;
                            sessionKills += dropped;
                            ThongKe.addKills(dropped);
                        }
                    }
                    lastMobCount = currentMobCount;

                    // Attack AoE bo sung
                    fireAttack(myChar, mobs);
                    lastAttackTime = System.currentTimeMillis();
                    sleep(ATTACK_DELAY_MS);
                } else {
                    lastMobCount = 0;
                    sleep(IDLE_DELAY_MS);
                }

                // === PHAN THAN LENH LIMITER ===
                if (isLimitPhanThan) {
                    long now2 = System.currentTimeMillis();
                    if (now2 - lastLimitCheckTime >= LIMIT_CHECK_INTERVAL) {
                        lastLimitCheckTime = now2;
                        dropExcessPhanThan();
                    }
                }

            } catch (Exception e) {
                sleep(1000);
            }
        }
        isRunning = false;
    }

    /**
     * Goi tu Code.run loop — khong can lam gi.
     * Giu method de khong bi loi compile o Code.java.
     */
    public static void checkHang() {
        // Khong lam gi — TS goc tu xu ly
    }

    // =============================================
    // PHAN THAN LENH LIMITER
    // =============================================

    /**
     * Quet hanh trang, dem so Phan Than Lenh (ID 545).
     * Neu vuot qua LIMIT_PHANTHAN_MAX, vut bot item thua.
     */
    public static void dropExcessPhanThan() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null || myChar.arrItemBag == null) return;

            // Buoc 1: dem so luong va luu danh sach index
            int count = 0;
            int[] indices = new int[myChar.arrItemBag.length];
            for (int i = 0; i < myChar.arrItemBag.length; i++) {
                Item item = myChar.arrItemBag[i];
                if (item != null && item.template != null && item.template.id == PHANTHAN_ITEM_ID) {
                    indices[count] = i;
                    count++;
                }
            }

            if (count <= LIMIT_PHANTHAN_MAX) return;

            // Buoc 2: vut tu cuoi danh sach (giu cac slot dau)
            int excess = count - LIMIT_PHANTHAN_MAX;
            int dropped = 0;
            for (int j = count - 1; j >= 0 && dropped < excess; j--) {
                Item item = myChar.arrItemBag[indices[j]];
                if (item != null) {
                    try {
                        Service.gI().gameAH(item.indexUI, 1); // Ban thay vi vut
                        dropped++;
                        try { Thread.sleep(200); } catch (Exception e) {}
                    } catch (Exception e) {}
                }
            }

            if (dropped > 0) {
                safeNotify("B\u00e1n " + dropped + " Ph\u00e2n Th\u00e2n L\u1ec7nh (gi\u1eef " + LIMIT_PHANTHAN_MAX + ")");
            }
        } catch (Exception e) {}
    }

    // =============================================
    // ATTACK
    // =============================================

    /** Thu thap mob song trong MAX_ATTACK_RANGE. Gioi han MAX_MOB_PER_ATTACK con. */
    private static MyVector collectMobsInRange(Char myChar) {
        reusableMobs.removeAllElements();
        try {
            int cx = myChar.cx;
            int cy = myChar.cy;
            int size = GameScr.vMob.size();
            for (int i = 0; i < size && reusableMobs.size() < MAX_MOB_PER_ATTACK; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    // Skip mob dang cooldown
                    if (isMobOnCooldown(mob.x, mob.y)) continue;
                    if (mob.hp > 0 && mob.status != 0 && mob.status != 1) {
                        if (Math.abs(cx - mob.x) + Math.abs(cy - mob.y) <= MAX_ATTACK_RANGE) {
                            reusableMobs.addElement(mob);
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return reusableMobs;
    }

    /** Chon skill AOE tot nhat va danh 1 lan. */
    private static void fireAttack(Char myChar, MyVector mobs) {
        try {
            long now = System.currentTimeMillis();
            if (cachedSkillId < 0 || now - lastSkillSelectTime > SKILL_RESELECT_MS) {
                cachedSkillId = pickBestAoeSkill(myChar);
                if (cachedSkillId >= 0) {
                    Service.gI().gameAG(cachedSkillId);
                }
                lastSkillSelectTime = now;
            }

            reusableChars.removeAllElements();
            if (cachedSkillId >= 0) {
                Service.gI().gameAA(mobs, reusableChars, 2); // Skill attack
            } else {
                Service.gI().gameAA(mobs, reusableChars, 1); // Danh thuong
            }
            // Fast Attack: gui them N packet attack
            if (ExploitConfig.isFastAttack && ExploitConfig.isActive()) {
                int atkType = cachedSkillId >= 0 ? 2 : 1;
                for (int fa = 0; fa < ExploitConfig.FAST_ATTACK_COUNT; fa++) {
                    try { Thread.sleep(5); } catch (Exception ex) {}
                    Service.gI().gameAA(mobs, reusableChars, atkType);
                }
            }
            attacksSent++;
            // Danh dau cooldown cho cac mob vua danh
            if (isCooldownEnabled) {
                for (int m = 0; m < mobs.size(); m++) {
                    try {
                        Mob mob = (Mob) mobs.elementAt(m);
                        markMobCooldown(mob.x, mob.y);
                    } catch (Exception ex) {}
                }
            }
        } catch (Exception e) {}
    }

    /** Tim skill AOE tot nhat: type 3 > type 1 > type 0. */
    private static int pickBestAoeSkill(Char myChar) {
        if (myChar.vSkillFight == null) return -1;

        Skill bestAoe = null;
        Skill bestChieu = null;
        Skill bestNormal = null;

        int size = myChar.vSkillFight.size();
        for (int i = 0; i < size; i++) {
            try {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                int type = s.template.type;
                if (type == 3 && bestAoe == null) bestAoe = s;
                else if (type == 1 && bestChieu == null) bestChieu = s;
                else if (type == 0 && bestNormal == null) bestNormal = s;
            } catch (Exception e) {}
        }

        if (bestAoe != null) return bestAoe.template.id;
        if (bestChieu != null) return bestChieu.template.id;
        if (bestNormal != null) return bestNormal.template.id;
        return -1;
    }

    // =============================================
    // STATS
    // =============================================

    /** Hien thi thong ke. */
    public static void showFullStats() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;

            long now = System.currentTimeMillis();
            long sec = (sessionStartTime > 0) ? (now - sessionStartTime) / 1000 : 1;
            if (sec <= 0) sec = 1;

            long gainExp = myChar.cEXP - startExp;
            if (gainExp < 0) gainExp = 0;
            int gainYen = myChar.yen - startYen;
            if (gainYen < 0) gainYen = 0;
            int gainXu = myChar.xu - startXu;
            if (gainXu < 0) gainXu = 0;
            int gainLuong = myChar.luong - startLuong;
            if (gainLuong < 0) gainLuong = 0;

            int aliveMobs = 0;
            try {
                int sz = GameScr.vMob.size();
                for (int i = 0; i < sz; i++) {
                    Object o = GameScr.vMob.elementAt(i);
                    if (o instanceof Mob) {
                        Mob m = (Mob) o;
                        if (m.hp > 0 && m.status != 0 && m.status != 1) aliveMobs++;
                    }
                }
            } catch (Exception e) {}

            safeNotify("Up (" + formatTime(sec) + "): " + sessionKills + " Qu\u00e1i | Map: " + aliveMobs + " | Exp: +" + formatNumber(gainExp) + " | Y\u00ean: +" + formatNumber(gainYen) + " | Xu: +" + formatNumber(gainXu) + " | L\u01b0\u1ee3ng: +" + gainLuong);
        } catch (Exception e) {}
    }

    private static String formatNumber(long val) {
        if (val >= 1000000000L) return (val / 100000000L / 10.0f) + "B";
        if (val >= 1000000L) return (val / 100000L / 10.0f) + "M";
        if (val >= 1000L) return (val / 100L / 10.0f) + "k";
        return String.valueOf(val);
    }

    private static String formatTime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return h + "h" + m + "m";
        if (m > 0) return m + "m" + s + "s";
        return s + "s";
    }

    // =============================================
    // HELPERS
    // =============================================

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }

    /** Thong bao an toan — GameScr.gameAC co the crash (ArrayIndexOutOfBounds). */
    private static void safeNotify(String msg) {
        System.out.println("[TsPro] " + msg);
        try {
            GameScr.gameAC(msg);
        } catch (Exception e) {
            // gameAC crash — thong bao da in ra System.out
        }
    }
}
