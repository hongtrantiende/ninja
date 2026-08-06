/**
 * TsBoost — Bo sung tang toc cho TS/AK goc.
 *
 * Chay song song voi TanSat (Code.gameAB):
 * - TS goc: xu ly target, di chuyen, heal, nhat do, hoi sinh, next map
 * - TsBoost: spam attack AoE tat ca quai trong range + auto buff + map watchdog
 *
 * Lenh: tsp (bat/tat mode boost)
 * Khi mode ON, moi lan bat ts/ak se tu dong kich hoat TsBoost.
 */
public class TsBoost implements Runnable {
    public static boolean modeEnabled = true;   // Mac dinh ON
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int ATTACK_DELAY_MS = 50;     // Delay giua moi lan attack
    private static final int IDLE_DELAY_MS = 200;       // Delay khi het quai
    private static final int BUFF_INTERVAL_MS = 15000;  // Buff moi 15 giay
    private static final int MAX_ATTACK_RANGE = 400;    // Range danh toi da (px)
    private static final int STATS_INTERVAL_MS = 30000;  // Stats moi 30 giay
    private static final int WRONG_MAP_TIMEOUT_MS = 6000; // 6s sai map = retry GoMap
    private static final int SKILL_RESELECT_MS = 5000;   // Chon lai skill moi 5s
    private static final int ATTACK_KILL_WINDOW_MS = 600; // Quai chet trong 600ms = minh giet

    // === REUSABLE VECTORS (tranh GC) ===
    private static final MyVector reusableMobs = new MyVector();
    private static final MyVector reusableChars = new MyVector();

    // === STATE ===
    private static long lastBuffTime = 0;
    private static long wrongMapSince = 0;
    private static int cachedSkillId = -1;
    private static long lastSkillSelectTime = 0;

    // === KILL TRACKING ===
    private static int totalKills = 0;
    private static int sessionKills = 0;
    private static int lastMobCount = -1;
    private static long statsStartTime = 0;
    private static long sessionStartTime = 0;
    private static long startExp = 0;
    private static int startYen = 0;
    private static int startXu = 0;
    private static int startLuong = 0;

    // === SKILL EFFECTS ===
    private static boolean savedTimBG = false;

    // =============================================
    // LIFECYCLE
    // =============================================

    /** Toggle mode on/off. */
    public static void toggleMode() {
        modeEnabled = !modeEnabled;
        if (modeEnabled) {
            GameScr.gameAC("Ts Pro: ON!");
            if (Code.gameAB != null && !isRunning) {
                start();
            }
        } else {
            stop();
            GameScr.gameAC("Ts Pro: OFF");
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
        lastBuffTime = 0;
        wrongMapSince = 0;
        cachedSkillId = -1;
        lastMobCount = -1;
        savedTimBG = Code.timBG;
        Code.timBG = true; // Tat hieu ung skill giam lag
        thread = new Thread(new TsBoost());
        thread.start();
    }

    /** Tat boost thread. */
    public static void stop() {
        isRunning = false;
        thread = null;
        Code.timBG = false; // Luon khoi phuc hieu ung
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
                for (int i = 0; i < 120; i++) {
                    if (!modeEnabled) return;
                    if (Code.gameAB != null) {
                        if (!isRunning) {
                            start();
                            GameScr.gameAC("Ts Pro ON theo TS!");
                        }
                        return;
                    }
                    try { Thread.sleep(250L); } catch (Exception e) {}
                }
            }
        }).start();
    }

    /** Hook: goi khi ts/ak tat. */
    public static void onTsStopped() {
        stop();
    }

    // =============================================
    // MAIN LOOP
    // =============================================

    public void run() {
        sleep(500);

        // Doi Code.gameAB xuat hien
        for (int wait = 0; wait < 20 && isRunning && Code.gameAB == null; wait++) {
            sleep(500);
        }

        totalKills = 0;
        lastMobCount = -1;
        statsStartTime = System.currentTimeMillis();
        long lastAttackTime = 0;

        while (isRunning) {
            try {
                // TS goc da tat -> dung
                if (Code.gameAB == null) {
                    sleep(1000);
                    if (Code.gameAB == null) break;
                }

                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    sleep(1000);
                    continue;
                }

                // Dang chet -> doi TS goc hoi sinh
                if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                    wrongMapSince = 0;
                    sleep(500);
                    continue;
                }

                // === MAP WATCHDOG ===
                if (!isBossHuntingMode()) {
                    Auto currentAuto = Code.gameAB;
                    if (currentAuto != null && currentAuto.mapID >= 0
                            && TileMap.mapID != currentAuto.mapID) {
                        if (wrongMapSince == 0) {
                            wrongMapSince = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() - wrongMapSince > WRONG_MAP_TIMEOUT_MS) {
                            GameScr.gameAC("Ts Pro: Sai map! (" + TileMap.mapID + " -> " + currentAuto.mapID + ") Retry...");
                            try { GameCanvas.endDlg(); } catch (Exception e) {}
                            try { LockGame.gameBK(); } catch (Exception e) {}
                            try { TileMap.GoMap(currentAuto.mapID); } catch (Exception e) {}
                            wrongMapSince = System.currentTimeMillis();
                            sleep(2000);
                            continue;
                        }
                    } else {
                        wrongMapSince = 0;
                    }
                }

                // === AUTO BUFF ===
                long now = System.currentTimeMillis();
                if (now - lastBuffTime > BUFF_INTERVAL_MS) {
                    autoBuff(myChar);
                    lastBuffTime = now;
                }

                // === COLLECT MOBS TRONG RANGE ===
                MyVector mobs = collectMobsInRange(myChar);

                if (mobs.size() > 0) {
                    // Kill tracking
                    int currentMobCount = mobs.size();
                    long now2 = System.currentTimeMillis();
                    if (lastMobCount >= 0 && currentMobCount < lastMobCount) {
                        int dropped = lastMobCount - currentMobCount;
                        if (now2 - lastAttackTime < ATTACK_KILL_WINDOW_MS) {
                            totalKills += dropped;
                            sessionKills += dropped;
                            ThongKe.addKills(dropped);
                        }
                    }
                    lastMobCount = currentMobCount;

                    // Attack AoE
                    fireAttack(myChar, mobs);
                    lastAttackTime = System.currentTimeMillis();
                    sleep(ATTACK_DELAY_MS);
                } else {
                    lastMobCount = 0;
                    sleep(IDLE_DELAY_MS);
                }

            } catch (Exception e) {
                sleep(500);
            }
        }
        // Khoi phuc hieu ung skill khi thoat
        Code.timBG = false;
        isRunning = false;
    }

    // =============================================
    // ATTACK
    // =============================================

    /** Thu thap mob song trong MAX_ATTACK_RANGE. */
    private static MyVector collectMobsInRange(Char myChar) {
        reusableMobs.removeAllElements();
        try {
            int cx = myChar.cx;
            int cy = myChar.cy;
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
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
    // AUTO BUFF
    // =============================================

    /** Tu dong dung tat ca skill type 2 (buff/support). */
    private static void autoBuff(Char myChar) {
        try {
            if (myChar.vSkillFight == null) return;
            int size = myChar.vSkillFight.size();
            for (int i = 0; i < size; i++) {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                if (s.template.type == 2) {
                    Service.gI().gameAG(s.template.id);
                    Service.gI().gameAR();
                    sleep(50);
                }
            }
        } catch (Exception e) {}
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

            GameScr.gameAC("Up (" + formatTime(sec) + "): " + sessionKills + " Qu\u00e1i | Map: " + aliveMobs + " | Exp: +" + formatNumber(gainExp) + " | Y\u00ean: +" + formatNumber(gainYen) + " | Xu: +" + formatNumber(gainXu) + " | L\u01b0\u1ee3ng: +" + gainLuong);
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

    private static boolean isBossHuntingMode() {
        return AutoSanBoss.isRunning || AutoBossEvent.isEnabled;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
