/**
 * TsBoost — Ket hop ts goc + gb all + ghost move xa.
 *
 * Khi bat: chay song song voi ts thuong.
 * - Ts goc (Code.gameAB): xu ly target, di chuyen, heal, nhat do, hoi sinh
 * - TsBoost: spam attack TAT CA quai trong vMob (KHONG gioi han range)
 *   + multi-skill (cycle qua tat ca skill attack)
 *   + auto buff
 *   + ghost move den quai xa khi het quai gan (giong tsxa + hut VP xa)
 *
 * Lenh: tsp (bat/tat mode boost)
 * Khi mode ON, moi lan bat ts/ak se tu dong kich hoat TsBoost.
 * Khi tat ts/ak se tu dong tat TsBoost.
 */
public class TsBoost implements Runnable {
    public static boolean modeEnabled = true;   // Mac dinh ON, giong hut VP
    public static boolean isRunning = false;
    private static Thread thread;

    // === CONFIG ===
    private static final int ATTACK_DELAY_MS = 40;    // Delay giua moi lan attack
    private static final int IDLE_DELAY_MS = 300;      // Delay khi het quai
    private static final int BUFF_INTERVAL_MS = 15000; // Buff moi 15 giay
    private static final int NEARBY_RANGE = 150;       // Quai < 150px = gan
    private static final int GHOST_MOVE_DELAY_MS = 200; // Delay sau ghost move
    private static final int STUCK_TIMEOUT_MS = 20000; // 20 giay khong giet quai = stuck
    private static final int STATS_INTERVAL_MS = 30000; // Hien stats moi 30 giay

    // Luu vi tri ban dau de quay ve khi het quai
    private static int homeX = 0;
    private static int homeY = 0;
    private static long lastBuffTime = 0;

    // === KILL TRACKING ===
    private static int totalKills = 0;
    private static int lastMobCount = -1;
    private static long lastMobChangeTime = 0;
    private static long statsStartTime = 0;

    /** Toggle mode on/off. */
    public static void toggleMode() {
        modeEnabled = !modeEnabled;
        if (modeEnabled) {
            GameScr.gameAC("Ts Pro: ON! Danh ALL quai + ghost xa!");
            // Neu ts dang chay, bat luon
            if (Code.gameAB != null && !isRunning) {
                start();
            }
        } else {
            stop();
            GameScr.gameAC("Ts Pro: OFF");
        }
    }

    /** Bat boost thread. Goi khi ts/ak duoc bat va mode dang ON. */
    public static void start() {
        if (isRunning) return;
        if (!modeEnabled) return;

        Char myChar = Char.getMyChar();
        if (myChar != null) {
            homeX = myChar.cx;
            homeY = myChar.cy;
        }

        isRunning = true;
        lastBuffTime = 0;
        thread = new Thread(new TsBoost());
        thread.start();
    }

    /** Tat boost thread. */
    public static void stop() {
        isRunning = false;
        thread = null;
    }

    /** Hook: goi khi ts/ak bat. */
    public static void onTsStarted() {
        if (!modeEnabled) return;
        if (isRunning) return;
        if (Code.gameAB != null) {
            start();
        } else {
            // ts tao TanSat tre — doi TanSat xuat hien roi bat
            syncAfterTs();
        }
    }

    /**
     * Doi TanSat xuat hien (toi da 30 giay) roi bat TsBoost.
     * Giong AutoPickup.syncAfterAutoCommand().
     */
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

    public void run() {
        sleep(500);

        // Doi Code.gameAB xuat hien (ts tao TanSat tre)
        for (int wait = 0; wait < 20 && isRunning && Code.gameAB == null; wait++) {
            sleep(500);
        }

        // Reset tracking
        totalKills = 0;
        lastMobCount = -1;
        lastMobChangeTime = System.currentTimeMillis();
        statsStartTime = System.currentTimeMillis();

        while (isRunning) {
            try {
                // Ts goc da tat thi minh cung tat
                if (Code.gameAB == null) {
                    sleep(1000);
                    if (Code.gameAB == null) break;
                }

                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    sleep(1000);
                    continue;
                }

                // Dang chet thi doi ts goc hoi sinh
                if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                    sleep(500);
                    continue;
                }

                // Auto buff moi 15 giay
                long now = System.currentTimeMillis();
                if (now - lastBuffTime > BUFF_INTERVAL_MS) {
                    autoBuff(myChar);
                    lastBuffTime = now;
                }

                // Lay tat ca mob song
                MyVector mobs = collectAllAliveMobs();

                if (mobs.size() > 0) {
                    // === KILL TRACKING: dem quai giet ===
                    int currentMobCount = mobs.size();
                    long now2 = System.currentTimeMillis();

                    if (lastMobCount >= 0 && currentMobCount < lastMobCount) {
                        totalKills += (lastMobCount - currentMobCount);
                    }
                    if (currentMobCount != lastMobCount) {
                        lastMobCount = currentMobCount;
                        lastMobChangeTime = now2;
                    }

                    // === ANTI-STUCK: 20s quai khong giam → reload zone ===
                    if (lastMobChangeTime > 0
                            && now2 - lastMobChangeTime > STUCK_TIMEOUT_MS
                            && currentMobCount > 0) {
                        GameScr.gameAC("Ts Pro: KET " + (STUCK_TIMEOUT_MS/1000) + "s! Reload zone...");
                        reloadZone();
                        lastMobChangeTime = now2;
                        lastMobCount = -1;
                        sleep(2000);
                        continue;
                    }

                    // === STATS: hien thi moi 30 giay ===
                    if (now2 - statsStartTime > STATS_INTERVAL_MS) {
                        long sec = (now2 - statsStartTime) / 1000;
                        GameScr.gameAC("Ts Pro: " + totalKills + " kills / " + sec + "s");
                        statsStartTime = now2;
                        totalKills = 0;
                    }

                    // === GHOST MOVE: bay den cum quai xa neu het quai gan ===
                    int nearbyCount = countNearbyMobs(myChar.cx, myChar.cy);
                    if (nearbyCount == 0) {
                        Mob farMob = findFarMob(myChar.cx, myChar.cy);
                        if (farMob != null) {
                            // Ghost move toi quai xa
                            Char.gameAC(farMob.x, farMob.y);
                            myChar.cx = farMob.x;
                            myChar.cy = farMob.y;
                            sleep(GHOST_MOVE_DELAY_MS);
                        }
                    }

                    // === ATTACK: danh TAT CA quai (khong gioi han range) ===
                    fireAllSkills(myChar, mobs);
                    sleep(ATTACK_DELAY_MS);
                } else {
                    // Het quai tren map → quay ve home doi respawn
                    lastMobCount = 0;
                    lastMobChangeTime = System.currentTimeMillis();
                    if (Math.abs(myChar.cx - homeX) + Math.abs(myChar.cy - homeY) > NEARBY_RANGE) {
                        Char.gameAC(homeX, homeY);
                        myChar.cx = homeX;
                        myChar.cy = homeY;
                    }
                    sleep(IDLE_DELAY_MS);
                }

            } catch (Exception e) {
                sleep(500);
            }
        }
        isRunning = false;
    }

    // =============================================
    // ANTI-STUCK — reload zone khi ket
    // =============================================

    /**
     * Reload zone: chuyen khu de server load lai quai moi.
     * Giong nhu tu sat roi hoi sinh — force reload toan bo mob.
     */
    private static void reloadZone() {
        try {
            int zone = TileMap.zoneID;
            // Re-enter cung zone = force reload mob
            Service.gI().gameAA(zone, -1);
        } catch (Exception e) {}
    }

    // =============================================
    // GHOST MOVE — bay den quai xa giong tsxa + hut VP
    // =============================================

    /**
     * Tim con quai song xa nhat de ghost move den.
     */
    private static Mob findFarMob(int cx, int cy) {
        Mob best = null;
        int bestDist = 0;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0 || mob.status == 0 || mob.status == 1) continue;
                int dist = Math.abs(cx - mob.x) + Math.abs(cy - mob.y);
                if (dist > NEARBY_RANGE && dist > bestDist) {
                    best = mob;
                    bestDist = dist;
                }
            }
        } catch (Exception e) {}
        return best;
    }

    /**
     * Dem quai song gan vi tri hien tai.
     */
    private static int countNearbyMobs(int cx, int cy) {
        int count = 0;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0 || mob.status == 0 || mob.status == 1) continue;
                if (Math.abs(cx - mob.x) + Math.abs(cy - mob.y) <= NEARBY_RANGE) {
                    count++;
                }
            }
        } catch (Exception e) {}
        return count;
    }

    // =============================================
    // ATTACK — danh tat ca quai, multi-skill
    // =============================================

    /**
     * Thu thap TAT CA mob song trong GameScr.vMob.
     * Khong gioi han range — giong gb all.
     */
    private static MyVector collectAllAliveMobs() {
        MyVector mobs = new MyVector();
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    if (mob.hp > 0 && mob.status != 0 && mob.status != 1) {
                        mobs.addElement(mob);
                    }
                }
            }
        } catch (Exception e) {}
        return mobs;
    }

    /**
     * Gui attack voi TAT CA skill attack (type 0, 1, 3) — multi-skill.
     * Moi skill gui 1 packet attack voi tat ca mob.
     */
    private static void fireAllSkills(Char myChar, MyVector mobs) {
        if (myChar.vSkillFight == null) {
            // Khong co skill → danh thuong
            try {
                MyVector chars = new MyVector();
                Service.gI().gameAA(mobs, chars, 1);
            } catch (Exception e) {}
            return;
        }

        boolean firedAny = false;
        int skillCount = myChar.vSkillFight.size();

        for (int i = 0; i < skillCount; i++) {
            try {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                int type = s.template.type;

                // Chi fire skill attack (type 0=binh thuong, 1=chieu, 3=AOE)
                if (type != 0 && type != 1 && type != 3) continue;

                // Select skill + attack tat ca mob
                Service.gI().gameAG(s.template.id);
                MyVector chars = new MyVector();
                Service.gI().gameAA(mobs, chars, 2);

                firedAny = true;
                sleep(10);
            } catch (Exception e) {}
        }

        // Fallback: danh thuong neu khong fire duoc skill nao
        if (!firedAny) {
            try {
                MyVector chars = new MyVector();
                Service.gI().gameAA(mobs, chars, 1);
            } catch (Exception e) {}
        }
    }

    // =============================================
    // AUTO BUFF
    // =============================================

    /**
     * Auto buff — tu dong dung tat ca skill type 2 (buff/support).
     */
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

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
