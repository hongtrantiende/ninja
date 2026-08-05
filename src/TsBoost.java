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
    private static final int ATTACK_DELAY_MS = 50;    // Delay giua moi lan attack (50ms giam lag)
    private static final int IDLE_DELAY_MS = 200;      // Delay khi het quai
    private static final int BUFF_INTERVAL_MS = 15000; // Buff moi 15 giay
    private static final int NEARBY_RANGE = 150;       // Quai < 150px = gan
    private static final int GHOST_MOVE_DELAY_MS = 50;  // Delay sau ghost move
    private static final int ZONE_WAIT_MOB_MS = 3000;  // Doi 3s xem zone moi co quai khong
    private static final int MAX_ZONE_RETRIES = 3;     // Thu toi da 3 zone
    private static final int STUCK_TIMEOUT_MS = 10000; // 10 giay khong giet quai = reload zone
    private static final int SUICIDE_STUCK_TIMEOUT_MS = 30000; // 30 giay khong giet quai = tu sat ve lang
    private static final int STATS_INTERVAL_MS = 30000; // Hien stats moi 30 giay
    private static final int IDLE_NUDGE_MS = 5000;     // 5 giay dung im = nudge

    // === REUSABLE VECTORS (tranh tao moi moi frame = giam GC/lag) ===
    private static final MyVector reusableMobs = new MyVector();
    private static final MyVector reusableChars = new MyVector();

    // Luu vi tri ban dau de quay ve khi het quai
    private static int homeX = 0;
    private static int homeY = 0;
    private static long lastBuffTime = 0;

    // === FULL STATS TRACKING ===
    private static int totalKills = 0;
    private static int sessionKills = 0;
    private static int lastMobCount = -1;
    private static long lastMobChangeTime = 0;
    private static long statsStartTime = 0;
    private static long sessionStartTime = 0;
    private static long startExp = 0;
    private static int startYen = 0;
    private static int startXu = 0;
    private static int startLuong = 0;

    // === IDLE DETECTION ===
    private static int lastPosX = 0;
    private static int lastPosY = 0;
    private static long lastMoveTime = 0;

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

                // === KHI DANG SAN BOSS: chi attack, KHONG can thiep di chuyen ===
                boolean isBossHunting = AutoSanBoss.isRunning;

                if (mobs.size() > 0) {
                    int currentMobCount = mobs.size();
                    long now2 = System.currentTimeMillis();

                    // === KILL TRACKING: dem quai giet ===
                    if (lastMobCount >= 0 && currentMobCount < lastMobCount) {
                        int killed = (lastMobCount - currentMobCount);
                        totalKills += killed;
                        sessionKills += killed;
                        ThongKe.addKills(killed);
                        lastMobChangeTime = now2; // Cap nhat moc thoi gian DIET quai!
                    }
                    if (lastMobChangeTime == 0) {
                        lastMobChangeTime = now2;
                    }
                    lastMobCount = currentMobCount;

                    if (!isBossHunting) {
                        // === ANTI-STUCK: BO QUA khi dang danh boss / ak ===
                        boolean hasBoss = hasBossOnMap();

                        // === ANTI-STUCK LỚP 2 (30s): 30s khong DIỆT duoc quai -> Tu sat ve lang ===
                        if (!hasBoss
                                && lastMobChangeTime > 0
                                && now2 - lastMobChangeTime > SUICIDE_STUCK_TIMEOUT_MS
                                && currentMobCount > 0) {
                            GameScr.gameAC("Ts Pro: KẸT 30s (Diệt không tăng)! Tự sát về làng...");
                            suicideAndReturn();
                            lastMobChangeTime = now2;
                            lastMobCount = -1;
                            sleep(3000);
                            continue;
                        }

                        // === ANTI-STUCK LỚP 1 (10s): 10s khong DIỆT duoc quai -> Reload zone ===
                        if (!hasBoss
                                && lastMobChangeTime > 0
                                && now2 - lastMobChangeTime > STUCK_TIMEOUT_MS
                                && currentMobCount > 0) {
                            GameScr.gameAC("Ts Pro: KẸT 10s (Diệt không tăng)! Reload zone...");
                            reloadZone();
                            lastMobChangeTime = now2;
                            lastMobCount = -1;
                            sleep(2000);
                            continue;
                        }

                        // Removed Smart Zone
                    } // end !isBossHunting

                    // === STATS: Da hien thi qua 3 dong HUD goc man hinh ===
                    if (now2 - statsStartTime > STATS_INTERVAL_MS) {
                        statsStartTime = now2;
                    }

                    if (!isBossHunting) {
                        // === IDLE NUDGE: dung im 5s co quai => ghost move den quai gan ===
                        long now3 = System.currentTimeMillis();
                        if (myChar.cx != lastPosX || myChar.cy != lastPosY) {
                            lastPosX = myChar.cx;
                            lastPosY = myChar.cy;
                            lastMoveTime = now3;
                        } else if (now3 - lastMoveTime > IDLE_NUDGE_MS && currentMobCount > 0) {
                            Mob nearest = findNearestMob(myChar.cx, myChar.cy);
                            if (nearest != null) {
                                Char.gameAC(nearest.x, nearest.y);
                                myChar.cx = nearest.x;
                                myChar.cy = nearest.y;
                                lastMoveTime = now3;
                            }
                        }

                        // === GHOST MOVE: bay den cum quai xa neu het quai gan ===
                        int nearbyCount = countNearbyMobs(myChar.cx, myChar.cy);
                        if (nearbyCount == 0) {
                            Mob farMob = findFarMob(myChar.cx, myChar.cy);
                            if (farMob != null) {
                                Char.gameAC(farMob.x, farMob.y);
                                myChar.cx = farMob.x;
                                myChar.cy = farMob.y;
                                sleep(GHOST_MOVE_DELAY_MS);
                            }
                        }
                    } // end !isBossHunting

                    // === ATTACK: danh TAT CA quai voi skill AOE tot nhat ===
                    fireAttack(myChar, mobs);
                    sleep(ATTACK_DELAY_MS);
                } else {
                    // Het quai tren map
                    lastMobCount = 0;
                    lastMobChangeTime = System.currentTimeMillis();
                    
                    if (!isBossHunting && Char.ChuyenMapHetQuai && Code.gameAB != null) {
                        try { Code.gameAB.gameAM(); } catch (Exception e) {}
                        sleep(500);
                    } else {
                        // San boss hoac khong bat chuyen khu -> doi respawn
                        sleep(IDLE_DELAY_MS);
                    }
                }

            } catch (Exception e) {
                sleep(500);
            }
        }
        isRunning = false;
    }

    // =============================================
    // ANTI-STUCK — tu sat ve lang & reload zone
    // =============================================

    // =============================================
    // FULL STATS DISPLAY — Hien thi 5 chi so Up
    // =============================================

    /**
     * Hien thi bang thong ke day du 5 chi so (Quai, Exp, Yen, Xu, Luong).
     */
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

            String timeStr = formatTime(sec);
            String expStr = formatNumber(gainExp);
            String yenStr = formatNumber(gainYen);
            String xuStr = formatNumber(gainXu);
            int aliveMobs = collectAllAliveMobs().size();

            GameScr.gameAC("Up (" + timeStr + "): " + sessionKills + " Quái | Quái map: " + aliveMobs + " | Exp: +" + expStr + " | Yên: +" + yenStr + " | Xu: +" + xuStr + " | Lượng: +" + gainLuong);
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

    /**
     * Tu sat ve lang (giong nut Tu Sat / Ve Lang trong menu Auto).
     */
    private static void suicideAndReturn() {
        try {
            Service.gI().gameAK();
            TileMap.gameAF();
        } catch (Exception e) {}
    }

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

    /**
     * Smart Zone Switch: Thu chuyen khu, doi quai xuat hien.
     * Neu 3s khong co quai -> chuyen khu khac (thu toi da 5 lan).
     * Tranh spam cung 1 zone khong co quai.
     */
    private static void smartZoneSwitch() {
        for (int retry = 0; retry < MAX_ZONE_RETRIES && isRunning; retry++) {
            try {
                Code.gameAB.gameAM();
            } catch (Exception e) { return; }

            // Doi quai xuat hien trong ZONE_WAIT_MOB_MS
            long waitStart = System.currentTimeMillis();
            boolean hasMobs = false;
            while (System.currentTimeMillis() - waitStart < ZONE_WAIT_MOB_MS && isRunning) {
                sleep(200);
                MyVector mobs = collectAllAliveMobs();
                if (mobs.size() >= 5) {
                    hasMobs = true;
                    break;
                }
            }

            if (hasMobs) {
                GameScr.gameAC("Khu " + TileMap.zoneID + " có quái! Farm...");
                lastMobCount = -1;
                lastMobChangeTime = System.currentTimeMillis();
                return;
            }
            // Zone nay it quai -> thu zone tiep theo
            GameScr.gameAC("Khu " + TileMap.zoneID + " ít quái, thử khu khác...");
        }
        // Het 5 lan thu -> dung lai doi respawn
        lastMobCount = -1;
        lastMobChangeTime = System.currentTimeMillis();
    }

    /**
     * Kiem tra co boss tren map khong.
     * Dung Auto.gameAN (boss vector) + quet mob.isBoss / levelBoss.
     * Khi co boss -> bo qua anti-stuck de tranh tu sat/reload giua chung danh boss.
     */
    private static boolean hasBossOnMap() {
        try {
            // Check boss vector cua Auto
            if (Auto.gameAN != null && Auto.gameAN.size() > 0) return true;

            // Quet vMob xem co boss nao khong
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    if (mob.hp > 0 && (mob.isBoss || mob.levelBoss > 0)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }

    // =============================================
    // GHOST MOVE — bay den quai xa giong tsxa + hut VP
    // =============================================

    /**
     * Tim con quai song GAN nhat (dung cho idle nudge).
     */
    private static Mob findNearestMob(int cx, int cy) {
        Mob best = null;
        int bestDist = Integer.MAX_VALUE;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob == null || mob.hp <= 0 || mob.status == 0 || mob.status == 1) continue;
                int dist = Math.abs(cx - mob.x) + Math.abs(cy - mob.y);
                if (dist < bestDist) {
                    best = mob;
                    bestDist = dist;
                }
            }
        } catch (Exception e) {}
        return best;
    }

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
        reusableMobs.removeAllElements();
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    if (mob.hp > 0 && mob.status != 0 && mob.status != 1) {
                        reusableMobs.addElement(mob);
                    }
                }
            }
        } catch (Exception e) {}
        return reusableMobs;
    }

    /**
     * Chon skill AOE tot nhat (uu tien type 3 > 1 > 0) va danh 1 lan.
     * KHONG cycle qua tat ca skill moi frame — de server xu ly splash/lan tu nhien!
     */
    private static int cachedSkillId = -1;
    private static long lastSkillSelectTime = 0;
    private static final int SKILL_RESELECT_MS = 5000; // Chon lai skill moi 5 giay

    private static void fireAttack(Char myChar, MyVector mobs) {
        try {
            // Chon skill AOE tot nhat (chi select lai moi 5s, ko spam select)
            long now = System.currentTimeMillis();
            if (cachedSkillId < 0 || now - lastSkillSelectTime > SKILL_RESELECT_MS) {
                cachedSkillId = pickBestAoeSkill(myChar);
                if (cachedSkillId >= 0) {
                    Service.gI().gameAG(cachedSkillId);
                }
                lastSkillSelectTime = now;
            }

            // Gui 1 attack packet duy nhat — reuse vector tranh tao moi
            reusableChars.removeAllElements();
            if (cachedSkillId >= 0) {
                Service.gI().gameAA(mobs, reusableChars, 2); // Skill attack
            } else {
                Service.gI().gameAA(mobs, reusableChars, 1); // Danh thuong
            }
        } catch (Exception e) {}
    }

    /**
     * Tim skill attack co dien lan/AOE tot nhat.
     * Uu tien: type 3 (AOE) > type 1 (chieu) > type 0 (thuong)
     */
    private static int pickBestAoeSkill(Char myChar) {
        if (myChar.vSkillFight == null) return -1;

        Skill bestAoe = null;   // type 3
        Skill bestChieu = null; // type 1
        Skill bestNormal = null;// type 0

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

        // Uu tien AOE > Chieu > Thuong
        if (bestAoe != null) return bestAoe.template.id;
        if (bestChieu != null) return bestChieu.template.id;
        if (bestNormal != null) return bestNormal.template.id;
        return -1;
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
